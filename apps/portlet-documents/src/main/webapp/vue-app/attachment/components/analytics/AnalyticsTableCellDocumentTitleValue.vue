<template>
  <v-progress-circular
    v-if="loading"
    size="24"
    color="primary"
    indeterminate />
  <div
    v-else-if="DocumentTitle"
    class="clickable float-left primary--text text-truncate"
    :style="`max-width: ${cellWidth}`"
    :title="DocumentTitle"
    @click="openPreview">
    {{ DocumentTitle }}
  </div>
  <div v-else-if="DocumentAccessDenied" class="d-flex">
    <i :title="$t('analytics.errorRetrievingDataForValue', {0: value})" class="uiIconColorError my-auto"></i>
    <span class="text-no-wrap text-sub-title my-auto ml-1">
      {{ $t('analytics.notAccessibleFile') }}
    </span>
  </div>
  <div v-else class="d-flex">
    <i :title="$t('analytics.errorRetrievingDataForValue', {0: value})" class="uiIconColorError my-auto"></i>
    <span class="text-no-wrap text-sub-title my-auto ml-1">
      {{ $t('analytics.DeletedFile') }}
    </span>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      default: () => null,
    },
    column: {
      type: Object,
      default: () => null,
    },
  },
  data: () => ({
    loading: true,
    attachment: {},
  }),
  computed: {
    cellWidth() {
      return this.column && this.column.width || '30vw';
    },
    DocumentTitle() {
      return this.attachment && this.attachment.title && unescape(this.attachment.title);
    },
    DocumentAccessDenied() {
      return this.attachment && this.attachment.acl && !this.attachment.acl.canAccess;
    },
    currentLanguage() {
      return eXo && eXo.env && eXo.env.portal && eXo.env.portal.language.replace('_','-') || 'en';
    },
    absoluteDateModified(options) {
      return new Date(this.attachment.date).toLocaleString(this.currentLanguage, options).split('/').join('-');
    },
    fileInfo() {
      return `${this.$t('documents.preview.updatedOn')} ${this.absoluteDateModified} ${this.$t('documents.preview.updatedBy')} ${this.attachment.lastEditor} ${this.attachment.size}`;
    },
  },
  created() {
    if (this.value) {
      this.loading = true;
      this.error = false;
      this.$attachmentService.getAttachmentById(this.value)
        .then(attachment => {
          this.attachment = attachment;
        })
        .catch(() => this.attachment = {
          notFound: true,
          id: this.value
        })
        .finally(() => this.loading = false);
    } else {
      this.loading = false;
    }
  },
  methods: {
    openPreview() {
      documentPreview.init({
        doc: {
          id: this.attachment.id,
          repository: 'repository',
          workspace: 'collaboration',
          path: this.attachment.nodePath || this.attachment.path,
          title: this.attachment.title,
          downloadUrl: this.attachment.downloadUrl,
          openUrl: this.attachment.url || this.attachment.openUrl,
          breadCrumb: this.attachment.previewBreadcrumb,
          fileInfo: this.fileInfo,
          size: this.attachment.size
        },
        version: {
          number: this.attachment.version
        },
        showComments: false
      });
    },
  },
};
</script>
