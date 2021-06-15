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
        :current-space="currentSpace" />
      <attachments-list-drawer
        ref="attachmentsListDrawer"
        :attachments="attachments" />
      <attachments-notification-alerts style="z-index:1035;" />
    </div>
  </v-app>
</template>

<script>
export default {
  data () {
    return {
      attachments: [],
      attachmentAppConfiguration: {},
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
    }
  },
  created() {
    this.$root.$on('entity-attachments-updated', () => this.initEntityAttachmentsList());
    this.$root.$on('remove-attachment-item', attachment => {
      this.removeAttachedFile(attachment);
    });
    this.$root.$on('add-new-uploaded-file', file => {
      this.attachments.push(file);
    });
    this.$root.$on('attachments-changed-from-drives', (selectedFromDrives, removedFilesFromDrive) => {
      this.updateAttachmentsFromDrives(selectedFromDrives, removedFilesFromDrive);
    });
    this.$root.$on('remove-destination-path-for-file', (folderName, currentDrive, pathDestinationFolder) => {
      this.deleteDestinationPathForFile(folderName, currentDrive, pathDestinationFolder);
    });
    this.$root.$on('add-destination-path-for-file', (destinationFileName, pathDestinationFolder, folder, isPublic, currentDrive) => {
      this.addDestinationFolderForFile(destinationFileName, pathDestinationFolder, folder, isPublic, currentDrive);
    });
    this.$root.$on('add-destination-path-for-all', (defaultDestinationFolderPath, pathDestinationFolder, currentDrive) => {
      this.addDestinationFolderForAll(defaultDestinationFolderPath, pathDestinationFolder, currentDrive);
    });
    this.$root.$on('reset-attachment-list', () => {
      this.attachments = [];
    });
    document.addEventListener('open-attachments-app-drawer', (event) => {
      this.attachmentAppConfiguration = event.detail;
      this.attachments = [];
      this.openAttachmentsAppDrawer();
      if (this.entityType && this.entityId) {
        this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.startLoading();
        this.initEntityAttachmentsList().then(() => {
          this.initDefaultDrive();
        }).finally(() => {
          this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.endLoading();
        });
      } else {
        this.initDefaultDrive();
      }
    });
    document.addEventListener('open-attachments-list-drawer', (event) => {
      this.attachmentAppConfiguration = event.detail;
      this.attachments = [];
      this.openAttachmentsDrawerList();
      if (this.entityType && this.entityId) {
        this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.startLoading();
        this.initEntityAttachmentsList().then(() => {
          this.initDefaultDrive();
        }).finally(() => {
          this.$refs.attachmentsListDrawer.$refs.attachmentsListDrawer.endLoading();
        });
      } else {
        this.initDefaultDrive();
      }
    });
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    initEntityAttachmentsList() {
      if (this.entityType && this.entityId) {
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
          this.uploadingCount--;
        }
      } else if (file.isSelectedFromDrives) {
        const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id);
        this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
      } else {
        this.$refs.attachmentsAppDrawer.$refs.attachmentsAppDrawer.startLoading();
        this.$attachmentService.removeEntityAttachment(this.entityId, this.entityType, file.id).then(() => {
          const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id);
          this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
          this.$root.$emit('attachments-notification-alert', {
            message: this.$t('attachments.delete.success'),
            type: 'success',
          });
          this.$root.$emit('entity-attachments-updated');
          this.$refs.attachmentsAppDrawer.$refs.attachmentsAppDrawer.endLoading();
        });
      }
    },
    deleteDestinationPathForFile(fileName, currentDrive, pathDestinationFolder) {
      for (let i = 0; i < this.attachments.length; i++) {
        if (this.attachments[i].name === fileName) {
          this.attachments[i].pathDestinationFolderForFile = '';
          this.attachments[i].fileDrive = currentDrive;
          this.attachments[i].destinationFolder = pathDestinationFolder;
          this.attachments[i].isPublic = true;
          break;
        }
      }
    },
    addDestinationFolderForFile(destinationFileName, pathDestinationFolder, folder, isPublic, currentDrive) {
      for (let i = 0; i < this.attachments.length; i++) {
        if (this.attachments[i].name === destinationFileName) {
          this.attachments[i].pathDestinationFolderForFile = folder;
          this.attachments[i].destinationFolder = pathDestinationFolder.startsWith('/') ? pathDestinationFolder.substring(1) : pathDestinationFolder;
          this.attachments[i].fileDrive = currentDrive;
          // TODO: get 'isPublic' property of file from rest, now 'isPublic' assigned to 'isPublic' property of destination folder
          this.attachments[i].isPublic = isPublic;
        }
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
      if (selectedFromDrives) {
        this.attachments.push(...selectedFromDrives);
      }
      if (removedFilesFromDrive) {
        removedFilesFromDrive.forEach(attachment => {
          const attachmentIndex = this.attachments.findIndex(file => file.id === attachment.id);
          if (attachmentIndex !== -1) {
            this.attachments.splice(attachmentIndex, 1);
          }
        });
      }
    }
  }
};
</script>