<template>
  <div class="select-from-drives">
    <div class="d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">Platform Documents</v-subheader>
      <v-divider />
    </div>
    <div class="lastContent d-flex align-center justify-center">
      <a
        title="Select on server"
        class="uploadButton d-flex align-center"
        href="#"
        rel="tooltip"
        data-placement="bottom"
        @click="openSelectFromDrivesDrawer()">
        <i class="uiIcon32x32FolderDefault uiIcon32x32LightGray"></i>
        <v-icon
          color="#fff"
          x-small
          class="iconCloud">cloud</v-icon>
        <span class="text colorText">{{ $t('attachments.drawer.existingUploads') }}</span>
      </a>
    </div>
  </div>
</template>

<script>
export default {
  methods: {
    openSelectFromDrivesDrawer() {
      this.$root.$emit('open-select-from-drives');
    },
  }
};
</script>