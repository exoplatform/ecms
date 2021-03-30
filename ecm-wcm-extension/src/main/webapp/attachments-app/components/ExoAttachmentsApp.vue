<template>
  <v-app class="attachments-application border-box-sizing transparent" flat>
    <template v-if="$slots.attachmentsButton" @click="openAttachmentsAppDrawer()">
      <slot name="attachmentsButton"></slot>
    </template>
    <exo-attachments-drawer></exo-attachments-drawer>
  </v-app>
</template>

<script>
export default {
  methods: {
    openAttachmentsAppDrawer() {
      this.$root.$emit('open-attachments-app-drawer');
    },
  }
};
</script>