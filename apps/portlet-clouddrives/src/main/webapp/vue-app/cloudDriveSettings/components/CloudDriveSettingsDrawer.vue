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
  <div>
    <exo-drawer
      ref="cloudDriveSettingsDrawer"
      class="cloudDriveSettingsDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right>
      <template slot="title">
        {{ $t("cloudDriveSettings.drawer.title") }}
      </template>
      <template slot="content">
        <v-list
          two-line>
          <v-list-item>
            <v-list-item-avatar class="rounded-0">
              <v-avatar tile size="40">
                <img src="/clouddrives/skin/images/Google.png" :alt="$t('cloudDriveSettings.drawer.google')">
              </v-avatar>
            </v-list-item-avatar>
            <v-list-item-content>
              <template>
                <v-list-item-title class="title">
                  {{ $t("cloudDriveSettings.drawer.google") }}
                </v-list-item-title>
              </template>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                class="btn">
                {{ $t("cloudDriveSettings.drawer.button.connect") }}
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-list-item>
            <v-list-item-avatar class="rounded-0">
              <v-avatar
                tile
                size="40"
                height="auto">
                <img src="/clouddrives/skin/images/OneDrive.png" :alt="$t('cloudDriveSettings.drawer.onedrive')">
              </v-avatar>
            </v-list-item-avatar>
            <v-list-item-content>
              <template>
                <v-list-item-title class="title">
                  {{ $t("cloudDriveSettings.drawer.onedrive") }}
                </v-list-item-title>
              </template>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                class="btn">
                {{ $t("cloudDriveSettings.drawer.button.connect") }}
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
export default {
  methods: {
    open() {
      this.connectionInProgress = false;
      this.$root.$emit('agenda-connectors-init');
      if (this.$refs.cloudDriveSettingsDrawer) {
        this.$refs.cloudDriveSettingsDrawer.open();
      }
    },
    close() {
      if (this.$refs.cloudDriveSettingsDrawer) {
        this.$refs.cloudDriveSettingsDrawer.close();
      }
    },
  },
};
</script>
