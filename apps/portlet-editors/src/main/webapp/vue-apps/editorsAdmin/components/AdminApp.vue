<template>
  <v-app id="editors-admin" class="VuetifyApp">
    <v-container style="width: 95%" class="v-application--is-ltr">
      <div v-show="error" class="alert alert-error">{{ $t(error) }}</div>
      <v-row class="white">
        <v-col xs12 px-3>
          <h4 class="editorsTitle">
            {{ $t('editors.admin.title') }}
          </h4>
        </v-col>
      </v-row>
      <v-row>
        <v-col xs12>
          <v-simple-table :dense="true" class="uiGrid table table-hover table-striped providersTable">
            <template v-slot:default>
              <thead>
                <tr class="providersTableRow">
                  <th class="text-left">{{ $t('editors.admin.table.Provider') }}</th>
                  <th class="text-left">{{ $t('editors.admin.table.Description') }}</th>
                  <th class="text-left" style="width: 5%">{{ $t('editors.admin.table.Active') }}</th>
                  <th class="text-left" style="width: 5%">{{ $t('editors.admin.table.Permissions') }}</th>
                </tr>
              </thead>
              <tbody v-if="providers.length > 0">
                <tr 
                  v-for="item in providers" 
                  :key="item.provider" 
                  class="providersTableRow">
                  <td><div>{{ $t(`editors.admin.${item.provider}.name`) }}</div></td>
                  <td><div>{{ $t(`editors.admin.${item.provider}.description`) }}</div></td>
                  <td class="center actionContainer">
                    <div>
                      <v-switch
                        :input-value="item.active"
                        :ripple="false"
                        color="#568dc9"
                        class="providersSwitcher"
                        @change="changeStatus(item)"/>
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <a 
                      data-placement="bottom" 
                      rel="tooltip" 
                      class="actionIcon" 
                      data-original-title="Edit" 
                      @click.stop="changeSettings(item)">
                      <i class="uiIconEdit uiIconLightGray"></i>
                    </a>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-col>
        <v-dialog
          v-model="showDialog" 
          width="500"
          style="overflow-x: hidden"
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
import { getInfo, postInfo, parsedErrorMsg } from "../EditorsAdminAPI";

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
            selectedProvider: null,
            error: null
        };
    },
    created() {
        this.getProviders();
    },
    methods: {
        getProviders() {
          getInfo(this.services.providers).then(data => {
            this.error = null;
            this.providers = data.editors;
          }).catch(err => { this.error = parsedErrorMsg(err); });
        },
        changeStatus(provider) {
            const updateRest = provider.links.filter(({ rel, href }) => rel === "update")[0].href;
            postInfo(updateRest, { active: !provider.active }).then(data => { 
                this.error = null;
                this.providers.map(p => {
                  if (p.provider === provider.provider) {
                    p.active = !provider.active;
                  }
                })
            }).catch(err => this.error = parsedErrorMsg(err));
      },
      changeSettings(item) {
        this.selectedProvider = item;
        this.showDialog = true;
      }
    }
};
</script>

<style scoped lang="less">
.editorsTitle {
  color: #4d5466;
  font-size: 24px;
  position: relative;
  overflow: hidden;

  &:after {
    border-bottom: 1px solid #dadada;
    height: 11px;
    content: "";
    position: absolute;
    width: 100%;
    margin-left: 10px;
  }
}

.providersTable {
  border-left: 0;

  &Row {
    th, td {
      height: 20px;
      padding: 5px 15px;
    }

    &:nth-child(even):hover>td, &:nth-child(even)>td {
      background: #f6f7fa !important;
    }

    &:nth-child(odd):hover>td, &:nth-child(odd)>td {
      background: #fff !important;
    }
  }
}

.providersSwitcher {
  padding: 0;
  margin: 0;
  height: 25px;
}

.alert {
  position: fixed;
  top: 70px;
  left: 50%;
  transform: translate(-50%, 0);
  z-index: 1000;
}
</style>