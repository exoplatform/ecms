<template>
  <div id="exoAttachmentsApp">
    <div :class="{ open: showAttachmentsDrawer }" class="attachments drawer ignore-vuetify-classes" @keydown.esc.self="toggleAttachmentsDrawer()">
      <div :class="showDocumentSelector? 'documentSelector' : ''" class="attachmentsHeader header">
        <a v-if="showDocumentSelector" class="backButton" @click="toggleServerFileSelector()">
          <i class="uiIconBack"> </i>
        </a>
        <a v-if="!showDocumentSelector" class="backButton" @click="toggleAttachmentsDrawer()">
          <i class="uiIconBack"> </i>
        </a>
        <span class="attachmentsTitle">{{ drawerTitle || $t('attachments.drawer.header') }}</span>
        <a class="attachmentsCloseIcon" @click="toggleAttachmentsDrawer()">×</a>
        <v-progress-linear :active="cloudDriveConnecting" absolute bottom indeterminate></v-progress-linear>
      </div>
      <div :class="showDocumentSelector? 'serverFiles' : 'attachments'" class="content">
        <div v-show="!showDocumentSelector">
          <div v-show="attachmentInfo && isActivityStream && (privateFilesAttached || fromAnotherSpaces.length > 0)" class="alert alert-info attachmentsAlert">
            {{ $t('attachments.alert.sharing.attachedFrom') }}
            {{ $t(`attachments.alert.sharing.${privateFilesAttached && !fromAnotherSpaces.length ? 'personal' : 'space'}`) }}
            <b v-show="fromAnotherSpaces.length > 0">
              {{ fromAnotherSpaces }}
            </b>
            {{ $t('attachments.alert.sharing.availableFor') }} {{ $t('attachments.alert.sharing.connections') }}
          </div>
          <div v-show="attachmentInfo && !isActivityStream && (privateFilesAttached || fromAnotherSpaces.length > 0)" class="alert alert-info attachmentsAlert">
            {{ $t('attachments.alert.sharing.attachedFrom') }}
            {{ $t(`attachments.alert.sharing.${privateFilesAttached && !fromAnotherSpaces.length ? 'personal' : 'space'}`) }}
            <b v-show="fromAnotherSpaces.length > 0">
              {{ fromAnotherSpaces }}
            </b>
            {{ $t('attachments.alert.sharing.availableFor') }} <b>{{ spaceGroupId }}</b> {{ $t('attachments.alert.sharing.members') }}
          </div>
        </div>
        <div v-show="!showDocumentSelector" class="attachmentsContent">
          <div class="multiploadFilesSelector">
            <div id="DropFileBox" ref="dropFileBox" class="dropFileBox">
              <div class="contentAttachments">
                <div class="contentDragAndDrop">
                  <div class="contentDrop">
                    <div class="icon"><i class="uiIconTemplate uiIcon32x32LightGray colorText"></i></div>
                    <div><span class="dropMsg colorText">{{ $t('attachments.drawer.dropOrPaste') }}</span></div>
                  </div>
                  <div class="contentUpload">
                    <a :title="$t('attachments.drawer.upload')" class="uploadButton" href="#" rel="tooltip" data-placement="bottom" @click="uploadFile">
                      <i class="fas fa-download uiIcon32x32LightGray colorIcon"></i>
                      <span class="text colorText">{{ $t('attachments.drawer.upload') }}</span>
                      <span class="mobileText">{{ $t('attachments.drawer.upload') }}</span>
                    </a>
                  </div>
                </div>
                <div class="contentOR">
                  <div class="item"><span class="colorText"><hr class="leftLine"></span></div>
                  <div class="itemOR"><span class="or colorText"> {{ $t('attachments.drawer.or') }}  </span></div>
                  <div class="item"><span class="colorText"><hr class="rightLine"></span></div>
                </div>
                <div class="lastContent">
                  <a title="Select on server" class="uploadButton" href="#" rel="tooltip" data-placement="bottom" @click="toggleServerFileSelector()">
                    <i class="uiIcon32x32FolderDefault uiIcon32x32LightGray"></i>
                    <v-icon color="#fff" x-small class="iconCloud">cloud</v-icon>
                    <span class="text colorText">{{ $t('attachments.drawer.existingUploads') }}</span>
                  </a>
                </div>
              </div>
            </div>
            <div class="fileHidden" style="display:none">
              <input ref="uploadInput" class="file" name="file" type="file" multiple="multiple" style="display:none" @change="handleFileUpload($refs.uploadInput.files)">
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

          <div class="limitMessage">
            <div class="sizeLimit">{{ $t('attachments.drawer.maxFileSize').replace('{0}', maxFileSize) }}</div>
            <div class="countLimit">{{ $t('attachments.drawer.maxFileCount').replace('{0}', maxFilesCount) }}</div>
          </div>

          <div class="uploadedFiles">
            <div class="uploadedFilesTitle">{{ $t('attachments.drawer.title') }} ({{ value.length }})</div>
            <div v-if="value.length > 0" class="destinationFolder">
              <div v-if="showDestinationPath && !displayMessageDestinationFolder" class="folderLocation">
                <div :title="schemaFolder[0]" class="drive" rel="tooltip" data-placement="top">{{ schemaFolder[0] }}</div>
                <div v-if="schemaFolder.length > 1" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[1]" :class="schemaFolder.length === 2 ? 'active' : '' " class="folderName" rel="tooltip" data-placement="top">{{ schemaFolder[1] }}</div>
                </div>
                <div v-if="schemaFolder.length === 3" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[2]" :class="schemaFolder[2] !== 'Activity Stream Documents' ? 'path' : 'active' " class="folderName" rel="tooltip" data-placement="top">{{ schemaFolder[2] }}</div>
                </div>
                <div v-if="schemaFolder.length > 3" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="schemaFolder[2]" class="folderName" rel="tooltip" data-placement="top">...</div>
                </div>
                <div v-for="folder in schemaFolder.slice(schemaFolder.length-1,schemaFolder.length)" v-show="schemaFolder.length > 3" :key="folder" class="folder">
                  <div><span class="uiIconArrowRight colorIcon"></span></div>
                  <div :title="folder" :class="schemaFolder[schemaFolder.length - 1] === folder && schemaFolder[schemaFolder.length - 1].length < 11 ?'active' : 'path'" class="folderName" rel="tooltip" data-placement="top">{{ folder }}</div>
                </div>
              </div>
              <div v-if="displayMessageDestinationFolder" class="messageDestination">
                <p :title="$t('attachments.drawer.destination.attachment.message')" rel="tooltip" data-placement="top">{{ $t('attachments.drawer.destination.attachment.message') }}</p>
              </div>
              <button :disabled="displayMessageDestinationFolder" class="buttonSelect" @click="toggleSelectDestinationFolder()">
                <i :title="!displayMessageDestinationFolder ? $t('attachments.drawer.destination.attachment') : $t('attachments.drawer.destination.attachment.access') " :class="displayMessageDestinationFolder ? 'disabled' : ''" class="uiIconFolder " rel="tooltip" data-placement="top"></i>
              </button>
            </div>
            <div class="uploadedFilesItems">
              <div v-for="attachedFile in value" :key="attachedFile.name" class="uploadedFilesItem">
                <div class="showDestination">
                  <div class="showFile"><exo-attachment-item :file="attachedFile"></exo-attachment-item></div>
                </div>
                <div class="destinationFolder">
                  <div class="folderLocation">
                    <div class="emptyMessage">
                    </div>
                    <div v-if="attachedFile.pathDestinationFolderForFile && attachedFile.uploadId" class="box">
                      <div><p :title="attachedFile.pathDestinationFolderForFile" class="folder" rel="tooltip" data-placement="top">{{ attachedFile.pathDestinationFolderForFile }}</p></div>
                      <div class="folderName">
                        <a class="colorIcon" @click="deleteDestinationFolderForFile(attachedFile.name)">x</a>
                      </div>
                    </div>
                    <div>
                      <i v-if="!attachedFile.pathDestinationFolderForFile && attachedFile.uploadId" :title="$t('attachments.drawer.destination.folder')" rel="tooltip" data-placement="top" class="fas fa-folder fa-sm colorIcon" @click="openSelectDestinationFolderForFile(attachedFile)"></i>
                    </div>
                    <div>
                      <i v-if="!attachedFile.uploadId" :title="$t('attachments.drawer.destination.attachment.access')" rel="tooltip" data-placement="top" class="fas fa-ban fa-xs colorIconStop" ></i>
                    </div>
                    <div class="btnTrash">
                      <i :title="$t('attachments.drawer.delete')" rel="tooltip" data-placement="top" class="fas fa-trash fa-xs colorIcon" @click="removeAttachedFile(attachedFile)"></i>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <exo-folders-files-selector 
          v-if="showDocumentSelector && !showDestinationFolder && !showDestinationFolderForFile" 
          :attached-files="value" 
          :space-id="spaceId"
          :is-cloud-enabled="isCloudDriveEnabled"
          :extension-refs="$refs"
          :connected-drive="connectedDrive"
          :cloud-drives-in-progress="drivesInProgress"
          @itemsSelected="toggleServerFileSelector"
          @cancel="toggleServerFileSelector()"
        ></exo-folders-files-selector>
        <exo-folders-files-selector v-if="showDocumentSelector && showDestinationFolder && !showDestinationFolderForFile"
                                    :is-cloud-enabled="isCloudDriveEnabled"
                                    :mode-folder-selection="showDestinationFolder"
                                    @itemsSelected="addDestinationFolder"
                                    @cancel="toggleServerFileSelector()">
        </exo-folders-files-selector>
        <exo-folders-files-selector v-if="showDocumentSelector && showDestinationFolderForFile"
                                    :is-cloud-enabled="isCloudDriveEnabled"
                                    :mode-folder-selection="showDestinationFolderForFile"
                                    :mode-folder-selection-for-file="modeFolderSelectionForFile"
                                    @itemsSelected="addDestinationFolderForFile"
                                    @cancel="toggleServerFileSelector()"></exo-folders-files-selector>
        <div v-for="action in attachmentsComposerActions" :key="action.key" :class="`${action.appClass}Action`">
          <component v-dynamic-events="action.component.events" v-if="action.component" v-bind="action.component.props ? action.component.props : {}"
                     :is="action.component.name" :ref="action.key"></component>
        </div>
      </div>
      <div v-if="!showDocumentSelector" class="attachmentsFooter footer ignore-vuetify-classes">
        <a class="btn btn-primary ignore-vuetify-classes" @click="toggleAttachmentsDrawer()">{{ $t('attachments.drawer.apply') }}</a>
      </div>
    </div>
    <div v-show="showAttachmentsDrawer && showAttachmentsBackdrop" class="drawer-backdrop" @click="toggleAttachmentsDrawer()"></div>
  </div>
