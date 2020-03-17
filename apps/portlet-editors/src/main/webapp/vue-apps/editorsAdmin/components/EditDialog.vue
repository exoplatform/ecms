<template>
  <v-card>
    <v-card-title class="headline grey lighten-2 justify-space-between" primary-title>
      {{ this.$t('editors.admin.modal.title') }}
      <v-btn icon @click="closeDialog">
        <v-icon>close</v-icon>
      </v-btn>
    </v-card-title>
    <v-card-text style="min-height: 350px">
      <v-container>
        <v-row class="providerName">
          {{ $t(`editors.admin.${provider.provider}.name`) }}
        </v-row>
        <v-row>
          <v-col>
            <label class="searchLabel" style="margin-bottom: 10px">Enter user or group name</label>
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
                slot-scope="{ item }" 
                class="permissionsItem">
                <v-list-tile-avatar><img :src="item.avatarUrl" class="permissionsItemAvatar"></v-list-tile-avatar>
                <v-list-tile-content class="permissionsItemName">
                  {{ item.displayName }}
                </v-list-tile-content>
              </template>
            </v-autocomplete>
          </v-col>
        </v-row>
        <v-row>
          <v-col>
            <label class="searchLabel" style="margin-bottom: 10px">Who has permission</label>
            <v-col cols="12" md="8"><ul><li v-for="permission in existingPermissions" :key="permission">{{ permission }}
              <v-icon 
                v-if="permission.length > 0" 
                style="color: #568dc9" 
                @click="removePermission(permission)">
                delete
              </v-icon>
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
                <v-icon
                  style="color: #568dc9" 
                  @click="removePermission(permission.name)">
                  delete
                </v-icon>
              </li>
            </ul> -->
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions class="dialogFooter footer">
      <v-btn 
        color="primary" 
        style="margin-right: 10px"
        class="btn btn-primary dialogFooterBtn"
        text
        @click="saveChanges">
        Save
      </v-btn>
      <v-btn
        class="btn dialogFooterBtn"
        text
        @click="closeDialog">
        Cancel
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
        this.loading = true
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
          console.log(data);
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

<style scoped>
.providerName {
  color: #333;
  font-weight: bold;
  margin-bottom: 10px;
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
}

.permissionsItem--large {
  height: 40px;
  margin: 5px 0;
  justify-content: space-between;
}

.permissionsItemAvatar {
  max-height: 26px;
  margin-right: 5px;
}

.permissionsItemAvatar--large {
  max-height: 40px;
}

.permissionsItemName {
  color: #303030;
  font-family: inherit;
  font-size: 13px;
  line-height: 18px;
}

.searchLabel {
  color: #333;
}

.searchPermissions {
  border: Solid 2px #e1e8ee;
  border-radius: 5px;
  box-shadow: none;
  padding-top: 0 !important;
}

.searchPermissions:focus {
  border-color:#a6bad6;
  box-shadow:inset 0 1px 1px rgba(0,0,0,.075),0 0 5px #c9d5e6;
}

.searchChip.v-chip {
  background: #ccddef;
  color: #568dc9;
  border: 1px solid #568dc9;
  border-radius: 15px;
  margin: 4px 10px 4px 4px;
  font-size: 13px;
}

.dialogFooter {
  align-items: center;
  justify-content: center;
  padding-bottom: 20px;
}

.btn.v-size--default.dialogFooterBtn {
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
}

.btn.btn-primary.v-size--default.dialogFooterBtn {
  color: #fff !important;
}
</style>