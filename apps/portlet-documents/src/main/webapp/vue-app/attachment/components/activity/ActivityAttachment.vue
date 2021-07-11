<template>
  <v-card
    :loading="loading"
    height="210px"
    max-height="210px"
    width="250px"
    max-width="100%"
    elevation="0"
    class="activity-attachment overflow-hidden mx-2 border-color d-flex flex-column"
    @click="openPreview">
    <v-card-text class="activity-attachment-thumbnail d-flex flex-grow-1 pa-0">
      <v-img
        v-if="image"
        :src="attachment.image"
        max-height="152px"
        max-width="250px"
        contain
        eager
        @error="image = null" />
      <v-icon
        v-else
        :class="attachment.icon"
        class="ma-auto d-flex"
        size="80px" />
    </v-card-text>
    <v-card-text class="activity-attachment-title d-flex font-weight-bold border-top-color py-2">
      <ellipsis
        :title="attachment.name"
        :data="attachment.name"
        :line-clamp="2"
        end-char="..."
        class="text-color text-wrap text-break mx-0 my-auto" />
    </v-card-text>
    <v-expand-transition>
      <v-card
        v-if="invalid"
        class="d-flex flex-column transition-fast-in-fast-out v-card--reveal"
        elevation="0"
        style="height: 100%;">
        <v-card-text class="pb-0 d-flex flex-row">
          <v-icon color="error">fa-exclamation-circle</v-icon>
          <p class="my-auto ms-2 font-weight-bold">
            {{ $t('attachments.errorAccessingFile') }}
          </p>
        </v-card-text>
        <v-card-text class="flex-grow-1">
          <p>{{ $t('attachments.alert.unableToAccessFile') }}</p>
        </v-card-text>
        <v-card-actions class="pt-0">
          <v-btn
            text
            color="primary"
            @click="closeErrorBox">
            {{ $t('attachments.close') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-expand-transition>
  </v-card>
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
    image: false,
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
  created() {
    this.image = this.attachment && this.attachment.image;
  },
  methods: {
    closeErrorBox(event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      window.setTimeout(() => {
        this.invalid = false;
      }, 50);
    },
    openPreview() {
      if (this.invalid) {
        return;
      }
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