</template>

<script>
import axios from 'axios';
import * as attachmentsService from '../attachmentsService.js';
import { getAttachmentsComposerExtensions } from '../extension';

export default {
  directives: {
    DynamicEvents: {
      bind: function (el, binding, vnode) {
        const allEvents = binding.value;
        if (allEvents) {
          allEvents.forEach((event) => {
            if (vnode.componentInstance) {
              // register handler in the dynamic component
              vnode.componentInstance.$on(event.event, (eventData) => {
                const param = eventData ? eventData : event.listenerParam;
                // when the event is fired, the eventListener function is going to be called
                vnode.context[event.listener](param);
              });
            }
          });
        }
      },
      unbind: function (el, binding, vnode) {
        if (vnode.componentInstance) {
          vnode.componentInstance.$off();
        }
      },
    }
  },
  props: {
    value: {
      type: Array,
      default: () => []
    },
    spaceId: {
      type: String,
      default: ''
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
    }
  },
  data() {
    return {
      showDestinationFolder:false,
      message: '',
      uploadingFilesQueue: [],
      uploadingCount : 0,
      maxUploadInProgressCount : 2,
      maxProgress : 100,
      showDocumentSelector: false,
      fileSizeLimitError: false,
      filesCountLimitError: false,
      sameFileError: false,
      sameFileErrorMessage: `${this.$t('attachments.drawer.sameFile.error')}`,
      BYTES_IN_MB: 1048576,
      MESSAGES_DISPLAY_TIME: 5000,
      drawerTitle: null,
      pathDestinationFolder : '',
      showDestinationPath: false,
      schemaFolder: [],
      destinationFileName: '',
      showDestinationFolderForFile:false,
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
      isCloudDriveEnabled : false,
    };
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
          this.pathDestinationFolder = '';
          this.showDestinationPath = false;
          this.schemaFolder = [];
          this.displayMessageDestinationFolder = true;
          this.addDefaultPath();
        }
        if (this.value.length > 0 && !this.pathDestinationFolder) {
          for (let i = 0; i < this.value.length; i++) {
            if (!this.value[i].pathDestinationFolder) {
              this.value[i].pathDestinationFolder = this.pathDestinationFolder;
            }
          }
        }
        this.privateFilesAttached = this.value.some(file => file.isPublic === false);
        this.fromAnotherSpaces = this.value.filter(({ space }) => space && space.name !== this.groupId)
          .map(({ space }) => space.title).filter((value, i, self) => self.indexOf(value) === i).join(',');
      }
    }
  },
  mounted() {
    ['drag', 'dragstart', 'dragend', 'dragover', 'dragenter', 'dragleave', 'drop'].forEach( function( evt ) {
      /*
        For each event add an event listener that prevents the default action
        (opening the file in the browser) and stop the propagation of the event (so
        no other elements open the file in the browser)
      */
      this.$refs.dropFileBox.addEventListener(evt, function(e) {
        e.preventDefault();
        e.stopPropagation();
      }.bind(this), false);
    }.bind(this));

    /*
      Capture the files from the drop event and add them to our local files
      array.
    */
    this.$refs.dropFileBox.addEventListener('drop', function(e) {
      this.handleFileUpload( e.dataTransfer.files );
    }.bind(this));

    window.require(['SHARED/jquery'], function($) {
      $('#exoAttachmentsApp *[rel="tooltip"]').tooltip();
    });
  },
  created(){
    this.addDefaultPath();
    this.getCloudDriveStatus();
    document.addEventListener('extension-AttachmentsComposer-attachments-composer-action-updated', () => this.attachmentsComposerActions = getAttachmentsComposerExtensions());
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
  },
  methods: {
    toggleAttachmentsDrawer: function() {
      this.showAttachmentsDrawer = !this.showAttachmentsDrawer;
      if (this.showAttachmentsDrawer){
        document.addEventListener('paste', this.onPaste, false);
      } else {
        document.removeEventListener('paste', this.onPaste, false);
      }
    },
    uploadFile: function() {
      this.$refs.uploadInput.click();
    },
    handleFileUpload: function(files) {
      const newFilesArray = Array.from(files);

      newFilesArray.sort(function(file1, file2) {
        return file1.size - file2.size;
      });

      const newAttachedFiles = [];
      newFilesArray.forEach(file => {
        newAttachedFiles.push({
          originalFileObject: file,
          name: file.name,
          size: file.size,
          mimetype: file.type,
          uploadId: this.getNewUploadId(),
          uploadProgress: 0,
          destinationFolder: this.pathDestinationFolder,
          pathDestinationFolderForFile:'',
          isPublic: true
        });
      });

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
    getNewUploadId: function() {
      const maxUploadId = 100000;
      return Math.floor(Math.random() * maxUploadId);
    },
    queueUpload: function(file) {
      if(this.value.length >= this.maxFilesCount) {
        this.filesCountLimitError = true;
        return;
      }

      const fileSizeInMb = file.size / this.BYTES_IN_MB;
      if(fileSizeInMb > this.maxFileSize) {
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
      if(this.uploadingCount < this.maxUploadInProgressCount) {
        this.sendFileToServer(file);
      } else {
        this.uploadingFilesQueue.push(file);
      }
    },
    processNextQueuedUpload: function() {
      if(this.uploadingFilesQueue.length > 0) {
        this.sendFileToServer(this.uploadingFilesQueue.shift());
      }
    },
    sendFileToServer : function(file) {
      this.uploadingCount++;
      this.$emit('uploadingCountChanged', this.uploadingCount);

      const formData = new FormData();
      formData.append('file', file.originalFileObject);

      const uploadUrl =`${eXo.env.server.context}/upload?action=upload&uploadId=${file.uploadId}`;
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

            if(!responseObject.upload[file.uploadId] || !responseObject.upload[file.uploadId].percent ||
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
    removeAttachedFile: function(file) {
      if(!file.id) {
        this.value = this.value.filter(attachedFile => attachedFile.uploadId !== file.uploadId);
        if(file.uploadProgress !== this.maxProgress) {
          this.uploadingCount--;
          this.$emit('uploadingCountChanged', this.uploadingCount);
          this.processNextQueuedUpload();
        }
      } else {
        this.value = this.value.filter(attachedFile => attachedFile.id !== file.id);
      }
      this.$emit('input', this.value);
    },
    addDestinationFolder(pathDestinationFolder, folderName) {
      this.pathDestinationFolder = pathDestinationFolder;
      if (pathDestinationFolder === '') {
        this.showDestinationPath = false;
      } else {
        this.showDestinationPath = true;
      }
      for (let i = 0; i < this.value.length; i++) {
        if(!this.value[i].destinationFolder){
          this.value[i].destinationFolder = this.pathDestinationFolder;
        }
      }
      this.schemaFolder = [];
      const namesOfFolders = folderName.split('/');
      for (let i = 0; i < namesOfFolders.length; i++) {
        this.schemaFolder[i] = namesOfFolders[i];
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? this.$t('attachments.drawer.existingUploads') : this.$t('attachments.drawer.header');
      if (!this.showDocumentSelector) {
        this.showDestinationFolder = false;
      }
    },
    addDestinationFolderForFile(pathDestinationFolder, folder, isPublic){
      for (let i =0 ;i< this.value.length;i++){
        if (this.value[i].name === this.destinationFileName){
          this.value[i].pathDestinationFolderForFile = folder;
          this.value[i].destinationFolder = pathDestinationFolder;
          // TODO: get 'isPublic' property of file from rest, now 'isPublic' assigned to 'isPublic' property of destination folder
          this.value[i].isPublic = isPublic;
        }
      }
      this.pathDestinationFolder = '';
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? this.$t('attachments.drawer.existingUploads') : this.$t('attachments.drawer.header');
      if (!this.showDocumentSelector) {
        this.showDestinationFolderForFile = false;
      }
      this.modeFolderSelectionForFile = false;
    },
    toggleServerFileSelector(selectedFiles){
      if (selectedFiles) {
        this.value = selectedFiles;
        this.attachmentInfo = true;
        this.$emit('input', this.value);
        this.$emit('attachmentsChanged', this.value);
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? this.$t('attachments.drawer.existingUploads') : this.$t('attachments.drawer.header');
      if (!this.showDocumentSelector){
        this.showDestinationFolder = false;
        this.showDestinationFolderForFile = false;
      }
    },
    toggleSelectDestinationFolder(){
      this.showDestinationFolder = true ;
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? this.$t('attachments.drawer.destination.folder') : this.$t('attachments.drawer.header');
    },
    openSelectDestinationFolderForFile(file){
      this.modeFolderSelectionForFile = true;
      this.destinationFileName = file.name;
      this.showDestinationFolderForFile = true;
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? this.$t('attachments.drawer.destination.folder') : this.$t('attachments.drawer.header');
    },
    addDefaultPath(){
      if(eXo.env.portal.spaceId){
        attachmentsService.getSpaceById(eXo.env.portal.spaceId).then( space => {
          this.schemaFolder.push(space.displayName);
          this.schemaFolder.push('Activity Stream Documents');
          this.showDestinationPath=true;
          this.isActivityStream = false;
          this.spaceGroupId = space.groupId;
        });
      }else {
        this.schemaFolder.push(eXo.env.portal.userName);
        this.schemaFolder.push('Public');
        this.schemaFolder.push('Activity Stream Documents');
        this.showDestinationPath=true;
        this.isActivityStream = true;
      }
    },
    deleteDestinationFolderForFile(fileName){
      for (let i=0;i<this.value.length;i++){
        if(this.value[i].name === fileName){
          this.value[i].showDestinationFolderForFile = '';
          this.value[i].pathDestinationFolderForFile = '';
          this.value[i].isPublic = true;
          break;
        }
      }
    },
    setCloudDriveProgress({ progress }) {
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
    getFilename (textItem, textItemType, sendFileToServer) {
      let fileName;
      const thiss = this;
      if (textItem) {
        textItem.getAsString((htmlString) => {
          if(textItemType === 'text/plain') {
            fileName = decodeURI(htmlString).split('/').pop();
          } else if (textItemType === 'text/html') {
            const img = thiss.parseHTML(htmlString).querySelectorAll('img')[0];
            fileName = img.src.split('/').pop();
          }
          sendFileToServer(fileName);
        });
      } else {
        fileName = `image-${ Date.now() }.png`;
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
      attachmentsService.isCloudDriveEnabled().then(data => {
        this.isCloudDriveEnabled = data.result === 'true';
      });
    },
  }
};
</script>