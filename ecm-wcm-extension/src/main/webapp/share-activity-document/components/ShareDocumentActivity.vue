<template>
  <v-app :id="id">
    <share-activity-drawer
      ref="shareDocumentDrawer"
      class="activityDrawer"
      @share-activity="shareDocument" />
    <exo-news-notification-alerts />
  </v-app>
</template>
<script>
export default {
  props: {
    id: {
      type: String,
      default: ''
    }
  },
  data: () => ({
    activityId: '',
    activityType: '',
  }),
  created() {
    this.$root.$on('document-share-drawer-open', params => {
      this.activityId = params.activityId;
      this.activityType = params.activityType;
      this.openDrawer();
    });
  },
  methods: {
    openDrawer() {
      this.$refs.shareDocumentDrawer.open();
    },
    shareDocument(spaces, description) {
      const spacesList = [];
      spaces.forEach(space => {
        this.$spaceService.getSpaceByPrettyName(space,'identity').then(data => {
          spacesList.push(data.displayName);
        });
      });
      const sharedActivity = {
        title: description,
        type: this.activityType,
        targetSpaces: spaces,
      };
      this.$documentServices.shareDocumentOnSpaces(this.activityId, sharedActivity)
        .then(() => {
          this.$root.$emit('news-shared', this.activityId, spacesList);
          this.$root.$emit('clear-suggester');
          this.close();
        });
    },
    close() {
      this.$refs.shareDocumentDrawer.close();
    },
  }
};
</script>