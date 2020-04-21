const CloudDrivePlugin = [{
  key: "connectCloudDrive",
  rank: 20,
  iconClass: "uiIconCloudDrive",
  iconName: "cloud",
  appClass: "connectCloudDrive",
  component: {
    name: "connect-cloud-drive",
    events: [
      {
        "event": "openConnectedFolder",
        "listener": "openFolder"
      }
    ]
  },
  enabled: true,
  onExecute(cloudDriveComponent) {
    cloudDriveComponent.showCloudDrawer = true;
  }
}];

require(["SHARED/extensionRegistry", "SHARED/cloudDriveApp"], function(extensionRegistry, cloudDriveApp) {
  cloudDriveApp.init();
  for (const extension of CloudDrivePlugin) {
    extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension);
  }
  
});