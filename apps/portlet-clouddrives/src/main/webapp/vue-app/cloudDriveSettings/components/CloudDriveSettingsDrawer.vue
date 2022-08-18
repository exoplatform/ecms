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
      id="cloudDriveSettingsDrawer"
      class="cloudDriveSettingsDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right>
      <template slot="title">
        {{ $t("cloudDriveSettings.drawer.title") }}
      </template>
      <template slot="content">
        <v-list
          v-if="connectors && Object.keys(this.connectors).length !== 0"
          two-line>
          <v-list-item
            v-for="item in connectors"
            :key="item.id">
            <v-list-item-avatar class="rounded-0">
              <v-avatar
                tile
                size="40"
                height="auto">
                <img :src="`${item.image}`" :alt="item.name">
              </v-avatar>
            </v-list-item-avatar>
            <v-list-item-content>
              <template v-if="item.user">
                <v-list-item-title>
                  {{ $t('cloudDriveSettings.drawer.connectedAccountWith') }}:
                </v-list-item-title>
                <v-list-item-subtitle :title="item.user" class="font-italic">
                  {{ item.user }}
                </v-list-item-subtitle>
              </template>
              <template v-else>
                <v-list-item-title class="title">
                  {{ item.name }}
                </v-list-item-title>
              </template>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                class="btn"
                :loading="item.loading"
                @click="connect(item)">
                {{ $t("cloudDriveSettings.drawer.button.connect") }}
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
        <div
          v-else
          class="noEnabledConnectors d-flex flex-column align-center">
          <span class="uiIconCloudDriveConnector material-icons ma-5">cloud_off</span>
          <p>{{ $t('cloudDriveSettings.drawer.noConnector') }}</p>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
export default {
  props: {
    connectors: {
      type: Object,
      default: null,
    },
  },
  methods: {
    open() {
      if (this.$refs.cloudDriveSettingsDrawer) {
        this.$refs.cloudDriveSettingsDrawer.open();
      }
    },
    close() {
      if (this.$refs.cloudDriveSettingsDrawer) {
        this.$refs.cloudDriveSettingsDrawer.close();
      }
    },
    connect(providerId) {
      this.$root.$emit('cloud-drive-connect', providerId);
    }
  },
};
</script>
