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
<template>
  <v-app>
    <v-card
      class="border-radius ma-4"
      flat>
      <v-list two-line>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color">
              test cloud drive
            </v-list-item-title>
            <v-list-item-subtitle class="my-3 text-sub-title font-italic">
              {{ $t('mailIntegration.settings.connectMail.description') }}
            </v-list-item-subtitle>
            <v-list-item-subtitle :title="displayAccountTooltip">
              <span class="my-auto text-capitalize text-truncate"> {{ emailName }} </span>
              <span class="my-auto text-truncate"> {{ account }} </span>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
              icon
              @click="openDrawer">
              <i class="uiIconEdit uiIconLightBlue pb-2"></i>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
    <cloud-drive-settings-drawer ref="CloudDriveSettingsDrawer" />
  </v-app>
</template>

<script>
export default {
  data: () => ({
    mailIntegrationEnabled: false,
    displayed: true,
    editMode: false,
    emailName: '',
    account: '',
    mailIntegrationSetting: [],
  }),
  computed: {
    displayAccountTooltip() {
      return `${this.emailName} ${this.account}`;
    }
  },
  methods: {
    openDrawer() {
      this.$refs.CloudDriveSettingsDrawer.openDrawer();
    }
  },
};
</script>