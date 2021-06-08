<template>
  <div class="attachments-drawer">
    <exo-drawer
      ref="attachmentsAppDrawer"
      class="attachmentsAppDrawer"
      right>
      <template slot="title">
        <div class="attachmentsDrawerHeader">
          <span>{{ drawerTitle }}</span>
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
          <attachments-upload-input
            :attachments="attachments"
            :max-files-count="maxFilesCount"
            :max-files-size="maxFileSize"
            :current-drive="currentDrive"
            :path-destination-folder="pathDestinationFolder" />
          <attachments-select-from-drive v-if="entityId && entityType" />
          <attachments-uploaded-files
            :attachments="attachments"
            :schema-folder="schemaFolder"
            :max-files-count="maxFilesCount"
            :current-space="currentSpace"
            :current-drive="currentDrive" />
        </div>
        <attachments-drive-explorer-drawer
          :is-cloud-enabled="isCloudDriveEnabled"
          :mode-folder-selection-for-file="modeFolderSelectionForFile"
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
            v-bind="action.component.props ? action.component.props : {}"
            :is="action.component.name"
            v-if="action.component"
            :ref="action.key"
            v-dynamic-events="action.component.events" />
        </div>
      </template>
      <template slot="footer">
        <div class="d-flex justify-end">
          <v-btn
            class="btn mr-3"
            @click="closeAttachmentsAppDrawer()">
            {{ $t('attachments.drawer.cancel') }}
          </v-btn>
          <v-btn
            :disabled="!attachmentsChanged || attachments.length === 0"
            class="btn btn-primary"
            @click="uploadAddedAttachments()">
            {{ $t('attachments.upload') }}
          </v-btn>
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
      drawerTitle: `${this.$t('attachments.upload.document')}`,
      pathDestinationFolder: '',
      showDestinationPath: false,
      schemaFolder: [],
      destinationFileName: '',
      modeFolderSelectionForFile: false,
      cloudDriveConnecting: false,
      connectedDrive: {},
      isActivityStream: true,
      drivesInProgress: {},
      attachmentInfo: false,
      isCloudDriveEnabled: false,
      defaultDestinationFolderPath: '',
      defaultSchemaFolder: [],
      workspace: 'collaboration',
      uploadMode: '',
      dragAndDropEventListenerInitialized: false,
      currentDrive: {},
      uploadedFiles: [],
      attachmentsChanged: false,
    };
  },
  computed: {
    uploadFinished() {
      return this.attachments.some(file => !file.uploadId);
    },
    entityHasNewAttachments() {
      return this.attachments.some(attachment => attachment.uploadId);
    },
    filesUploadedSuccessLabel() {
      return this.entityType && this.entityId && this.$t('attachments.upload.success') || this.$t('documents.upload.success');
    }
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
    uploadingCount(newValue) {
      if (this.uploadMode === 'save' && newValue === 0) {
        if (this.uploadFinished) {
          if (this.entityId && this.entityType) {
            if (this.entityHasAttachments) {
              this.updateLinkedAttachmentsToEntity().then(() => {
                this.closeAndResetAttachmentsDrawer();
              });
            } else {
              this.linkUploadedAttachmentsToEntity().then(() => {
                this.closeAndResetAttachmentsDrawer();
              });
            }
          } else {
            this.closeAndResetAttachmentsDrawer();
          }
        }
      }
    }
  },
  created() {
    document.addEventListener('paste', this.onPaste, false);
    this.$root.$on('open-select-from-drives', () => {
      this.openSelectFromDrivesDrawer();
    });this.$root.$on('change-attachment-destination-path', attachment => {
      this.openSelectDestinationFolderForFile(attachment);
    });
    this.$root.$on('open-attachments-app-drawer', () => {
      this.attachmentsChanged = false;
      this.openAttachmentsAppDrawer();
    });
    this.$root.$on('attachments-default-folder-path-initialized', (defaultDestinationFolderPath, folderName) => {
      this.initDefaultDestinationFolderPath(defaultDestinationFolderPath, folderName);
    });
    this.$root.$on('remove-destination-for-file', (folderName) => {
      this.deleteDestinationPathForFile(folderName);
    });
    this.$root.$on('select-destination-path-for-file', (pathDestinationFolder, folder, isPublic, currentDrive) => {
      this.addDestinationFolderForFile(pathDestinationFolder, folder, isPublic, currentDrive);
    });
    this.$root.$on('select-destination-path-for-all', (pathDestinationFolder, folderName, currentDrive) => {
      this.addDestinationFolderForAll(pathDestinationFolder, folderName, currentDrive);
    });
    this.getCloudDriveStatus();
    document.addEventListener('extension-AttachmentsComposer-attachments-composer-action-updated', () => this.attachmentsComposerActions = getAttachmentsComposerExtensions());
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$refs.attachmentsAppDrawer.open();
    },
    closeAttachmentsAppDrawer() {
      document.removeEventListener('paste', this.onPaste, false);
      this.$refs.attachmentsAppDrawer.close();
    },
    uploadFileToDestinationPath: function (file) {
      this.uploadingCount++;
      this.$refs.attachmentsAppDrawer.startLoading();
      this.$attachmentService.uploadAttachment(
        this.workspace,
        file.fileDrive.name,
        file.destinationFolder,
        eXo.env.portal.portalName,
        file.uploadId,
        file.name,
        eXo.env.portal.language,
        'keep',
        'save'
      ).then((uploadedFile) => {
        if (uploadedFile) {
          this.addNewUploadedFileToAttachments(file, uploadedFile);
          this.uploadingCount--;
        }
        this.$refs.attachmentsAppDrawer.endLoading();
        this.processNextQueuedUpload();
      }).catch(() => {
        this.uploadingCount--;
        this.$refs.attachmentsAppDrawer.endLoading();
        this.$emit('uploadingCountChanged', this.uploadingCount);
        this.$root.$emit('attachments-notification-alert', {
          message: this.$t('attachments.upload.failed').replace('{0}', file.name),
          type: 'error',
        });
      });
    },
    addDestinationFolderForAll(pathDestinationFolder, folder, currentDrive) {
      this.currentDrive = currentDrive;
      this.pathDestinationFolder = pathDestinationFolder;
      this.schemaFolder = folder.split('/');
      this.$root.$emit('add-destination-path-for-all', this.defaultDestinationFolderPath, this.pathDestinationFolder, this.currentDrive);
    },
    addDestinationFolderForFile(pathDestinationFolder, folder, isPublic, currentDrive) {
      this.$root.$emit('add-destination-path-for-file', this.destinationFileName, pathDestinationFolder, folder, isPublic, currentDrive);
      this.modeFolderSelectionForFile = false;
    },
    deleteDestinationPathForFile(folderName) {
      this.$root.$emit('remove-destination-path-for-file', folderName, this.currentDrive, this.pathDestinationFolder);
    },
    openSelectDestinationFolderForFile(file) {
      this.modeFolderSelectionForFile = true;
      this.destinationFileName = file.name;
      this.$root.$emit('open-drive-explorer-drawer');
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
    uploadAddedAttachments() {
      this.uploadMode = 'save';
      this.$refs.attachmentsAppDrawer.startLoading();
      if (this.entityHasNewAttachments) { //added new uploaded files
        this.attachments.filter(attachment => attachment.uploadId).forEach(file => {
          this.queueUpload(file);
        });
      } else if (this.attachmentsChanged) { //updated from drives
        this.updateLinkedAttachmentsToEntity().then(() => this.closeAndResetAttachmentsDrawer());
      }
    },
    queueUpload(file) {
      if (this.uploadingCount < this.maxUploadInProgressCount) {
        this.uploadFileToDestinationPath(file);
      } else {
        this.uploadingFilesQueue.push(file);
      }
    },
    processNextQueuedUpload: function () {
      if (this.uploadingFilesQueue.length > 0) {
        this.uploadFileToDestinationPath(this.uploadingFilesQueue.shift());
      }
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
    closeAndResetAttachmentsDrawer() {
      this.closeAttachmentsAppDrawer();
      this.$root.$emit('attachments-notification-alert', {
        message: this.filesUploadedSuccessLabel,
        type: 'success',
      });
      this.$refs.attachmentsAppDrawer.endLoading();

      //get the last 10 uploaded files to be sent within the custom event
      const lastUploadedFiles = this.uploadedFiles.sort((doc1, doc2) => doc2.date - doc1.date).slice(-10);
      document.dispatchEvent(new CustomEvent('attachments-upload-finished', {'detail': {'list': Object.values(lastUploadedFiles)}}));
      this.uploadedFiles = [];
    },
    linkUploadedAttachmentsToEntity() {
      this.$refs.attachmentsAppDrawer.startLoading();
      const attachmentIds = this.attachments.map(attachment => attachment.id);
      return this.$attachmentService.linkUploadedAttachmentsToEntity(this.entityId, this.entityType, attachmentIds).then(() => {
        this.$root.$emit('entity-attachments-updated');
        document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
      }).finally(() => {
        this.$refs.attachmentsAppDrawer.endLoading();
      });
    },
    updateLinkedAttachmentsToEntity() {
      this.$refs.attachmentsAppDrawer.startLoading();
      const attachmentIds = this.attachments.map(attachment => attachment.id);
      return this.$attachmentService.updateLinkedAttachmentsToEntity(this.entityId, this.entityType, attachmentIds).then(() => {
        this.$root.$emit('entity-attachments-updated');
        document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
      }).finally(() => {
        this.$refs.attachmentsAppDrawer.endLoading();
      });
    },
    openSelectFromDrivesDrawer() {
      this.$root.$emit('open-select-from-drives-drawer');
    },
    addNewUploadedFileToAttachments(file, uploadedFile) {
      uploadedFile = this.$attachmentService.convertXmlToJson(uploadedFile);
      file.drive = file.fileDrive.title;
      file.id = uploadedFile.UUID;
      file.uploadId = '';

      uploadedFile.drive = file.fileDrive.title;
      uploadedFile.id = uploadedFile.UUID;
      this.uploadedFiles.push(uploadedFile);
    },
  }
};
</script>