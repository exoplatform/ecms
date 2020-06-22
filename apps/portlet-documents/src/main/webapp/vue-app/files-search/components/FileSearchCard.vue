<template>
  <v-card class="searchFileCard d-flex flex-column" outlined>
    <div class="mx-auto flex-grow-1 clickable pa-4" @click="$refs.documentDetail.$el.click()">
      <div
        ref="excerptNode"
        :title="excerptText"
        class="text-wrap text-break">
      </div>
    </div>
    <div>
      <exo-document
        ref="documentDetail"
        :document="result"
        hide-time
        class="light-grey-background flex-grow-0 border-top-color py-0 px-1" />
    </div>
  </v-card>
</template>

<script>
const MAX_EXCERPT_CHARS_LENGTH = 130;

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
  computed: {
    excerpts() {
      return this.result && this.result.excerpts;
    },
    excerptTitle() {
      return this.excerpts && this.excerpts['title'] && window.decodeURIComponent(this.excerpts['title'][0]);
    },
    excerptName() {
      return this.excerpts && this.excerpts['name'] && window.decodeURIComponent(this.excerpts['name'][0]);
    },
    excerptContent() {
      return this.excerpts && this.excerpts['attachment.content'] && this.excerpts['attachment.content'].join('<br />');
    },
    excerptHtml() {
      let excerpt = this.excerptTitle || this.excerptName || '';
      if (this.excerptContent) {
        if (excerpt) {
          excerpt = `<br />${this.excerptContent}`;
        } else {
          excerpt = this.excerptContent;
        }
      }
      return excerpt;
    },
    excerptText() {
      return $('<div />').html(this.excerptHtml).text();
    },
  },
  mounted() {
    this.computeEllipsis();
  },
  methods: {
    computeEllipsis() {
      if (!this.excerptHtml || this.excerptHtml.length === 0) {
        return;
      }
      const stNode = this.$refs.excerptNode;
      if (!stNode) {
        return;
      }
      stNode.innerHTML = this.excerptHtml;

      const lineClamp = 5;
      const stNodeLineHeight =  22;
      const maxHeight = stNodeLineHeight * lineClamp;

      let stNodeHeight = stNode.getBoundingClientRect().height || stNodeLineHeight;
      if (stNodeHeight > maxHeight) {
        while (stNodeHeight > maxHeight) {
          const newHtml = this.deleteLastChars(stNode.innerHTML, 10);
          if (newHtml.length === stNode.innerHTML.length) {
            break;
          }
          stNode.innerHTML = newHtml;
          stNodeHeight = stNode.getBoundingClientRect().height || stNodeLineHeight;
        }

        stNode.innerHTML = this.deleteLastChars(stNode.innerHTML, 4);
        stNode.innerHTML = `${stNode.innerHTML}...`;
      }
    },
    deleteLastChars(html, charsToDelete) {
      if (html.slice(-1) === '>') {
        // Replace empty tags
        html = html.replace(/<[a-zA-Z 0-9 "'=]*><\/[a-zA-Z 0-9]*>$/g, '');
      }

      charsToDelete = charsToDelete || 1;

      let newHtml = '';
      if (html.slice(-1) === '>') {
        // Delete last inner html char
        newHtml = html.replace(new RegExp(`([^>]{${charsToDelete}})(</)([a-zA-Z 0-9]*)(>)$`), '$2$3');
        newHtml = $('<div />').html(newHtml).html();
        if (newHtml.length === html.length) {
          newHtml = html.replace(new RegExp(`([^>]*)(</)([a-zA-Z 0-9]*)(>)$`), '$2$3');
        }
      } else {
        newHtml = html.substring(0, html.trimRight().length - charsToDelete);
      }
      return newHtml;
    },
  }
};
</script>
