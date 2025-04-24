<template>
  <v-app>
    <div>
      <div
        v-if="displayAttachments"
        class="actionItem action clickable pb-1 pt-3"
        @click="openAttachmentListDrawer">
        <v-icon
          size="14"
          color="primary"
          class="pe-1">
          fa-paperclip
        </v-icon>
        <a class="viewAllAttachments primary--text font-weight-bold text-decoration-underline">
          {{ $t('attachments.view.all') }} ({{ attachmentsLength }})
        </a>
      </div>
      <changes-reminder
        v-if="!isMobile"
        :reminder="reminder" />
    </div>
  </v-app>
</template>
<script>
export default {
  props: {
    activityId: {
      type: String,
      default: null,
    },
    spaceId: {
      type: String,
      default: null
    },
    message: {
      type: String,
      default: null,
    },
    maxMessageLength: {
      type: Number,
      default: 0,
    },
    templateParams: {
      type: Object,
      default: null,
    },
    files: {
      type: Array,
      default: () => []
    },
  },
  data: () => ({
    attachments: null,
    entityType: 'activity',
    reminder: {},
    attachedFiles: {
      type: Array,
      default: () => []
    }
  }),
  computed: {
    attachmentsLength() {
      return this.attachedFiles.length;
    },
    displayAttachments() {
      return this.attachmentsLength > 0;
    },
    attachmentDrawerParams() {
      return {
        entityType: this.entityType,
        entityId: '',
        defaultFolder: 'Activity Stream Documents',
        sourceApp: 'activityStream',
        attachments: this.attachments,
        spaceId: this.spaceId,
        attachToEntity: false, // Activity attachments are managed by composer instead of drawer
      };
    },
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs' || this.$vuetify.breakpoint.name === 'sm';
    },
  },
  watch: {
    files(files){
      this.attachedFiles = files;
    }
  },
  created() {
    document.addEventListener('open-activity-attachments', () => this.openAttachmentDrawer());
    document.addEventListener('attachment-added', event => this.addAttachment(event.detail));
    document.addEventListener('attachment-removed', event => this.removeAttachment(event.detail));
    document.addEventListener('message-composer-opened', () => this.openComposerChangesReminder());
  },
  methods: {
    retrieveAttachments() {
      this.attachedFiles = this.files;
      this.attachments = JSON.parse(JSON.stringify(this.attachedFiles));

      this.files.forEach((attachment, index) => {
        if (this.activityId) {
          this.$attachmentService.getAttachmentByEntityAndId(this.entityType, this.activityId, attachment.id)
            .then(fileAttachment => this.attachments.splice(index, 1, fileAttachment));
        } else {
          this.$attachmentService.getAttachmentById(attachment.id)
            .then(fileAttachment => this.attachments.splice(index, 1, fileAttachment));
        }
      });
    },
    openAttachmentDrawer() {
      this.retrieveAttachments();
      this.$nextTick()
        .then(() => {
          document.dispatchEvent(new CustomEvent('open-attachments-app-drawer', {detail: this.attachmentDrawerParams}));
        });
    },
    openAttachmentListDrawer() {
      this.retrieveAttachments();
      this.$nextTick()
        .then(() => {
          document.dispatchEvent(new CustomEvent('open-attachments-list-drawer', {detail: this.attachmentDrawerParams}));
        });
    },
    addAttachment(file) {
      this.attachedFiles.push(file.attachment);
      document.dispatchEvent(new CustomEvent('activity-composer-edited', {detail: this.attachedFiles}));
    },
    removeAttachment(file) {
      const index = this.attachedFiles.findIndex(attachment => attachment.id === file.id);
      if (index >= 0) {
        this.attachedFiles.splice(index, 1);
      }
      document.dispatchEvent(new CustomEvent('activity-composer-edited', {detail: this.attachedFiles}));
    },
    openComposerChangesReminder() {
      this.reminder = {
        'name': 'activityComposerAttachFile' ,
        'title': `${this.$t('activity.attach.file.reminder.title')}`,
        'description': `${this.$t('activity.attach.file.reminder.description')}`,
        'img': '/eXoWCMResources/skin/images/Icons/attachFileIcon.gif',
      };
      this.$nextTick()
        .then(() => {
          document.dispatchEvent(new CustomEvent('changes-reminder-open'));
        });
    },
  },
};
</script>
