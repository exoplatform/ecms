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
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color">
              {{ $t("cloudDriveSettings.label.title") }}
            </v-list-item-title>
            <v-list-item-subtitle class="my-3 text-color">
              {{ $t("cloudDriveSettings.label.subtitle") }}
            </v-list-item-subtitle>
            <v-list-item-subtitle class="my-3 text-sub-title font-italic" @click="openDriveConnectorsDrawer">
              {{ $t("cloudDriveSettings.label.description") }}
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
              icon
              @click="openDriveConnectorsDrawer">
              <em class="uiIconEdit uiIconLightBlue pb-2"></em>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
    <cloud-drive-settings-drawer ref="cloudDriveSettingsDrawer" :connectors="enabledConnectors" />
    <cloud-drive-connector @connectors-loaded="connectors = $event" />
  </v-app>
</template>

<script>
export default {
  data: () => ({
    connectors: {},
  }),
  computed: {
    enabledConnectors() {
      return Object.keys(this.connectors).reverse().map(key => ({...this.connectors[key]})) || [];
    },
  },
  methods: {
    openDriveConnectorsDrawer() {
      this.$refs.cloudDriveSettingsDrawer.open();
    }
  }
};
</script>