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
        :attach-to-entity="attachToEntity"
        :files="files" />
      <attachments-list-drawer
        ref="attachmentsListDrawer"
        :supported-documents="supportedDocuments"
        :attachments="attachments"
        :open-attachments-in-editor="openAttachmentsInEditor" />
      <attachments-notification-alerts />
    </div>
  </v-app>
</template>

<script>
export default {
  data () {
    return {
      attachments: [],
      files: [],
      currentSpace: {},
      defaultDrive: null,
      defaultFolder: null,
      spaceId: null,
      entityType: null,
      entityId: null,
      sourceApp: null,
      drawerList: false,
      attachToEntity: true,
      supportedDocuments: null,
      openAttachmentsInEditor: false
    };
  },
  computed: {
    entityHasAttachments() {
      return this.attachments && this.attachments.length;
    }
  },
  created() {
    this.$root.$on('entity-attachments-updated', () => this.initEntityAttachmentsList());
    this.$root.$on('remove-attachment-item', attachment => {
      this.removeAttachedFile(attachment);
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
    this.$root.$on('add-destination-path-for-all', (defaultDestinationFolderPath, folderRelativePath, currentDrive) => {
      this.addDestinationFolderForAll(defaultDestinationFolderPath, folderRelativePath, currentDrive);
    });
    this.$root.$on('reset-attachment-list', () => {
      this.attachments = [];
    });
    document.addEventListener('open-attachments-app-drawer', (event) => {
      this.drawerList = false;
      this.readConfiguration(event.detail);
      this.initAttachmentEnvironment();
      this.openAttachmentsAppDrawer();
    });
    document.addEventListener('open-attachments-list-drawer', (event) => {
      this.drawerList = true;
      this.readConfiguration(event.detail);
      this.initAttachmentEnvironment();
      this.openAttachmentsDrawerList();
    });
    document.addEventListener('documents-supported-document-types-updated', this.refreshSupportedDocumentExtensions);
    this.refreshSupportedDocumentExtensions();
    document.addEventListener('mark-attachment-as-viewed', this.markAttachmentAsViewed);
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
  methods: {
    markAttachmentAsViewed(event) {
      const file = event.detail.file;
      const userName = eXo.env.portal.userName;
      return Vue.prototype.$attachmentService.markAttachmentAsViewed(file.id, userName).then(views => {
        document.dispatchEvent(new CustomEvent('document-views-updated', {detail: {file: file, views: views}}));
      });
    },
    refreshSupportedDocumentExtensions () {
      this.supportedDocuments = extensionRegistry.loadExtensions('documents', 'supported-document-types');
    },
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    readConfiguration(config) {
      config = config || {};
      this.spaceId = this.getURLQueryParam('spaceId') || config.spaceId || eXo.env.portal.spaceId;
      if (this.spaceId) {
        this.$spaceService.getSpaceById(this.spaceId)
          .then(space => {
            if (space) {
              this.currentSpace = space;
              const spaceGroupId = space.groupId.split('/spaces/')[1];
              this.defaultDrive = {
                name: `.spaces.${spaceGroupId}`,
                title: space.displayName,
                isSelected: true
              };
            }
          });
      } else {
        this.defaultDrive = config.defaultDrive || {
          isSelected: true,
          name: eXo.env.portal.spaceGroup && `.spaces.${eXo.env.portal.spaceGroup}` || 'Personal Documents',
          title: eXo.env.portal.spaceDisplayName || 'Personal Documents'
        };
      }
      this.defaultFolder = config.defaultFolder
        || (eXo.env.portal.spaceId && '/') || 'Public';
      this.sourceApp = config.sourceApp || null;
      this.files = config.files || null;
      this.attachments = config.attachments || [];
      if (typeof config.attachToEntity !== 'undefined') {
        this.attachToEntity = config.attachToEntity;
        this.attachments.forEach((attachment) => {
          if (attachment.acl) {
            attachment.acl.canDetach = true;
          }
        });
      }
      this.entityType = config.entityType;
      this.entityId = config.entityId;
      this.openAttachmentsInEditor = config.openAttachmentsInEditor || false;
    },
    startLoadingList() {
      if (this.drawerList && this.$refs.attachmentsListDrawer) {
        this.$refs.attachmentsListDrawer.startLoading();
      } else if (!this.drawerList && this.$refs.attachmentsAppDrawer) {
        this.$refs.attachmentsAppDrawer.startLoading();
      }
    },
    endLoadingList() {
      if (this.drawerList && this.$refs.attachmentsListDrawer) {
        this.$refs.attachmentsListDrawer.endLoading();
      } else if (!this.drawerList && this.$refs.attachmentsAppDrawer) {
        this.$refs.attachmentsAppDrawer.endLoading();
      }
    },
    initAttachmentEnvironment() {
      if (this.entityType && this.entityId && this.attachToEntity) {
        this.startLoadingList();
        this.initEntityAttachmentsList()
          .finally(() => this.endLoadingList());
      }
    },
    initEntityAttachmentsList() {
      if (this.entityType && this.entityId) {
        return this.$attachmentService.getEntityAttachments(this.entityType, this.entityId).then(attachments => {
          attachments.forEach(attachment => attachment.name = attachment.title);
          Object.assign(this.attachments, attachments);
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
        if (this.attachments.length === 0) {
          this.$root.$emit('end-loading-attachment-drawer');
        }
      } else if (this.attachToEntity) {
        this.startLoadingList();
        this.$attachmentService.removeEntityAttachment(this.entityId, this.entityType, file.id)
          .then(() => {
            this.$root.$emit('remove-attached-file', file);
            const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id);
            this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
            this.$root.$emit('attachments-notification-alert', {
              message: this.$t('attachments.detach.success'),
              type: 'success',
            });
            this.initEntityAttachmentsList();
            document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
            document.dispatchEvent(new CustomEvent('attachment-removed', {detail: file}));
          })
          .catch(e => {
            console.error(e);
            this.$root.$emit('attachments-notification-alert', {
              message: this.$t('attachments.delete.failed').replace('{0}', file.title),
              type: 'error',
            });
          })
          .finally(() => this.endLoadingList());
      } else {
        document.dispatchEvent(new CustomEvent('attachment-removed', {detail: file}));
        this.$root.$emit('remove-attached-file', file);
        const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id);
        this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
      }
    },
    addDestinationFolderForAll(defaultDestinationFolderPath, folderRelativePath, currentDrive) {
      this.attachments.forEach(attachment => {
        if (attachment.id && (!attachment.destinationFolder || attachment.destinationFolder === defaultDestinationFolderPath)) {
          this.$attachmentService.moveAttachmentToNewPath(
            currentDrive.name,
            folderRelativePath,
            attachment.id,
            this.entityType,
            this.entityId
          ).then(() => {
            this.$root.$emit('entity-attachments-updated');
            document.dispatchEvent(new CustomEvent('entity-attachments-updated'));
          });
        }
      });
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
    }
  }
};
</script>