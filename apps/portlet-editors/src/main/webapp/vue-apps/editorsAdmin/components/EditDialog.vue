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
          <div>
            {{ $t(`editors.admin.${provider.provider}.name`) }}
            <div>Permissions: <ul><li v-for="permission in provider.permissions" :key="permission">{{ permission }}</li></ul></div>
          </div>
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
    }
  },
  created() {
    console.log('dialogcreated');
    console.log(this.provider);
  }
}
</script>