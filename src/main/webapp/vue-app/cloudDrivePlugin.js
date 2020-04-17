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
        "event": "cloudDriveConnected",
        "listener": "openFolder"
      }
    ]
  },
  enabled: true,
  onExecute() {
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