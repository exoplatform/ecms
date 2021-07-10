<template>
  <v-list-item
    :id="id"
    class="py-1"
    @click="openPreview">
    <v-list-item-icon class="me-3">
      <v-icon :class="attachment.icon" size="41" />
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title class="font-weight-bold">
        <ellipsis
          :title="attachment.name"
          :data="attachment.name"
          :line-clamp="2"
          end-char="..."
          class="text-color ma-0 text-wrap" />
      </v-list-item-title>
    </v-list-item-content>
    <v-list-item-action
      v-if="invalid || loading"
      class="my-auto">
      <v-progress-circular
        v-if="loading"
        color="primary"
        indeterminate />
      <v-tooltip v-else bottom>
        <template #activator="{ on, attrs }">
          <v-icon
            color="error"
            v-bind="attrs"
            v-on="on">
            fa-exclamation-circle
          </v-icon>
        </template>
        {{ $t('attachments.alert.unableToAccessFile') }}
      </v-tooltip>
    </v-list-item-action>
  </v-list-item>
</template>

<script>
export default {
  props: {
    attachment: {
      type: Object,
      default: null,
    },
    activity: {
      type: Object,
      default: null,
    },
    index: {
      type: Number,
      default: 0,
    },
    count: {
      type: Number,
      default: 0,
    },
  },
  data: () => ({
    loading: false,
    invalid: false,
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric',
    },
  }),
  computed: {
    id() {
      return `PreviewAttachment_${this.activity.id}_${this.index}`;
    },
    nextId() {
      return (this.index + 1) < this.count && `#PreviewAttachment_${this.activity.id}_${this.index + 1}` || '';
    },
    previousId() {
      return this.index && `#PreviewAttachment_${this.activity.id}_${this.index - 1}` || '';
    },
    username() {
      return this.activity && this.activity.identity.profile && this.activity.identity.profile.username || '';
    },
    fullname() {
      return this.activity && this.activity.identity.profile && this.activity.identity.profile.fullname || '';
    },
    avatarUrl() {
      return this.activity && this.activity.identity.profile && this.activity.identity.profile.avatar || '';
    },
    profileUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/profile/${this.username}`;
    },
    author() {
      return {
        username: this.username,
        fullname: this.fullname,
        avatarUrl: this.avatarUrl,
        profileUrl: this.profileUrl,
      };
    },
    icon() {
      return this.attachment.icon;
    },
    activityDate() {
      return this.activity && this.activity.createDate && new Date(this.activity.createDate);
    },
    relativePostTimeLabel() {
      return this.activityDate && this.$dateUtil.getRelativeTimeLabelKey(this.activityDate) || '';
    },
    relativePostTimeDate() {
      return this.activityDate && this.$dateUtil.getRelativeTimeValue(this.activityDate) || 1;
    },
    relativePostTime() {
      return this.activityDate && this.$t(this.relativePostTimeLabel, {0: this.relativePostTimeDate}) || '';
    },
    spaceURL() {
      return this.activity && this.activity.activityStream && this.activity.activityStream.space && this.activity.activityStream.space.groupId.replace('/spaces/', '');
    },
  },
  methods: {
    openPreview() {
      this.loading = true;
      this.$attachmentService.getAttachmentById(this.attachment.id)
        .then(attachment => {
          const updaterFullName = attachment && attachment.updater && attachment.updater.profile && attachment.updater.profile.fullname || '';
          const updateDate = new Date(attachment.updated);
          const updateDateInfo = this.$dateUtil.formatDateObjectToDisplay(updateDate, this.dateFormat);
          const fileInfo = `${this.$t('documents.preview.updatedOn')} ${updateDateInfo} ${this.$t('documents.preview.updatedBy')} ${updaterFullName} ${attachment.size}`;
          documentPreview.init({
            doc: {
              id: this.attachment.id,
              repository: this.attachment.repository,
              workspace: this.attachment.workspace,
              path: this.attachment.path,
              title: this.attachment.name,
              icon: this.icon,
              size: attachment.size,
              openUrl: attachment.openUrl,
              downloadUrl: attachment.downloadUrl,
              breadCrumb: attachment.previewBreadcrumb,
              fileInfo,
            },
            author: this.author,
            activity: {
              id: this.activity.id,
              liked: !!this.activity.length,
              likes: this.activity.length,
              status: this.activity.title,
              postTime: this.relativePostTime,
              spaceURL: this.spaceURL,
              next: this.nextId,
              previous: this.previousId,
            },
            version: {
              number: attachment.version && Number(attachment.version) || 0,
            }
          });
        })
        .catch(e => {
          console.error(e);
          this.invalid = true;
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>