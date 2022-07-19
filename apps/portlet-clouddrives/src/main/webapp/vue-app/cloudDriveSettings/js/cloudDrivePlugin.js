/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
export const CloudDrivePlugin = [{
  // configuration defined here is used in portlet-clouddrives/src/main/webapp/vue-app/components/ConnectCloudDrive.vue with
  // ecm-wcm-extension/src/main/webapp/attachments-selector/components/ExoAttachments.vue and connects them
  // key should be unique and used in parent component as a ref to ConnectCloudDrive component
  key: 'connectCloudDrive',
  rank: 20,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName: 'cloud',
  // appClass is a class of container which cosist of action button and ConnectCloudDrive component
  appClass: 'connectCloudDrive',
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component: {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name: 'connect-cloud-drive',
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    // event is name of the event that ConnectCloudDrive component emits
    // listener is name of the method that ExoAttachments has
    // all params added in 'emit()' inside ConnectCloudDrive component will be available inside ExoAttachments methods
    events: [
      // show progress line if some of cloud drives is in progress and hide if no drive in progress
      {
        'event': 'updateProgress',
        'listener': 'setCloudDriveProgress'
      },
      // display cloud drive shortcut in composer
      {
        'event': 'addDrive',
        'listener': 'addCloudDrive'
      },
      // update connecting drive progress in composer
      {
        'event': 'updateDrivesInProgress',
        'listener': 'changeCloudDriveProgress'
      }
    ]
  },
  // enabled just show that this extension is enabled, if enabled: false CloudDriveComponent will not appear on page
  enabled: true,
  // onExecute will be executed after click on action button, which is placed inside parent component
  onExecute(cloudDriveComponent) {
    cloudDriveComponent.showCloudDrawer = true;
  }
}];

window.CloudDrivePlugin = CloudDrivePlugin;