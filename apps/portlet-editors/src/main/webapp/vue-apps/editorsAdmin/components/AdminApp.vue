<template>
  <v-app id="editors-admin" class="VuetifyApp">
    <v-container style="width: 95%" class="v-application--is-ltr">
      <v-row class="white">
        <v-col xs12 px-3>
          <h4 class="editorsTitle">
            {{ $t('editors.admin.title') }}
          </h4>
        </v-col>
      </v-row>
      <v-row>
        <v-col xs12>
          <v-simple-table class="providersTable">
            <template v-slot:default>
              <thead>
                <tr>
                  <th class="text-left">{{ $t('editors.admin.table.Provider') }}</th>
                  <th class="text-left">{{ $t('editors.admin.table.Description') }}</th>
                  <th class="text-left" style="width: 5%">{{ $t('editors.admin.table.Active') }}</th>
                  <th class="text-left" style="width: 5%">{{ $t('editors.admin.table.Permissions') }}</th>
                </tr>
              </thead>
              <tbody v-if="providers.length > 0">
                <tr v-for="item in providers" :key="item.provider">
                  <td>{{ $t(`editors.admin.${item.provider}.name`) }}</td>
                  <td>{{ $t(`editors.admin.${item.provider}.description`) }}</td>
                  <td>
                    <v-switch
                      :input-value="item.active"
                      class="v-input--switch--inset"
                      @change="changeStatus(item)"
                    ></v-switch>
                  </td>
                  <td class="text-center">
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
        </v-col>
      </v-row>
      <div class="text-center">
        <v-dialog
          v-model="showDialog"
          width="600"
        >
          <v-card>
            <v-card-title
              class="headline grey lighten-2 justify-space-between"
              primary-title
            >
              {{ this.$t('editors.admin.modal.title') }}
              <v-btn icon @click="showDialog = false">
                <v-icon>close</v-icon>
              </v-btn>
            </v-card-title>
            <v-card-text>
              <div class="v-skeleton-loader__bone blockProvidersInner">
                <div class="permissionsSkeleton">
                  <div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText">
                  </div>
                  <div class="permissionsRow">
                    <div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton searchLabel">
                    </div>
                    <div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText searchField">
                    </div>
                  </div>
                  <div class="permissionsRow permissionsRow--btns">
                    <div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonButton">
                    </div>
                    <div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonButton">
                    </div>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-dialog>
      </div>
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

<style scoped>
  .editorsTitle {
    color: #4d5466;
    font-size: 24px;
    position: relative;
    overflow: hidden;
  }

  .editorsTitle:after {
    border-bottom: 1px solid #dadada;
    height: 11px;
    content: "";
    position: absolute;
    width: 100%;
    margin-left: 10px;
  }

  .permissionsSkeleton {
    display: flex;
    flex-direction: column;
    padding: 15px 15px 0;
    width: 100%;
  }

  .permissionsRow {
    display: flex;
    align-items: center;
  }

  .permissionsRow--btns {
    justify-content: center;
    margin-top: 10px;
  }

  .permissionsRow > .searchLabel {
    width: 10%;
    height: 12px;
    border-radius: 15px;
    margin-top: 16px;
    margin-bottom: 16px;
    margin-right: 10px;
  }

  .searchField {
    height: 20px;
  }

  .skeletonButton {
    width: 20%;
    height: 40px;
    border-radius: 15px;
    margin-right: 10px;
  }
</style>