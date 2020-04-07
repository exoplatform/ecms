<template>
  <v-app id="editors-admin" class="VuetifyApp">
    <v-container style="width: 95%" class="v-application--is-ltr">
      <div v-show="error" class="alert alert-error">
        {{ i18n.te(`${errorResourceBase}.${error}`) ? $t(`${errorResourceBase}.${error}`) : error }}
      </div>
      <v-row class="white">
        <v-col xs12 px-3>
          <h4 class="editorsTitle">
            {{ $t("editors.admin.title") }}
          </h4>
        </v-col>
      </v-row>
      <v-row>
        <v-col xs12>
          <v-simple-table :dense="true" class="uiGrid table table-hover table-striped providersTable">
            <template v-slot:default>
              <thead>
                <tr class="providersTableRow">
                  <th class="text-left">{{ $t("editors.admin.table.Provider") }}</th>
                  <th class="text-left">{{ $t("editors.admin.table.Description") }}</th>
                  <th class="text-left" style="width: 5%">{{ $t("editors.admin.table.Active") }}</th>
                  <th class="text-left" style="width: 5%">{{ $t("editors.admin.table.Permissions") }}</th>
                </tr>
              </thead>
              <tbody v-if="providers.length > 0">
                <tr 
                  v-for="item in providers" 
                  :key="item.provider" 
                  class="providersTableRow">
                  <td>
                    <div>
                      {{ i18n.te(`editors.admin.${item.provider}.name`) 
                        ? $t(`editors.admin.${item.provider}.name`) 
                        : item.provider 
                      }}
                    </div>
                  </td>
                  <td>
                    <div>
                      {{ i18n.te(`editors.admin.${item.provider}.description`) 
                        ? $t(`editors.admin.${item.provider}.description`) 
                        : "" 
                      }}
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <div>
                      <v-switch
                        :input-value="item.active"
                        :ripple="false"
                        color="#568dc9"
                        class="providersSwitcher"
                        @change="changeActive(item)" />
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <edit-dialog
                      :provider-name="item.provider"
                      :provider-link="item.links.self.href"
                      :search-url="services.identities"
                      :i18n="i18n" />
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
import { postData, getData } from "../EditorsAdminAPI";
import EditDialog from "./EditDialog.vue";

export default {
  components: {
    EditDialog
  },
  props: {
    services: {
      type: Object,
      required: true
    },
    i18n: {
      type: Object,
      required: true
    },
    language: {
      type: String,
      required: true
    },
    resourceBundleName: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      providers: [],
      switcher: false,
      error: null
    };
  },
  created() {
    this.getProviders();
  },
  methods: {
    async getProviders() {
      // services object contains urls for requests
      try {
        const data = await getData(this.services.providers);
        this.error = null;
        this.providers = data.editors;
        const resourcesPromises = this.providers.map(({ provider }) => this.getProviderResources(provider));
        Promise.all(resourcesPromises).then(res => {
          res.map(localized => {
            this.i18n.mergeLocaleMessage(this.language, localized.getLocaleMessage(this.language));
          });
        });
      } catch (err) {
        this.error = err.message;
      }
    },
    getProviderResources(providerId) {
      const resourceUrl = 
        `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.${providerId}.${this.resourceBundleName}-${this.language}.json`;
      return exoi18n.loadLanguageAsync(this.language, resourceUrl);
    },
    async changeActive(provider) {
      // getting rest for updating provider status
      try {
        const data = await postData(provider.links.self.href, { active: !provider.active });
        this.error = null;
        this.providers.map(p => {
          if (p.provider === provider.provider) {
            p.active = !provider.active;
          }
        });
      } catch (err) {
        this.error = err.message;
      }
    }
  }
};
</script>

<style scoped lang="less">
// TODO think about moving styles to separate file
// .editorsTitle {
//   color: #4d5466;
//   font-size: 24px;
//   position: relative;
//   overflow: hidden;

//   &:after {
//     border-bottom: 1px solid #dadada;
//     height: 11px;
//     content: "";
//     position: absolute;
//     width: 100%;
//     margin-left: 10px;
//   }
// }

// .providersTable {
//   border-left: 0;

//   &Row {
//     th,
//     td {
//       height: 20px;
//       padding: 5px 15px;
//     }

//     &:nth-child(even):hover > td,
//     &:nth-child(even) > td {
//       background: #f6f7fa !important;
//     }

//     &:nth-child(odd):hover > td,
//     &:nth-child(odd) > td {
//       background: #fff !important;
//     }
//   }
// }

// .providersSwitcher {
//   padding: 0;
//   margin: 0;
//   height: 25px;
// }

// .alert {
//   position: fixed;
//   top: 70px;
//   left: 50%;
//   transform: translate(-50%, 0);
//   z-index: 1000;
// }
</style>
