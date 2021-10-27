<template>
  <div class="attachments-drawer">
    <exo-drawer
      ref="attachmentsAppDrawer"
      :confirm-close="newUploadedFilesInProgress"
      :confirm-close-labels="confirmAbortUploadLabels"
      class="attachmentsAppDrawer"
      right
      @closed="resetAttachmentsDrawer">
      <template slot="title">
        <div class="attachmentsDrawerHeader">
          <span>{{ $t('attachments.upload.document') }}</span>
        </div>
      </template>
      <template slot="content">
        <div class="attachmentsContent pt-0 pa-5">
          <div v-show="showSelectedAttachmentsFromOtherDriveInfo" class="alert alert-info attachmentsAlert">
            {{ $t('attachments.alert.sharing.attachedFrom') }}
            {{ selectedFromOtherDriveLabel }}
            <b v-show="fromAnotherSpacesAttachments">
              {{ fromAnotherSpacesAttachments }}
            </b>
            {{ $t('attachments.alert.sharing.availableFor') }} <b>{{ currentSpaceDisplayName }}</b> {{ $t('attachments.alert.sharing.members') }}
          </div>
          <attachment-create-document-input
            v-if="!entityType && ! entityId"
            :attachments="attachments"
            :max-files-count="maxFilesCount"
            :max-files-size="maxFileSize"
            :current-drive="currentDrive"
            :path-destination-folder="pathDestinationFolder" />
          <attachments-upload-input
            :attachments="attachments"
            :max-files-count="maxFilesCount"
            :max-files-size="maxFileSize"
            :current-drive="currentDrive"
            :path-destination-folder="pathDestinationFolder" />
          <attachments-select-from-drive v-if="entityId && entityType" />
          <attachments-uploaded-files
            :attachments="attachments"
            :new-uploaded-files="newUploadedFiles"
            :schema-folder="schemaFolder"
            :max-files-count="maxFilesCount"
            :current-space="currentSpace"
            :current-drive="currentDrive"
            :entity-id="entityId"
            :entity-type="entityType" />
        </div>
        <attachments-drive-explorer-drawer
          :is-cloud-enabled="isCloudDriveEnabled"
          :entity-id="entityId"
          :entity-type="entityType"
          :default-drive="defaultDrive"
          :default-folder="defaultFolder"
          :attached-files="attachments" />
        <div
          v-for="action in attachmentsComposerActions"
          :key="action.key"
          :class="`${action.appClass}Action`">
          <component
            :is="action.component.name"
            v-if="action.component"
            :ref="action.key"
            v-dynamic-events="action.component.events"
            v-bind="action.component.props ? action.component.props : {}" />
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
import {getAttachmentsComposerExtensions} from '../../../js/extension';

