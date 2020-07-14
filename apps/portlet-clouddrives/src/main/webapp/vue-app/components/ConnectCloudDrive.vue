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
      <transition name="fade" mode="in-out">
        <div v-show="showAlertMessage" :class="`alert-${alert.type} cloudDriveAlert--${alert.type}`" class="alert cloudDriveAlert">
          <i :class="`uiIcon${capitalized(alert.type)}`"></i>{{ alert.message }}
        </div>
      </transition>
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
                <i :class="`uiIcon-${item.id} uiIconEcmsBlue`"></i>
              </v-list-item-icon>
              <v-list-item-content class="cloudDriveListItem__content">
                <v-list-item-title class="cloudDriveListItem__title">
                  {{ $t("UIPopupWindow.title.ConnectYour") }} {{ item.name }}
                </v-list-item-title>
              </v-list-item-content>
              <v-progress-linear 
                :active="item.id === connectingProvider && Object.keys(drivesInProgress).length > 0"
                indeterminate
                absolute
                bottom
              >
              </v-progress-linear>
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
      drivesInProgress: {}, // contain all drives that are in connecting process, drive name is a key and progress percent is a value
      alert: { message: "", type: "" },
      showAlertMessage: false,
      MESSAGE_TIMEOUT: 5000
    };
  },
  watch: {
    showAlertMessage: function(newVal) {
      if (newVal) {
        setTimeout(() => this.showAlertMessage = false, this.MESSAGE_TIMEOUT);
      }
    },
    drivesInProgress: function() {
      if (this.showCloudDrawer) { this.showCloudDrawer = false; }
    }
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
        cloudDrives.init(data.workspace, data.homePath);
        this.providers = cloudDrives.getProviders();
      } catch (err) {
        this.alert = { message: err.message, type: "error" };
        this.showAlertMessage = true;
      }
    }
  },
  methods: {
    connectToCloudDrive: function(providerId) {
      this.connectingProvider = providerId;
      this.$emit("updateProgress", { progress: 0 });
      const fullProgress = 100;
      cloudDrives.connect(providerId).then(
        data => {
          this.openDriveFolder(data.drive.path, data.drive.title);
          this.drivesInProgress = { ...this.drivesInProgress, [data.drive.title]: fullProgress };
          this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

          this.$emit("updateProgress", { progress: fullProgress });
          const latency = 3000;
          setTimeout(() => {

            delete this.drivesInProgress[data.drive.title]; // connection is finished, so remove drive from drivesInProgress
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

            this.$emit("updateProgress", { progress: null });
          }, latency);
          this.showCloudDrawer = false;
        },
        (error) => {
          if (error) {
            this.alert = { message: error, type: "error" };
            this.showAlertMessage = true;
            this.toggleCloudDrawer();
          } else {
            // if error undefined/null action was cancelled
            this.alert = { message: "Canceled", type: "info" };
            this.showAlertMessage = true;
          }
          this.$emit("updateProgress", { progress: null });
        },
        progressData => {
          if (progressData.drive.title) {
            this.drivesInProgress = { ...this.drivesInProgress, [progressData.drive.title]: progressData.progress };
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component
            this.openDriveFolder(progressData.drive.path, progressData.drive.title);
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
      const createdDrive = {
        name: title,
        title: title,
        path: path ? path.split("/").pop() : "",
        driveTypeCSSClass: `uiIconEcmsDrive-${this.connectingProvider}`,
        type: "drive",
        css: "uiIcon16x16FolderDefault uiIcon16x16nt_folder",
        driverType: "Personal Drives",
        isCloudDrive: true
      };
      this.$emit("addDrive", createdDrive); // display drive in "My drives" section
    },
    capitalized(value) {
      return typeof value !== "string" ? "" :  value.charAt(0).toUpperCase() + value.slice(1);
    }
  },
};
</script>
