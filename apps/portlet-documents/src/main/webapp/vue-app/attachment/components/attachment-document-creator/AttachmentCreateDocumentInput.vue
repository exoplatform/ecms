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
          <i :class="doc.icon" class="uiIcon32x32"></i>
          <span class="mt-3">{{ $t(doc.label) }}</span>
        </div>
      </div>
      <v-text-field 
        v-if="!NewDocInputHidden"
        v-model="newDocTitleInput"
        outlined
        dense
        autofocus
        placeholder="Untitled document" 
        class="pt-2"
        @keyup.enter="createNewDoc()">
        <div slot="append" class="d-flex mt-1">
          <span class="me-2">{{ selectedDocType.extension }}</span>
          <v-icon
            class="clickable px-1"
            color="primary"
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
    };
  },
  computed: {
    NewDocumentTitle() {
      return this.newDocTitleInput && `${this.newDocTitleInput}${this.selectedDocType.extension}` || this.untitledNewDoc;
    },
    untitledNewDoc() {
      return `${this.$t('documents.untitledDocument')}${this.selectedDocType.extension}`;
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
      this.$root.$emit('start-loading-attachment-drawer');
      this.$attachmentService.createNewDoc(this.NewDocumentTitle, this.selectedDocType.type, this.currentDrive.name, this.pathDestinationFolder).then((doc) => {
        this.$root.$emit('end-loading-attachment-drawer');
        this.resetNewDocInput();
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/oeditor?docId=${doc.id}`, '_blank');
      });
    },
    showNewDocInput(doc) {
      this.NewDocInputHidden = false;
      this.selectedDocType = doc;
    },
    resetNewDocInput() {
      this.NewDocInputHidden = true;
      this.newDocTitleInput = '';
    }
  }
};
</script>