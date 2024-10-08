
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
    },
    isEmptyNoteTranslation: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      attachments: [],
      displayUploadedFiles: true,
      createEntityTypeFolder: false,
      displayCreateDocumentInput: false,
      originalAttachmentsList: [],
      attachmentListUpdated: false,
      isDrawerClosedEventHandled: false
    };
  },
  computed: {
    attachmentAppConfiguration() {
      return {
        'entityId': this.entityId,
        'entityType': this.entityType,
        'defaultDrive': null,
        'defaultFolder': 'Activity Stream Documents',
        'spaceId': this.spaceId,
        'attachments': this.attachments,
        'displayUploadedFiles': this.displayUploadedFiles,
        'createEntityTypeFolder': this.createEntityTypeFolder,
        'sourceApp': 'note',
        'showCustomDrawerOverlay': true,
        'attachToEntity': this.attachToEntity,
        'displayCreateDocumentInput': this.displayCreateDocumentInput
      };
    },
    attachToEntity() {
      return !!this.entityId && this.entityType !== 'WIKI_PAGE_VERSIONS';
    },
    processAutoSave() {
      return this.attachmentListUpdated && !this.attachToEntity;
    }
  },
  created() {
    document.addEventListener('open-notes-attachments', this.openAttachmentDrawer);
    document.addEventListener('attachments-app-drawer-closed', this.handleDrawerClosedEvent);
    document.addEventListener('note-draft-auto-save-done', (event) => {
      if (this.attachmentListUpdated && event.detail.draftId) {
        this.updateLinkedAttachmentsToEntity(event.detail.draftId);
      }
    });
    document.addEventListener('article-draft-auto-save-done', (event) => {
      if (this.attachmentListUpdated && event.detail.draftId) {
        this.updateLinkedAttachmentsToEntity(event.detail.draftId);
      }
    });
  },
  beforeDestroy() {
    document.removeEventListener('note-draft-auto-save-done');
    document.removeEventListener('open-notes-attachments');
    document.addEventListener('attachments-app-drawer-closed');
  },
  methods: {
    openAttachmentDrawer() {
      this.originalAttachmentsList = [];
      this.attachments = [];
      if (this.entityId > 0 && this.entityType && this.spaceId && !this.isEmptyNoteTranslation) {
        this.initEntityAttachmentsList().then(() => {
          document.dispatchEvent(new CustomEvent('open-attachments-app-drawer', {detail: this.attachmentAppConfiguration}));
        });
      } else {
        document.dispatchEvent(new CustomEvent('open-attachments-app-drawer', {detail: this.attachmentAppConfiguration}));
      }
    },
    handleDrawerClosedEvent() {
      if (!this.isDrawerClosedEventHandled) {
        this.emitEditorExtensionsDataUpdatedEvent(event);
        this.isDrawerClosedEventHandled = true;
        setTimeout(() => {
          this.isDrawerClosedEventHandled = false;
        }, 1000);
      }
    },
    emitEditorExtensionsDataUpdatedEvent() {
      const attachmentAdded = this.attachments.filter((item) => !this.originalAttachmentsList.some(originalItem => originalItem.id === item.id)).length > 0;
      const attachmentRemoved = this.originalAttachmentsList.filter((originalItem) => !this.attachments.some(item => item.id === originalItem.id)).length > 0;
      this.attachmentListUpdated = attachmentRemoved || attachmentAdded;
      document.dispatchEvent(new CustomEvent('note-editor-extensions-data-updated', {
        detail: {
          showAutoSaveMessage: true,
          processAutoSave: this.processAutoSave
        }
      }));
    },
    initEntityAttachmentsList() {
      if (this.entityType && this.entityId) {
        return this.$attachmentService.getEntityAttachments(this.entityType, this.entityId).then(attachments => {
          if (attachments && attachments.length) {
            attachments.forEach((attachment) => {
              attachment.name = attachments.title;
            });
            this.attachments.push(...attachments);
            this.originalAttachmentsList.push(...attachments);
          }
        });
      } else {return Promise.resolve();}
    },
    updateLinkedAttachmentsToEntity(entityId) {
      const attachmentIds = this.attachments.filter(attachment => attachment.id).map(attachment => attachment.id);
      if (attachmentIds.length === 0) {
        return this.$attachmentService.removeAllAttachmentsFromEntity(entityId, 'WIKI_DRAFT_PAGES').then(() => {
          document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
          this.attachmentListUpdated = false;
        }).catch(e => {
          console.error(e);
          this.$refs.attachmentsAppDrawer.endLoading();
          this.$root.$emit('alert-message', this.$t('attachments.link.failed'), 'error');
          this.attachmentListUpdated = false;
        });
      } else {
        return this.$attachmentService.updateLinkedAttachmentsToEntity(entityId, 'WIKI_DRAFT_PAGES', attachmentIds)
          .then(() => {
            document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
            this.attachmentListUpdated = false;
          })
          .catch(e => {
            this.attachmentListUpdated = false;
            console.error(e);
            this.$root.$emit('alert-message', this.$t('attachments.link.failed'), 'error');
          });
      }
    },
  }
};
</script>