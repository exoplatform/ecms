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
                      @change="changeStatus(item)"/>
                  </td>
                  <td class="text-center">
                    <v-btn 
                      text
                      icon
                      color="indigo" 
                      @click.stop="changeSettings(item)">
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
        <v-dialog
          v-model="showDialog" 
          width="500" 
          @click:outside="showDialog = false">
          <edit-dialog
            :provider="selectedProvider" 
            :search-url="services.identities"
            @onDialogClose="showDialog = false" />
        </v-dialog>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
import { getInfo, postInfo } from "../EditorsAdminAPI";

export default {
    props: {
        services: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            providers: [],
            switcher: false,
            showDialog: false,
            selectedProvider: null
        };
    },
    created() {
        this.getProviders();
    },
    methods: {
        getProviders() {
          getInfo(this.services.providers).then(data => this.providers = data.editors);
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
      },
      changeSettings(item) {
        this.selectedProvider = item;
        this.showDialog = true;
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