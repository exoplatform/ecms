<template>
  <div class="attachments-drawer">
    <exo-drawer
      ref="attachmentsAppDrawer"
      class="attachmentsAppDrawer"
      right>
      <template slot="title">
        <div class="attachmentsDrawerHeader">
          <v-btn
            v-if="entityId && entityType || showDocumentSelector"
            class="backButton"
            icon
            small
            dark
            outlined
            @click="showDocumentSelector ? toggleServerFileSelector() : closeAttachmentsAppDrawer()">
            <i class="uiIconBack"></i>
          </v-btn>
          <span>{{ drawerTitle }}</span>
        </div>
      </template>
      <template slot="content">
        <div v-show="!showDocumentSelector" class="attachmentsContent">
          <div class="d-flex align-center">
            <v-subheader class="text-sub-title pl-0 d-flex">{{ $t('attachments.upload') }}</v-subheader>
            <v-divider></v-divider>
          </div>
          <div class="multiploadFilesSelector">
            <div id="DropFileBox" ref="dropFileBox" class="dropFileBox theme--light" aria-controls @click="uploadFile">
              <i class="uiIconEcmsUploadVersion uiIcon32x32"></i>
              <v-subheader class="text-sub-title ml-3 d-none d-sm-flex" href="#" rel="tooltip" data-placement="bottom">
                {{ $t('attachments.drawer.uploadOrdrop') }}
              </v-subheader>
            </div>
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
            <div class="fileHidden d-none">
              <input ref="uploadInput" class="file" name="file" type="file" multiple="multiple" style="display:none"
                     @change="handleFileUpload($refs.uploadInput.files)">
            </div>
            <transition name="fade">
              <div v-show="fileSizeLimitError" class="sizeExceeded alert alert-error">
                <i class="uiIconError"></i>
                {{ $t('attachments.drawer.maxFileSize.error').replace('{0}', maxFileSize) }}
              </div>
            </transition>
            <transition name="fade">
              <div v-show="filesCountLimitError" class="countExceeded alert alert-error">
                <i class="uiIconError"></i>
                {{ $t('attachments.drawer.maxFileCount.error').replace('{0}', maxFilesCount) }}
              </div>
            </transition>
            <transition name="fade">
              <div v-show="sameFileError" class="sameFile alert alert-error">
                <i class="uiIconError"></i>
                {{ sameFileErrorMessage }}
              </div>
            </transition>
          </div>

          <div class="uploadedFiles">
            <div class="attachments-list d-flex align-center">
              <v-subheader class="text-sub-title pl-0 d-flex">{{ $t('attachments.drawer.title') }} ({{ value.length }})
              </v-subheader>
              <v-divider></v-divider>
            </div>
            <div v-if="value.length === 0" class="no-files-attached d-flex flex-column align-center text-sub-title">
              <div class="d-flex pl-6 not-files-icon">
                <i class="uiIconAttach uiIcon64x64"></i>
                <i class="uiIconCloseCircled uiIcon32x32"></i>
              </div>
              <span>{{ $t('no.attachments') }}</span>
            </div>
            <div v-if="value.length > 0" class="destinationFolder mb-4 ml-5">
              <div :title="schemaFolder[0]" class="drive" rel="tooltip" data-placement="top">{{ schemaFolder[0] }}
              </div>
              <div v-if="showDestinationPath && !displayMessageDestinationFolder" class="folderLocation">
                <div v-if="schemaFolder.length > 1" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[1]" :class="schemaFolder.length === 2 ? 'active' : '' " class="folderName"
                       rel="tooltip" data-placement="top">{{ schemaFolder[1] }}
                  </div>
                </div>
                <div v-if="schemaFolder.length === 3" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[2]"
                       :class="schemaFolder[2] !== 'Activity Stream Documents' ? 'path' : 'active' " class="folderName"
                       rel="tooltip" data-placement="top">{{ schemaFolder[2] }}
                  </div>
                </div>
                <div v-if="schemaFolder.length > 3" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[2]" class="folderName" rel="tooltip" data-placement="top">...</div>
                </div>
                <div v-for="folder in schemaFolder.slice(schemaFolder.length-1,schemaFolder.length)"
                     v-show="schemaFolder.length > 3" :key="folder" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="folder"
                       :class="schemaFolder[schemaFolder.length - 1] === folder && schemaFolder[schemaFolder.length - 1].length < 11 ?'active' : 'path'"
                       class="folderName" rel="tooltip" data-placement="top">{{ folder }}
                  </div>
                </div>
              </div>
              <div v-if="displayMessageDestinationFolder" class="messageDestination">
                <p :title="$t('attachments.drawer.destination.attachment.message')" rel="tooltip" data-placement="top">
                  {{ $t('attachments.drawer.destination.attachment.message') }}</p>
              </div>
              <button :disabled="displayMessageDestinationFolder" class="buttonSelect d-flex pl-1 flex-column-reverse"
                      @click="toggleSelectDestinationFolder()">
                <i
                  :title="!displayMessageDestinationFolder ? $t('attachments.drawer.destination.attachment') : $t('attachments.drawer.destination.attachment.access') "
                  :class="displayMessageDestinationFolder ? 'disabled' : ''" class="uiIconFolder " rel="tooltip"
                  data-placement="top"></i>
              </button>
            </div>
            <div class="uploadedFilesItems ml-5">
              <transition-group name="list-complete" tag="div" class="d-flex flex-column">
                <span
                  v-for="attachedFile in value"
                  :key="attachedFile"
                  class="list-complete-item"
                >
                  <attachment-item :file="attachedFile"></attachment-item>
                </span>
              </transition-group>
            </div>
          </div>
        </div>
        <attachments-folders-files-selector v-show="showDocumentSelector"
                                            :is-cloud-enabled="isCloudDriveEnabled"
                                            :mode-folder-selection="true"
                                            :mode-folder-selection-for-file="modeFolderSelectionForFile"
                                            :entity-id="entityId"
                                            :entity-type="entityType"
                                            :default-drive="defaultDrive"
                                            :default-folder="defaultFolder"
                                            @cancel="toggleServerFileSelector()"></attachments-folders-files-selector>
        <div v-for="action in attachmentsComposerActions" :key="action.key" :class="`${action.appClass}Action`">
          <component v-dynamic-events="action.component.events" v-if="action.component"
                     v-bind="action.component.props ? action.component.props : {}"
                     :is="action.component.name" :ref="action.key"></component>
        </div>
      </template>
      <template v-if="!showDocumentSelector" slot="footer">
        <div class="d-flex align-center justify-space-between">
          <div class="limitMessage d-flex align-center grey--text">
            <i class="uiIconWarning my-auto pr-2 grey--text"></i>
            <div class="d-flex flex-column caption align-start warningMessages">
              <span class="sizeLimit">{{ $t('attachments.drawer.maxFileSize').replace('{0}', maxFileSize) }}</span>
              <span class="countLimit">{{ $t('attachments.drawer.maxFileCount').replace('{0}', maxFilesCount) }}</span>
            </div>
          </div>
          <div class="attachmentDrawerButtons d-flex">
            <v-btn class="btn mr-3"
                   @click="closeAttachmentsAppDrawer()">{{ $t('attachments.drawer.cancel') }}
            </v-btn>
            <v-btn :disabled="!value.length"
                   class="btn btn-primary"
                   @click="uploadAddedAttachments()">{{ $t('attachments.upload') }}
            </v-btn>
          </div>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
