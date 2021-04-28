<template>
  <div id="attachmentsApp" class="attachments-application border-box-sizing transparent">
    <div v-if="$scopedSlots.attachmentsButton || $scopedSlots.attachmentsList" class="d-flex attachmentsIntegrationSlot">
      <div class="openAttachmentsButton me-2" @click="openAttachmentsAppDrawer()">
        <slot name="attachmentsButton"></slot>
      </div>
      <div class="attachedFilesList" @click="openAttachmentsDrawerList()">
        <slot :attachments="attachments" name="attachedFilesList"></slot>
      </div>
    </div>
    <div v-else-if="entityId && entityType">
      <i class="uiIconAttach" @click="openAttachmentsAppDrawer()"></i>
      <a class="ms-2">View all attachments ({{ attachments && attachments.length }})</a>
    </div>
    <attachments-drawer
      :attachments="attachments"
      :entity-id="entityId"
      :entity-type="entityType"
      :default-drive="defaultDrive"
      :default-folder="defaultFolder"
    />
    <attachments-notification-alerts style="z-index:1035;"/>
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
  created() {
    this.initDefaultDrive();
    if (this.entityType && this.entityId) {
      this.initEntityAttachmentsList();
    }
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    initEntityAttachmentsList() {
      this.$attachmentsService.getEntityAttachments(this.entityType, this.entityId).then(attachments => {
        attachments.forEach(attachments => {
          attachments.name =  attachments.title;
        });
        this.attachments = attachments;
      });
    },
    initDefaultDrive() {
      if (!this.defaultDrive) {
        const spaceId = this.getURLQueryParam('spaceId') ? this.getURLQueryParam('spaceId') :
          `${eXo.env.portal.spaceId}` ? `${eXo.env.portal.spaceId}` :
            this.spaceId;
        if (spaceId) {
          this.$attachmentsService.getSpaceById(spaceId).then(space => {
            if(space) {
              const spaceGroupId = space.groupId.split('/spaces/')[1];
              this.defaultDrive = {
                name: `.spaces.${spaceGroupId}`,
                title: spaceGroupId,
                isSelected: true
              };
            }
          });
        } else if (this.entityId && this.entityType){
          this.defaultDrive = {
            isSelected: true,
            name: 'Personal Documents',
            title: 'Personal Documents'
          };
        }
      }
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    }
  }
};
</script>