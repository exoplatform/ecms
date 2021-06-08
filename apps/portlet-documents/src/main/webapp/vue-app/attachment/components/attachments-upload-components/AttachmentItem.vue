<template>
  <div :class="allowToPreview && 'clickable'" class="attachment d-flex">
    <v-list-item-avatar
      :class="smallAttachmentIcon ? 'me-0' :'me-3'"
      class="border-radius"
      @click="openPreview()">
      <div v-if="attachment.uploadProgress < 100" class="fileProgress">
        <v-progress-circular
          :rotate="-90"
          :size="40"
          :width="4"
          :value="attachment.uploadProgress"
          color="primary">
          {{ attachment.uploadProgress }}
        </v-progress-circular>
      </div>
      <div
        v-else
        :class="smallAttachmentIcon && 'smallAttachmentIcon'"
        class="fileType">
        <i :class="getIconClassFromFileMimeType(attachment.mimetype)"></i>
      </div>
    </v-list-item-avatar>
    <v-list-item-content @click="openPreview()">
      <v-list-item-title class="uploadedFileTitle">
        {{ attachment.name }}
      </v-list-item-title>
      <v-list-item-subtitle v-if="attachment.uploadId" class="d-flex v-messages uploadedFileSubTitle">
        <v-chip
          v-if="attachment.pathDestinationFolderForFile"
          close
          small
          class="attachment-location px-2"
          @click:close="$root.$emit('remove-destination-for-file', attachment.name)"
          @click="openSelectDestinationFolderForFile(attachment)">
          {{ attachment.pathDestinationFolderForFile }}
        </v-chip>
        <a
          v-if="!attachment.pathDestinationFolderForFile"
          :title="$t('attachments.drawer.destination.folder')"
          rel="tooltip"
          data-placement="top"
          class="attachmentDestinationPath primary--text"
          @click="openSelectDestinationFolderForFile(attachment)">{{ $t('attachments.ChooseLocation') }}</a>
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action class="d-flex flex-row align-center">
      <v-icon
        v-if="attachment.isSelectedFromDrives && privateFilesAttached || fromAnotherSpaceAttachment || fromAnotherDriveAttachment"
        :title="attachmentPrivacyLabel"
        size="14"
        color="primary"
        depressed>
        fa-info-circle
      </v-icon>
      <v-btn
        v-if="attachment.uploadProgress && attachment.uploadProgress !== 100 && allowToRemove"
        class="d-flex flex-column pb-3 align-end"
        outlined
        x-small
        height="18"
        width="18"
        @click="confirmDeleteAttachment(attachment)">
        <i class="uiIconCloseCircled error--text"></i>
      </v-btn>
      <div
        v-else-if="allowToRemove"
        :class="!canRemoveAttachment && 'not-allowed'"
        :title="!canRemoveAttachment && $t('attachments.remove.notAuthorize')"
        class="remove-button">
        <v-btn
          :disabled="!canRemoveAttachment"
          class="d-flex flex-column pb-3 align-end"
          outlined
          x-small
          height="24"
          width="24"
          @click="deleteAttachment(attachment)">
          <i
            :class="!canRemoveAttachment && 'grey--text' || 'error--text'"
            class="uiIconTrash uiIcon24x24">
          </i>
        </v-btn>
      </div>
    </v-list-item-action>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :message="$t('attachments.message.confirmDeleteAttachment')"
      :title="$t('attachments.title.confirmDeleteAttachment')"
      :ok-label="$t('attachments.yes')"
      :cancel-label="$t('attachments.no')"
      @ok="confirmDeleteAttachment" />
  </div>
