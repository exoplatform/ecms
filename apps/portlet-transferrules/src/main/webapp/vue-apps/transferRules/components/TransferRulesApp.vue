<template>
  <v-app id="transferRules">
    <v-card class="ma-4 border-radius" flat>
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title text-color font-weight-bold transfertRulesTitle">
              {{ $t('documents.transferRules.label') }}
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mb-1 text-color font-weight-medium textSize">
              {{ $t('documents.transferRules.suspend.download') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title font-italic textSize">
              <div>
                {{ $t('documents.transferRules.suspend.download.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <label class="switch d-inline-block">
              <input
                v-model="downloadDocumentStatus"
                type="checkbox"
                class="d-none"
                @click="changeDownloadDocumentStatus(!downloadDocumentStatus)">
              <div class="slider text-left rounded-pill"><span class="absolute-activate">{{ $t(`documents.transferRules.button.yes`) }}</span></div>
              <span class="absolute-deactivated text-right">{{ $t(`documents.transferRules.button.no`) }}</span>
            </label>
          </v-list-item-action>
        </v-list-item>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="mb-1 text-color font-weight-medium textSize">
              {{ $t('documents.transferRules.suspend.sharing') }}
            </v-list-item-title>
            <v-list-item-subtitle class="text-sub-title font-italic textSize">
              <div>
                {{ $t('documents.transferRules.suspend.sharing.description') }}
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <label class="switch d-inline-block">
              <input
                v-model="sharedDocumentStatus"
                type="checkbox"
                class="d-none"
                @click="changeSharedDocumentStatus(!sharedDocumentStatus)">
              <div class="slider text-left rounded-pill"><span class="absolute-activate">{{ $t(`documents.transferRules.button.yes`) }}</span></div>
              <span class="absolute-deactivated text-right">{{ $t(`documents.transferRules.button.no`) }}</span>
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
      sharedDocumentStatus: false,
      downloadDocumentStatus: false,
      uploadDocumentStatus: false,
    };
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  },
  created() {
    transferRulesService.getTransfertRulesDocumentStatus().then(
      (data) => {
        this.sharedDocumentStatus = data.sharedDocumentStatus === 'true';
        this.downloadDocumentStatus = data.downloadDocumentStatus === 'true';
      });
  },
  methods: {
    changeSharedDocumentStatus(sharedDocumentStatus) {
      transferRulesService.saveSharedDocumentStatus(sharedDocumentStatus);
    },
    changeDownloadDocumentStatus(downloadDocumentStatus) {
      transferRulesService.saveDownloadDocumentStatus(downloadDocumentStatus);
    },
  }
};
</script>