import axios from 'axios';
import {getAttachmentsComposerExtensions} from '../js/extension';

export default {
  props: {
    value: {
      type: Array,
      default: () => []
    },
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
  },
  data() {
    return {
      showDestinationFolder: false,
      message: '',
      uploadingFilesQueue: [],
      uploadingCount: 0,
      maxUploadInProgressCount: 2,
      maxProgress: 100,
      showDocumentSelector: false,
      fileSizeLimitError: false,
      filesCountLimitError: false,
      sameFileError: false,
      sameFileErrorMessage: `${this.$t('attachments.drawer.sameFile.error')}`,
      BYTES_IN_MB: 1048576,
      MESSAGES_DISPLAY_TIME: 5000,
      drawerTitle: `${this.$t('attachments.upload.document')}`,
      pathDestinationFolder: '',
      showDestinationPath: false,
      schemaFolder: [],
      destinationFileName: '',
      showDestinationFolderForFile: false,
      modeFolderSelectionForFile: false,
      showAttachmentsDrawer: false,
      displayMessageDestinationFolder: true,
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
    };
  },
  computed: {
    uploadFinished() {
      return this.value.some(file => !file.uploadId);
    }
  },
  watch: {
    fileSizeLimitError: function () {
      if (this.fileSizeLimitError) {
        setTimeout(() => this.fileSizeLimitError = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    filesCountLimitError: function () {
      if (this.filesCountLimitError) {
        setTimeout(() => this.filesCountLimitError = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    sameFileError: function () {
      if (this.sameFileError) {
        setTimeout(() => this.sameFileError = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    attachmentInfo: function () {
      if (this.attachmentInfo) {
        setTimeout(() => this.attachmentInfo = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    value: {
      deep: true,
      handler() {
        this.$emit('attachmentsChanged', this.value);
        this.displayMessageDestinationFolder = !this.value.some(val => val.uploadId != null && val.uploadId !== '');
        if (this.value.length === 0) {
          this.pathDestinationFolder = this.defaultDestinationFolderPath;
          this.schemaFolder = this.defaultSchemaFolder;
          this.displayMessageDestinationFolder = true;
          this.showDestinationPath = true;
        }
        this.privateFilesAttached = this.value.some(file => file.isPublic === false);
        this.fromAnotherSpaces = this.value.filter(({space}) => space && space.name !== this.groupId)
          .map(({space}) => space.title).filter((value, i, self) => self.indexOf(value) === i).join(',');
      }
    },
    uploadingCount(newValue) {
      if (this.uploadMode === 'save' && newValue === 0) {
        if (this.uploadFinished) {
          this.closeAndResetAttachmentsDrawer();
        } else {
          this.$refs.attachmentsAppDrawer.endLoading();
        }
      }
    }
  },
  created() {
    this.$root.$on('remove-attachment-item', attachment => {
      this.removeAttachedFile(attachment);
    });
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
      this.$nextTick(() => {
        if(!this.dragAndDropEventListenerInitialized){
          ['drag', 'dragstart', 'dragend', 'dragover', 'dragenter', 'dragleave', 'drop'].forEach(function (evt) {

            /*
              For each event add an event listener that prevents the default action
              (opening the file in the browser) and stop the propagation of the event (so
              no other elements open the file in the browser)
            */
            document.addEventListener(evt, function (e) {
              e.preventDefault();
              e.stopPropagation();
            }.bind(this), false);
          }.bind(this));

          document.addEventListener('dragover', function () {
            this.$refs.dropFileBox.classList.add('dragStart');
          }.bind(this));

          /*
            Capture the files from the drop event and add them to our local files
            array.
          */
          this.$refs.dropFileBox.addEventListener('drop', function (e) {
            this.$refs.dropFileBox.classList.remove('dragStart');
            this.handleFileUpload(e.dataTransfer.files);
          }.bind(this));

          document.addEventListener('dragleave', function () {
            this.$refs.dropFileBox.classList.remove('dragStart');
          }.bind(this));

          document.addEventListener('drop', function () {
            this.$refs.dropFileBox.classList.remove('dragStart');
          }.bind(this));

          window.require(['SHARED/jquery'], function ($) {
            $('#exoAttachmentsApp *[rel="tooltip"]').tooltip();
          });
          this.dragAndDropEventListenerInitialized = true;
        }
      });

    },
    closeAttachmentsAppDrawer() {
      this.$refs.attachmentsAppDrawer.close();
    },
    uploadFile: function () {
      this.$refs.uploadInput.click();
    },
    handleFileUpload: function (files) {
      const newFilesArray = Array.from(files);

      newFilesArray.sort(function (file1, file2) {
        return file1.size - file2.size;
      });

      const newAttachedFiles = [];
      newFilesArray.forEach(file => {
        newAttachedFiles.push({
          originalFileObject: file,
          fileDrive: this.currentDrive,
          name: file.name,
          size: file.size,
          mimetype: file.type,
          uploadId: this.getNewUploadId(),
          uploadProgress: 0,
          destinationFolder: this.pathDestinationFolder,
          pathDestinationFolderForFile: '',
          isPublic: true
        });
      });

      this.uploadMode = 'temp';
      newAttachedFiles.forEach(newFile => {
        this.queueUpload(newFile);
      });
      this.$refs.uploadInput.value = '';
    },
    getFormattedFileSize(fileSize) {
      const formattedSizePrecision = 2;
      const sizeMB = fileSize / this.BYTES_IN_MB;
      return sizeMB.toFixed(formattedSizePrecision);
    },
    getNewUploadId: function () {
      const maxUploadId = 100000;
      return Math.floor(Math.random() * maxUploadId);
    },
    queueUpload: function (file) {
      if (this.uploadMode === 'temp') {
        if (this.value.length >= this.maxFilesCount) {
          this.filesCountLimitError = true;
          return;
        }

        const fileSizeInMb = file.size / this.BYTES_IN_MB;
        if (fileSizeInMb > this.maxFileSize) {
          this.fileSizeLimitError = true;
          return;
        }
        const fileExists = this.value.some(f => f.name === file.name);
        if (fileExists) {
          this.sameFileErrorMessage = this.sameFileErrorMessage.replace('{0}', file.name);
          this.sameFileError = true;
          return;
        }

        this.value.push(file);
      }
      if (this.uploadingCount < this.maxUploadInProgressCount) {
        if (this.uploadMode === 'temp') {
          this.sendFileToServer(file);
        } else {
          this.uploadFileToDestinationPath(file);
        }
      } else {
        this.uploadingFilesQueue.push(file);
      }
    },
    processNextQueuedUpload: function () {
      if (this.uploadingFilesQueue.length > 0) {
        if (this.uploadMode === 'temp') {
          this.sendFileToServer(this.uploadingFilesQueue.shift());
        } else {
          this.uploadFileToDestinationPath(this.uploadingFilesQueue.shift());
        }
      }
    },
    sendFileToServer: function (file) {
      this.uploadingCount++;
      this.$emit('uploadingCountChanged', this.uploadingCount);

      const formData = new FormData();
      formData.append('file', file.originalFileObject);

      const uploadUrl = `${eXo.env.server.context}/upload?action=upload&uploadId=${file.uploadId}`;
      // Had to use axios here since progress observation is still not supported by fetch
      axios.request({
        method: 'POST',
        url: uploadUrl,
        credentials: 'include',
        data: formData,
        onUploadProgress: (progress) => {
          file.uploadProgress = Math.round(progress.loaded * this.maxProgress / progress.total);
        }
      }).then(() => {

        // Check if the file has correctly been uploaded (progress=100) before refreshing the upload list
        const progressUrl = `${eXo.env.server.context}/upload?action=progress&uploadId=${file.uploadId}`;
        fetch(progressUrl)
          .then(response => response.text())
          .then(responseText => {
            // TODO fix malformed json from upload service
            let responseObject;
            try {
              // trick to parse malformed json
              eval(`responseObject = ${responseText}`); // eslint-disable-line no-eval
            } catch (err) {
              return;
            }

            if (!responseObject.upload[file.uploadId] || !responseObject.upload[file.uploadId].percent ||
              responseObject.upload[file.uploadId].percent !== this.maxProgress.toString()) {
              this.removeAttachedFile(file.uploadId);
            } else {
              file.uploadProgress = this.maxProgress;
            }
          });
        this.uploadingCount--;
        this.$emit('uploadingCountChanged', this.uploadingCount);
        this.processNextQueuedUpload();
      });
    },
    uploadFileToDestinationPath: function (file) {
      this.uploadingCount++;
      this.$emit('uploadingCountChanged', this.uploadingCount);
      this.$attachmentsService.uploadAttachment(
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
          this.uploadedFiles.push(this.$attachmentsService.convertXmlToJson(uploadedFile));
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
    removeAttachedFile: function (file) {
      if (!file.id) {
        this.value = this.value && this.value.filter(attachedFile => attachedFile.uploadId !== file.uploadId);
        if (file.uploadProgress !== this.maxProgress) {
          this.uploadingCount--;
          this.$emit('uploadingCountChanged', this.uploadingCount);
          this.processNextQueuedUpload();
        }
      } else {
        this.value = this.value.filter(attachedFile => attachedFile.id !== file.id);
      }
      this.$emit('input', this.value);
    },
    addDestinationFolderForAll(pathDestinationFolder, folder, currentDrive) {
      this.currentDrive = currentDrive;
      this.pathDestinationFolder = pathDestinationFolder;
      if (pathDestinationFolder === '') {
        this.showDestinationPath = false;
      } else {
        this.showDestinationPath = true;
      }
      for (let i = 0; i < this.value.length; i++) {
        if (!this.value[i].destinationFolder || this.value[i].destinationFolder === this.defaultDestinationFolderPath) {
          this.value[i].destinationFolder = this.pathDestinationFolder;
          this.value[i].fileDrive = this.currentDrive;
        }
      }
      this.schemaFolder = [];
      const namesOfFolders = folder.split('/');
      for (let i = 0; i < namesOfFolders.length; i++) {
        this.schemaFolder[i] = namesOfFolders[i];
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector ? `${this.$t('attachments.drawer.existingUploads')}` : `${this.$t('attachments.upload.document')}`;
      if (!this.showDocumentSelector) {
        this.showDestinationFolder = false;
      }
    },
    addDestinationFolderForFile(pathDestinationFolder, folder, isPublic, currentDrive) {
      for (let i = 0; i < this.value.length; i++) {
        if (this.value[i].name === this.destinationFileName) {
          this.value[i].pathDestinationFolderForFile = folder;
          this.value[i].destinationFolder = pathDestinationFolder.startsWith('/') ? pathDestinationFolder.substring(1) : pathDestinationFolder;
          this.value[i].fileDrive = currentDrive;
          // TODO: get 'isPublic' property of file from rest, now 'isPublic' assigned to 'isPublic' property of destination folder
          this.value[i].isPublic = isPublic;
        }
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector ? `${this.$t('attachments.drawer.existingUploads')}` : `${this.$t('attachments.upload.document')}`;
      if (!this.showDocumentSelector) {
        this.showDestinationFolderForFile = false;
      }
      this.modeFolderSelectionForFile = false;
    },
    toggleServerFileSelector(selectedFiles) {
      if (selectedFiles) {
        this.value = selectedFiles;
        this.attachmentInfo = true;
        this.$emit('input', this.value);
        this.$emit('attachmentsChanged', this.value);
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector ? `${this.$t('attachments.drawer.existingUploads')}` : `${this.$t('attachments.upload.document')}`;
      if (!this.showDocumentSelector) {
        this.showDestinationFolder = false;
        this.showDestinationFolderForFile = false;
      }
    },
    toggleSelectDestinationFolder() {
      this.showDestinationFolder = true;
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector ? `${this.$t('attachments.drawer.destination.folder')}` : `${this.$t('attachments.upload.document')}`;
    },
    openSelectDestinationFolderForFile(file) {
      this.modeFolderSelectionForFile = true;
      this.destinationFileName = file.name;
      this.showDestinationFolderForFile = true;
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector ? `${this.$t('attachments.drawer.destination.folder')}` : `${this.$t('attachments.upload.document')}`;
    },
    addDefaultPath() {
      if (eXo.env.portal.spaceId) {
        this.$attachmentsService.getSpaceById(eXo.env.portal.spaceId).then(space => {
          this.schemaFolder.push(space.displayName);
          this.schemaFolder.push('Activity Stream Documents');
          this.showDestinationPath = true;
          this.isActivityStream = false;
          this.spaceGroupId = space.groupId;
        });
      } else {
        this.schemaFolder.push(eXo.env.portal.userName);
        this.schemaFolder.push('Personal Documents');
        this.schemaFolder.push('Documents');
        this.showDestinationPath = true;
        this.isActivityStream = true;
      }
    },
    deleteDestinationPathForFile(fileName) {
      for (let i = 0; i < this.value.length; i++) {
        if (this.value[i].name === fileName) {
          this.value[i].showDestinationFolderForFile = '';
          this.value[i].pathDestinationFolderForFile = '';
          this.value[i].fileDrive = this.currentDrive;
          this.value[i].destinationFolder = this.pathDestinationFolder;
          this.value[i].isPublic = true;
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
            this.handleFileUpload([myNewFile]);
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
      this.$attachmentsService.isCloudDriveEnabled().then(data => {
        this.isCloudDriveEnabled = data.result === 'true';
      });
    },
    uploadAddedAttachments() {
      this.uploadMode = 'save';
      this.$refs.attachmentsAppDrawer.startLoading();
      this.value.forEach(file => {
        this.queueUpload(file);
      });
    },
    initDefaultDestinationFolderPath(defaultDestinationFolderPath, folderName) {
      this.showDestinationPath = true;
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
      localStorage.setItem('newlyUploadedAttachments', JSON.stringify(this.uploadedFiles));
      this.value = [];
      this.$refs.attachmentsAppDrawer.endLoading();
      document.dispatchEvent(new CustomEvent('attachments-upload-finished'));
    },
  }
};
</script>