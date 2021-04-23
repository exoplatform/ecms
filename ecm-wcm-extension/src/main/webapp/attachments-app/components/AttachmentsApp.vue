<template>
  <div class="attachments-application border-box-sizing transparent">
    <template v-if="$slots.attachmentsButton" @click="openAttachmentsAppDrawer()">
      <slot name="attachmentsButton"></slot>
    </template>
    <attachments-drawer
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
  created() {
    this.initDefaultDrive();
  },
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
    initDefaultDrive() {
      if (!this.defaultDrive.name) {
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
        } else if(this.entityId && this.entityType){
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