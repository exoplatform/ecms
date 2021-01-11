<template>
  <v-app id="dlpQuarantine">
    <v-list>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="title mb-0">
            <v-row no-gutters class="col-4">
              <v-col class="col-4 pb-0 pt-5">
                <h4 class="font-weight-bold ma-0">{{ $t('documents.dlp.quarantine.label') }}</h4>
              </v-col>
              <v-col class="col-4">
                <v-switch
                  v-if="dlpFeatureStatusLoaded"
                  v-model="dlpFeatureEnabled"
                  dense
                  @change="saveDlpFeatureStatus(dlpFeatureEnabled)"/>
              </v-col>
            </v-row>
          </v-list-item-title>
          <v-list-item-subtitle class="text-sub-title font-italic">
            {{ $t('documents.dlp.quarantine.enableDisable') }}
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
      <v-divider class="mx-5"/>
    </v-list>
    <v-data-table
      :headers="headers"
      :items="documents"
      :items-per-page="5"
      class="px-5">
      <template slot="item.actions" slot-scope="{ item }">
        <v-btn
          v-exo-tooltip.bottom.body="$t('documents.dlp.quarantine.previewDownload')"
          icon
          text>
          <i class="uiIconWatch"></i>
        </v-btn>
        <v-btn
          v-exo-tooltip.bottom.body="$t('documents.dlp.quarantine.validateDoc')"
          primary
          icon
          text>
          <i class="uiIconValidate"></i>
        </v-btn>
        <v-btn
          v-exo-tooltip.bottom.body="$t('documents.dlp.quarantine.deleteDoc')"
          primary
          icon
          text>
          <i class="uiIconTrash"></i>
        </v-btn>
      </template>
    </v-data-table>

  </v-app>
</template>

<script>
import * as dlpAdministrationServices from '../dlpAdministrationServices';
export default {
  data () {
    return {
      headers: [
        {
          text: this.$t && this.$t('documents.dlp.quarantine.content'),
          align: 'center',
          sortable: false,
          value: 'content',
        },
        { text: this.$t && this.$t('documents.dlp.quarantine.keywordDetected'),
          align: 'center',
          sortable: false,
          value: 'keyword'
        },
        { text: this.$t && this.$t('documents.dlp.quarantine.createdDate'),
          align: 'center',
          sortable: false,
          value: 'date' 
        },
        { text: this.$t && this.$t('documents.dlp.quarantine.author'),
          align: 'center',
          sortable: false,
          value: 'author'
        },
        { text: this.$t && this.$t('documents.dlp.quarantine.actions'),
          align: 'center',
          sortable: false,
          value: 'actions'
        },
      ],
      documents: [
        {
          content: 'Lorem Ipsum',
          keyword: 'test',
          date: '2 Apr 2020 08:19:03',
          author: 'test User',
        },
        {
          content: 'Lorem Ipsum',
          keyword: 'test2',
          date: '2 Apr 2020 08:19:03',
          author: 'test User2',
        },
      ],
      totalSize : 5,
      dlpFeatureEnabled: null,
      dlpFeatureStatusLoaded: false,
    };
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  },
  created() {
    this.getDlpFeatureStatus();
  },
  methods: {
    saveDlpFeatureStatus(status) {
      dlpAdministrationServices.saveDlpFeatureStatus(status);
    },
    getDlpFeatureStatus() {
      dlpAdministrationServices.isDlpFeatureActive().then(status => {
        this.dlpFeatureEnabled = status.value;
        this.dlpFeatureStatusLoaded = true;
      });
    }
  },
};
</script>
