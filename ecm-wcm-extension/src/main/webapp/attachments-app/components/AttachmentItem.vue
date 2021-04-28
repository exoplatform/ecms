<template>
  <div class="attachment">
    <v-list-item-avatar class="rounded-lg me-3">
      <div v-if="file.uploadProgress < 100" class="fileProgress">
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
    <v-list-item-content>
      <v-list-item-title class="uploadedFileTitle">{{ file.name }}</v-list-item-title>
      <v-list-item-subtitle v-if="file.uploadId" class="d-flex uploadedFileSubTitle">
        <v-chip
          v-if="file.pathDestinationFolderForFile"
          close
          small
          class="attachment-location px-2"
          @click:close="$root.$emit('remove-destination-for-file', file.name)"
          @click="openSelectDestinationFolderForFile(file)"
        >
          {{ file.pathDestinationFolderForFile }}
        </v-chip>
        <a v-if="!file.pathDestinationFolderForFile"
           :title="$t('attachments.drawer.destination.folder')"
           rel="tooltip" data-placement="top" class="attachmentDestinationPath primary--text"
           @click="openSelectDestinationFolderForFile(file)">Choose Location</a>
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action>
      <v-btn
        v-if="file.uploadProgress && file.uploadProgress !== 100 && allowToRemove"
        class="d-flex flex-column pb-3 pa-0 align-end"
        outlined
        height="18"
        width="18"
        @click="removeItem(file)">
        <i class="uiIconCloseCircled error--text"></i>
      </v-btn>
      <v-btn
        v-else-if="allowToRemove"
        class="d-flex flex-column pb-3 pa-0 align-end"
        outlined
        height="24"
        width="24"
        @click="removeItem(file)">
        <i class="uiIconTrash uiIcon24x24 error--text"></i>
      </v-btn>
    </v-list-item-action>
  </div>
</template>
<script>
export default {
  props: {
    file: {
      type: Object,
      default: () => null
    },
    allowToRemove: {
      type: Boolean,
      default: true
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
        return 'uiIconFileTypeDefault';
      }
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