const CloudDrivePlugin = [{
  key: "connectCloudDrive",
  rank: 20,
  iconClass: "uiIconCloudDrive",
  iconName: "cloud",
  appClass: "connectCloudDrive",
  component: {
    name: "connect-cloud-drive"
  },
  enabled: true,
  onExecute(cloudDriveComponent) {
    console.log(cloudDriveComponent);
    cloudDriveComponent.showCloudDrawer = true;
  }
}];

require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  for (const extension of CloudDrivePlugin) {
    extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension);
  }
});