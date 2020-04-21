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
      documentsService.getDocuments(this.query, this.folder, this.type, this.limit).then(
        documents => { 
          this.documents = documents;
        }
      );
    }
  }
</script>