<template>
  <div class="attachment">
    <a
      :title="file.name"
      rel="tooltip"
      class="fileTitle d-flex flex-column v-messages"
      data-placement="bottom">
      <div :class="file.isSelected && 'selected'" class="fileType d-flex">
        <div class="file-type-icon pa-0 col-9 text-right pt-2">
          <i :class="getIconClassFromFileMimeType(file.mimetype)"></i>
        </div>
        <v-icon
          v-show="file.isSelected"
          color="primary"
          class="align-self-start pe-1"
          size="18">fas fa-check-square
        </v-icon>
      </div>
      <div class="selectionLabel text-truncate text-color center">{{ file.name }}</div>
    </a>
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