<template>
  <div class="attachment">
    <v-list-item-avatar class="rounded-lg">
      <div class="fileType">
        <i :class="getIconClassFromFileMimeType(file.mimetype)"></i>
      </div>
    </v-list-item-avatar>

    <v-list-item-content class="serverFileContent align-center">
      <v-list-item-title class="serverFileTitle'">
        {{ file.name }}
      </v-list-item-title>
    </v-list-item-content>
  </div>
</template>

<script>
export default {
  props: {
    file: {
      type: Object,
      default: () => null,
    }
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
    }
  }
};
</script>