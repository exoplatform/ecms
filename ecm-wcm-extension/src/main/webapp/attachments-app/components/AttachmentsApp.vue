<template>
  <div id="attachmentsApp" class="attachments-application border-box-sizing transparent">
    <div class="d-flex attachmentsIntegrationSlot">
      <div v-if="$scopedSlots.attachmentsButton" class="openAttachmentsButton me-2" @click="openAttachmentsAppDrawer()">
        <slot name="attachmentsButton"></slot>
      </div>
      <v-btn
        v-else-if="entityId && entityType"
        class="pb-4"
        icon
        color="grey"
        @click="openAttachmentsAppDrawer()"
      >
        <i class="uiIconAttach" ></i>
      </v-btn>
      <div v-if="$scopedSlots.attachmentsList" class="attachedFilesList" @click="openAttachmentsDrawerList()">
        <slot :attachments="attachments" name="attachedFilesList"></slot>
      </div>
      <div v-else-if="entityId && entityType">
        <div v-if="attachments.length" class="attachmentsList">
          <a class="ms-2" @click="openAttachmentsDrawerList()">{{ $t('attachments.view.all') }} ({{ attachments && attachments.length }})</a>
          <v-list v-if="!$scopedSlots.attachmentsList" dense>
            <v-list-item-group>
              <attachment-item
                v-for="attachment in attachments.slice(0, 2)"
                :key="attachment.id"
                :file="attachment"
                :allow-to-remove="false"/>
            </v-list-item-group>
          </v-list>
        </div>
        <div v-else class="emptyList">
          <span class="noAttachementLabel">{{ $t('attachments.list.empty') }}</span>
        </div>
      </div>
    </div>

    <attachments-drawer
      :attachments="attachments"
      :entity-id="entityId"
      :entity-type="entityType"
      :default-drive="defaultDrive"
      :default-folder="defaultFolder"
    />
    <attachments-list-drawer
      :attachments="attachments"/>
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
    this.$root.$on('entity-attachments-updated', () => this.initEntityAttachmentsList());
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
    },
    openAttachmentsDrawerList() {
      this.$root.$emit('open-attachments-list-drawer');
    }
  }
};
</script>