<template>
  <div id="connectCloudApp">
    <div :class="{ open: showCloudDrawer }" class="connect-cloud drawer ignore-vuetify-classes" @keydown.esc="closeCloudDrawer()">
      <div class="header cloudDriveHeader">
        <a class="backButton" @click="closeCloudDrawer()">
          <i class="uiIconBack"></i>
        </a>
        <a class="backButton">
          <i class="uiIconBack"> </i>
        </a>
        <span class="cloudDriveTitle">{{ $t("UIPopupWindow.title.ConnectCloudDriveForm") }}</span>
        <a class="cloudDriveCloseIcon" @click="closeCloudDrawer()">×</a>
      </div>
      <div class="content">
        <v-list dense class="cloudDriveList ignore-vuetify-classes">
          <v-list-item-group v-model="selectedService" color="primary">
            <v-list-item v-for="(item, i) in cloudServices" :key="i" :ripple="false" class="cloudDriveListItem">
              <v-list-item-icon class="cloudDriveListItem__icon">
                <i :class="`uiIconEcmsConnectDialog-${item.id} uiIconEcmsBlue`"></i>
              </v-list-item-icon>
              <v-list-item-content class="cloudDriveListItem__content">
                <v-list-item-title> {{ $t("UIPopupWindow.title.ConnectYour") }} {{ item.name }} </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list-item-group>
        </v-list>
      </div>
    </div>
    <div v-show="showDrawer" class="drawer-backdrop" @click="closeCloudDrawer()"></div>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Array,
      default: () => []
    },
  },
  data() {
    return {
      showCloudDrawer: true,
      cloudServices: [
        { name: "GDrive", id: "gdrive" },
        { name: "OneDrive", id: "onedrive" },
        { name: "Dropbox", id: "dropbox" }
      ],
      selectedService: null
    };
  },
  watch: {
    value: function(newValue) {
      console.log(newValue);
    }
  },
  methods: {
    closeCloudDrawer: function() {
      this.showCloudDrawer = false;
    }
  }
};
</script>
