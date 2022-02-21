<template>
  <div class="create-new-document">
    <div class="d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">
        {{ $t('documents.label.new') }}
      </v-subheader>
      <v-divider />
    </div>
    <div class=" create-new-doc-input px-10">
      <div class="d-flex justify-space-around">
        <div
          v-for="doc in newDocumentActions"
          :key="doc.id"
          class="d-flex flex-column align-center clickable"
          @click="showNewDocInput(doc)">
          <v-icon
            :color="doc.color"
            class="uiIcon32x32">
            {{ doc.icon }}
          </v-icon>
          <span class="mt-3">{{ $t(doc.label) }}</span>
        </div>
      </div>
      <v-text-field
        v-show="!NewDocInputHidden"
        ref="NewDocInputHidden"
        v-model="newDocTitleInput"
        :rules="documentTitleRules"
        :placeholder="$t('documents.untitledDocument')"
        class="pt-2"
        outlined
        dense
        autofocus
        @keyup.enter="createNewDoc()">
        <div slot="append" class="d-flex mt-1">
          <span class="text-color me-2">{{ selectedDocType.extension }}</span>
          <v-icon
            :class="documentTitleMaxLengthReached && 'not-allowed' || 'clickable'"
            :color="documentTitleMaxLengthReached && 'grey--text' || 'primary'"
            class="px-1"
            small
            @click="createNewDoc()">
            fa-check
          </v-icon>
          <v-icon
            class="clickable px-0"
            color="red"
            small
            @click="resetNewDocInput()">
            fa-times
          </v-icon>
        </div>
      </v-text-field>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    attachments: {
      type: Array,
      default: () => []
    },
    currentDrive: {
      type: Object,
      default: () => null
    },
    pathDestinationFolder: {
      type: Object,
      default: () => null
    },
    maxFilesCount: {
      type: Number,
      default: parseInt(`${eXo.env.portal.maxToUpload}`)
    },
    maxFileSize: {
      type: Number,
      default: parseInt(`${eXo.env.portal.maxFileSize}`)
    },
  },
  data() {
    return {
      newDocTitleInput: '',
      newCreatedDocs: [],
      selectedDocType: {},
      NewDocInputHidden: true,
      extensionApp: 'attachment',
      newDocumentActionExtension: 'new-document-action',
      newDocumentActions: {},
      MAX_DOCUMENT_TITLE_LENGTH: 510,
      documentTitleRules: [title => !title || title && title.trim().length <= this.MAX_DOCUMENT_TITLE_LENGTH - this.selectedDocType.extension.length || this.newDocTitleMaxLengthLabel],
    };
  },
  computed: {
    cleanedNewDocumentTitle() {
      return this.newDocTitleInput && this.newDocTitleInput.trim();
    },
    newDocumentTitle() {
      return this.cleanedNewDocumentTitle && `${this.cleanedNewDocumentTitle}${this.selectedDocType.extension}` || this.untitledNewDoc;
    },
    documentTitleMaxLengthReached() {
      return this.newDocumentTitle && this.newDocumentTitle.length > this.MAX_DOCUMENT_TITLE_LENGTH;
    },
    untitledNewDoc() {
      return `${this.$t('documents.untitledDocument')}${this.selectedDocType.extension}`;
    },
    maxFileCountErrorLabel() {
      return this.$t('attachments.drawer.maxFileCount.error').replace('{0}', `<b> ${this.maxFilesCount} </b>`);
    },
    newDocCreationFailedLabel() {
      return this.$t('attachment.new.document.failed');
    },
    newDocTitleMaxLengthLabel() {
      return this.$t('attachment.new.document.title.max.length');
    },
    newDocTitleExistLabel() {
      return this.$t('attachment.document.title.exist');
    },
  },
  created() {
    this.$root.$on(`${this.extensionApp}-${this.newDocumentActionExtension}-updated`, this.refreshNewDocumentsActions);
    this.$root.$on('hide-create-new-document-input', this.resetNewDocInput);
    this.refreshNewDocumentsActions();
  },
  methods: {
    refreshNewDocumentsActions() {
      const extensions = extensionRegistry.loadExtensions(this.extensionApp, this.newDocumentActionExtension);
      extensions.forEach(extension => {
        if (extension.id) {
          this.newDocumentActions[extension.id] = extension;
        }
      });
    },
    createNewDoc() {
      if (this.documentTitleMaxLengthReached) {
        return;
      }
      this.$root.$emit('start-loading-attachment-drawer');
      this.$attachmentService.createNewDoc(this.newDocumentTitle, this.selectedDocType.type, this.currentDrive.name, this.pathDestinationFolder)
        .then((resp) => {
          if (resp && resp.status && resp.status === 409) {
            this.$root.$emit('attachments-notification-alert', {
              message: this.newDocTitleExistLabel,
              type: 'error',
            });
            this.$root.$emit('end-loading-attachment-drawer');
          } else {
            return resp;
          }
        })
        .then((doc) => this.manageNewCreatedDocument(doc))
        .catch(() => {
          this.$root.$emit('attachments-notification-alert', {
            message: this.newDocCreationFailedLabel,
            type: 'error',
          });
          this.$root.$emit('end-loading-attachment-drawer');
        });
    },
    showNewDocInput(doc) {
      if (this.attachments.length >= this.maxFilesCount) {
        this.$root.$emit('attachments-notification-alert', {
          message: this.maxFileCountErrorLabel,
          type: 'error',
        });
        return;
      }
      this.$refs.NewDocInputHidden.focus();
      this.NewDocInputHidden = false;
      this.selectedDocType = doc;
    },
    resetNewDocInput() {
      this.NewDocInputHidden = true;
      this.newDocTitleInput = '';
    },
    manageNewCreatedDocument(doc) {
      if (doc && doc.id) {
        doc.drive = this.currentDrive.title;
        doc.date = doc.created;
        this.$root.$emit('add-new-created-document', doc);
        this.$root.$emit('end-loading-attachment-drawer');
        this.resetNewDocInput();
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/oeditor?docId=${doc.id}`, '_blank');
      }
    }
  }
};
</script>