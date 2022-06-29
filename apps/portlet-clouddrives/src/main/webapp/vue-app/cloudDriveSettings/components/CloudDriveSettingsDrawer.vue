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
      ref="agendaConnectorsDrawer"
      class="agendaConnectorsDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right>
      <template slot="title">
        {{ $t('agenda.connectYourPersonalAgenda') }}
      </template>
      <template slot="content">
        <v-list
          two-line>
          <v-list-item
            v-for="connector in enabledConnectors"
            :key="connector.name">
            <v-list-item-avatar class="rounded-0">
              <v-avatar tile size="40">
                <img :src="connector.avatar">
              </v-avatar>
            </v-list-item-avatar>
            <v-list-item-content>
              <v-alert
                v-if="!connector.canConnect"
                type="error"
                class="my-auto">
                {{ $t('agenda.connectoInitializationFailed') }}
              </v-alert>
              <template v-else-if="connector.connected">
                <v-list-item-title>
                  {{ $t('agenda.connectedAccountWith') }}:
                </v-list-item-title>
                <v-list-item-subtitle :title="connector.user" class="font-italic">
                  {{ connector.user }}
                </v-list-item-subtitle>
              </template>
              <template v-else>
                <v-list-item-title class="title">
                  {{ $t(connector.name) }}
                </v-list-item-title>
              </template>
            </v-list-item-content>
            <v-list-item-action v-if="connector.canConnect">
              <v-btn
                v-if="connector.isSignedIn && connector.user"
                :loading="connector.loading"
                class="btn"
                @click="disconnect(connector)">
                {{ $t('agenda.disconnect') }}
              </v-btn>
              <v-btn
                v-else
                :loading="connector.loading"
                class="btn"
                @click="connect(connector)">
                {{ $t('agenda.connect') }}
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-list-item>
            <v-list-item-content>
              <div class="d-flex">
                <span class="my-auto pe-4">
                  <v-icon
                    size="16"
                    class="text-light-color"
                    depressed>
                    fa-info-circle
                  </v-icon>
                </span>
                <span class="my-auto me-auto font-italic text-light-color">
                  {{ $t('agenda.allowedToConnectOnlyOneConnector') }}
                </span>
              </div>
            </v-list-item-content>
          </v-list-item>
          <v-card-text v-show="errorMessage" class="errorMessage">
            <v-alert type="error">
              {{ errorMessage }}
            </v-alert>
          </v-card-text>
        </v-list>
      </template>
    </exo-drawer>
    <exo-confirm-dialog
      ref="confirmConnectDialog"
      :title="confirmConnectDialogLabels.title"
      :message="confirmConnectDialogLabels.message"
      :ok-label="confirmConnectDialogLabels.ok"
      :cancel-label="confirmConnectDialogLabels.cancel"
      @ok="confirmConnect" />
  </div>
</template>

<script>
export default {
  props: {
    connectors: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    connectionInProgress: false,
    errorMessage: '',
    selectedConnector: null
  }),
  computed: {
    enabledConnectors() {
      return this.connectors && this.connectors.slice().filter(connector => connector.enabled) || [];
    },
    confirmConnectDialogLabels() {
      return {
        title: this.$t('agenda.agendaConnectors.confirmConnectDialog.title'),
        message: this.$t('agenda.agendaConnectors.confirmConnectDialog.message'),
        ok: this.$t('agenda.agendaConnectors.confirmConnectDialog.ok'),
        cancel: this.$t('agenda.agendaConnectors.confirmConnectDialog.cancel')
      };
    },
  },
  created() {
    this.$root.$on('agenda-connectors-drawer-open', this.open);
    this.$root.$on('agenda-connector-connected', () => {
      // Avoiding closing the drawer automatically
      // when the user didn't pressed the connect button
      if (this.connectionInProgress) {
        this.close();
      }
    });
  },
  methods: {
    open() {
      this.connectionInProgress = false;
      this.$root.$emit('agenda-connectors-init');
      if (this.$refs.agendaConnectorsDrawer) {
        this.$refs.agendaConnectorsDrawer.open();
      }
    },
    close() {
      if (this.$refs.agendaConnectorsDrawer) {
        this.$refs.agendaConnectorsDrawer.close();
      }
    },
    connect(connector) {
      this.connectionInProgress = true;
      this.selectedConnector = connector;
      if (this.enabledConnectors.some(c => c.isSignedIn && c.user)) {
        this.$refs.confirmConnectDialog.open();
      }
      else {
        this.confirmConnect();
      }
    },
    confirmConnect() {
      this.$root.$emit('agenda-connector-connect', this.selectedConnector);
    },
    disconnect(connector) {
      this.connectionInProgress = true;
      this.$root.$emit('agenda-connector-disconnect', connector);
    },
  },
};
</script>