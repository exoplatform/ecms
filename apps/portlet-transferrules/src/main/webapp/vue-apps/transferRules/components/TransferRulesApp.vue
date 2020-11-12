<template>
  <v-app id="transferRules">
    <v-card class="ma-4 border-radius" flat>
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color">
              {{ $t('documents.transferRules.label') }}
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mb-1 text-color">
              {{ $t('documents.transferRules.title') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title text-capitalize font-italic">
              <div>
                {{ $t('documents.transferRules.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-switch
              v-model="active"
              @change="save"/>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
  </v-app>
</template>

<script>
export default {
  data () {
    return {
      active: true,
    };
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  }
};
</script>