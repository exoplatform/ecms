<!--
Copyright (C) 2022 eXo Platform SAS.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<script>
import { getUserDrive, disconnect, saveUserSettings, getUserSettings } from '../js/cloudDriveService';

export default {
  data: function() {
    return {
      providers: {},
      userDrive: {}, // user Personal Documents drive
      connectingProvider: '', // provider that is in connecting process
      drivesInProgress: {}, // contain all drives that are in connecting process, drive name is a key and progress percent is a value
      alert: { message: '', type: '' }, // alert for error or info messages displayed at the top
      connectorsImages: []
    };
  },
  created() {
    this.$root.$on('cloud-drive-connect', this.connectToCloudDrive);
    this.$root.$on('cloud-drive-disconnect', this.disconnectFromCloudDrive);
    this.connectorsImages = extensionRegistry.loadExtensions('cloud-drive-connectors', 'images') || [];
    getUserDrive()
      .then(data => {
        this.userDrive = {
          name: data.name,
          title: data.name,
          isSelected: false,
          workspace: data.workspace,
          homePath: data.homePath,
        };
        // get providers from cloudDrives module, note that providers should already exist in module at this stage
        this.providers = cloudDrives.getProviders();
        // get image paths from cloudDrive connectors addon
        if (this.connectorsImages && this.connectorsImages.length !== 0) {
          Object.values(this.providers).forEach((provider) => {
            provider.image = Object.values(this.connectorsImages[0]).find(connector => connector.id === provider.id).path;
          });
        }
        this.$emit('connectors-loaded', this.providers);

      }).catch(err => {
        this.alert = { message: err.message, type: 'error' };
      });
  },
  mounted() {
    this.init();
  },
  methods: {
    disconnectFromCloudDrive: function(provider) {
      this.$set(provider, 'loading', true);
      disconnect(this.userDrive.workspace, provider.user, provider.id)
        .then(() => {
          // after disconnect successful
          this.$set(provider, 'user', null);
          this.$emit('display-alert', this.$t('cloudDriveSettings.alert.disconnectSuccessMessage'));
        }).catch(() => {
          this.$emit('display-alert', this.$t('cloudDriveSettings.alert.disconnectErrorMessage'), 'error');
        }).finally(() => {
          // end loading disconnect button
          this.$set(provider, 'loading', false);
        });
    },
    connectToCloudDrive: function(provider) {
      // start loading connect button
      this.$set(provider, 'loading', true);
      this.$emit('connectors-loaded', this.providers);
      // init cloudDrives module with Personal Documents workspace and path recieved in getUserDrive()
      // note: cloudDrives.init() also is called by server
      // initialize cloud drive context node
      cloudDrives.init(this.userDrive.workspace, this.userDrive.homePath);
      this.connectingProvider = provider.id;
      // show progress line at the top in composer
      this.$emit('updateProgress', { progress: 0 });
      const fullProgress = 100; // means 100%
      cloudDrives.connect(provider.id).then(
        data => {
          this.openDriveFolder(data.drive.path, data.drive.title); // display drive in composer
          this.drivesInProgress = Object.assign({}, this.drivesInProgress, {[data.drive.title]: fullProgress });
          this.$emit('updateDrivesInProgress', { drives: this.drivesInProgress }); // drives update in parent component
          this.$emit('updateProgress', { progress: fullProgress });
          const latency = 3000;
          setTimeout(() => {
            // drives deletion after latency, cause emitting events from component to component can take some time
            delete this.drivesInProgress[data.drive.title]; // connection is finished, so remove drive from drivesInProgress
            this.$emit('updateDrivesInProgress', { drives: this.drivesInProgress }); // drives update in parent component
            // hide progress line at the top of composer
            // note: this will hide progress line after any connecting drive is finish its connecting
            // if another drive is in connecting progress progress line will appear again, but it's hiding can be visible to user
            this.$emit('updateProgress', { progress: null });
          }, latency);
          const userEmail = data.drive.email;
          const settings = {
            connector: provider.id,
            account: userEmail
          };
          saveUserSettings(settings).then(() => {
            // after connect successful
            if (data.drive.connected) {
              this.$set(provider, 'user', userEmail);
            }
            this.$emit('display-alert', this.$t('cloudDriveSettings.alert.successMessage'));
          }).catch(() => {
            this.$emit('display-alert', this.$t('cloudDriveSettings.alert.errorSaveUserSettings'), 'error');
          }).finally(() => {
            // end loading connect button
            this.$set(provider, 'loading', false);
          });
        },
        (error) => {
          if (error) {
            this.alert = { message: error, type: 'error' };
          } else {
            // if error is undefined/null action was cancelled
            this.alert = { message: 'Canceled', type: 'info' };
          }
          // end loading connect button
          this.$set(provider, 'loading', false);
          this.$emit('display-alert', this.$t('cloudDriveSettings.alert.errorMessage'), 'error');

          this.$emit('updateProgress', { progress: null }); // hide progress line at the top of composer
        },
        progressData => {
          if (progressData.drive.title) {
            this.drivesInProgress = Object.assign({}, this.drivesInProgress, {[progressData.drive.title]: progressData.progress });
            // update drivesInProgress in attachmentsComposer, so display drive actual progress at every time progress updates
            this.$emit('updateDrivesInProgress', { drives: this.drivesInProgress }); // drives update in parent component
            this.openDriveFolder(progressData.drive.path, progressData.drive.title); // display drive in composer
          }

          this.$emit('updateProgress', { progress: progressData.progress }); // update progress at the top of composer
        }
      );
    },
    openDriveFolder: function(path, title) {
      // createdDrive should consist of the same properties as drives in exoAttachments as it will be added to existing drives array
      const createdDrive = {
        name: title,
        title: title,
        path: path ? path.split('/').pop() : '',
        // class name should be one of the existing platform icon classes, it's a drive shortcut
        driveTypeCSSClass: `uiIconEcmsDrive-${this.connectingProvider}`,
        type: 'drive',
        css: 'uiIcon16x16FolderDefault uiIcon16x16nt_folder',
        driverType: 'Personal Drives',
        isCloudDrive: true
      };
      this.$emit('addDrive', createdDrive); // display drive in "My drives" section
      // note: after next drives fetching in composer this drive will be replaced by drive recieved from rest
    },
    capitalized(value) { // capitalize the first letter of value
      return typeof value !== 'string' ? '' :  value.charAt(0).toUpperCase() + value.slice(1);
    },
    init() {
      getUserSettings().then(res => {
        Object.keys(res).forEach((element, index) => {
          Object.values(this.providers).forEach((provider) => {
            if (element === provider.id) {
              this.$set(provider, 'user', Object.values(res)[index]);
            }
          });
        });
      });
    }
  },
};
</script>