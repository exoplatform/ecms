<template>
  <div class="attachment">
    <div class="fileType">
      <i :class="getIconClassFromFileMimeType(file.mimetype)"></i>
    </div>
    <div class="fileDetails">
      <div class="fileDetails1">
        <div class="fileNameLabel" data-toggle="tooltip" rel="tooltip" data-placement="top" v-html="file.name"></div>
        <div class="fileSize">{{ getFormattedFileSize(file.size) }} {{ $t(`attachments.composer.file.size.${measure}`) }}</div>
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
      BYTES_IN_KB: 1024,
      BYTES_IN_MB: 1048576,
      BYTES_IN_GB: 1073741824,
      MB_IN_GB: 10,
      measure: 'bytes'
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
      const minGb = this.MB_IN_GB * this.BYTES_IN_MB; // equals 0.01 GB
      const minMb = 10000; // equals 0.01 MB, which is the smallest number with precision `formattedSizePrecision`
      const minKb = 10; // equals 0.01 KB, which is the smallest number with precision `formattedSizePrecision`
      let size = fileSize;
      if (fileSize < minKb) {
        this.measure = 'bytes';
      } else if (fileSize < minMb) {
        size = fileSize / this.BYTES_IN_KB;
        this.measure = 'kilo';
      } else if (fileSize < minGb) {
        size = fileSize / this.BYTES_IN_MB;
        this.measure = 'mega';
      } else {
        size = fileSize / this.BYTES_IN_GB;
        this.measure = 'giga';
      }
      return (+size).toFixed(formattedSizePrecision);
    },
  }
};
</script>