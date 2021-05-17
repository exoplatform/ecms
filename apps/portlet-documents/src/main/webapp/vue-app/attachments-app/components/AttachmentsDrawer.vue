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
          <attachments-upload-input
            :attachments="attachments"
            :max-files-count="maxFilesCount"
            :max-files-size="maxFileSize"
            :current-drive="currentDrive"
            :path-destination-folder="pathDestinationFolder" />
          <attachments-uploaded-files
            :attachments="attachments"
            :schema-folder="schemaFolder" />

          <!-- Select From Drives Disabled for the moment -->
          <!--<div class="d-flex align-center">
            <v-subheader class="text-sub-title pl-0 d-flex">Platform Documents</v-subheader>
            <v-divider></v-divider>
          </div>
          <div class="lastContent d-flex align-center justify-center">
            <a title="Select on server" class="uploadButton d-flex align-center" href="#" rel="tooltip" data-placement="bottom"
               @click="toggleServerFileSelector()">
              <i class="uiIcon32x32FolderDefault uiIcon32x32LightGray"></i>
              <v-icon color="#fff" x-small class="iconCloud">cloud</v-icon>
              <span class="text colorText">{{ $t('attachments.drawer.existingUploads') }}</span>
            </a>
          </div>-->
        </div>
        <attachments-drive-explorer-drawer
          :is-cloud-enabled="isCloudDriveEnabled"
          :mode-folder-selection="true"
          :mode-folder-selection-for-file="modeFolderSelectionForFile"
          :entity-id="entityId"
          :entity-type="entityType"
          :default-drive="defaultDrive"
          :default-folder="defaultFolder"
          @cancel="toggleServerFileSelector()" />
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
        <div class="d-flex align-center justify-space-between">
          <div class="limitMessage d-flex align-center grey--text">
            <i class="uiIconWarning my-auto pr-2 grey--text"></i>
            <div class="d-flex flex-column caption align-start warningMessages">
              <span class="sizeLimit">{{ $t('attachments.drawer.maxFileSize').replace('{0}', maxFileSize) }}</span>
              <span class="countLimit">{{ $t('attachments.drawer.maxFileCount').replace('{0}', maxFilesCount) }}</span>
            </div>
          </div>
          <div class="attachmentDrawerButtons d-flex">
            <v-btn
              class="btn mr-3"
              @click="closeAttachmentsAppDrawer()">
              {{ $t('attachments.drawer.cancel') }}
            </v-btn>
            <v-btn
              :disabled="!attachments.length && !attachmentsChanged"
              class="btn btn-primary"
              @click="uploadAddedAttachments()">
              {{ $t('attachments.upload') }}
            </v-btn>
          </div>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
