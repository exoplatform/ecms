<template>
  <div class="attachment">
    <v-list-item-avatar class="rounded-lg">
      <div v-if="file.uploadProgress !== 100" class="fileProgress">
        <v-progress-circular
          :rotate="-90"
          :size="40"
          :width="4"
          :value="file.uploadProgress"
          color="primary"
        >
          {{ file.uploadProgress }}
        </v-progress-circular>
      </div>
      <div v-else class="fileType">
        <i :class="getIconClassFromFileMimeType(file.mimetype)"></i>
      </div>
    </v-list-item-avatar>

    <v-list-item-content :class="!file.uploadId && 'serverFileContent'">
      <v-list-item-title :class="!file.uploadId && 'serverFileTitle'">{{ file.name }}</v-list-item-title>

      <v-list-item-subtitle class="d-flex">
        <v-chip
          v-if="file.pathDestinationFolderForFile && file.uploadId"
          close
          small
          class="attachment-location"
          @click="openSelectDestinationFolderForFile(file)"
        >
          {{ file.pathDestinationFolderForFile }}
        </v-chip>
        <a v-if="!file.pathDestinationFolderForFile && file.uploadId"
           :title="$t('attachments.drawer.destination.folder')"
           rel="tooltip" data-placement="top" class="attachmentDestinationPath primary--text"
           @click="openSelectDestinationFolderForFile(file)">Choose Location</a>
      </v-list-item-subtitle>
    </v-list-item-content>

    <v-list-item-action v-if="file.uploadId">
      <v-btn
        v-if="file.uploadProgress !== 100"
        outlined
        height="18"
        width="18"
        @click="removeItem(file)">
        <i class="uiIconCloseCircled error--text"></i>
      </v-btn>
      <v-btn
        v-else
        outlined
        height="24"
        width="24"
        @click="removeItem(file)">
        <i class="uiIconTrash error--text"></i>
      </v-btn>
    </v-list-item-action>
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
    getIconClassFromFileMimeType: function (fileMimeType) {
      if (fileMimeType) {
        const fileMimeTypeClass = fileMimeType.replace(/\./g, '').replace('/', '').replace('\\', '');
        return this.file.isCloudFile
          ? `uiIcon32x32${fileMimeType.replace(/[/.]/g, '')}`
          : `uiIconFileType${fileMimeTypeClass} uiIconFileTypeDefault`;
      } else {
        return '';
      }
    },
    getFormattedFileSize: function (fileSize) {
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
    removeItem(file) {
      this.$root.$emit('remove-attachment-item', file);
    },
    openSelectDestinationFolderForFile(file) {
      this.$root.$emit('change-attachment-destination-path', file);
    }
  }
};
</script>