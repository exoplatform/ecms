<template>
  <v-flex>
    <v-list class="documents-timeline-list">
      <v-list-item-group color="primary">
        <exo-document
          v-for="(document,i) in documents"
          :key="i"
          :document="document" />
      </v-list-item-group>
    </v-list>
  </v-flex>
</template>
<script>
export default {
  props: {
    documents: {
      type: Array,
      default: () => [],
    }
  }
};
</script>