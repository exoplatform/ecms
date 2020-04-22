<template>
  <div id="connectCloudApp">
    <div
      :class="{ open: showCloudDrawer }"
      class="connect-cloud drawer ignore-vuetify-classes"
      @keydown.esc="toggleCloudDrawer()"
    >
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
            <v-list-item
              v-for="item in providers"
              :key="item.id"
              :ripple="false"
              class="cloudDriveListItem"
              @click="connectToCloudDrive(item.id)"
            >
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
  model: {
    prop: "currentDrive",
    event: "changeCurrentDrive"
  },
  props: {
    showCloudDrawer: {
      type: Boolean,
      default: () => false
    },
    currentDrive: {
      type: Object,
      default: () => ({})
    }
  },
  data: function() {
    return { providers: {}, userDrive: {}, error: null };
  },
  async created() {
    try {
      const data = await getUserDrive();
      this.userDrive = {
        name: data.name,
        title: data.name,
        isSelected: false
      };
      cloudDrive.init(data.workspace, data.homePath);
      this.providers = cloudDrive.getProviders();
    } catch (err) {
      this.error = err.message;
    }
  },
  methods: {
    connectToCloudDrive: function(providerId) {
      // openDriveFolder() method should be called only one time, but as progress callback may be called several times or
      // it may not be called driveFolderOpened should monitor this 
      let driveFolderOpened = false;
      cloudDrive.connect(providerId).then(
        data => { 
          if (!driveFolderOpened) {
            this.openDriveFolder(data.drive.path, data.drive.title);
            driveFolderOpened = true;
          }
        },
        () => { driveFolderOpened = true; },
        progress => { 
          if (progress.drive.path && !driveFolderOpened) { 
            this.openDriveFolder(progress.drive.path, progress.drive.title);
            driveFolderOpened = true;
          }
        }
      );
    },
    toggleCloudDrawer: function() {
      this.showCloudDrawer = !this.showCloudDrawer;
    },
    openDriveFolder: function(path, title) {
      const folderPath = path.split("/").pop();
      const createdDrive = {
        id: folderPath,
        name: folderPath,
        title: title,
        path: folderPath,
        isSelected: true,
      };
      if (this.currentDrive.name !== this.userDrive.name) {
        this.$emit("changeCurrentDrive", this.userDrive);
      }
      this.$emit("openConnectedFolder", createdDrive);
      this.toggleCloudDrawer();
    }
  }
};
</script>
