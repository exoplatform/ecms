<template>
  <div class="attachmentsUploadBlock">
    <div class="d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">
        {{ $t('attachments.upload') }}
      </v-subheader>
      <v-divider />
    </div>
    <div class="multiUploadFilesSelector d-flex flex-column">
      <div
        id="DropFileBox"
        ref="dropFileBox"
        class="dropFileBox py-10 ml-5 d-flex flex-column align-center theme--light"
        aria-controls
        @click="uploadFile">
        <i class="uiIconEcmsUploadVersion uiIcon32x32"></i>
        <v-subheader
          class="upload-drag-drop-label text-sub-title mt-3 d-flex flex-column">
          <span>{{ $t('attachments.drawer.uploadOrDrop') }}</span>
          <span>({{ $t('attachments.drawer.maxFileSize').replace('{0}', maxFileSize) }})</span>
        </v-subheader>
      </div>
      <div class="fileHidden d-none">
        <input
          ref="uploadInput"
          class="file"
          name="file"
          type="file"
          multiple="multiple"
          style="display:none"
          @change="handleFileUpload($refs.uploadInput.files)">
      </div>
    </div>
  </div>
</template>

<script>
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
      MESSAGES_DISPLAY_TIME: 5000,
      BYTES_IN_MB: 1048576,
      maxUploadInProgressCount: 2,
      uploadingFilesQueue: [],
      uploadingCount: 0,
      maxProgress: 100,
      newUploadedFiles: [],
    };
  },
  computed: {
    maxFileCountErrorLabel: function () {
      return this.$t('attachments.drawer.maxFileCount.error').replace('{0}', `<b> ${this.maxFilesCount} </b>`);
    },
    maxFileSizeErrorLabel: function () {
      return this.$t('attachments.drawer.maxFileSize.error').replace('{0}', `<b> ${this.maxFileSize} </b>`);
    },
  },
  watch: {
    uploadingCount(newValue) {
      if (newValue === 0) {
        this.$root.$emit('link-new-added-attachments', this.newUploadedFiles);
        this.newUploadedFiles = [];
      }
    }
  },
  created() {
    this.initDragAndDropEvents();
    this.$root.$on('handle-pasted-files-from-clipboard',
      pastedFiles => this.handleFileUpload(pastedFiles));
  },
  methods: {
    initDragAndDropEvents() {
      this.$nextTick(() => {
        if (!this.dragAndDropEventListenerInitialized){
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
        const controller = new AbortController();
        const signal = controller.signal;
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
          isPublic: true,
          signal: signal
        });
      });

      this.uploadMode = 'temp';
      newAttachedFiles.forEach(newFile => {
        this.queueUpload(newFile);
      });
      this.$refs.uploadInput.value = null;
    },
    queueUpload: function (file) {
      if (this.uploadMode === 'temp') {
        if (this.attachments.length >= this.maxFilesCount) {
          this.$root.$emit('attachments-notification-alert', {
            message: this.maxFileCountErrorLabel,
            type: 'error',
          });
          return;
        }

        const fileSizeInMb = file.size / this.BYTES_IN_MB;
        if (fileSizeInMb > this.maxFileSize) {
          this.$root.$emit('attachments-notification-alert', {
            message: this.maxFileSizeErrorLabel,
            type: 'error',
          });
          return;
        }
        const fileExists = this.attachments.some(f => f.name === file.name);
        if (fileExists) {
          const sameFileErrorMessage = this. $t('attachments.drawer.sameFile.error').replace('{0}', `<b> ${file.name} </b>`);
          this.$root.$emit('attachments-notification-alert', {
            message: sameFileErrorMessage,
            type: 'error',
          });
          return;
        }

        this.$root.$emit('add-new-uploaded-file', file);
        this.newUploadedFiles.push(file);
      }
      if (this.uploadingCount < this.maxUploadInProgressCount) {
        if (this.uploadMode === 'temp') {
          this.sendFileToServer(file);
        }
      } else {
        this.uploadingFilesQueue.push(file);
      }
    },
    sendFileToServer(file){
      this.uploadingCount++;
      this.$uploadService.upload(file.originalFileObject, file.uploadId, file.signal)
        .catch(error => {
          this.$root.$emit('attachments-notification-alert', {
            message: error,
            type: 'error',
          });
          this.removeAttachedFile(file);
        });
      this.controlUpload(file);
    },
    controlUpload(file){
      window.setTimeout(() => {
        this.$uploadService.getUploadProgress(file.uploadId)
          .then(percent => {
            file.uploadProgress = Number(percent);
            if (file.uploadProgress < 100) {
              this.controlUpload(file);
            } else {
              this.uploadingCount--;
              this.processNextQueuedUpload();
            }
          })
          .catch(error => {
            this.removeAttachedFile(file);
            this.$root.$emit('attachments-notification-alert', {
              message: error,
              type: 'error',
            });
          });
      }, 200);
    },
    processNextQueuedUpload: function () {
      if (this.uploadingFilesQueue.length > 0) {
        if (this.uploadMode === 'temp') {
          this.sendFileToServer(this.uploadingFilesQueue.shift());
        }
      }
    },
    getNewUploadId: function () {
      const maxUploadId = 100000;
      return Math.floor(Math.random() * maxUploadId);
    },
    removeAttachedFile(file) {
      this.$root.$emit('remove-attachment-item', file);
    }
  }
};
</script>