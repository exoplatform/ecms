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
        <span class="cloudDriveTitle">{{ $t("ConnectDriveDrawer.title.ConnectYourService") }}</span>
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
              @click.native="connectToCloudDrive(item.id)"
            >
              <v-list-item-icon class="cloudDriveListItem__icon">
                <i :class="`uiIconEcmsConnectDialog-${item.id} uiIconEcmsBlue`"></i>
              </v-list-item-icon>
              <v-list-item-content class="cloudDriveListItem__content">
                <v-list-item-title class="cloudDriveListItem__title"> {{ $t("UIPopupWindow.title.ConnectYour") }} {{ item.name }} </v-list-item-title>
              </v-list-item-content>
              <v-progress-linear :active="item.id === connectingProvider" indeterminate absolute bottom></v-progress-linear>
            </v-list-item>
          </v-list-item-group>
        </v-list>
      </div>
    </div>
  </div>
</template>

<script>
import { getUserDrive, notifyError } from "../cloudDriveService";

export default {
  model: {
    // define name of prop below, this prop will be passed by parent in v-model
    prop: "currentDrive",
    // name of event that will change currentDrive in parent
    event: "changeCurrentDrive",
  },
  props: {
    currentDrive: {
      type: Object,
      default: () => ({})
    }
  },
  data: function() {
    return {
      providers: {},
      userDrive: {},
      connectingProvider: "",
      showCloudDrawer: false,
      drivesOpened: false,
      drivesInProgress: {} // contain all drives that are in connecting process, drive name is a key and progress percent is a value
    };
  },
  async created() {
    if (!this.showCloudDrawer) {
      try {
        const data = await getUserDrive();
        this.userDrive = {
          name: data.name,
          title: data.name,
          isSelected: false,
        };
        // init cloudDrive module after response from getUserDrive
        cloudDrive.init(data.workspace, data.homePath);
        this.providers = cloudDrive.getProviders();
      } catch (err) {
        notifyError(this.$t(err.message));
      }
    }
  },
  methods: {
    connectToCloudDrive: function(providerId) {
      this.connectingProvider = providerId;
      this.$emit("updateProgress", { progress: 0 });
      const fullProgress = 100;
      cloudDrive.connect(providerId).then(
        data => {
          if (!this.drivesOpened) {
            this.openDriveFolder(data.drive.path, data.drive.title);
          }

          this.drivesInProgress[data.drive.title] = fullProgress;
          this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

          this.$emit("updateProgress", { progress: fullProgress });
          const latency = 3000;
          setTimeout(() => {

            delete this.drivesInProgress[data.drive.title]; // connection is finished, so remove drive from drivesInProgress
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

            this.$emit("updateProgress", { progress: null });
          }, latency);
          this.connectingProvider = "";
          this.showCloudDrawer = false;
          this.drivesOpened = false;
        },
        () => {
          this.connectingProvider = "";
          this.drivesOpened = false;
          this.$emit("updateProgress", { progress: null });
        },
        progressData => {
          if (progressData.drive.title) {

            this.drivesInProgress[progressData.drive.title] = progressData.progress;
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

            if (!this.drivesOpened) {
              this.openDriveFolder(progressData.drive.path, progressData.drive.title);
            }
          }
          this.$emit("updateProgress", { progress: progressData.progress });
          this.showCloudDrawer = false;
        }
      );
    },
    toggleCloudDrawer: function() {
      this.showCloudDrawer = !this.showCloudDrawer;
    },
    openDriveFolder: function(path, title) {
      if (path) {
        const folderPath = path.split("/").pop();
        const createdDrive = {
          name: title,
          title: title,
          path: folderPath,
          driveTypeCSSClass: `uiIconEcms24x24Drive${title.replace(" ", "")} uiIconEcms24x24DrivePrivate`,
          type: "drive",
          css: "uiIcon16x16FolderDefault uiIcon16x16nt_folder",
          driverType: "Personal Drives",
          isCloudDrive: true
        };
        this.$emit("addDrive", createdDrive); // display drive in "My drives" section
        this.drivesOpened = true;
        this.showCloudDrawer = false;
      }
    }
  },
};
</script>