import {getAttachmentsComposerExtensions} from '../js/extension';

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
      showAttachmentsDrawer: false,
      cloudDriveConnecting: false,
      connectedDrive: {},
      privateFilesAttached: false,
      isActivityStream: true,
      fromAnotherSpaces: '',
      spaceGroupId: '',
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
    entityHasAttachments() {
      return this.attachments.some(attachment => attachment.id);
    },
    entityHasNewAttachments() {
      return this.attachments.some(attachment => attachment.uploadId);
    }
  },
  watch: {
    attachmentInfo: function () {
      if (this.attachmentInfo) {
        setTimeout(() => this.attachmentInfo = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    attachments: {
      deep: true,
      handler() {
        this.$emit('attachmentsChanged', this.attachments);
        this.attachmentsChanged = true;
        this.displayMessageDestinationFolder = !this.attachments.some(val => val.uploadId != null && val.uploadId !== '');
        if (this.attachments.length === 0) {
          this.pathDestinationFolder = this.defaultDestinationFolderPath;
          this.schemaFolder = this.defaultSchemaFolder;
        }
        this.privateFilesAttached = this.attachments.some(file => file.isPublic === false);
        this.fromAnotherSpaces = this.attachments.filter(({space}) => space && space.name !== this.groupId)
          .map(({space}) => space.title).filter((value, i, self) => self.indexOf(value) === i).join(',');
      }
    },
    uploadingCount(newValue) {
      if (this.uploadMode === 'save' && newValue === 0) {
        if (this.uploadFinished) {
          if (this.entityId && this.entityType) {
            this.linkUploadedAttachmentsToEntity().then(() => {
              this.closeAndResetAttachmentsDrawer();
            }).catch(() => {
              this.$refs.attachmentsAppDrawer.endLoading();
            });
          } else {
            this.closeAndResetAttachmentsDrawer();
          }
        } else {
          this.$refs.attachmentsAppDrawer.endLoading();
        }
      }
    }
  },
  created() {
    document.addEventListener('paste', this.onPaste, false);
    this.$root.$on('change-attachment-destination-path', attachment => {
      this.openSelectDestinationFolderForFile(attachment);
    });
    this.$root.$on('open-attachments-app-drawer', () => {
      this.openAttachmentsAppDrawer();
    });
    this.$root.$on('attachments-default-folder-path-initialized', (defaultDestinationFolderPath, folderName) => {
      this.initDefaultDestinationFolderPath(defaultDestinationFolderPath, folderName);
    });
    this.$root.$on('remove-destination-for-file', folderName => {
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
      this.$emit('uploadingCountChanged', this.uploadingCount);
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
          uploadedFile = this.$attachmentService.convertXmlToJson(uploadedFile);
          uploadedFile.drive = file.fileDrive.title;
          this.uploadedFiles.push(uploadedFile);
          file.uploadId = '';
          this.uploadingCount--;
          this.$emit('uploadingCountChanged', this.uploadingCount);
        }
        this.processNextQueuedUpload();
      }).catch(() => {
        this.uploadingCount--;
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
      for (let i = 0; i < this.attachments.length; i++) {
        if (!this.attachments[i].destinationFolder || this.attachments[i].destinationFolder === this.defaultDestinationFolderPath) {
          this.attachments[i].destinationFolder = this.pathDestinationFolder;
          this.attachments[i].fileDrive = this.currentDrive;
        }
      }
    },
    addDestinationFolderForFile(pathDestinationFolder, folder, isPublic, currentDrive) {
      for (let i = 0; i < this.attachments.length; i++) {
        if (this.attachments[i].name === this.destinationFileName) {
          this.attachments[i].pathDestinationFolderForFile = folder;
          this.attachments[i].destinationFolder = pathDestinationFolder.startsWith('/') ? pathDestinationFolder.substring(1) : pathDestinationFolder;
          this.attachments[i].fileDrive = currentDrive;
          // TODO: get 'isPublic' property of file from rest, now 'isPublic' assigned to 'isPublic' property of destination folder
          this.attachments[i].isPublic = isPublic;
        }
      }
      this.modeFolderSelectionForFile = false;
    },
    toggleServerFileSelector(selectedFiles) {
      if (selectedFiles) {
        this.attachments = selectedFiles;
        this.attachmentInfo = true;
        this.$emit('input', this.attachments);
        this.$emit('attachmentsChanged', this.attachments);
      }
    },
    openSelectDestinationFolderForFile(file) {
      this.modeFolderSelectionForFile = true;
      this.destinationFileName = file.name;
      this.$root.$emit('open-drive-explorer-drawer');
    },
    deleteDestinationPathForFile(fileName) {
      for (let i = 0; i < this.attachments.length; i++) {
        if (this.attachments[i].name === fileName) {
          this.attachments[i].pathDestinationFolderForFile = '';
          this.attachments[i].fileDrive = this.currentDrive;
          this.attachments[i].destinationFolder = this.pathDestinationFolder;
          this.attachments[i].isPublic = true;
          break;
        }
      }
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
      if (this.entityHasNewAttachments) {
        this.attachments.filter(attachment => attachment.uploadId).forEach(file => {
          this.queueUpload(file);
        });
      } else if (this.attachmentsChanged){
        this.closeAndResetAttachmentsDrawer();
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
        message: this.$t('attachments.upload.success'),
        type: 'success',
      });
      this.attachments = this.entityType && this.entityId ? this.attachments : [];
      this.$refs.attachmentsAppDrawer.endLoading();
      document.dispatchEvent(new CustomEvent('attachments-upload-finished', {'detail': {'list': Object.values(this.uploadedFiles)}}));
      this.uploadedFiles = [];
    },
    linkUploadedAttachmentsToEntity() {
      const attachmentIds = this.uploadedFiles.map(attachment => attachment.UUID);
      return this.$attachmentService.linkUploadedAttachmentsToEntity(this.entityId, this.entityType, attachmentIds).then(() => {
        this.$root.$emit('entity-attachments-updated');
      });
    }
  }
};
</script>