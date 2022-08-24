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
  <div v-if="connectedConnector.length > 0">
    <div
      class="d-flex my-2"
      v-for="(connector, index) in connectedConnector"
      :key="index">
      <v-avatar
        tile
        size="24"
        height="auto">
        <img :src="connector.image" :alt="connector.image">
      </v-avatar>
      <a
        class="mx-2 my-auto"
        @click="openDriveConnectorsDrawer">
        {{ connector.user }}
      </a>
    </div>
  </div>
  <div
    v-else
    @click="openDriveConnectorsDrawer">
    <slot
      name="connectButton">
    </slot>
  </div>
</template>
<script>
export default {
  props: {
    connectors: {
      type: Object,
      default: () => null
    },
  },
  computed: {
    connectedConnector() {
      return Object.values(this.connectors).filter(connector => connector.user);
    },
  },
  methods: {
    openDriveConnectorsDrawer() {
      this.$root.$emit('cloud-drive-connectors-drawer-open');
    },
  }
};
</script>