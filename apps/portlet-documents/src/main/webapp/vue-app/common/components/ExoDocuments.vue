<template>
  <v-container pa-0>
    <v-layout
      row
      mx-0
      class="white">
      <v-flex
        xs12>
        <v-layout
          row
          mx-0>
          <v-list>
            <v-list-item-group color="primary">
              <exo-document
                v-for="(document,i) in documents"
                :key="i"
                :document="document" />
            </v-list-item-group>
          </v-list>
        </v-layout>
      </v-flex>
    </v-layout>
  </v-container>
</template>
<script>
  export default {
    props: {
      documents: {
        type: Array,
        default: () => [],
      }
    }
  }
</script>