<template>
  <div id="connectCloudApp">
    <div :class="{ open: showCloudDrawer }" class="connect-cloud drawer ignore-vuetify-classes" @keydown.esc="toggleCloudDrawer()">
      <div class="header cloudDriveHeader">
        <a class="backButton" @click="toggleCloudDrawer()">
          <i class="uiIconBack"></i>
        </a>
        <span class="cloudDriveTitle">{{ $t("UIPopupWindow.title.ConnectCloudDriveForm") }}</span>
        <a class="cloudDriveCloseIcon" @click="toggleCloudDrawer()">×</a>
      </div>
      <div class="content">
        <v-list dense class="cloudDriveList ignore-vuetify-classes">
          <v-list-item-group v-if="providers" color="primary">
            <v-list-item v-for="item in providers" :key="item.id" :ripple="false" class="cloudDriveListItem" @click="connectToCloudDrive(item.id)">
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
  </div>
</template>

<script>
import { getUserDrive } from "../cloudDriveService";

export default {
  props: {
    showCloudDrawer: {
      type: Boolean,
      default: () => false
    }
  },
  data: function() {
    return { providers: {} };
  },
  async created() {
    try {
      const data = await getUserDrive();
      cloudDrive.init(data.workspace, data.homePath);
      this.providers = cloudDrive.getProviders();
    } catch (err) {
      console.log(err);
    }
  },
  methods: {
    connectToCloudDrive: function(providerId) {
      cloudDrive.connect(providerId).then(data => {
        const folderPath = data.drive.path.split("/").pop();
        const createdDrive = {
          id: folderPath,
          name: folderPath,
          title: data.drive.title,
          path: folderPath,
          folderTypeCSSClass: "uiIcon24x24nt_unstructured",
          isSelected: true
        };
        this.$emit("cloudDriveConnected", createdDrive);
        this.toggleCloudDrawer();
      }).catch(err => console.log(err));
    },
    toggleCloudDrawer: function() {
      this.showCloudDrawer = !this.showCloudDrawer;
    }
  }
};
</script>
