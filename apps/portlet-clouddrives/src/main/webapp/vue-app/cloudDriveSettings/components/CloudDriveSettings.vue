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
    <template v-if="displayed">
      <v-card
        class="card-border-radius"
        flat>
        <v-list>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="title text-color">
                {{ $t("cloudDriveSettings.label.title") }}
              </v-list-item-title>
              <v-list-item-subtitle class="my-1 text-color">
                {{ $t("cloudDriveSettings.label.subtitle") }}
              </v-list-item-subtitle>
              <v-list-item-subtitle class="my-1 text-sub-title font-italic">
                <cloud-drive-settings-status :connectors="connectors">
                  <template slot="connectButton">
                    {{ $t("cloudDriveSettings.label.description") }}
                  </template>
                </cloud-drive-settings-status>
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
      <cloud-drive-settings-drawer :connectors="enabledConnectors" />
      <cloud-drive-connector @connectors-loaded="connectors = $event" @display-alert="displayAlert" />
      <cloud-drive-alert />
    </template>
  </v-app>
</template>

<script>
export default {
  data: () => ({
    connectors: {},
    displayed: true,
  }),
  computed: {
    enabledConnectors() {
      return this.connectors || {};
    },
  },
  created() {
    document.addEventListener('hideSettingsApps', () => this.displayed = false);
    document.addEventListener('showSettingsApps', () => this.displayed = true);
  },
  methods: {
    openDriveConnectorsDrawer() {
      this.$root.$emit('cloud-drive-connectors-drawer-open');
    },
    displayAlert(message, type) {
      this.$root.$emit('connectors-connection-alert', {
        message,
        type: type || 'success',
      });
    }
  }
};
</script>