</template>
<script>
export default {
  props: {
    attachment: {
      type: Object,
      default: () => null
    },
    allowToRemove: {
      type: Boolean,
      default: true
    },
    allowToPreview: {
      type: Boolean,
      default: false
    },
    smallAttachmentIcon: {
      type: Boolean,
      default: false
    },
    currentSpace: {
      type: {},
      default: () => null
    },
    currentDrive: {
      type: {},
      default: () => null
    },
  },
  data() {
    return {
      BYTES_IN_KB: 1024,
      BYTES_IN_MB: 1048576,
      BYTES_IN_GB: 1073741824,
      MB_IN_GB: 10,
      measure: 'bytes'
    };
  },
  computed: {
    fromAnotherSpaceAttachment() {
      return this.attachmentSpaceId && this.attachmentSpaceId !== this.currentSpaceId && this.attachmentSpaceId || false;
    },
    fromAnotherDriveAttachment() {
      return this.attachmentCurrentDriveName && this.currentDriveName !== this.attachmentCurrentDriveName && !this.attachmentSpaceId || false;
    },
    privateFilesAttached() {
      return this.attachment && !this.attachment.isPublic && this.attachmentCurrentDriveName === 'Personal Documents';
    },
    selectedFromOtherDriveLabel() {
      return this.$t(`attachments.alert.sharing.${this.otherDriveType}`);
    },
    otherDriveType() {
      return this.privateFilesAttached ? 'personal' : this.fromAnotherSpaceAttachment ? 'space' : this.fromAnotherDriveAttachment ? 'otherDrive' : '';
    },
    attachmentSpaceDisplayName() {
      return this.attachment && this.attachment.space && this.attachment.space.title;
    },
    attachmentCurrentDriveName() {
      return this.attachment && this.attachment.fileDrive && this.attachment.fileDrive.title;
    },
    currentSpaceId() {
      return this.currentSpace && this.currentSpace.groupId && this.currentSpace.groupId.split('/spaces/')[1];
    },
    currentDriveName() {
      return this.currentDrive && this.currentDrive.title;
    },
    attachmentSpaceId() {
      return this.attachment && this.attachment.space && this.attachment.space.name && this.attachment.space.name.split('.spaces.')[1];
    },
    attachedFromOtherDrivesLabel() {
      return `${this.$t('attachments.alert.sharing.attachedFrom')} ${this.selectedFromOtherDriveLabel} ${this.fromAnotherSpaceAttachment && this.attachmentSpaceDisplayName || this.fromAnotherDriveAttachment && this.attachmentCurrentDriveName || ''}.`;
    },
    attachmentsWillBeDisplayedForLabel() {
      return this.$t('attachments.alert.sharing.availableFor');
    },
    attachmentPrivacyLabel() {
      return `${this.attachedFromOtherDrivesLabel} ${this.attachmentsWillBeDisplayedForLabel}`;
    },
    canRemoveAttachment() {
      return this.attachment && this.attachment.acl && this.attachment.acl.canDelete || !this.attachment.id || this.attachment.isSelectedFromDrives;
    },
  },
  methods: {
    getIconClassFromFileMimeType: function (fileMimeType) {
      if (fileMimeType) {
        const fileMimeTypeClass = fileMimeType.replace(/\./g, '').replace('/', '').replace('\\', '');
        return this.attachment.isCloudFile
          ? `uiIcon32x32${fileMimeType.replace(/[/.]/g, '')}`
          : `uiIconFileType${fileMimeTypeClass} uiIconFileTypeDefault`;
      } else {
        return 'uiIconFileTypeDefault';
      }
    },
    deleteAttachment() {
      if (!this.attachment.id || this.attachment.isSelectedFromDrives) {
        this.confirmDeleteAttachment();
      } else if (this.canRemoveAttachment) {
        this.$refs.deleteConfirmDialog.open();
      }
    },
    confirmDeleteAttachment() {
      if (this.canRemoveAttachment) {
        this.$root.$emit('remove-attachment-item', this.attachment);
      }
    },
    openSelectDestinationFolderForFile(attachment) {
      this.$root.$emit('change-attachment-destination-path', attachment);

    },
    absoluteDateModified(options) {
      const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';
      return new Date(this.attachment.date).toLocaleString(lang, options).split('/').join('-');
    },
    fileInfo() {
      return `${this.$t('documents.preview.updatedOn')} ${this.absoluteDateModified()} ${this.$t('documents.preview.updatedBy')} ${this.attachment.lastEditor} ${this.attachment.size}`;
    },
    openPreview() {
      if (this.allowToPreview && this.attachment.id) {
        const self = this;
        window.require(['SHARED/documentPreview'], function (documentPreview) {
          documentPreview.init({
            doc: {
              id: self.attachment.id,
              repository: 'repository',
              workspace: 'collaboration',
              path: self.attachment.nodePath || self.attachment.path,
              title: self.attachment.title,
              downloadUrl: self.attachment.downloadUrl,
              openUrl: self.attachment.url || self.attachment.openUrl,
              breadCrumb: self.attachment.previewBreadcrumb,
              fileInfo: self.fileInfo()
            },
            version: {
              number: self.attachment.version
            },
            showComments: false
          });
        });
      }
    }
  }
};
</script>