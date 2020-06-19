<template>
  <v-card class="searchFileCard d-flex flex-column" outlined>
    <div class="mx-auto flex-grow-1 clickable pa-4" @click="$refs.documentDetail.$el.click()">
      <ellipsis
        :title="result.excerpt"
        :data="result.excerpt"
        :line-clamp="5"
        end-char="..."/>
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
};
</script>
