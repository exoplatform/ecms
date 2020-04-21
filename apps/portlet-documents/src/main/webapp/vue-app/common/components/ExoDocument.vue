<template>
  <v-list-item @click.prevent="openPreview()">
    <v-list-item-icon class="mx-0">
      <v-icon :color="documentIcon.color" x-large>
        {{ documentIcon.ico }}
      </v-icon>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title v-text="document.title"/>
      <v-list-item-subtitle>
        <div class="color-title">
          {{ document.date }}
          <v-icon color="#a8b3c5">
            mdi-menu-right
          </v-icon>
          {{ document.drive }}
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
      }
    },
    computed: {
      downloadUrl() {
        return `/rest/jcr/repository/collaboration${this.document.path}`;
      },
      openUrl() {
        const path = this.document.drive === 'Private' ? 'Personal+Documents' : `.space.${this.document.drive}`;
        return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/documents?path=${path}${this.document.path}`;
      },
      documentIcon() {
        const icon = {}
        if (this.document.mimeType.includes('pdf')) {
          icon.ico = 'mdi-file-pdf';
          icon.color = '#d07b7b';
        } else if (this.document.mimeType.includes('mlpresentation')) {
          icon.ico = 'mdi-file-powerpoint';
          icon.color = '#e45030';
        } else if (this.document.mimeType.includes('mlsheet')) {
          icon.ico = 'mdi-file-excel';
          icon.color = '#1a744b';
        } else if (this.document.mimeType.includes('mldocument')) {
          icon.ico = 'mdi-file-word';
          icon.color = '#094d7f';
        } else if (this.document.mimeType.includes('textplain')) {
          icon.ico = 'mdi-clipboard-text';
          icon.color = '#1c9bd7';
        } else if (this.document.mimeType.includes('image')) {
          icon.ico = 'mdi-image';
          icon.color = '#eab320';
        } else {
          icon.ico = 'mdi-file';
          icon.color = '#cdcccc';
        }
        return icon;
      }
    },
    methods: {
      openPreview() {
        documentPreview.init({
          doc: {
            id: this.document.id,
            repository: 'repository',
            workspace: 'collaboration',
            path: this.document.path,
            title: this.document.title,
            downloadUrl: this.downloadUrl,
            openUrl: this.openUrl
          },
        });
      }
    }
  }
</script>