const CloudDrivePlugin = {
  key: "cloud-drive",
  rank: 20,
  labelKey: "Connect cloud documents",
  description: "Connect cloud documents",
  iconClass: "uiIconGeneralDrive",
  appClass: "cloud-drive",
  component: "connect-cloud",
  onExecute() {
    require(['SHARED/CloudDriveApp'], function(cloudDriveApp) {
      cloudDriveApp.init();
    });
  }
};

require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  extensionRegistry.registerExtension("ActivityComposer", "activity-composer-action", CloudDrivePlugin);
});

// registering component instead of extension
// import ConnectCloud from "./components/ConnectCloud.vue";

// require(["SHARED/extensionRegistry"], function(extensionRegistry) {
//   extensionRegistry.registerComponent("exoAttachmentsApp", "connect-cloud", ConnectCloud);
//   console.log(extensionRegistry);
// });