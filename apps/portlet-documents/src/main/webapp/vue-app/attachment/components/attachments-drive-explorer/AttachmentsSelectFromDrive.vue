<template>
  <div class="select-from-drives">
    <div class="d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">{{ $t('attachments.platformDocuments') }}</v-subheader>
      <v-divider />
    </div>
    <div class="lastContent d-flex align-center justify-center">
      <v-tooltip
        bottom>
        <template #activator="{ on, attrs }">
          <v-btn
            v-bind="attrs"
            v-on="on"
            class="uploadButton d-flex align-center"
            color="primary"
            text
            @click="openSelectFromDrivesDrawer()">
            <i class="uiIcon32x32FolderDefault uiIcon32x32LightGray"></i>
            <span class="ms-1">{{ $t('attachments.drawer.existingUploads') }}</span>
          </v-btn>
        </template>
        {{ $t('attachments.drawer.selectFromExistingUploads') }}
      </v-tooltip>
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