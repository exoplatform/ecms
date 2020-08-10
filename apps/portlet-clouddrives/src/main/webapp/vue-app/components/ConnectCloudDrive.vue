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
            <!-- cloud drive providers list -->
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
      userDrive: {}, // user Personal Documents drive
      connectingProvider: "", // provider that is in connecting process
      showCloudDrawer: false, // show or hide cloud drive drawer
      drivesInProgress: {}, // contain all drives that are in connecting process, drive name is a key and progress percent is a value
      alert: { message: "", type: "" }, // alert for error or info messages displayed at the top
      showAlertMessage: false,
      MESSAGE_TIMEOUT: 5000 // alert message hides after 5 sec
    };
  },
  watch: {
    // when showAlertMessage changes to true, wait some time, than hide
    showAlertMessage: function(newVal) {
      if (newVal) {
        setTimeout(() => this.showAlertMessage = false, this.MESSAGE_TIMEOUT);
      }
    },
    // when drive connecting process started hide cloud drive drawer
    drivesInProgress: function() {
      if (this.showCloudDrawer) { this.showCloudDrawer = false; }
    }
  },
  async created() {
    if (!this.showCloudDrawer) {
      try {
        // get user drive only once when component created
        const data = await getUserDrive();
        this.userDrive = {
          name: data.name,
          title: data.name,
          isSelected: false,
          workspace: data.workspace,
          homePath: data.homePath,
        };
        // get providers from cloudDrives module, note that providers should already exist in module at this stage
        this.providers = cloudDrives.getProviders();
      } catch (err) {
        this.alert = { message: err.message, type: "error" };
        this.showAlertMessage = true;
      }
    }
  },
  methods: {
    connectToCloudDrive: function(providerId) {
      // init cloudDrives module with Personal Documents workspace and path recieved in getUserDrive()
      // note: cloudDrives.init() also is called by server
      // initialize cloud drive context node
      cloudDrives.init(this.userDrive.workspace, this.userDrive.homePath);
      this.connectingProvider = providerId;
      // show progress line at the top in composer
      this.$emit("updateProgress", { progress: 0 });
      const fullProgress = 100; // means 100%
      cloudDrives.connect(providerId).then(
        data => {
          this.openDriveFolder(data.drive.path, data.drive.title); // display drive in composer
          this.drivesInProgress = { ...this.drivesInProgress, [data.drive.title]: fullProgress };
          this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component

          this.$emit("updateProgress", { progress: fullProgress });
          const latency = 3000;
          setTimeout(() => {
            // drives deletion after latency, cause emitting events from component to component can take some time
            delete this.drivesInProgress[data.drive.title]; // connection is finished, so remove drive from drivesInProgress
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component
            // hide progress line at the top of composer
            // note: this will hide progress line after any connecting drive is finish its connecting
            // if another drive is in connecting progress progress line will appear again, but it's hiding can be visible to user
            this.$emit("updateProgress", { progress: null });
          }, latency);
          this.showCloudDrawer = false; // hide cloud drive drawer after drive connected
          // note: if drawer was opened before and some drive finished its connecting this will close drawer
        },
        (error) => {
          if (error) {
            this.alert = { message: error, type: "error" };
            this.showAlertMessage = true;
            this.toggleCloudDrawer();
          } else {
            // if error is undefined/null action was cancelled
            this.alert = { message: "Canceled", type: "info" };
            this.showAlertMessage = true;
          }
          this.$emit("updateProgress", { progress: null }); // hide progress line at the top of composer
        },
        progressData => {
          if (progressData.drive.title) {
            this.drivesInProgress = { ...this.drivesInProgress, [progressData.drive.title]: progressData.progress };
            // update drivesInProgress in attachmentsComposer, so display drive actual progress at every time progress updates
            this.$emit("updateDrivesInProgress", { drives: this.drivesInProgress }); // drives update in parent component
            this.openDriveFolder(progressData.drive.path, progressData.drive.title); // display drive in composer
          }
          this.$emit("updateProgress", { progress: progressData.progress }); // update progress at the top of composer
          this.showCloudDrawer = false; // hide cloud drive drawer when progress begins
        }
      );
    },
    toggleCloudDrawer: function() {
      this.showCloudDrawer = !this.showCloudDrawer;
    },
    openDriveFolder: function(path, title) {
      // createdDrive should consist of the same properties as drives in exoAttachments as it will be added to existing drives array
      const createdDrive = {
        name: title,
        title: title,
        path: path ? path.split("/").pop() : "",
        // class name should be one of the existing platform icon classes, it's a drive shortcut
        driveTypeCSSClass: `uiIconEcmsDrive-${this.connectingProvider}`,
        type: "drive",
        css: "uiIcon16x16FolderDefault uiIcon16x16nt_folder",
        driverType: "Personal Drives",
        isCloudDrive: true
      };
      this.$emit("addDrive", createdDrive); // display drive in "My drives" section
      // note: after next drives fetching in composer this drive will be replaced by drive recieved from rest
    },
    capitalized(value) { // capitalize the first letter of value
      return typeof value !== "string" ? "" :  value.charAt(0).toUpperCase() + value.slice(1);
    }
  },
};
</script>
