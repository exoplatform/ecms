<template>
  <v-app id="editors-admin" class="VuetifyApp" flat>
    <v-container pa-0>
      <v-layout row mx-0 class="white">
        <v-flex d-flex xs12 px-3>
          <v-layout mx-0 align-center>
            <v-flex d-flex xs12>
              <v-card flat color="transparent">
                <a @click="navigateTo('tasks/my-task')">
                  <v-card-text class="body-1 text-uppercase color-title px-0">
                    {{ this.$t('editors.admin.title') }}
                  </v-card-text>
                </a>
              </v-card>
            </v-flex>
          </v-layout>
        </v-flex>
      </v-layout>
      <v-simple-table>
        <template v-slot:default>
          <thead>
            <tr>
              <th class="text-left">Provider</th>
              <th class="text-left">Active</th>
              <th class="text-left">Actions</th>
            </tr>
          </thead>
          <tbody v-if="providers.length > 0">
            <tr v-for="item in providers" :key="item.provider">
              <td>{{ item.provider }}</td>
              <td>
                <v-switch
                  :input-value="item.active"
                  :label="`${item.active.toString()}`"
                  @change="changeStatus(item)"
                ></v-switch>
              </td>
              <td>
                <v-btn text icon color="indigo" @click.stop="showDialog = true">
                  <i class="material-icons">
                    settings
                  </i>
                </v-btn>
              </td>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
      <v-layout row justify-center>
        <v-dialog v-model="showDialog" persistent max-width="600">
          <v-card>
            <v-card-title class="headline grey lighten-2">Settings
              <v-btn icon @click="showDialog = false">
                <v-icon>close</v-icon>
              </v-btn>
            </v-card-title>
            <v-card-text>
            </v-card-text>
          </v-card>
        </v-dialog>
      </v-layout>
    </v-container>
  </v-app>
</template>

<script>
import { getInfo, postInfo } from "../EditorsAdminAPI";

export default {
    props: {
        entryPoint: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            providers: [],
            switcher: false,
            showDialog: false
        };
    },
    created() {
        this.getProviders();
    },
    methods: {
        getProviders() {
          getInfo(this.entryPoint).then(data => this.providers = data.editors);
        },
        changeStatus(provider) {
            const updateRest = provider.links.filter(({ rel, href }) => rel === "update")[0].href;
            postInfo(updateRest, { active: !provider.active }).then(data => { 
                this.providers.map(p => {
                  if (p.provider === provider.provider) {
                    p.active = !provider.active;
                  }
                })
            });
      }
    }
};
</script>
