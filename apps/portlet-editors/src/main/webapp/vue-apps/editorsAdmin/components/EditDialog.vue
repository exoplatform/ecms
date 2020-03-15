<template>
  <v-card>
    <v-card-title class="headline grey lighten-2 justify-space-between" primary-title>
      {{ this.$t('editors.admin.modal.title') }}
      <v-btn icon @click="closeDialog">
        <v-icon>close</v-icon>
      </v-btn>
    </v-card-title>
    <v-card-text style="min-height: 450px">
      <v-container>
        <v-row class="providerName">
          {{ $t(`editors.admin.${provider.provider}.name`) }}
        </v-row>
        <v-row>
          <v-col cols="12" md="8">
            <v-autocomplete
              :label="label"
              v-model="select"
              :loading="loading"
              :items="items"
              :search-input.sync="search"
              cache-items
              class="mx-4"
              flat
              hide-no-data
              hide-details
              solo-inverted
              chips
              multiple
              attach
              item-text="displayName"
              item-value="name">
              <template slot="selection" slot-scope="data">
                <v-chip
                  :input-value="data"
                  close 
                  class="chip--select-multi"
                  @click:close="handleCloseClick(data.item)">
                  {{ data.item.displayName }}
                </v-chip>
              </template>
            </v-autocomplete>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="6" md="4">Who has permission</v-col>
          <v-col cols="12" md="8"><ul><li v-for="permission in provider.permissions" :key="permission">{{ permission }}
            <v-icon @click="removePermission(permission)">delete</v-icon>
          </li></ul></v-col>
        </v-row>
      </v-container>
      <!-- <div class="v-skeleton-loader__bone blockProvidersInner">
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
      </div> -->
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <v-btn 
        small
        color="primary" 
        @click="saveChanges">
        Save
      </v-btn>
      <v-btn 
        small 
        color="primary" 
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
        label: "Enter users or spaces names...",
      }
  },
  watch: {
    search (val) {
      return val && val !== this.select && this.querySelections(val);
    },
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
        postInfo(updateRest, { permissions: this.select }).then(data => { 
          console.log(data);
        });
      },
      closeDialog() {
        this.$emit('onDialogClose');
      },
      removePermission(name) {
        this.provider.permissions = this.provider.permissions.filter(perm => perm !== name);
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
</style>