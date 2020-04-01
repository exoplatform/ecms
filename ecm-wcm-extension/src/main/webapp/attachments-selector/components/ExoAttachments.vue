<template>
  <div id="exoAttachmentsApp">
    <div :class="{ open: showAttachments || showAttachmentsDrawer }" class="attachments drawer ignore-vuetify-classes" @keydown.esc="closeAttachments()">
      <div :class="showDocumentSelector? 'documentSelector' : ''" class="attachmentsHeader header">
        <a v-if="showDocumentSelector" class="backButton" @click="toggleServerFileSelector()">
          <i class="uiIconBack"> </i>
        </a>
        <a v-if="!showDocumentSelector" class="backButton" @click="closeAttachments()">
          <i class="uiIconBack"> </i>
        </a>
        <span class="attachmentsTitle">{{ drawerTitle }}</span>
        <a class="attachmentsCloseIcon" @click="closeAttachments()">×</a>
      </div>
      <div :class="showDocumentSelector? 'serverFiles' : 'attachments'" class="content">
        <div v-show="!showDocumentSelector" class="attachmentsContent">
          <div class="multiploadFilesSelector">
            <div id="DropFileBox" ref="dropFileBox" class="dropFileBox">
              <div class="contentAttachments">
                <div class="contentDargAndDrop">
                  <div class="contentDrop">
                    <div class="icon"><i class="uiIconTemplate uiIcon32x32LightGray colorText"></i></div>
                    <div><span class="dropMsg colorText">{{ $t('attachments.drawer.drop') }}</span></div>
                  </div>
                  <div class="contentUpload">
                    <div class="icon"><i class="fas fa-download uiIcon32x32LightGray colorIcon"></i></div>
                    <div class="uploadMobile">
                      <a :title="$t('attachments.drawer.upload')" class="uploadButton" href="#" rel="tooltip" data-placement="bottom" @click="uploadFile">
                        <span class="text colorText">{{ $t('attachments.drawer.upload') }}</span>
                        <span class="mobileText">{{ $t('attachments.drawer.upload') }}</span>
                      </a>
                    </div>
                  </div>
                </div>
                <div class="contentOR">
                  <div class="item"><span class="colorText"><hr class="leftLine"></span></div>
                  <div class="itemOR"><span class="or colorText"> {{ $t('attachments.drawer.or') }}  </span></div>
                  <div class="item"><span class="colorText"><hr class="rightLine"></span></div>
                </div>
                <div class="lastContent">
                  <div class="icon">
                    <i class="uiIconFolderSearch uiIcon32x32LightGray"></i>
                  </div>
                  <div class="text">
                    <a title="Select on server" class="uploadButton" href="#" rel="tooltip" data-placement="bottom" @click="toggleServerFileSelector()">
                      <span class="text colorText">{{ $t('attachments.drawer.existingUploads') }}</span>
                    </a>
                  </div>
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
            <div class="uploadedFilesItems">
              <div v-for="attachedFile in value" :key="attachedFile.name" class="uploadedFilesItem">
                <exo-attachment-item :file="attachedFile"></exo-attachment-item>
                <div class="removeFile">
                  <a :title="$t('attachments.drawer.delete')" href="#" class="actionIcon" rel="tooltip"
                     data-placement="top" @click="removeAttachedFile(attachedFile)">
                    <i class="uiIcon uiIconLightGray"></i>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
        <exo-server-files-selector v-if="showDocumentSelector" :attached-files="value" :space-id="spaceId" @attachExistingServerAttachment="toggleServerFileSelector" @cancel="toggleServerFileSelector()"></exo-server-files-selector>
      </div>
      <div v-if="!showDocumentSelector" class="attachmentsFooter footer ignore-vuetify-classes">
        <a class="btn btn-primary ignore-vuetify-classes" @click="closeAttachments()">{{ $t('attachments.drawer.apply') }}</a>
      </div>
    </div>
    <div v-show="showAttachments || showAttachmentsDrawer" class="drawer-backdrop" @click="closeAttachments()"></div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
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
      default: 20
    },
    maxFileSize: {
      type: Number,
      required: false,
      default: 25
    },
    showAttachmentsDrawer: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showAttachments: false,
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
      drawerTitle: `${this.$t('attachments.drawer.header')}`
    };
  },
  watch: {
    fileSizeLimitError: function() {
      if(this.fileSizeLimitError) {
        setTimeout(() => this.fileSizeLimitError = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    filesCountLimitError: function() {
      if(this.filesCountLimitError) {
        setTimeout(() => this.filesCountLimitError = false, this.MESSAGES_DISPLAY_TIME);
      }
    },
    sameFileError: function() {
      if(this.sameFileError) {
        setTimeout(() => this.sameFileError = false, this.MESSAGES_DISPLAY_TIME);
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
  },
  methods: {
    closeAttachments: function() {
      this.$emit('HideAttachmentsDrawer', this.showAttachments);
      this.showAttachments = false;
      document.getElementsByClassName('attachments drawer')[0].className = 'attachments drawer';
      document.getElementById('exoAttachmentsApp').getElementsByClassName('drawer-backdrop')[0].style.display = 'none';
    },
    setUploadingCount: function(uploadingCount) {
      this.uploading = uploadingCount > 0;
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
          uploadProgress: 0
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
    toggleServerFileSelector(selectedFiles){
      if (selectedFiles) {
        this.value = selectedFiles;
        this.$emit('input', this.value);
      }
      this.showDocumentSelector = !this.showDocumentSelector;
      this.drawerTitle = this.showDocumentSelector? `${this.$t('attachments.drawer.existingUploads')}` : `${this.$t('attachments.drawer.header')}`;
    }
  }
};
</script>