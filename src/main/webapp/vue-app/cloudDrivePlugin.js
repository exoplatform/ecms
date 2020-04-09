const CloudDrivePlugin = [{
  key: "connectCloudDrive",
  rank: 20,
  iconClass: "uiIconCloudDrive",
  appClass: "connectCloudDrive",
  component: {
    name: "connect-cloud-drive"
  },
  enabled: true,
  onExecute(cloudDriveComponent) {
    console.log(cloudDriveComponent);
    require(['SHARED/cloudDriveApp'], function(cloudDriveApp) {
      cloudDriveApp.init();
    });
  }
}];

require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  for (const extension of CloudDrivePlugin) {
    extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension);
  }
});