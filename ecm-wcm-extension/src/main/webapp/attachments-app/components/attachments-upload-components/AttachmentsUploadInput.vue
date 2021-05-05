<template>
  <div class="attachmentsUploadBlock">
    <div class="d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">{{ $t('attachments.upload') }}</v-subheader>
      <v-divider></v-divider>
    </div>
    <div class="multiUploadFilesSelector d-flex flex-column">
      <div id="DropFileBox" ref="dropFileBox" class="dropFileBox py-10 ml-5 d-flex flex-column align-center theme--light" aria-controls @click="uploadFile">
        <i class="uiIconEcmsUploadVersion uiIcon32x32"></i>
        <v-subheader class="text-sub-title ml-3 d-none d-sm-flex" href="#" rel="tooltip" data-placement="bottom">
          {{ $t('attachments.drawer.uploadOrDrop') }}
        </v-subheader>
      </div>
      <div class="fileHidden d-none">
        <input ref="uploadInput" class="file" name="file" type="file" multiple="multiple" style="display:none"
               @change="handleFileUpload($refs.uploadInput.files)">
      </div>
      <div class="uploadErrorMessages">
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
          <div v-show="sameFileError" class="sameFile ms-2 alert alert-error">
            <i class="uiIconError"></i>
            {{ sameFileErrorMessage }}
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  props: {
    attachments: {
      type: Array,
      default: () => []
    },
    maxFilesCount: {
      type: Number,
      default: parseInt(`${eXo.env.portal.maxToUpload}`)
    },
    maxFileSize: {
      type: Number,
      default: parseInt(`${eXo.env.portal.maxFileSize}`)
    },
    currentDrive: {
      type: Object,
      default: () => null
    },
    pathDestinationFolder: {
      type: Object,
      default: () =>  null
    },
  },
  data() {
    return {
      fileSizeLimitError: false,
      filesCountLimitError: false,
      sameFileError: false,
      sameFileErrorMessage: `${this.$t('attachments.drawer.sameFile.error')}`,
      MESSAGES_DISPLAY_TIME: 5000,
      BYTES_IN_MB: 1048576,
      maxUploadInProgressCount: 2,
      uploadingFilesQueue: [],
      uploadingCount: 0,
      maxProgress: 100,
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
  },
  created() {
    this.initDragAndDropEvents();
    this.$root.$on('handle-pasted-files-from-clipboard',
      pastedFiles => this.handleFileUpload(pastedFiles));
  },
  methods: {
    initDragAndDropEvents() {
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
      this.$refs.uploadInput.attachments = '';
    },
    queueUpload: function (file) {
      if (this.uploadMode === 'temp') {
        if (this.attachments.length >= this.maxFilesCount) {
          this.filesCountLimitError = true;
          return;
        }

        const fileSizeInMb = file.size / this.BYTES_IN_MB;
        if (fileSizeInMb > this.maxFileSize) {
          this.fileSizeLimitError = true;
          return;
        }
        const fileExists = this.attachments.some(f => f.name === file.name);
        if (fileExists) {
          this.sameFileErrorMessage = this.sameFileErrorMessage.replace('{0}', file.name);
          this.sameFileError = true;
          return;
        }

        this.attachments.push(file);
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
              this.removeAttachedFile(file);
            } else {
              file.uploadProgress = this.maxProgress;
            }
          });
        this.uploadingCount--;
        this.$emit('uploadingCountChanged', this.uploadingCount);
        this.processNextQueuedUpload();
      });
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
    getNewUploadId: function () {
      const maxUploadId = 100000;
      return Math.floor(Math.random() * maxUploadId);
    },
    removeAttachedFile() {
      this.$root.$emit('remove-attachment-item');
    }
  }
};
</script>