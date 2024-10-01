<template>
  <div class="NoteAttachments">
    <attachment-app
      v-if="entityId > 0 && entityType && spaceId"
      :entity-id="entityId"
      :space-id="spaceId"
      :default-folder="'Activity Stream Documents'"
      :display-uploaded-files="true"
      :create-entity-type-folder="false"
      :show-drawer-overlay="true"
      :entity-type="processedEntityType">
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
    lang: {
      type: String,
      default: ''
    }
  },
  computed: {
    processedEntityType() {
      return this.entityType && this.lang && `${this.entityType}_${this.lang}` || this.entityType;
    }
  },
  watch: {
    entityId(newVal) {
      if (newVal > 0) {
        document.dispatchEvent(new CustomEvent('toggle-attach-button', {
          detail: { enable: true }
        }));
      } else {
        document.dispatchEvent(new CustomEvent('toggle-attach-button', {
          detail: { enable: false }
        }));
      }
    }
  },
  created() {
    //these events to handel the single and multiple files upload
    //synchronizing the draft saving with the editor
    document.addEventListener('new-file-upload-progress', this.emitEditorExtensionsUpdatingDataEvent);
    document.addEventListener('new-file-upload-done', this.emitEditorExtensionsDataUpdatedEvent);
    document.addEventListener('open-notes-attachments', () => this.openAttachmentDrawer());
    document.addEventListener('attachment-removed', () => this.emitEditorExtensionsDataUpdatedEvent());
    document.addEventListener('attachment-added-from-drives', () => this.emitEditorExtensionsDataUpdatedEvent());
  },
  methods: {
    openAttachmentDrawer() {
      if (this.entityId > 0 && this.entityType && this.spaceId){
        this.$refs.openAttachmentDrawerButton.click();
      }
    },
    emitEditorExtensionsUpdatingDataEvent() {
      const eventDetails = {
        showAutoSaveMessage: true,
        processAutoSave: this.entityType === 'Page'
      };
      document.dispatchEvent(new CustomEvent('editor-extensions-data-start-updating', eventDetails));
    },
    emitEditorExtensionsDataUpdatedEvent() {
      const eventDetails = {
        showAutoSaveMessage: true,
        processAutoSave: this.entityType === 'Page'
      };
      document.dispatchEvent(new CustomEvent('editor-extensions-data-updated', eventDetails));
    }
  }
};
</script>