<template>
  <v-app class="VuetifyApp" flat>
    <exo-documents :documents="documents"/>
  </v-app>
</template>
<script>
  import * as documentsService from '../../common/js/DocumentsService.js'; 
  export default {
    props: {
      query: {
        type: String,
        default: null,
      },
      folder: {
        type: String,
        default: null,
      },
      type: {
        type: String,
        default: null,
      },
      limit: {
        type: String,
        default: null,
      },
    },
    data() {
      return {
        documents: [],
      }
    },
    created(){
      if (this.query != null && this.query !== 'null') {
        documentsService.getDocumentsByQuery(this.query, this.limit).then(
          documents => { 
            this.documents = documents;
          }
        );
      }
      else if (this.folder != null && this.folder !== 'null') {
        documentsService.getDocumentsByFolder(this.folder, this.limit).then(
          documents => { 
            this.documents = documents;
          }
        );
      }
      else if (this.type != null) {
        if (this.type === 'recent') {
          documentsService.getRecentDocuments(this.limit).then(
            documents => { 
              this.documents = documents;
            }
          );
        }
        if (this.type === 'favorite') {
          documentsService.getFavoriteDocuments(this.limit).then(
            documents => { 
              this.documents = documents;
            }
          );
        }
        if (this.type === 'shared') {
          documentsService.getSharedDocuments(this.limit).then(
            documents => { 
              this.documents = documents;
            }
          );
        }
      }
    }
  }
</script>