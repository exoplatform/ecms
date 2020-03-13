<template>
  <div class="text-center">
    <v-dialog
      v-model="show"
      width="600"
    >
      <v-card>
        <v-card-title
          class="headline grey lighten-2 justify-space-between"
          primary-title
        >
          {{ this.$t('editors.admin.modal.title') }}
          <v-btn icon @click="show = false">
            <v-icon>close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text>
          <v-container>
            <v-row class="providerName">
              {{ $t(`editors.admin.${provider.provider}.name`) }}
            </v-row>
            <v-row>
              <v-col cols="6" md="4">Permissions:</v-col>
              <v-col cols="12" md="8"><ul><li v-for="permission in provider.permissions" :key="permission">{{ permission }}</li></ul></v-col>
            </v-row>
            <v-row>
              <v-col>
                <label>Add permission</label>
                <v-text-field
                  hide-details
                  single-line
                  solo
                  placeholder="Enter users or spaces names..."
                ></v-text-field>
              </v-col>
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
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import { getInfo } from "../EditorsAdminAPI";

export default {
  props: {
    provider: {
      type: Object,
      default: function () {
        return {}
      }
    },
    show: {
      type: Boolean,
      default: function () {
        return true
      }
    },
    searchUrl: {
      type: String,
      required: true
    }
  },
  created() {
    console.log('dialogcreated');
    console.log(this.searchUrl);
    this.getProviders();
  },
  methods: {
     getProviders() {
          getInfo("http://localhost:8080/portal/rest/identitity/search").then(data => console.log(data));
        },
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