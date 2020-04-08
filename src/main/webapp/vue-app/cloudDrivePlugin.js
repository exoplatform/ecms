const CloudDrivePlugin = {
  key: "connectCloudDrive",
  rank: 20,
  labelKey: "Connect cloud documents",
  description: "Connect cloud documents",
  iconClass: "uiIconCloudDrive",
  appClass: "connectCloudDrive",
  component: "connect-cloud-drive",
  enabled: true,
  onExecute() {
    require(['SHARED/cloudDriveApp'], function(cloudDriveApp) {
      cloudDriveApp.init();
    });
  }
};

require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", CloudDrivePlugin);
});
