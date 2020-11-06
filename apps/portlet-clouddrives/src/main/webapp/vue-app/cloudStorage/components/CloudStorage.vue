<template>
  <v-app id="cloudStorageApp" class="VuetifyApp">
    <v-container style="width: 95%" class="v-application--is-ltr">
      <v-row class="white">
        <v-col xs12 px-3>
          <h4 class="Title">
            {{ $t("cloudStorage.label.title") }}
          </h4>
        </v-col>
      </v-row>
      <v-row>
        <v-col xs12>
          <v-simple-table :dense="true" class="uiGrid table table-hover table-striped providersTable">
            <template v-slot:default>
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
                        @change="setCloudDriveStatus()"/>
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
import { isCloudDriveEnabled } from "../../cloudDriveService";

export default {
  data: function() {
    return {
      status: false,
    };
  },
  created() {
    isCloudDriveEnabled().then(data => {
      this.status = data.result === "true";
    });
  },
  methods: {
    setCloudDriveStatus() {
      try {
        fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`, {
          method: "PATCH",
          credentials: "include",
        })
          .then(resp => resp && resp.ok && resp.json());
      } catch (err) {
        this.error = err.message;
      }
    },
  }

};
</script>