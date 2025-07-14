<template>
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      :aria-label="$t('search.access.to.result', {0 : excerptText})"
      :href="fileUrl"
      @click.stop.prevent="openFilePreview">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="ms-1 me-3">
            <v-icon
              size="32"
              :color="fileIconColor"
              class="mt-2">
              {{ fileIconClass }}
            </v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title class="d-flex flex-row full-width align-center">
              <h1
                class="flex-grow-1 title font-weight-bold primary--text pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                :aria-label="fileTitleText"
                v-sanitized-html="fileTitle"></h1>
              <span>
                <span v-show="hover || isMobile" class="flex-row align-center justify-center">
                  <v-btn
                    icon
                    small
                    class="me-2"
                    @click.prevent.stop="showInfo">
                    <v-icon class="icon-default-color" size="16">
                      as fa-info-circle
                    </v-icon>
                  </v-btn>
                  <file-favorite-action
                    :file="result"
                    @removed="$emit('refresh-favorite')" />

                </span>
              </span>
            </v-list-item-title>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row align-center mx-auto full-width">
                <span class="d-flex flex-row align-center" v-if="space">
                  <a
                    v-bind="attrs"
                    v-on="on"
                    :href="spaceUrl"
                    class="flex-nowrap flex-shrink-0 d-flex spaceAvatar">
                    <v-avatar
                      :size="18"
                      tile
                      class="my-auto">
                      <img
                        :src="space.avatarUrl"
                        alt=""
                        class="object-fit-cover ma-auto"
                        loading="lazy">
                    </v-avatar>
                    <p v-if="!isMobile" class="ms-2 my-auto text-subtitle">{{ space.displayName }}</p>
                  </a>
                </span>
                <exo-user-avatar
                  v-if="fileAuthor"
                  :profile-id="fileAuthor"
                  :size="18"
                  small-font-size
                  :avatar="isMobile"
                  :popover="false" />
                <span v-if="fileUpdateDate" class="d-flex flex-row align-center">
                  <v-icon
                    size="3"
                    class="icon-default-color mx-3">fas fa-circle</v-icon>
                  <v-icon
                    size="12"
                    class="icon-default-color">fas fa-calendar-alt</v-icon>
                </span>
                <date-format class="ms-1 my-auto" :value="fileUpdateDate" />
                <span v-if="fileLastEditor" class="d-flex flex-row align-center">
                  <v-icon
                    size="3"
                    class="icon-default-color mx-3">fas fa-circle</v-icon>
                  <exo-user-avatar
                    :profile-id="fileLastEditor"
                    :size="18"
                    small-font-size
                    :avatar="isMobile"
                    :popover="false" />
                </span>
              </span>
              <div
                v-if="excerptHtml"
                class="pt-2 text-wrap text-body-2 text-color text-break"
                :class="{
                  'text-truncate-2': isMobile,
                  'text-truncate-3': !isMobile,
                }"
                v-sanitized-html="excerptHtml">
              </div>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card>
  </v-hover>
</template>

<script>

export default {
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      defaultFileIcon: {
        class: 'fas fa-file',
        color: '#707070'
      },
    };
  },
  computed: {
    fileTitle() {
      return window.decodeURIComponent(this.result?.title);
    },
    fileTitleText() {
      return this.$utils.htmlToText(this.fileTitle);
    },
    excerptHtml() {
      return this.result.excerpts['attachment.content'] && this.result?.excerpt || '';
    },
    excerptText() {
      return this.$utils.htmlToText(this.excerptHtml);
    },
    fileIcon() {
      return this.$documentsIconsExtension[0]?.get(this.result?.fileType) || this.defaultFileIcon;
    },
    fileId() {
      return this.result?.id;
    },
    downloadUrl() {
      return this.$documentsUtils.getDownloadUrl(this.fileId, this.fileUpdateDate);
    },
    fileIconClass() {
      return this.fileIcon?.class;
    },
    fileIconColor() {
      return this.fileIcon?.color;
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
    fileUrl() {
      return this.result?.url;
    },
    isFileEditable() {
      const type = this.result?.fileType || '';
      return this.$supportedDocuments && this.$supportedDocuments.filter(doc => doc.edit && doc.mimeType === type && !this.result.cloudDrive).length > 0;
    },
    isFileReadable() {
      const type = this.result?.fileType || '';
      return this.$supportedDocuments && this.$supportedDocuments.filter(doc => doc.mimeType === type).length > 0;
    },
    fileLastEditor() {
      return this.result?.lastEditor;
    },
    fileAuthor() {
      return this.result?.author;
    },
    fileUpdateDate() {
      return this.result?.date;
    },
    space() {
      return this.result?.space;
    },
    spaceUrl() {
      if (!this.space?.id) {
        return '#';
      }
      return `${eXo.env.portal.context}/s/${this.space?.id}`;
    }
  },
  methods: {
    openFilePreview() {
      if (this.isFileEditable || this.isFileReadable) {
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/oeditor?docId=${this.result.id}${this.isFileReadable && !this.isFileEditable && '&mode=view' || ''}&backTo=${window.location.pathname}`, '_blank');
      } else {
        const file = {
          'id': this.fileId,
          'mimetype': this.result?.fileType,
          'downloadUrl': this.downloadUrl,
          'filename': this.result?.filename,
          'source': 'documents'
        };
        document.dispatchEvent(new CustomEvent('open-attachments-preview', {detail: {'attachments': [file],'id': this.fileId }}));
      }
    },
    showInfo() {
      document.dispatchEvent(new CustomEvent('open-document-info-drawer', {detail: this.fileId}));
    },
  }
};
</script>
