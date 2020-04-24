const CloudDrivePlugin = [{
  key: "connectCloudDrive",
  rank: 20,
  iconName: "cloud",
  appClass: "connectCloudDrive",
  component: {
    name: "connect-cloud-drive",
    events: [
      {
        "event": "openConnectedFolder",
        // listener must be the name of the method existing in composer, this method will be called on event emit
        "listener": "updateCloudDriveFolder"
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