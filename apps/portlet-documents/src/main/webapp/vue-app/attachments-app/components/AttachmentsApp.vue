<template>
  <div
    id="attachmentsApp"
    :class="entityType && entityId && 'v-card__text pl-0'"
    class="attachments-application border-box-sizing transparent">
    <div class="d-flex attachmentsIntegrationSlot">
      <div
        v-if="$scopedSlots.attachmentsButton"
        class="openAttachmentsButton me-2"
        @click="openAttachmentsAppDrawer()">
        <slot name="attachmentsButton"></slot>
      </div>
      <div v-else-if="entityId && entityType" :class="!attachmentsToDisplay.length && 'v-main align-center'">
        <v-icon size="18" color="primary">
          fa-paperclip
        </v-icon>
        <div
          v-if="!attachmentsToDisplay.length"
          class="addAttachments d-flex align-center ms-3"
          @click="openAttachmentsAppDrawer()">
          <a class="addAttachementLabel primary--text font-weight-bold text-decoration-underline">{{ $t('attachments.add') }}</a>
          <v-btn
            icon
            color="primary">
            <v-icon size="16">
              fa-plus
            </v-icon>
          </v-btn>
        </div>
      </div>
      <div
        v-if="$scopedSlots.attachmentsList"
        class="attachedFilesList"
        @click="openAttachmentsDrawerList()">
        <slot :attachments="attachmentsToDisplay" name="attachedFilesList"></slot>
      </div>
      <div v-else-if="entityId && entityType" class="attachmentsPreview v-card__text ms-3 pa-0">
        <div v-if="attachmentsToDisplay.length" class="attachmentsList">
          <a class="viewAllAttachments primary--text font-weight-bold text-decoration-underline" @click="openAttachmentsDrawerList()">
            {{ $t('attachments.view.all') }} ({{ attachmentsToDisplay && attachmentsToDisplay.length }})
          </a>
          <v-list v-if="!$scopedSlots.attachmentsList" dense>
            <v-list-item-group>
              <attachment-item
                v-for="attachment in attachmentsToDisplay.slice(0, 2)"
                :key="attachment.id"
                :attachment="attachment"
                :allow-to-remove="false"
                :allow-to-preview="true" />
            </v-list-item-group>
          </v-list>
        </div>
      </div>
    </div>

    <attachments-drawer
      ref="attachmentsAppDrawer"
      :attachments="attachments"
      :entity-id="entityId"
      :entity-type="entityType"
      :default-drive="defaultDrive"
      :default-folder="defaultFolder" />
    <attachments-list-drawer
      :attachments="attachmentsToDisplay" />
    <attachments-notification-alerts style="z-index:1035;" />
  </div>
</template>

<script>
export default {
  props: {
    entityId: {
      type: String,
      default: ''
    },
    entityType: {
      type: String,
      default: ''
    },
    defaultDrive: {
      type: Object,
      default: () => null
    },
    defaultFolder: {
      type: String,
      default: ''
    },
    spaceId: {
      type: String,
      default: ''
    },
  },
  data () {
    return {
      attachments: []
    };
  },
  computed: {
    attachmentsToDisplay() {
      return this.attachments.filter(attachment => attachment.id);
    }
  },
  watch: {
    spaceId() {
      this.initDefaultDrive();
    },
    entityType() {
      this.initDefaultDrive();
      this.initEntityAttachmentsList();
    },
    entityId() {
      this.initDefaultDrive();
      this.initEntityAttachmentsList();
    },
  },
  created() {
    if (!this.defaultDrive) {
      this.initDefaultDrive();
    }
    this.initEntityAttachmentsList();
    this.$root.$on('entity-attachments-updated', () => this.initEntityAttachmentsList());
    this.$root.$on('remove-attachment-item', attachment => {
      this.removeAttachedFile(attachment);
    });this.$root.$on('add-new-uploaded-file', file => {
      this.attachments.push(file);
    });
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    initEntityAttachmentsList() {
      if (this.entityType && this.entityId) {
        this.$attachmentService.getEntityAttachments(this.entityType, this.entityId).then(attachments => {
          attachments.forEach(attachments => {
            attachments.name = attachments.title;
          });
          this.attachments = attachments;
        });
      }
    },
    initDefaultDrive() {
      const spaceId = this.getURLQueryParam('spaceId') ? this.getURLQueryParam('spaceId') :
        `${eXo.env.portal.spaceId}` ? `${eXo.env.portal.spaceId}` :
          this.spaceId;
      if (spaceId) {
        this.$attachmentService.getSpaceById(spaceId).then(space => {
          if (space) {
            const spaceGroupId = space.groupId.split('/spaces/')[1];
            this.defaultDrive = {
              name: `.spaces.${spaceGroupId}`,
              title: spaceGroupId,
              isSelected: true
            };
          }
        });
      } else if (this.entityId && this.entityType) {
        this.defaultDrive = {
          isSelected: true,
          name: 'Personal Documents',
          title: 'Personal Documents'
        };
        this.defaultFolder = 'Public';
      }
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    },
    openAttachmentsDrawerList() {
      this.$root.$emit('open-attachments-list-drawer');
    },
    removeAttachedFile: function (file) {
      if (!file.id) {
        const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.uploadId === file.uploadId );
        this.attachments.splice(fileIndex, fileIndex >= 0 ? 1 : 0);
        if (file.uploadProgress !== this.maxProgress) {
          this.uploadingCount--;
          this.$emit('uploadingCountChanged', this.uploadingCount);
          this.processNextQueuedUpload();
        }
      } else {
        this.$refs.attachmentsAppDrawer.$refs.attachmentsAppDrawer.startLoading();
        this.$attachmentService.removeEntityAttachment(this.entityId, this.entityType, file.id).then(() => {
          const fileIndex = this.attachments.findIndex(attachedFile => attachedFile.id === file.id );
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
  }
};
</script>