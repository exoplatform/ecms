<template>
  <v-app flat>
    <exo-documents :documents="documents"/>
    <div v-if="!loading && documents.length === 0" class="noDocuments">
      <div class="noDocumentsContent">
        <i class="uiNoDocumentsIcon"></i>
        <div class="noDocumentsTitle">{{ $t('documents.label.noDocument') }}</div>
      </div>
    </div>
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
      cacheRecentDocuments: {
        type: Boolean,
        default: false,
      }
    },
    data() {
      return {
        documents: [],
        loading: true
      }
    },
    watch: {
      loading() {
        if (!this.loading) {
          this.$nextTick().then(() => this.$root.$emit('application-loaded'));
        }
      },
    },
    created(){
      this.retrieveDocuments();
      document.addEventListener('attachments-upload-finished', event => {
        if (event && event.detail && this.type === 'recent') {
          this.cacheRecentUploadedDocuments(event.detail.list);
        }
      });
    },
    methods: {
      retrieveDocuments() {
        if (this.query != null && this.query !== 'null') {
          documentsService.getDocumentsByQuery(this.query, this.limit).then(
            documents => {
              this.documents = documents;
            }
          ).finally(() => this.loading = false);
        }
        else if (this.folder != null && this.folder !== 'null') {
          documentsService.getDocumentsByFolder(this.folder, this.limit).then(
            documents => {
              this.documents = documents;
            }
          ).finally(() => this.loading = false);
        }
        else if (this.type != null) {
          if (this.type === 'recent') {
            documentsService.getRecentDocuments(this.limit).then(
              documents => {
                this.documents = documents;
                if (!documents.length) {
                  localStorage.setItem('newlyUploadedAttachments', JSON.stringify({}));
                } else {
                  this.manageCachedRecentDocuments(documents);
                }
              }
            ).finally(() => this.loading = false);
          }
          if (this.type === 'recentSpaces') {
            documentsService.getRecentSpacesDocuments(this.limit).then(
              documents => {
                this.documents = documents;
              }
            ).finally(() => this.loading = false);
          }
          if (this.type === 'favorite') {
            documentsService.getFavoriteDocuments(this.limit).then(
              documents => {
                this.documents = documents;
              }
            ).finally(() => this.loading = false);
          }
          if (this.type === 'shared') {
            documentsService.getSharedDocuments(this.limit).then(
              documents => {
                this.documents = documents;
              }
            ).finally(() => this.loading = false);
          }
        }
      },
      cacheRecentUploadedDocuments(newlyUploadedDocuments) {
        newlyUploadedDocuments.forEach(document => {
          document.fileType = document.mimetype;
          document.id = document.UUID;
          document.date = new Date(document.date).getTime();
          this.documents.unshift(document);
          this.documents.pop();
        });
        if (this.cacheRecentDocuments) {
          const docMimetype = ['pdf', 'presentation', 'sheet', 'word', 'plain'];
          newlyUploadedDocuments = newlyUploadedDocuments.filter(document => {
            if (new RegExp(docMimetype.join("|")).test(document.fileType)) {
              return document;
            }
          });
          localStorage.setItem('newlyUploadedAttachments', JSON.stringify(newlyUploadedDocuments));
        }
        this.retrieveDocuments();
      },
      manageCachedRecentDocuments(documents) {
        let newlyUploadedDocuments = JSON.parse(localStorage.getItem('newlyUploadedAttachments'));
        newlyUploadedDocuments = newlyUploadedDocuments.filter(document => {
          if (!documents.some(doc => document.id === doc.id)) {
            return document;
          }
        });
        this.documents.push(...newlyUploadedDocuments);
        this.documents.sort((doc1, doc2) => doc2.date - doc1.date);
        this.documents = this.documents.slice(0, parseInt(this.limit));
        localStorage.setItem('newlyUploadedAttachments', JSON.stringify(newlyUploadedDocuments));
      }
    }
  }
</script>