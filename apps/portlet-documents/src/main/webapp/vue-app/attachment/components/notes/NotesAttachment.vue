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
  data: () => ({
    attachments: [],
    attachedFiles: {
      type: Array,
      default: () => []
    }
  }),
  created() {
    document.addEventListener('open-notes-attachments', () => this.openAttachmentDrawer());
  },
  methods: {
    buildAttachmentDrawerParams() {
      return {
        entityType: this.entityType,
        entityId: this.entityId,
        sourceApp: 'noteEditor',
        attachments: this.attachments,
        spaceId: this.spaceId,
        displayUploadedFiles: true,
        defaultDrive: 'Activity Stream Documents',
        defaultFolder: 'Activity Stream Documents'

      };
    },
    retrieveAttachments() {
      if (this.entityId && this.entityType) {
        return this.$attachmentService.getEntityAttachments(this.entityType, this.entityId)
          .then(attachments => {
            attachments.forEach(attachments => {
              attachments.name = attachments.title;
            });
            this.attachments = attachments;
          });
      }
    },
    openAttachmentDrawer() {
      this.retrieveAttachments().then(() => {
        document.dispatchEvent(new CustomEvent('open-attachments-app-drawer', {detail: this.buildAttachmentDrawerParams()}));
      });
    },
  }
};
</script>