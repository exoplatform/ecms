const CloudDrivePlugin = [{
  key: "connectCloudDrive",
  rank: 20,
  iconName: "cloud",
  appClass: "connectCloudDrive",
  component: {
    name: "connect-cloud-drive",
    events: [
      {
        "event": "updateProgress",
        "listener": "setCloudDriveProgress"
      },
      {
        "event": "addDrive",
        "listener": "addCloudDrive"
      },
      {
        "event": "updateDrivesInProgress",
        "listener": "changeCloudDriveProgress"
      }
    ]
  },
  enabled: true,
  onExecute(cloudDriveComponent) {
    cloudDriveComponent.showCloudDrawer = true;
  }
}];

require(["SHARED/extensionRegistry", "SHARED/cloudDriveApp"], function(extensionRegistry, cloudDriveApp) {
  // init app only once with registering cloud drive extension
  cloudDriveApp.init();
  for (const extension of CloudDrivePlugin) {
    extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension);
  }
  
});