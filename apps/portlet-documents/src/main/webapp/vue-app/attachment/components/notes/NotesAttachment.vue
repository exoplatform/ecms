<template>
  <div class="NoteAttachments">
    <attachment-app
        v-if="entityId > 0 && entityType && spaceId"
        :entity-id="entityId"
        :space-id="spaceId"
        :default-folder="'Activity Stream Documents'"
        :display-uploaded-files="true"
        :create-entity-type-folder="false"
        :entity-type="entityType">
    <template #attachmentsButton>
      <button
        ref="openAttachmentDrawerButton"
        @click="openAttachmentDrawer"
        class="button-hidden d-none"></button>
    </template>
    <template #attachedFilesList />
    </attachment-app>
  </div>
</template>
<script>

export default {
  props: {
    entityId: {
      type: String,
      default: null,
    },
    spaceId: {
      type: String,
      default: null,
    },
    entityType: {
      type: String,
      default: null,
    },
  },
  created() {
    document.addEventListener('open-notes-attachments', () => this.openAttachmentDrawer());
  },
  methods: {
    openAttachmentDrawer() {
      this.$refs.openAttachmentDrawerButton.click();
    },
  }
};
</script>