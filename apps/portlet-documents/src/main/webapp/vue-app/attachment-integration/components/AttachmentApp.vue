<template>
  <div
    id="attachmentIntegration"
    :class="entityType && entityId && 'v-card__text pl-0'"
    class="attachment-application border-box-sizing transparent">
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
          <a class="addAttachmentLabel primary--text font-weight-bold text-decoration-underline">{{
            $t('attachments.add')
          }}</a>
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
          <a
            class="viewAllAttachments primary--text font-weight-bold text-decoration-underline"
            @click="openAttachmentsDrawerList()">
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
  data() {
    return {
      attachments: []
    };
  },
  computed: {
    attachmentsToDisplay() {
      return this.attachments.filter(attachment => attachment.id);
    },
    attachmentAppConfiguration() {
      return {
        'entityId': this.entityId,
        'entityType': this.entityType,
        'defaultDrive': this.defaultDrive,
        'defaultFolder': this.defaultFolder,
        'spaceId': this.spaceId
      };
    }
  },
  watch: {
    entityType() {
      this.initEntityAttachmentsList();
    },
    entityId() {
      this.initEntityAttachmentsList();
    },
  },
  created() {
    this.initEntityAttachmentsList();
    document.addEventListener('entity-attachments-updated', () => {
      this.initEntityAttachmentsList();
    });
  },
  methods: {
    openAttachmentsAppDrawer() {
      document.dispatchEvent(new CustomEvent('open-attachments-app-drawer', {detail: this.attachmentAppConfiguration}));
    },
    openAttachmentsDrawerList() {
      document.dispatchEvent(new CustomEvent('open-attachments-list-drawer', {detail: this.attachmentAppConfiguration}));
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
  }
};
</script>