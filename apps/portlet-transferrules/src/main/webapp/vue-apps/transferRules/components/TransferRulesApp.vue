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
              {{ $t('documents.transferRules.suspend.upload') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title text-capitalize font-italic">
              <div>
                {{ $t('documents.transferRules.suspend.upload.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <label class="switch">
              <input type="checkbox">
              <div class="slider round"><span class="absolute-activate">{{ $t(`documents.transferRules.button.yes`) }}</span></div>
              <span class="absolute-deactivated">{{ $t(`documents.transferRules.button.no`) }}</span>
            </label>
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mb-1 text-color">
              {{ $t('documents.transferRules.suspend.download') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title text-capitalize font-italic">
              <div>
                {{ $t('documents.transferRules.suspend.download.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <label class="switch">
              <input type="checkbox">
              <div class="slider round"><span class="absolute-activate">{{ $t(`documents.transferRules.button.yes`) }}</span></div>
              <span class="absolute-deactivated">{{ $t(`documents.transferRules.button.no`) }}</span>
            </label>
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mb-1 text-color">
              {{ $t('documents.transferRules.suspend.sharing') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title text-capitalize font-italic">
              <div>
                {{ $t('documents.transferRules.suspend.sharing.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <label class="switch">
              <input
                v-model="sharedDocumentActivated"
                type="checkbox"
                @click="changeSharedDocumentStatus(!sharedDocumentActivated)">
              <div class="slider round"><span class="absolute-activate">{{ $t(`documents.transferRules.button.yes`) }}</span></div>
              <span class="absolute-deactivated">{{ $t(`documents.transferRules.button.no`) }}</span>
            </label>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-card>
  </v-app>
</template>

<script>
import * as transferRulesService from '../transferRulesService.js';

export default {
  data () {
    return {
      sharedDocumentActivated: false,
      TransferRulesStatusModel: null,
    };
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  },
  created() {
    transferRulesService.getSharedDocumentStatus().then(
      (data) => {
        this.sharedDocumentActivated = data.isSharedDocumentActivated === 'true';
      });
  },
  methods: {
    changeSharedDocumentStatus(status) {
      this.TransferRulesStatusModel = {
        sharedDocumentStatus : status,
      };
      transferRulesService.saveSharedDocumentStatus(this.TransferRulesStatusModel);
    },
  }
};
</script>
