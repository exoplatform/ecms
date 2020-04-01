const CloudDrivePlugin = {
  key: "cloud-drive",
  rank: 20,
  labelKey: "Connect cloud documents",
  description: "Connect cloud documents",
  iconClass: "uiIconGeneralDrive",
  appClass: "cloud-drive",
  component: "connect-cloud",
  onExecute() {
    console.log("on execute");
    document.getElementsByClassName("connect-cloud drawer")[0].className += " open";
  }
};
require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  extensionRegistry.registerExtension("ActivityComposer", "activity-composer-action", CloudDrivePlugin);
});
