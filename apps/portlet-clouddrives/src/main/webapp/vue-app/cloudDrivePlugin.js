(function(extensionRegistry, cloudDriveApp) {
  const CloudDrivePlugin = [{
    // configuration defined here is used in portlet-clouddrives/src/main/webapp/vue-app/components/ConnectCloudDrive.vue with
    // ecm-wcm-extension/src/main/webapp/attachments-selector/components/ExoAttachments.vue and connects them
    // key should be unique and used in parent component as a ref to ConnectCloudDrive component
    key: "connectCloudDrive",
    rank: 20,
    // iconName is a name of the icon which is displayed on action button with 'onExecute' action
    // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
    // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
    iconName: "cloud",
    // appClass is a class of container which cosist of action button and ConnectCloudDrive component
    appClass: "connectCloudDrive",
    // component has property which will be passed to dynamic component inside parent
    // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
    component: {
      // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
      name: "connect-cloud-drive",
      // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
      // event is name of the event that ConnectCloudDrive component emits
      // listener is name of the method that ExoAttachments has
      // all params added in 'emit()' inside ConnectCloudDrive component will be available inside ExoAttachments methods
      events: [
        // show progress line if some of cloud drives is in progress and hide if no drive in progress
        {
          "event": "updateProgress",
          "listener": "setCloudDriveProgress"
        },
        // display cloud drive shortcut in composer
        {
          "event": "addDrive",
          "listener": "addCloudDrive"
        },
        // update connecting drive progress in composer
        {
          "event": "updateDrivesInProgress",
          "listener": "changeCloudDriveProgress"
        }
      ]
    },
    // enabled just show that this extension is enabled, if enabled: false CloudDriveComponent will not appear on page
    enabled: true,
    // onExecute will be executed after click on action button, which is placed inside parent component
    onExecute(cloudDriveComponent) {
      // click on action button will open cloud drive drawer with list of cloud drives available to connection
      cloudDriveComponent.showCloudDrawer = true;
    }
  }];

  // init app only once with registering cloud drive extension
  cloudDriveApp.init();
  
  for (const extension of CloudDrivePlugin) {
    // connect extension to AttachmentsComposer, "attachments-composer-action" is extension type
    // composer and extension type should be the same as in extension.js inside ecm-wcm-extension
    extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension);
  }

  window.CloudDrivePlugin = CloudDrivePlugin;
  return CloudDrivePlugin;
})(extensionRegistry, cloudDriveApp);