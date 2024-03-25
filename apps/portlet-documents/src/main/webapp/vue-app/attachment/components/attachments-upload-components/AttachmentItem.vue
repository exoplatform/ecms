<template>
  <v-main>
    <div :class="allowToPreview && 'clickable'" class="attachment d-flex">
      <v-list-item-avatar
        :class="smallAttachmentIcon ? 'me-0' :'me-3'"
        class="border-radius"
        @click="openFile()">
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
          <v-icon
            size="41"
            :color="icon.color">
            {{ icon.class }}
          </v-icon>
        </div>
      </v-list-item-avatar>
      <v-list-item-content @click="openFile()">
        <v-list-item-title class="uploadedFileTitle" :title="attachmentTitle">
          {{ attachmentTitle || notAccessibleAttachmentTitle }}
        </v-list-item-title>
        <v-list-item-subtitle v-if="canMoveAttachment" class="d-flex v-messages uploadedFileSubTitle">
          <v-chip
            v-if="attachment.pathDestinationFolderForFile"
            close
            small
            class="attachment-location px-2"
            @click:close="$root.$emit('remove-destination-for-file', attachment.id)"
            @click="openSelectDestinationFolderForFile(attachment)">
            {{ attachment.pathDestinationFolderForFile }}
          </v-chip>
          <a
            v-if="!attachment.pathDestinationFolderForFile"
            :title="$t('attachments.ChangeLocation')"
            rel="tooltip"
            data-placement="top"
            class="attachmentDestinationPath primary--text"
            @click="openSelectDestinationFolderForFile(attachment)">{{ $t('attachments.ChangeLocation') }}</a>
        </v-list-item-subtitle>
      </v-list-item-content>
      <v-list-item-action class="d-flex flex-row align-center">
        <v-icon
          v-if="attachment.isSelectedFromDrives && fromAnotherSpaceAttachment || fromAnotherDriveAttachment"
          :title="attachmentPrivacyLabel"
          size="14"
          color="primary"
          depressed>
          fa-info-circle
        </v-icon>
        <v-icon
          v-if="!canAccess"
          :title="notAccessibleAttachmentTooltip"
          size="14"
          color="primary"
          depressed>
          fa-info-circle
        </v-icon>
        <v-btn
          v-if="attachmentInProgress"
          class="d-flex align-end"
          outlined
          x-small
          height="18"
          width="18"
          @click="detachFile(attachment)">
          <i class="uiIconCloseCircled error--text"></i>
        </v-btn>
        <div
          v-if="allowToDetach && canAccess"
          :class="!canDetachAttachment && 'not-allowed'"
          :title="!canDetachAttachment && $t('attachments.remove.notAuthorize') || $t('attachment.detach')"
          class="remove-button">
          <v-btn
            :disabled="!canDetachAttachment"
            class="d-flex"
            outlined
            x-small
            height="24"
            width="24"
            @click="detachFile(attachment)">
            <v-icon
              :class="!canDetachAttachment && 'grey--text' || 'error--text'"
              small
              class="fas fa-unlink" />
          </v-btn>
        </div>
      </v-list-item-action>
    </div>
    <div
      class="d-flex">
      <p
        class="docActionItem me-4 ml-4 clickable"
        v-for="action in attachment.actions"
        @click="$emit(`${action}`, attachment)"
        :key="action">
        {{ $t(`attachments.upload.action.${action}`) }}
      </p>
    </div>
  </v-main>