export default {
  props: {
    maxFilesCount: {
      type: Number,
      required: false,
      default: parseInt(`${eXo.env.portal.maxToUpload}`)
    },
    maxFileSize: {
      type: Number,
      required: false,
      default: parseInt(`${eXo.env.portal.maxFileSize}`)
    },
    showAttachmentsBackdrop: {
      type: Boolean,
      default: true
    },
    entityId: {
      type: String,
      default: ''
    },
    entityType: {
      type: String,
      default: ''
    },
    defaultDrive: {
      type: Object,
      default: () => null
    },
    defaultFolder: {
      type: String,
      default: ''
    },
    attachments: {
      type: Array,
      default: () => []
    },
    entityHasAttachments: {
      type: Boolean,
      default: false
    },
    currentSpace: {
      type: {},
      default: () => null
    },
  },
  data() {
    return {
      showDestinationFolder: false,
      message: '',
      uploadingCount: 0,
      maxProgress: 100,
      maxUploadInProgressCount: 2,
      uploadingFilesQueue: [],
      MESSAGES_DISPLAY_TIME: 5000,
      pathDestinationFolder: '',
      showDestinationPath: false,
      schemaFolder: [],
      cloudDriveConnecting: false,
      connectedDrive: {},
      isActivityStream: true,
      drivesInProgress: {},
      attachmentInfo: false,
      isCloudDriveEnabled: false,
      defaultDestinationFolderPath: '',
      defaultSchemaFolder: [],
      workspace: 'collaboration',
      dragAndDropEventListenerInitialized: false,
      currentDrive: {},
      uploadedFiles: [],
      attachmentsChanged: false,
      newUploadedFiles: []
    };
  },
  computed: {
    uploadFinished() {
      return this.attachments.length > 0 && this.attachments.every(file => !file.uploadId);
    },
    entityHasNewAttachments() {
      return this.uploadedFiles.length > 0;
    },
    newUploadedFilesAdded() {
      return this.newUploadedFiles && this.newUploadedFiles.some(file => file.uploadId);
    },
    filesUploadedSuccessLabel() {
      return this.entityType && this.entityId && this.$t('attachments.upload.success') || this.$t('documents.upload.success');
    },
    newUploadedFilesInProgress() {
      return this.newUploadedFiles && this.newUploadedFiles.some(file => file.uploadProgress < 100);
    },
    confirmAbortUploadLabels() {
      return {
        title: this.$t('attachment.cancel.upload'),
        message: this.$t('attachment.abort.upload'),
        ok: this.$t('attachments.yes'),
        cancel: this.$t('attachments.no'),
      };
    },
  },
  watch: {
    attachments: {
      deep: true,
      handler() {
        this.attachmentsChanged = true;
        this.displayMessageDestinationFolder = !this.attachments.some(val => val.uploadId != null && val.uploadId !== '');
        if (this.attachments.length === 0) {
          this.pathDestinationFolder = this.defaultDestinationFolderPath;
          this.schemaFolder = this.defaultSchemaFolder;
        }
      }
    },
    uploadFinished() {
      if (this.uploadFinished && this.uploadingCount === 0 && this.entityHasNewAttachments) {
        this.$root.$emit('entity-attachments-updated');
        document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
        this.displaySuccessMessage();
        this.$refs.attachmentsAppDrawer.endLoading();
      }
    }
  },
  created() {
    document.addEventListener('paste', this.onPaste, false);
    this.$root.$on('open-select-from-drives', () => {
      this.openSelectFromDrivesDrawer();
    });
    this.$root.$on('open-attachments-app-drawer', () => {
      this.attachmentsChanged = false;
      this.openAttachmentsAppDrawer();
    });
    this.$root.$on('attachments-default-folder-path-initialized', (defaultDestinationFolderPath, folderName) => {
      this.initDefaultDestinationFolderPath(defaultDestinationFolderPath, folderName);
    });
    this.$root.$on('remove-destination-for-file', (folderId) => {
      this.deleteDestinationPathForFile(folderId);
    });
    this.$root.$on('select-destination-path-for-all', (pathDestinationFolder, folderName, currentDrive) => {
      this.addDestinationFolderForAll(pathDestinationFolder, folderName, currentDrive);
    });
    this.$root.$on('link-new-added-attachments', () => {
      this.uploadAddedAttachments();
    });
    this.$root.$on('add-new-uploaded-file', file => {
      this.newUploadedFiles.push(file);
    });
    this.$root.$on('attachments-changed-from-drives', (selectedFromDrives, removedFilesFromDrive) => {
      this.manageFilesFromDrives(selectedFromDrives, removedFilesFromDrive);
    });

    this.$root.$on('add-destination-path-for-file', (movedFile, pathDestinationFolder, folder, currentDrive) => {
      this.moveFileToNewDestinationFile(movedFile, pathDestinationFolder, folder, currentDrive);
    });
    this.$root.$on('abort-uploading-new-file', this.abortUploadingNewFile);
    this.$root.$on('remove-attached-file', this.removeAttachedFile);
    this.$root.$on('start-loading-attachment-drawer', () => this.$refs.attachmentsAppDrawer.startLoading());
    this.$root.$on('end-loading-attachment-drawer', () => this.$refs.attachmentsAppDrawer.endLoading());
    this.$root.$on('add-new-created-document', this.addNewCreatedDocument);
    this.getCloudDriveStatus();
    document.addEventListener('extension-AttachmentsComposer-attachments-composer-action-updated', () => this.attachmentsComposerActions = getAttachmentsComposerExtensions());
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$refs.attachmentsAppDrawer.open();
    },
    closeAttachmentsAppDrawer() {
      this.$root.$emit('reset-attachments-upload-input');
      document.removeEventListener('paste', this.onPaste, false);
      this.$refs.attachmentsAppDrawer.close();
    },
    uploadAddedAttachments() {
      if (this.newUploadedFilesAdded) { //added new uploaded files
        this.$refs.attachmentsAppDrawer.startLoading();
        this.attachments.filter(file => file.uploadId).forEach(file => {
          this.queueUpload(file);
        });
      } else if (this.attachmentsChanged) { //updated from drives
        this.updateLinkedAttachmentsToEntity();
      }
    },
    queueUpload(file) {
      if (this.uploadingCount < this.maxUploadInProgressCount) {
        this.uploadFileToDestinationPath(file);
      } else {
        const index = this.uploadingFilesQueue.findIndex(f => f.uploadId === file.uploadId);
        if (index === -1) {
          this.uploadingFilesQueue.push(file);
        }
      }
    },
    processNextQueuedUpload: function () {
      if (this.uploadingFilesQueue.length > 0) {
        this.uploadFileToDestinationPath(this.uploadingFilesQueue.shift());
      }
    },
    uploadFileToDestinationPath: function (file) {
      if (file.uploadId) {
        this.uploadingCount++;
        this.$attachmentService.uploadAttachment(
          this.workspace,
          file.fileDrive.name,
          file.destinationFolder,
          eXo.env.portal.portalName,
          file.uploadId,
          file.title,
          eXo.env.portal.language,
          'keep',
          'save'
        ).then((uploadedFile) => {
          if (uploadedFile) {
            uploadedFile = this.$attachmentService.convertXmlToJson(uploadedFile);
            this.sendDocumentAnalytics(uploadedFile);
            this.addNewUploadedFileToAttachments(file, uploadedFile);
            if (this.entityType && this.entityId) {
              this.linkUploadedAttachmentToEntity(file);
            } else {
              file.uploadId = '';
              file.acl = uploadedFile.acl;
              this.uploadingCount--;
              this.processNextQueuedUpload();
            }
          }
        }).catch(() => {
          this.uploadingCount--;
          this.processNextQueuedUpload();
          this.$emit('uploadingCountChanged', this.uploadingCount);
          this.$root.$emit('attachments-notification-alert', {
            message: this.$t('attachments.upload.failed').replace('{0}', file.title),
            type: 'error',
          });
        });
      }
    },
    addDestinationFolderForAll(pathDestinationFolder, folder, currentDrive) {
      this.currentDrive = currentDrive;
      this.pathDestinationFolder = pathDestinationFolder;
      this.schemaFolder = folder.split('/');
      this.$root.$emit('add-destination-path-for-all', this.defaultDestinationFolderPath, this.pathDestinationFolder, this.currentDrive);
    },
    moveFileToNewDestinationFile(movedFile, pathDestinationFolder, folder, newDestinationPathDrive) {
      this.$attachmentService.moveAttachmentToNewPath(
        newDestinationPathDrive.name,
        pathDestinationFolder,
        movedFile.id,
        this.entityType,
        this.entityId).then((updatedMovedFile) => {
        this.$root.$emit('entity-attachments-updated');
        document.dispatchEvent(new CustomEvent('entity-attachments-updated'));

        const movedAttachmentIndex = this.newUploadedFiles.findIndex(file => file.id === movedFile.id);
        const movedAttachment = Object.assign({}, this.newUploadedFiles[movedAttachmentIndex]);
        movedAttachment.pathDestinationFolderForFile = folder;
        movedAttachment.fileDrive = newDestinationPathDrive;
        this.newUploadedFiles.splice(movedAttachmentIndex, 1, movedAttachment);

        const movedFileIndex = this.uploadedFiles.findIndex(file => file.id === movedFile.id);
        updatedMovedFile.drive = folder;
        updatedMovedFile.date = updatedMovedFile.created;
        this.uploadedFiles.splice(movedFileIndex, 1, updatedMovedFile);

      });
    },
    deleteDestinationPathForFile(folderId) {
      this.$attachmentService.moveAttachmentToNewPath(
        this.currentDrive.name,
        this.pathDestinationFolder,
        folderId,
        this.entityType,
        this.entityId).then(() => {
        this.newUploadedFiles.filter(file => file.id === folderId).map(file => {
          file.pathDestinationFolderForFile = '';
          file.fileDrive = this.currentDrive;
        });
      });
    },
    setCloudDriveProgress({progress}) {
      this.cloudDriveConnecting = progress ? true : false;
    },
    onPaste(event) {
      if (event.clipboardData && event.clipboardData.items) {
        const items = Array.from(event.clipboardData.items);
        const textItem = items.filter(item => ~item.type.indexOf('text'))[0] || null;
        const textItemType = textItem ? textItem.type : '';
        const imageItem = items.filter(item => ~item.type.indexOf('image'))[0] || null;
        if (imageItem) {
          const pastedFile = imageItem.getAsFile();
          this.getFilename(textItem, textItemType, name => {
            //needed to create a new file to be able to rename it.
            const myNewFile = new File([pastedFile], name, {type: pastedFile.type});
            this.$root.$emit('handle-pasted-files-from-clipboard',[myNewFile]);
          });
        }
      }
    },
    getFilename(textItem, textItemType, sendFileToServer) {
      let fileName;
      const thiss = this;
      if (textItem) {
        textItem.getAsString((htmlString) => {
          if (textItemType === 'text/plain') {
            fileName = decodeURI(htmlString).split('/').pop();
          } else if (textItemType === 'text/html') {
            const img = thiss.parseHTML(htmlString).querySelectorAll('img')[0];
            fileName = img.src.split('/').pop();
          }
          sendFileToServer(fileName);
        });
      } else {
        fileName = `image-${Date.now()}.png`;
        sendFileToServer(fileName);
      }
    },
    parseHTML(html) {
      const t = document.createElement('template');
      t.innerHTML = html;
      return t.content.cloneNode(true);
    },
    addCloudDrive(drive) {
      this.connectedDrive = drive;
    },
    changeCloudDriveProgress(drives) { // listen clouddrives 'updateDrivesInProgress' event
      this.drivesInProgress = drives; // update progress for connecting drive to display that drive is in connection
    },
    getCloudDriveStatus() {
      this.$attachmentService.isCloudDriveEnabled().then(data => {
        this.isCloudDriveEnabled = data.result === 'true';
      });
    },
    initDefaultDestinationFolderPath(defaultDestinationFolderPath, folderName) {
      this.defaultDestinationFolderPath = defaultDestinationFolderPath || '';
      if (folderName) {
        this.defaultSchemaFolder = [];
        const namesOfFolders = folderName.split('/');
        for (let i = 0; i < namesOfFolders.length; i++) {
          this.defaultSchemaFolder[i] = namesOfFolders[i];
        }
      }
      this.pathDestinationFolder = this.defaultDestinationFolderPath;
      this.schemaFolder = this.defaultSchemaFolder;
      this.currentDrive = this.defaultDrive;
    },
    resetAttachmentsDrawer() {
      this.abortUploadingFiles();
      this.newUploadedFiles = [];
      this.$refs.attachmentsAppDrawer.endLoading();

      //get the last 10 uploaded files to be sent within the custom event
      const lastUploadedFiles = this.uploadedFiles.sort((doc1, doc2) => doc2.date - doc1.date).slice(-10);
      document.dispatchEvent(new CustomEvent('attachments-upload-finished', {'detail': {'list': Object.values(lastUploadedFiles)}}));
      this.uploadedFiles = [];
      this.$root.$emit('hide-create-new-document-input');
    },
    linkUploadedAttachmentToEntity(file) {
      return this.$attachmentService.linkUploadedAttachmentToEntity(this.entityId, this.entityType, file.id).then((linkedAttachment) => {
        file.acl = linkedAttachment.acl;
        file.uploadId = '';
        this.uploadingCount--;
        this.processNextQueuedUpload();
      }).catch(e => {
        console.error(e);
        this.$refs.attachmentsAppDrawer.endLoading();
        this.$root.$emit('attachments-notification-alert', {
          message: this.$t('attachments.link.failed'),
          type: 'error',
        });
      });
    },
    updateLinkedAttachmentsToEntity() {
      const attachmentIds = this.attachments.filter(attachment => attachment.id).map(attachment => attachment.id);
      if (attachmentIds.length === 0) {
        return this.removeAllAttachmentsFromEntity(this.entityId, this.entityType);
      } else {
        return this.$attachmentService.updateLinkedAttachmentsToEntity(this.entityId, this.entityType, attachmentIds).then(() => {
          this.$root.$emit('entity-attachments-updated');
          document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
          this.displaySuccessMessage();
        }).catch(e => {
          console.error(e);
          this.$refs.attachmentsAppDrawer.endLoading();
          this.$root.$emit('attachments-notification-alert', {
            message: this.$t('attachments.link.failed'),
            type: 'error',
          });
        });
      }
    },
    removeAllAttachmentsFromEntity(entityId, entityType) {
      return this.$attachmentService.removeAllAttachmentsFromEntity(entityId, entityType).then(() => {
        this.$root.$emit('entity-attachments-updated');
        document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
        this.displaySuccessMessage();
      }).catch(e => {
        console.error(e);
        this.$refs.attachmentsAppDrawer.endLoading();
        this.$root.$emit('attachments-notification-alert', {
          message: this.$t('attachments.link.failed'),
          type: 'error',
        });
      });
    },
    displaySuccessMessage() {
      this.$root.$emit('attachments-notification-alert', {
        message: this.filesUploadedSuccessLabel,
        type: 'success',
      });
    },
    openSelectFromDrivesDrawer() {
      this.$root.$emit('open-select-from-drives-drawer');
    },
    addNewUploadedFileToAttachments(file, uploadedFile) {
      file.drive = file.fileDrive.title;
      file.id = uploadedFile.UUID;
      uploadedFile.drive = file.fileDrive.title;
      uploadedFile.id = uploadedFile.UUID;
      uploadedFile.size = file.size;
      uploadedFile.previewBreadcrumb = JSON.parse(uploadedFile.previewBreadcrumb);
      uploadedFile.acl = JSON.parse(uploadedFile.acl);
      this.uploadedFiles.push(uploadedFile);
    },
    abortUploadingFiles() {
      if (this.newUploadedFilesInProgress) {
        this.$root.$emit('abort-attachments-new-upload');
        this.newUploadedFiles.forEach(file => {
          if (file.uploadProgress < 100) {
            this.$uploadService.abortUpload(file.uploadId);
          } else {
            this.$uploadService.deleteUpload(file.uploadId);
          }
        });
      }
    },
    manageFilesFromDrives(selectedFromDrives, removedFilesFromDrive) {
      if (selectedFromDrives && selectedFromDrives.length || removedFilesFromDrive && removedFilesFromDrive.length) {
        this.attachmentsChanged = true;
        this.newUploadedFiles.push(...selectedFromDrives);
        this.uploadAddedAttachments();
      }
    },
    abortUploadingNewFile(file) {
      if (file && file.uploadId) {
        const fileIndex = this.newUploadedFiles.findIndex(f => f.uploadId === file.uploadId);
        this.newUploadedFiles.splice(fileIndex, 1);
      }
    },
    removeAttachedFile(file) {
      if (file && file.id) {
        const fileIndex = this.newUploadedFiles.findIndex(f => f.id === file.id);
        this.newUploadedFiles.splice(fileIndex, 1);
      }
    },
    addNewCreatedDocument(file) {
      if (file && file.id) {
        this.sendDocumentAnalytics(file);
        this.newUploadedFiles.push(file);
        this.uploadedFiles.push(file);
      }
    },
    sendDocumentAnalytics(file) {
      if (file && file.UUID || file.id) {
        const operationOrigin = this.entityType || eXo.env.portal.selectedNodeUri;
        const documentId = file.UUID || file.id;
        const fileExtension = file.title.split('.').pop();
        const fileAnalytics = {
          'module': 'Drive',
          'subModule': 'attachment-drawer',
          'parameters': {
            'documentId': documentId,
            'origin': operationOrigin.toLowerCase(),
            'documentSize': file.size,
            'documentName': file.title,
            'documentExtension': fileExtension
          },
          'userId': eXo.env.portal.userIdentityId,
          'spaceId': eXo.env.portal.spaceId,
          'userName': eXo.env.portal.userName,
          'operation': 'fileCreated',
          'timestamp': Date.now()
        };
        document.dispatchEvent(new CustomEvent('exo-statistic-message', {detail: fileAnalytics}));
      }
    }
  }
};
</script>