<template>
  <v-list-item @click.prevent="openPreview()">
    <v-list-item-icon class="mx-1">
      <v-icon
        :color="documentIcon.color"
        size="35">
        {{ documentIcon.icon }}
      </v-icon>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title 
        v-sanitized-html="document.title" 
        :title="document.title" 
        class="text-truncate" />
      <v-list-item-subtitle v-if="!hideTime || !hideDrive">
        <div class="color-title">
          <span v-if="!hideTime" :title="absoluteDateModified()">
            {{ relativeDateModified }}
            <v-icon color="#a8b3c5">
              mdi-menu-right
            </v-icon>
          </span>
          <span v-if="!hideDrive" :title="document.drive">
            {{ document.drive }}
          </span>
        </div>
      </v-list-item-subtitle>
    </v-list-item-content>
  </v-list-item>
</template>
<script>
export default {
  props: {
    document: {
      type: Object,
      default: () => null,
    },
    hideTime: {
      type: Boolean,
      default: false,
    },
    hideDrive: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    documentIcon() {
      const icon = {};
      if (this.document.fileType.includes('pdf')) {
        icon.icon = 'fas fa-file-pdf';
        icon.color = '#FF0000';
      } else if (this.document.fileType.includes('presentation') || this.document.fileType.includes('powerpoint')) {
        icon.icon = 'fas fa-file-powerpoint';
        icon.color = '#CB4B32';
      } else if (this.document.fileType.includes('sheet') || this.document.fileType.includes('excel')) {
        icon.icon = 'fas fa-file-excel';
        icon.color = '#217345';
      } else if (this.document.fileType.includes('word') || this.document.fileType.includes('opendocument') || this.document.fileType.includes('rtf') ) {
        icon.icon = 'fas fa-file-word';
        icon.color = '#2A5699';
      } else if (this.document.fileType.includes('plain')) {
        icon.icon = 'fas fa-clipboard>';
        icon.color = '#1c9bd7';
      } else if (this.document.fileType.includes('image')) {
        icon.icon = 'fas fa-file-image';
        icon.color = '#999999';
      } else {
        icon.icon = 'fas fa-file';
        icon.color = '#578DC9';
      }
      return icon;
    },
    relativeDateModified() {
      return this.getRelativeTime(this.document.date);
    }
  },
  methods: {
    getRelativeTime(previous) {
      const msPerMinute = 60 * 1000;
      const msPerHour = msPerMinute * 60;
      const msPerDay = msPerHour * 24;
      const msPerMaxDays = msPerDay * 2;
      const elapsed = new Date().getTime() - previous;
      if (elapsed < msPerMinute) {
        return this.$t('documents.timeConvert.Less_Than_A_Minute');
      } else if (elapsed === msPerMinute) {
        return this.$t('documents.timeConvert.About_A_Minute');
      } else if (elapsed < msPerHour) {
        return this.$t('documents.timeConvert.About_?_Minutes').replace('{0}', Math.round(elapsed / msPerMinute));
      } else if (elapsed === msPerHour) {
        return this.$t('documents.timeConvert.About_An_Hour');
      } else if (elapsed < msPerDay) {
        return this.$t('documents.timeConvert.About_?_Hours').replace('{0}', Math.round(elapsed / msPerHour));
      } else if (elapsed === msPerDay) {
        return this.$t('documents.timeConvert.About_A_Day');
      } else if (elapsed < msPerMaxDays) {
        return this.$t('documents.timeConvert.About_?_Days').replace('{0}', Math.round(elapsed / msPerDay));
      } else {
        return this.absoluteDateModified({dateStyle: 'short'});
      }
    },
    absoluteDateModified(options) {
      const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';
      return new Date(this.document.date).toLocaleString(lang, options).split('/').join('-');
    },
    fileInfo() {
      return `${this.$t('documents.preview.updatedOn')} ${this.absoluteDateModified()} ${this.$t('documents.preview.updatedBy')} ${this.document.lastEditor} ${this.document.size}`;
    },
    openPreview() {
      documentPreview.init({
        doc: {
          id: this.document.id,
          repository: 'repository',
          workspace: 'collaboration',
          path: this.document.nodePath || this.document.path,
          title: this.document.title,
          downloadUrl: this.document.downloadUrl,
          openUrl: this.document.url || this.document.openUrl,
          breadCrumb: this.document.previewBreadcrumb,
          fileInfo: this.fileInfo(),
          size: this.document.size,
        },
        version: {                                                                 
          number: this.document.version 
        },
        showComments: false
      });
    }
  }
};
</script>