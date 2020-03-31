<template>
  <v-dialog 
    v-model="showDialog" 
    width="500" 
    style="overflow-x: hidden"
    @click:outside="showDialog = false">
    <template v-slot:activator="{ on }">
      <a 
        data-placement="bottom" 
        rel="tooltip" 
        class="actionIcon" 
        data-original-title="Edit" 
        v-on="on">
        <i class="uiIconEdit uiIconLightGray"></i>
      </a>
    </template>

    <v-card class="provider uiPopup">
      <div v-show="error" class="alert alert-error">{{ $t(error) }}</div>
      <v-card-title class="headline popupHeader justify-space-between providerHeader mb-0">
        <span class="PopupTitle popupTitle providerHeaderTitle">{{ this.$t("editors.admin.modal.title") }}</span>
        <i class="uiIconClose providerHeaderClose" @click="closeDialog"></i>
      </v-card-title>
      <v-card-text class="popupContent providerContent pa-4">
        <v-container class="permissions px-0">
          <v-row class="providerName ms-0">
            {{ i18n.te(`editors.admin.${providerName}.name`) ? $t(`editors.admin.${providerName}.name`) : providerName }}
          </v-row>
          <v-row class="search">
            <v-col>
              <label class="searchLabel">{{ this.$t("editors.admin.modal.SearchLabel") }}</label>
              <v-autocomplete
                v-model="selectedItems"
                :loading="searchLoading"
                :items="searchResults"
                :search-input.sync="search"
                :menu-props="{ maxHeight: 140 }"
                return-object
                color="#333"
                class="searchPermissions pt-0"             
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
                item-value="id"
                append-icon=""
                @input="selectionChange">
                <template slot="selection" slot-scope="data">
                  <v-chip
                    :input-value="data"
                    close
                    light
                    small
                    class="chip--select-multi searchChip"
                    @click:close="removeSelection(data.item)">
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
            <v-col class="permissionsContainer">
              <div class="d-flex justify-space-between">
                <label class="searchLabel ma-0">{{ this.$t("editors.admin.modal.WithPermissions") }}</label>
                <div class="d-flex align-center">
                  <v-checkbox
                    v-model="accessibleToAll"
                    :ripple="false"
                    color="#578dc9"
                    dense
                    class="ma-0"
                    data-test="everybodyCheckbox" />
                  <label style="color: #333">{{ this.$t("editors.admin.modal.Everybody") }}</label>
                </div>
              </div>
              <v-row v-if="!accessibleToAll">
                <v-col v-if="existingPermissions.length > 0">
                  <ul class="permissionsList ps-0">
                    <li
                      v-for="permission in existingPermissions"
                      :key="permission.id"
                      :class="[
                        'permissionsItem',
                        'permissionsItem--large',
                        permission.className === 'removed' ? 'permissionsItem--removed' : '']">
                      <v-tooltip bottom>
                        <template v-slot:activator="{ on }">
                          <div v-on="on">
                            <img :src="permission.avatarUrl" class="permissionsItemAvatar permissionsItemAvatar--large">
                            <span class="permissionsItemName">{{ permission.displayName }}</span>
                          </div>
                        </template>
                        <span>{{ permission.id }}</span>
                      </v-tooltip>
                      <i
                        v-show="existingPermissions.length > 0 && permission.displayName"
                        class="uiIconDelete permissionsItemDelete"
                        @click="removePermission(permission)">
                      </i>
                    </li>
                  </ul>
                </v-col>
                <v-col 
                  v-else 
                  cols="12" 
                  md="8">
                  <label>{{ this.$t("editors.admin.modal.None") }}</label>
                </v-col>
              </v-row>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>
      <v-card-actions class="dialogFooter footer justify-center pb-5">
        <v-btn 
          class="btn btn-primary dialogFooterBtn me-2" 
          text 
          data-test="saveButton" 
          @click.native="saveChanges">
          {{ this.$t("editors.admin.buttons.Save") }}
        </v-btn>
        <v-btn 
          class="btn dialogFooterBtn" 
          text 
          data-test="cancelButton" 
          @click.native="closeDialog">
          {{ this.$t("editors.admin.buttons.Cancel") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import { postData, getData } from "../EditorsAdminAPI";

export default {
  props: {
    providerLink: {
      type: String,
      required: true
    },
    providerName: {
      type: String,
      required: true
    },
    searchUrl: {
      type: String,
      required: true
    },
    i18n: {
      type: Object,
      required: true
    },
  },
  data() {
    return {
      searchLoading: false,
      searchResults: [],
      search: null,
      selectedItems: [],
      existingPermissions: [],
      error: null,
      showDialog: false,
      permissionChanges: [],
      accessibleToAll: false
    };
  },
  watch: {
    search(val) {
      return val && val !== this.selectedItems && this.querySelections(val);
    },
    async showDialog(val) {
      if (val) {
        try {
          const data = await getData(this.providerLink);
          this.accessibleToAll = data.permissions.some(({ id }) => id === "*");
          this.existingPermissions = data.permissions
            .filter(({ displayName }) => displayName !== null)
            .map(obj => ({ ...obj, className: "" }));
        } catch (err) {
          this.error = err.message;
        }
      }
    }
  },
  methods: {
    // updating items in dropdown depend on user input v
    async querySelections(v) {
      this.searchLoading = true;
      try {
        const data = await getData(`${this.searchUrl}/${v}`);
        this.searchResults = data.identities.filter(
          ({ displayName }) => (displayName || "").toLowerCase().indexOf((v || "").toLowerCase()) > -1
        ).filter(el => !this.existingPermissions.map(item => item.id).includes(el.id));
        this.searchLoading = false;
      } catch (err) {
        this.error = err.message;
      }
    },
    // removes selected item from selection
    removeSelection(value) {
      this.selectedItems = this.selectedItems.filter(({ id }) => id !== value.id);
    },
    async saveChanges() {
      let editedPermissions = this.existingPermissions.filter(el => !this.permissionChanges.map(item => item.id).includes(el.id));
      if (this.selectedItems) {
        editedPermissions = editedPermissions.concat(this.selectedItems);
      }
      // form array with permission names before sending request
      const newPermissions = this.accessibleToAll 
        ? [{ id: "*" }] 
        : editedPermissions.filter(({ id }) => id.length > 0).map(({ id }) => ({ id: id}));
      try {
        const data = await postData(this.providerLink, { permissions: newPermissions });
        this.error = null;
        // saving new permissions before closing
        this.closeDialog();
      } catch (err) {
        this.error = err.message;
      }
    },
    closeDialog() {
      // reset and clearing user changes
      this.error = null;
      this.selectedItems = null;
      this.showDialog = false;
    },
    // removing permission from list and also from selection
    removePermission(item) {
      this.permissionChanges.push(item);
      item.className = "removed";
      if (this.selectedItems) {
        this.selectedItems = this.selectedItems.filter(({ id }) => id !== item.id);
      }
    },
    selectionChange(selection) {
      this.search = "";
      // if everyone permission enabled, it will be automatically disabled in case of some another permission selected
      if (selection.length > 0 && this.accessibleToAll) {
        this.accessibleToAll = false;
      }
    }
  }
};
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
    max-height: 550px;
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

      &:focus {
        border-color: #a6bad6;
        box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 5px #c9d5e6;
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

  .permissions {
    &Container {
      min-height: 100px;
    }

    &List {
      min-height: 100px;
      overflow-y: auto;
    }

    &Item {
      display: flex;
      align-items: center;
      height: 30px;

      &--large {
        height: 40px;
        margin: 5px 0;
        justify-content: space-between;
      }

      &--removed {
        .permissionsItemName {
          text-decoration: line-through;
        }

        .permissionsItemDelete {
          color: #e1e8ee;
          cursor: initial;
        }
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
  }

  .dialogFooter {
    &Btn.v-size--default {
      font-family: Helvetica, arial, sans-serif;
      font-size: 15px;
      padding: 9px 20px;
      color: #4d5466;
      border: 1px solid #e1e8ee;
      box-shadow: none;
      box-sizing: border-box;
      border-radius: 3px;
      height: 40px;
      letter-spacing: normal;
      min-width: 100px;

      &:hover {
        background-color: #e1e8ee;
      }

      &.btn-primary {
        color: #fff;

        &:hover {
          background-color: #476a9c;
          background-position: 0 -45px;
          transition: background-position 0.1s linear;
        }
      }
    }
  }
}

.alert {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translate(-50%, 0);
  z-index: 1000;
  width: 80%;
}
</style>
