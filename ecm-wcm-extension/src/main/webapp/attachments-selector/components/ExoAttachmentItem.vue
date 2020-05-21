<template>
  <div class="attachment">
    <div class="fileType">
      <i :class="getIconClassFromFileMimeType(file.mimetype)"></i>
    </div>
    <div class="fileDetails">
      <div class="fileDetails1">
        <div class="fileNameLabel" data-toggle="tooltip" rel="tooltip" data-placement="top" v-html="file.name"></div>
        <div class="fileSize">{{ getFormattedFileSize(file.size) }} {{ $t('attachments.composer.file.size.mega') }}</div>
      </div>
      <div v-if="file.uploadProgress" class="fileDetails2">
        <div v-show="!file.id" :class="[file.uploadProgress === 100 ? 'upload-completed': '']"
             class="progress">
          <div :style="'width:' + file.uploadProgress + '%'" class="bar"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    file: {
      type: Object,
      required: false,
      default: function () {
        return new Object();
      }
    }
  },
  data() {
    return {
      BYTES_IN_MB: 1048576,
    };
  },
  methods: {
    getIconClassFromFileMimeType: function(fileMimeType) {
      if(fileMimeType) {
        const fileMimeTypeClass = fileMimeType.replace(/\./g, '').replace('/', '').replace('\\', '');
        return this.file.isCloudFile 
          ? `uiIcon32x32${fileMimeType.replace(/[/.]/g, '')}` 
          : `uiIconFileType${fileMimeTypeClass} uiIconFileTypeDefault`;
      } else {
        return '';
      }
    },
    getFormattedFileSize: function(fileSize) {
      const formattedSizePrecision = 2;
      const sizeMB = fileSize / this.BYTES_IN_MB;
      return sizeMB.toFixed(formattedSizePrecision);
    },
  }
};
</script>