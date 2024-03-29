<template>
  <v-app flat>
    <exo-documents :documents="displayedDocuments" />
    <div v-if="!loading && displayedDocuments.length === 0" class="noDocuments">
      <div class="noDocumentsContent">
        <i class="uiNoDocumentsIcon"></i>
        <div class="noDocumentsTitle">
          {{ $t('documents.label.noDocument') }}
        </div>
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
      cachedDocuments: [],
      twoMinInMS: 120000,
      loading: true,
    };
  },
  computed: {
    displayedDocuments() {
      return [...this.cachedDocuments, ...this.documents].slice(0, parseInt(this.limit));
    },
    documentMimeTypeRegex() {
      return this.type === 'recent' && /pdf|presentation|sheet|word|plain/ || /pdf|presentation|sheet|word|plain|image/;
    },
  },
  watch: {
    loading() {
      if (!this.loading) {
        this.retrieveCachedDocuments();
        this.$nextTick().then(() => this.$root.$applicationLoaded());
      }
    },
  },
  created(){
    this.retrieveDocuments();
    if (this.cacheRecentDocuments) {
      document.addEventListener('attachments-upload-finished', event => {
        if (event && event.detail && this.type === 'recent') {
          this.cacheRecentUploadedDocuments(event.detail.list);
        }
      });
    }
  },
  methods: {
    retrieveDocuments() {
      if (this.query != null && this.query !== 'null') {
        documentsService.getDocumentsByQuery(this.query, this.limit).then(
          documents => {
            this.documents = documents;
          }
        ).finally(() => this.loading = false);
      } else if (this.folder != null && this.folder !== 'null') {
        documentsService.getDocumentsByFolder(this.folder, this.limit).then(
          documents => {
            this.documents = documents;
          }
        ).finally(() => this.loading = false);
      } else if (this.type != null) {
        if (this.type === 'recent') {
          documentsService.getRecentDocuments(this.limit)
            .then(documents => this.documents = documents)
            .finally(() => this.loading = false);
        }
        if (this.type === 'recentSpaces') {
          documentsService.getRecentSpacesDocuments(this.limit)
            .then(documents => this.documents = documents)
            .finally(() => this.loading = false);
        }
        if (this.type === 'favorite') {
          documentsService.getFavoriteDocuments(this.limit)
            .then(documents => this.documents = documents)
            .finally(() => this.loading = false);
        }
        if (this.type === 'shared') {
          documentsService.getSharedDocuments(this.limit)
            .then(documents => this.documents = documents)
            .finally(() => this.loading = false);
        }
      }
    },
    retrieveCachedDocuments() {
      if (!this.cacheRecentDocuments) {
        return;
      }
      this.cachedDocuments = [];
      const cachedDocuments = this.getCachedDocuments();
      if (cachedDocuments && cachedDocuments.length) {
        cachedDocuments.sort((doc1, doc2) => doc2.date - doc1.date)
          .slice(0, parseInt(this.limit)).forEach(cachedDocument => {
            const docExistInESDocs = this.documents.some(doc => (doc.id || doc.UUID) === cachedDocument.id);
            if (!cachedDocument.timestamp || Date.now() - cachedDocument.timestamp > this.twoMinInMS && docExistInESDocs) {
              this.removeDocumentFromCache(cachedDocument.id);
            } else if (docExistInESDocs) {
              this.removeDocumentFromCache(cachedDocument.id);
            } else {
              this.cachedDocuments.push(cachedDocument);
            }
          });
        this.cachedDocuments.sort((doc1, doc2) => doc2.date - doc1.date);
      }
    },
    cacheRecentUploadedDocuments(newlyUploadedDocuments) {
      newlyUploadedDocuments.forEach(document => {
        document.fileType = document.mimetype;
        document.id = document.UUID || document.id;
        document.date = new Date(document.date).getTime();
        document.timestamp = Date.now();
        if (document.fileType.match(this.documentMimeTypeRegex)) {
          this.addDocumentToCache(document);
        }
      });
      this.retrieveCachedDocuments();
    },
    getCachedDocuments() {
      const cachedDocumentsString = localStorage.getItem('newlyUploadedAttachments');
      if (cachedDocumentsString) {
        const cachedDocumentsObject = JSON.parse(cachedDocumentsString);
        return Object.keys(cachedDocumentsObject).map(docId => cachedDocumentsObject[docId]);
      }
      return [];
    },
    addDocumentToCache(document) {
      const cachedDocumentsString = localStorage.getItem('newlyUploadedAttachments') || '{}';
      const cachedDocumentsObject = JSON.parse(cachedDocumentsString);
      cachedDocumentsObject[document.id] = document;
      localStorage.setItem('newlyUploadedAttachments', JSON.stringify(cachedDocumentsObject));
    },
    removeDocumentFromCache(docId) {
      const cachedDocumentsString = localStorage.getItem('newlyUploadedAttachments') || '{}';
      const cachedDocumentsObject = JSON.parse(cachedDocumentsString);
      delete cachedDocumentsObject[docId];
      localStorage.setItem('newlyUploadedAttachments', JSON.stringify(cachedDocumentsObject));
    },
  }
};
</script>