</template>
<script>
export default {
  props: {
    attachment: {
      type: Object,
      default: () => null
    },
    allowToDetach: {
      type: Boolean,
      default: true
    },
    allowToEdit: {
      type: Boolean,
      default: true
    },
    allowToPreview: {
      type: Boolean,
      default: false
    },
    openInEditor: {
      type: Boolean,
      default: false
    },
    isFileEditable: {
      type: Boolean,
      default: false
    },
    canEdit: {
      type: Boolean,
      default: false
    },
    entityId: {
      type: String,
      default: ''
    },
    canAccess: {
      type: Boolean,
      default: true
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
    selectedFromOtherDriveLabel() {
      return this.$t(`attachments.alert.sharing.${this.otherDriveType}`);
    },
    otherDriveType() {
      return this.fromAnotherSpaceAttachment ? 'space' : this.fromAnotherDriveAttachment ? 'otherDrive' : '';
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
    canDetachAttachment() {
      return this.attachmentHasPermission && this.attachmentHasPermission.canDetach || !this.attachment.id || this.attachment.isSelectedFromDrives || !this.entityId;
    },
    canMoveAttachment() {
      return this.canEdit && this.allowToEdit && !this.attachment.isSelectedFromDrives;
    },
    attachmentHasPermission() {
      return this.attachment && this.attachment.acl;
    },
    notAccessibleAttachmentTitle() {
      return !this.canAccess && this.$t('attachment.notAccessible.title') || '';
    },
    notAccessibleAttachmentTooltip() {
      return this.$t('attachment.notAccessible.tooltip');
    },
    attachmentInProgress() {
      return this.attachment.uploadProgress < 100;
    },
    attachmentTitle() {
      return this.attachment && this.attachment.title && unescape(this.attachment.title);
    },
    icon() {
      const type = this.attachment && this.attachment.mimetype || '';
      if (type.includes('pdf')) {
        return {
          class: 'fas fa-file-pdf',
          color: '#FF0000',
        };
      } else if (type.includes('presentation') || type.includes('powerpoint')) {
        return {
          class: 'fas fa-file-powerpoint',
          color: '#CB4B32',
        };
      } else if (type.includes('sheet') || type.includes('excel') || type.includes('csv')) {
        return {
          class: 'fas fa-file-excel',
          color: '#217345',
        };
      } else if (type.includes('word') || type.includes('opendocument') || type.includes('rtf')) {
        return {
          class: 'fas fa-file-word',
          color: '#2A5699',
        };
      } else if (type.includes('plain')) {
        return {
          class: 'fas fa-file-alt',
          color: '#385989',
        };
      } else if (type.includes('image')) {
        return {
          class: 'fas fa-file-image',
          color: '#999999',
        };
      } else if (type.includes('video') || type.includes('octet-stream') || type.includes('ogg')) {
        return {
          class: 'fas fa-file-video',
          color: '#79577A',
        };
      } else if (type.includes('zip') || type.includes('war') || type.includes('rar')) {
        return {
          class: 'fas fa-file-archive',
          color: '#717272',
        };
      } else if (type.includes('illustrator') || type.includes('eps')) {
        return {
          class: 'fas fa-file-contract',
          color: '#E79E24',
        };
      } else if (type.includes('html') || type.includes('xml') || type.includes('css')) {
        return {
          class: 'fas fa-file-code',
          color: '#6cf500',
        };
      } else {
        return {
          class: 'fas fa-file',
          color: '#476A9C',
        };
      }
    }
  },
  watch: {
    attachmentInProgress(newVal) {
      if (!newVal) {
        this.$root.$emit('end-loading-attachment-drawer');
      }
    },
  },
  methods: {
    markDocumentAsViewed() {
      document.dispatchEvent(new CustomEvent('mark-attachment-as-viewed', {detail: {file: this.attachment}}));
    },
    detachFile() {
      if (this.canDetachAttachment) {
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
    openFileInEditor() {
      if (this.attachment && this.attachment.id) {
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/oeditor?docId=${this.attachment.id}`, '_blank');
      }
    },
    openFile() {
      if (this.openInEditor && this.isFileEditable && this.attachment.acl?.canEdit) {
        this.openFileInEditor();
      } else {
        this.openPreview();
      }
    },
    openPreview() {
      if (this.allowToPreview && this.attachment.id) {
        const self = this;
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
            fileInfo: self.fileInfo(),
            size: self.attachment.size,
            isCloudDrive: self.attachment.cloudDrive

          },
          version: {
            number: self.attachment.version
          },
          showComments: false
        });
      }
      this.markDocumentAsViewed();
    }
  }
};
</script>
