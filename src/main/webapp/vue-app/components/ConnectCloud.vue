<template>
  <div id="connectCloudApp">
    <div :class="{ open: showCloudDrawer }" class="connect-cloud drawer ignore-vuetify-classes" @keydown.esc="closeCloudDrawer">
      <div class="attachmentsHeader header">
        <a class="backButton" @click="closeCloudDrawer">
          <i class="uiIconBack"></i>
        </a>
        <a class="backButton">
          <i class="uiIconBack"> </i>
        </a>
        <span class="attachmentsTitle">{{ $t("UIPopupWindow.title.ConnectCloudDriveForm") }}</span>
        <a class="attachmentsCloseIcon" @click="closeCloudDrawer">×</a>
      </div>
      <div class="content">
        Content
      </div>
    </div>
    <div v-show="showDrawer" class="drawer-backdrop" @click="closeCloudDrawer"></div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      showCloudDrawer: true
    };
  },
  methods: {
    closeCloudDrawer: function() {
      this.showCloudDrawer = false;
    }
  }
};
</script>
