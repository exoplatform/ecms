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
      default: () => null
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
      abortUploading: false,
      uploadedFilesCount: 0
    };
  },
  computed: {
    maxFileCountErrorLabel: function () {
      return this.$t('attachments.drawer.maxFileCount.error').replace('{0}', `<b> ${this.maxFilesCount} </b>`);
    },
    maxFileSizeErrorLabel: function () {
      return this.$t('attachments.drawer.maxFileSize.error').replace('{0}', `<b> ${this.maxFileSize} </b>`);
    },
    isNewUploadedFilesEmpty() {
      return this.newUploadedFiles && this.newUploadedFiles.length === 0;
    },
    uploadFinished() {
      return !this.isNewUploadedFilesEmpty && this.newUploadedFiles.every(file => file.uploadProgress && file.uploadProgress === 100);
    },
  },
  watch: {
    uploadFinished() {
      if (this.uploadFinished && this.uploadingCount === 0) {
        this.$root.$emit('link-new-added-attachments');
        this.uploadedFilesCount += this.newUploadedFiles.length;
        this.newUploadedFiles = [];
      }
    }
  },
  created() {
    this.initDragAndDropEvents();
    this.$root.$on('handle-pasted-files-from-clipboard', this.handleFileUpload);
    this.$root.$on('reset-attachments-upload-input', () => this.resetUploadInput());
    this.$root.$on('abort-attachments-new-upload', () => this.abortUploadingNewAttachments());
    this.$root.$on('abort-uploading-new-file', this.abortUploadingNewFile);
    this.$root.$on('handle-provided-files', files => this.handleFileUpload(files));
    this.$root.$on('attachment-continue-upload', (file) => {
      this.sendFileToServer(file, true);
    });
  },
  beforeDestroy() {
    this.$root.$off('handle-pasted-files-from-clipboard', this.handleFileUpload);
    this.$root.$off('reset-attachments-upload-input', this.resetUploadInput);
    this.$root.$off('abort-attachments-new-upload', this.abortUploadingNewAttachments);
    this.$root.$off('abort-uploading-new-file', this.abortUploadingNewFile);
    this.$root.$off('handle-provided-files', this.handleFileUpload);
  },
  methods: {
    initDragAndDropEvents() {
      this.$nextTick(() => {
        if (!this.dragAndDropEventListenerInitialized) {
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
      this.abortUploading = false;
      const newFilesArray = Array.from(files);

      newFilesArray.sort(function (file1, file2) {
        return file1.size - file2.size;
      });

      this.newUploadedFiles = [];
      const newAttachedFiles = [];
      newFilesArray.forEach(file => {
        const controller = new AbortController();
        const signal = controller.signal;
        newAttachedFiles.push({
          originalFileObject: file,
          fileDrive: this.currentDrive,
          title: file.name,
          size: file.size,
          mimetype: file.type,
          acl: file.acl,
          uploadId: this.getNewUploadId(),
          uploadProgress: 0,
          destinationFolder: this.pathDestinationFolder,
          pathDestinationFolderForFile: '',
          isPublic: true,
          signal: signal
        });
      });

      const existingAttachedFiles = newAttachedFiles.filter(file => this.attachments.some(f => f.title === file.title));
      if (existingAttachedFiles.length > 0) {
        const existingFiles = existingAttachedFiles.length === 1 ? existingAttachedFiles.map(file => file.title) : existingAttachedFiles.length;
        let sameFileErrorMessage = existingAttachedFiles.length === 1 ? this.$t('attachments.drawer.sameFile.error') : this.$t('attachments.drawer.sameFiles.error');
        sameFileErrorMessage = sameFileErrorMessage.replace('{0}', `<b> ${existingFiles} </b>`);
        document.dispatchEvent(new CustomEvent('alert-message', {detail: {
          useHtml: true,
          alertType: 'error',
          alertMessage: sameFileErrorMessage,
        }}));
      }

      newAttachedFiles.filter(file => !this.attachments.some(f => f.title === file.title)).every((newFile, index) => {
        if (index === this.maxFilesCount || this.maxFilesCount === 0 || this.uploadedFilesCount >= this.maxFilesCount) {
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
            useHtml: true,
            alertType: 'error',
            alertMessage: this.maxFileCountErrorLabel,
          }}));
          return false;
        } else {
          this.queueUpload(newFile);
          return true;
        }
      });
      this.$refs.uploadInput.value = null;
    },
    queueUpload: function (file) {
      const fileSizeInMb = file.size / this.BYTES_IN_MB;
      if (fileSizeInMb > this.maxFileSize) {
        document.dispatchEvent(new CustomEvent('alert-message', {detail: {
          useHtml: true,
          alertType: 'error',
          alertMessage: this.maxFileSizeErrorLabel
        }}));
        return;
      }
      this.checkExistenceActions(file.title).then(actions => {
        if (actions.length > 0) {
          file.actions = actions;
          file.waitAction = true;
          this.$root.$emit('alert-message', this.$root.$t('attachments.upload.conflict.message'), 'warning');
          this.$root.$emit('start-loading-attachment-drawer');
        }
        this.$root.$emit('add-new-uploaded-file', file);
        this.newUploadedFiles.push(file);

        if (this.uploadingCount < this.maxUploadInProgressCount) {
          this.sendFileToServer(file);
        } else {
          this.uploadingFilesQueue.push(file);
        }
      });
    },
    checkExistenceActions(fileName) {
      const actions = [];
      return this.$attachmentService.checkExistence(this.currentDrive.name, 'collaboration', this.pathDestinationFolder, fileName).then((data) => {
        const exist = data && data.firstChild;
        const versioned = exist && exist.firstChild;
        if (exist && exist.tagName === 'Existed') {
          actions.push('keepBoth');
        }
        if (versioned && (versioned.tagName === 'Versioned' || versioned.tagName === 'CanVersioning')) {
          actions.push('createVersion');
        }
        return actions;
      });
    },
    sendFileToServer(file, continueAction) {
      if (!file.aborted && !file.waitAction) {
        this.uploadingCount++;
        this.$uploadService.upload(file.originalFileObject, file.uploadId, file.signal)
          .then(() => delete file.originalFileObject)
          .catch(() => {
            this.$root.$emit('alert-message', this.$t('attachments.link.failed'), 'error');
            this.removeAttachedFile(file);
          });
        this.controlUpload(file, continueAction);
      } else {
        this.processNextQueuedUpload();
      }
    },
    controlUpload(file, continueAction) {
      if (file.aborted) {
        this.uploadingCount--;
        this.processNextQueuedUpload();
      } else {
        if (file.uploadId) {
          window.setTimeout(() => {
            this.$uploadService.getUploadProgress(file.uploadId)
              .then(percent => {
                if (this.abortUploading) {
                  return;
                } else {
                  file.uploadProgress = file.inProcess && 100 || Number(percent);
                  if (!file.uploadProgress || file.uploadProgress < 100) {
                    this.controlUpload(file);
                  } else {
                    this.uploadingCount--;
                    this.processNextQueuedUpload();
                  }
                  if (file.uploadProgress === 100 && continueAction && !file.inProcess) {
                    file.inProcess = true;
                    this.$root.$emit('continue-upload-to-destination-path', file);
                    const index = this.newUploadedFiles.findIndex(f => f.id === file.id);
                    this.newUploadedFiles.splice(index, 1);
                  }
                }
              })
              .catch(() => {
                this.removeAttachedFile(file);
                this.$root.$emit('alert-message', this.$t('attachments.link.failed'), 'error');
              });
          }, 200);
        }
      }
    },
    processNextQueuedUpload: function () {
      if (this.uploadingFilesQueue.length > 0) {
        this.sendFileToServer(this.uploadingFilesQueue.shift());
      }
    },
    getNewUploadId: function () {
      const maxUploadId = 100000;
      return Math.floor(Math.random() * maxUploadId);
    },
    removeAttachedFile(file) {
      this.$root.$emit('remove-attachment-item', file);
    },
    resetUploadInput() {
      this.newUploadedFiles = [];
      this.uploadingCount = 0;
      this.uploadedFilesCount = 0;
    },
    abortUploadingNewAttachments() {
      this.resetUploadInput();
      this.abortUploading = true;
    },
    abortUploadingNewFile(file) {
      if (file && file.uploadId) {
        const fileIndex = this.newUploadedFiles.findIndex(f => f.uploadId === file.uploadId);
        this.newUploadedFiles.splice(fileIndex, 1);
      }
      file.aborted = true;
      this.$uploadService.abortUpload(file.uploadId);
    }
  }
};
</script>
