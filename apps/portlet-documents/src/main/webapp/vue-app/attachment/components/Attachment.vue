<template>
  <v-app>
    <div
      :class="entityType && entityId && 'v-card__text pl-0'"
      class="attachments-application border-box-sizing transparent">
      <attachments-drawer
        ref="attachmentsAppDrawer"
        :attachments="attachments"
        :entity-has-attachments="entityHasAttachments"
        :entity-id="entityId"
        :entity-type="entityType"
        :default-drive="defaultDrive"
        :default-folder="defaultFolder"
        :current-space="currentSpace"
        :is-composer-attachment="isComposerAttachment" />
      <attachments-list-drawer
        ref="attachmentsListDrawer"
        :attachments="attachments"
        :is-composer-attachment="isComposerAttachment" />
      <attachments-notification-alerts />
    </div>
  </v-app>
</template>

<script>
export default {
  props: {
    attachmentAppConfiguration: {
      type: Object,
      default: () => null
    },
    attachments: {
      type: Array,
      default: () => []
    },
  },
  data () {
    return {
      currentSpace: {},
    };
  },
  computed: {
    defaultDrive() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.defaultDrive;
    },
    defaultFolder() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.defaultFolder;
    },
    spaceId() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.spaceId;
    },
    entityType() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.entityType;
    },
    entityId() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.entityId;
    },
    entityHasAttachments() {
      return this.attachments && this.attachments.length;
    },
    isComposerAttachment() {
      return this.attachmentAppConfiguration && this.attachmentAppConfiguration.isComposerAttachment;
    }
  },
  created() {
    this.$root.$on('entity-attachments-updated', () => this.initEntityAttachmentsList());
    this.$root.$on('remove-attachment-item', attachment => {
      this.removeAttachedFile(attachment);
    });
    this.$root.$on('remove-composer-attachment-item', attachment => {
      const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === attachment.id);
      this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
    });
    this.$root.$on('add-new-created-document', file => {
      this.attachments.push(file);
    });
    this.$root.$on('add-new-uploaded-file', file => {
      this.attachments.push(file);
    });
    this.$root.$on('attachments-changed-from-drives', (selectedFromDrives, removedFilesFromDrive) => {
      this.updateAttachmentsFromDrives(selectedFromDrives, removedFilesFromDrive);
    });
    this.$root.$on('add-destination-path-for-all', (defaultDestinationFolderPath, pathDestinationFolder, currentDrive) => {
      this.addDestinationFolderForAll(defaultDestinationFolderPath, pathDestinationFolder, currentDrive);
    });
    this.$root.$on('reset-attachment-list', () => {
      this.attachments = [];
    });
    document.addEventListener('open-attachments-app-drawer', (event) => {
      this.attachmentAppConfiguration = event.detail;
      if (!this.attachmentAppConfiguration) {
        if (eXo.env.portal.spaceDisplayName) {
          this.attachmentAppConfiguration = {
            'defaultDrive': {
              isSelected: true,
              name: `.spaces.${eXo.env.portal.spaceGroup}`,
              title: eXo.env.portal.spaceDisplayName,
            },
            'defaultFolder': 'Documents',
            'sourceApp': '',
          };
        } else {
          this.attachmentAppConfiguration = {
            'defaultDrive': {
              isSelected: true,
              name: 'Personal Documents',
              title: 'Personal Documents'
            },
            'defaultFolder': 'Documents',
            'sourceApp': '',
          };
        }
      } else {
        if (!this.attachmentAppConfiguration.defaultDrive) {
          if (eXo.env.portal.spaceDisplayName) {
            this.attachmentAppConfiguration.defaultDrive = {
              isSelected: true,
              name: `.spaces.${eXo.env.portal.spaceGroup}`,
              title: eXo.env.portal.spaceDisplayName,
            };
          } else {
            this.attachmentAppConfiguration.defaultDrive = {
              isSelected: true,
              name: 'Personal Documents',
              title: 'Personal Documents'
            };
          }
        }
        if (!this.attachmentAppConfiguration.defaultFolder) {
          this.attachmentAppConfiguration.defaultFolder = 'Documents';
        }
        if (!this.attachmentAppConfiguration.sourceApp) {
          this.attachmentAppConfiguration.sourceApp = '';
        } else {
          this.$root.$emit('set-source-app', this.attachmentAppConfiguration.sourceApp);
        }
      }
      this.attachments = this.attachmentAppConfiguration.attachments || [];
      this.openAttachmentsAppDrawer();
      this.initAttachmentEnvironment();
    });
    document.addEventListener('open-attachments-list-drawer', (event) => {
      this.attachmentAppConfiguration = event.detail;
      this.attachments = [];
      this.openAttachmentsDrawerList();
      this.initAttachmentEnvironment();
    });
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    initAttachmentEnvironment() {
      this.initDefaultDrive();
      if (this.entityType && this.entityId) {
        this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.startLoading();
        this.initEntityAttachmentsList().then(() => {
          this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.endLoading();
        });
      }
    },
    initEntityAttachmentsList() {
      if (this.entityType && this.entityId && !this.isComposerAttachment) {
        return this.$attachmentService.getEntityAttachments(this.entityType, this.entityId).then(attachments => {
          attachments.forEach(attachments => {
            attachments.name = attachments.title;
          });
          this.attachments = attachments;
        });
      }
    },
    openAttachmentsDrawerList() {
      this.$root.$emit('open-attachments-list-drawer');
    },
    removeAttachedFile: function (file) {
      if (!file.id) {
        const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.uploadId === file.uploadId);
        this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
        if (file.uploadProgress !== this.maxProgress) {
          this.$root.$emit('abort-uploading-new-file', file);
        }
      } else {
        this.$refs.attachmentsAppDrawer.$refs.attachmentsAppDrawer.startLoading();
        this.$attachmentService.removeEntityAttachment(this.entityId, this.entityType, file.id).then(() => {
          this.$root.$emit('remove-attached-file', file);
          const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id);
          this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
          this.$root.$emit('attachments-notification-alert', {
            message: this.$t('attachments.detach.success'),
            type: 'success',
          });
          this.initEntityAttachmentsList();
          document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
          this.$refs.attachmentsAppDrawer.$refs.attachmentsAppDrawer.endLoading();
        }).catch(e => {
          console.error(e);
          this.$root.$emit('attachments-notification-alert', {
            message: this.$t('attachments.delete.failed').replace('{0}', file.title),
            type: 'error',
          });
        });
      }
    },
    addDestinationFolderForAll(defaultDestinationFolderPath, pathDestinationFolder, currentDrive) {
      for (let i = 0; i < this.attachments.length; i++) {
        if (!this.attachments[i].destinationFolder || this.attachments[i].destinationFolder === defaultDestinationFolderPath) {
          this.attachments[i].destinationFolder = pathDestinationFolder;
          this.attachments[i].fileDrive = currentDrive;
        }
      }
    },
    initDefaultDrive() {
      const spaceId = this.getURLQueryParam('spaceId') ? this.getURLQueryParam('spaceId') :
        `${eXo.env.portal.spaceId}` ? `${eXo.env.portal.spaceId}` :
          this.attachmentAppConfiguration.spaceId;
      if (spaceId) {
        this.$attachmentService.getSpaceById(spaceId).then(space => {
          if (space) {
            this.currentSpace = space;
            const spaceGroupId = space.groupId.split('/spaces/')[1];
            this.attachmentAppConfiguration.defaultDrive = {
              name: `.spaces.${spaceGroupId}`,
              title: spaceGroupId,
              isSelected: true
            };
          }
        });
      } else if (this.attachmentAppConfiguration.entityId && this.attachmentAppConfiguration.entityType) {
        this.attachmentAppConfiguration.defaultDrive = {
          isSelected: true,
          name: 'Personal Documents',
          title: 'Personal Documents'
        };
        this.attachmentAppConfiguration.defaultFolder = 'Public';
      }
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    },
    updateAttachmentsFromDrives(selectedFromDrives, removedFilesFromDrive) {
      if (selectedFromDrives.length) {
        this.attachments.push(...selectedFromDrives);
      }
      if (removedFilesFromDrive.length) {
        removedFilesFromDrive.forEach(attachment => {
          const attachmentIndex = this.attachments.findIndex(file => file.id === attachment.id);
          if (attachmentIndex !== -1) {
            this.attachments.splice(attachmentIndex, 1);
          }
        });
      }
      this.$root.$emit('entity-attachments-updated', this.attachments);
    }
  }
};
</script>