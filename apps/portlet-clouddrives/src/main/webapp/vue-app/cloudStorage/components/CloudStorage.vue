<template>
  <v-app id="cloudStorageApp">
    <v-container class="application-body pa-5 ma-0">
      <v-row class="ma-0">
        <v-col class="pa-0">
          <span class="text-title me-3">
            {{ $t("cloudStorage.label.title") }}
          </span>
        </v-col>
      </v-row>
      <v-row class="ma-0">
        <v-col class="pa-0 my-4">
          <v-simple-table
            :dense="true"
            class="uiGrid table-hover table-striped providersTable"
            style="width: 96%">
            <template #default>
              <thead>
                <tr class="providersTableRow">
                  <th class="text-left" style="width: 27%">{{ $t("cloudStorage.label.provider") }}</th>
                  <th class="text-left">{{ $t("cloudStorage.label.description") }}</th>
                  <th class="text-left" style="width: 5%">{{ $t("cloudStorage.label.active") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr class="providersTableRow">
                  <td>
                    <div>{{ $t("cloudStorage.label.cloudDrives") }}</div>
                  </td>
                  <td>
                    <div>
                      <div>{{ $t("cloudStorage.label.ConnectionToCloud") }}</div>
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <div>
                      <v-switch
                        :input-value="status"
                        :ripple="false"
                        color="#568dc9"
                        class="providersSwitcher"
                        @change="setCloudDriveStatus()" />
                    </div>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
import { isCloudDriveEnabled } from '../js/cloudDriveService';

export default {
  data: function() {
    return {
      status: false,
    };
  },
  created() {
    isCloudDriveEnabled().then(data => {
      this.status = data.result === 'true';
    });
  },
  methods: {
    setCloudDriveStatus() {
      try {
        fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`, {
          method: 'PATCH',
          credentials: 'include',
        })
          .then(resp => resp && resp.ok && resp.json());
      } catch (err) {
        this.error = err.message;
      }
    },
  }

};
</script>