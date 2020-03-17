<template>
  <v-card class="provider uiPopup">
    <v-card-title class="headline popupHeader justify-space-between providerHeader">
      <span class="PopupTitle popupTitle providerHeaderTitle">{{ this.$t('editors.admin.modal.title') }}</span>
      <i class="uiIconClose providerHeaderClose" @click="closeDialog"></i>
    </v-card-title>
    <v-card-text class="popupContent providerContent">
      <v-container>
        <v-row class="providerName">
          {{ $t(`editors.admin.${provider.provider}.name`) }}
        </v-row>
        <v-row class="search">
          <v-col>
            <label class="searchLabel">{{ this.$t('editors.admin.modal.SearchLabel') }}</label>
            <v-autocomplete
              v-model="select"
              :loading="loading"
              :items="items"
              :search-input.sync="search"
              :menu-props="{ maxHeight: 200 }"
              color="#333"
              class="searchPermissions"
              cache-items
              flat
              hide-no-data
              hide-details
              solo-inverted
              hide-selected
              chips
              multiple
              attach
              dense
              dark
              item-text="displayName"
              item-value="name"
              append-icon="">
              <template slot="selection" slot-scope="data">
                <v-chip
                  :input-value="data"
                  close 
                  light
                  small
                  class="chip--select-multi searchChip"
                  @click:close="handleCloseClick(data.item)">
                  {{ data.item.displayName }}
                </v-chip>
              </template>
              <template 
                slot="item" 
                slot-scope="{ item, parent }" 
                class="permissionsItem">
                <v-list-tile-avatar><img :src="item.avatarUrl" class="permissionsItemAvatar"></v-list-tile-avatar>
                <v-list-tile-content class="permissionsItemName">
                  <v-list-tile-title v-html="parent.genFilteredText(item.displayName)" />
                </v-list-tile-content>
              </template>
            </v-autocomplete>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <label class="searchLabel" style="margin-bottom: 10px">{{ this.$t('editors.admin.modal.WithPermissions') }}</label>
            <v-col cols="12" md="8"><ul><li v-for="permission in existingPermissions" :key="permission">{{ permission }}
              <i 
                v-if="permission.length > 0"
                class="uiIconDelete permissionsItemDelete"
                @click="removePermission(permission)"></i>
            </li></ul></v-col>
            <!-- <ul v-if="items.length > 0" class="permissionsList">
              <li v-for="permission in items" :key="permission.name" class="permissionsItem permissionsItem--large">
                <v-tooltip bottom>
                  <template v-slot:activator="{ on }">
                    <div v-on="on">
                      <img :src="permission.avatarUrl" class="permissionsItemAvatar permissionsItemAvatar--large">
                      <span class="permissionsItemName">{{ permission.displayName }}</span>
                    </div>
                  </template>
                  <span>{{ permission.name }}</span>
                </v-tooltip>
                <i 
                  class="uiIconDelete permissionsItemDelete"
                  @click="removePermission(permission.name)">
                </i>
              </li>
            </ul> -->
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions class="dialogFooter footer">
      <v-btn
        style="margin-right: 10px"
        class="btn btn-primary dialogFooterBtn"
        text
        @click="saveChanges">
        {{ this.$t('editors.admin.buttons.Save') }}
      </v-btn>
      <v-btn
        class="btn dialogFooterBtn"
        text
        @click="closeDialog">
        {{ this.$t('editors.admin.buttons.Cancel') }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import { getInfo, postInfo } from "../EditorsAdminAPI";

export default {
  props: {
    provider: {
      type: Object,
      default: function () {
        return {}
      }
    },
    searchUrl: {
      type: String,
      required: true
    }
  },
  data () {
      return {
        loading: false,
        items: [],
        search: null,
        select: null,
        existingPermissions: this.provider.permissions
      }
  },
  computed: {
    editedItems: function() {
      return this.select ? this.existingPermissions.concat(this.select) : this.existingPermissions;
    }
  },
  watch: {
    search (val) {
      return val && val !== this.select && this.querySelections(val);
    },
    provider: function(newProvider) {
      this.existingPermissions = newProvider.permissions;
    }
  },
  methods: {
      querySelections (v) {
        this.loading = true;
        getInfo(`${this.searchUrl}/${v}`).then(data => {
          this.items = data.identities.filter(identity => ({
            displayName: (identity.displayName || '').toLowerCase().indexOf((v || '').toLowerCase()) > -1,
            ...identity
          }));
          this.loading = false;
        });
      },
      handleCloseClick(value) {
        this.select = this.select.filter(item => item !== value.name);
      },
      saveChanges() {
        const updateRest = this.provider.links.filter(({ rel, href }) => rel === "update")[0].href;
        postInfo(updateRest, { permissions: this.editedItems }).then(data => { 
          this.provider.permissions = this.editedItems;
          this.closeDialog();
        });
      },
      closeDialog() {
        this.editedItems = [];
        this.select = null;
        this.existingPermissions = this.provider.permissions;
        this.$emit('onDialogClose');
      },
      removePermission(name) {
        this.existingPermissions = this.existingPermissions.filter(perm => perm !== name);
      }
  }
}
</script>

<style scoped lang="less">
.provider {
  max-width: 100%;
  border: 0;

  &Header {
    padding: 12px 10px 12px 15px;
    height: 20px;

    &Title {
      line-height: 18px;
    }

    &Close { 
      top: 13px; 
    }
  }

  &Content {
    padding: 15px !important;
    min-height: 350px;
  }

  &Name {
    color: #333333;
    font-weight: bold;
    margin-bottom: 10px;
  }

  .search {
    &Label {
      color: #333333;
      margin-bottom: 10px;
    }

    &Permissions {
      border: Solid 2px #e1e8ee;
      border-radius: 5px;
      box-shadow: none;
      padding-top: 0 !important;

      &:focus {
        border-color:#a6bad6;
        box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 5px #c9d5e6;
      }
    }

    &Chip.v-chip {
      background: #ccddef;
      color: #568dc9;
      border: 1px solid #568dc9;
      border-radius: 15px;
      margin: 4px 10px 4px 4px;
      font-size: 13px;
    }
  }
  
  .permissionsList {
    max-height: 300px;
    padding-left: 0px;
    overflow-y: auto;
  }
  
  .permissionsItem {
    display: flex;
    align-items: center;
    height: 30px;

    &--large {
      height: 40px;
      margin: 5px 0;
      justify-content: space-between;
    }

    &Avatar {
      max-height: 26px;
      margin-right: 5px;

      &--large {
        max-height: 40px;
      }
    }

    &Name {
      color: #303030;
      font-family: inherit;
      font-size: 13px;
      line-height: 18px;
    }

    &Delete {
      cursor: pointer;
    }
  }
  
  .dialogFooter {
    align-items: center;
    justify-content: center;
    padding-bottom: 20px;

    &Btn.v-size--default {
      font-family: Helvetica,arial,sans-serif;
      font-size: 15px;
      padding: 9px 20px;
      color: #4d5466;
      border: 1px solid #e1e8ee;
      box-shadow: none;
      box-sizing: border-box;
      border-radius: 3px;
      height: 40px;
      letter-spacing: normal;
      min-width: 80px;

      &:hover {
        background-color: #e1e8ee;
      }

      &.btn-primary {
        color: #fff;

        &:hover {
          background-color: #476a9c;
          background-position: 0 -45px;
          transition: background-position .1s linear;
        }
      }
    }
  }
}
</style>