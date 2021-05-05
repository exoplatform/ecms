<template>
  <exo-drawer
    ref="attachmentsListDrawer"
    class="attachmentsListDrawer"
    right>
    <template slot="title">
      <div class="drawerTitle">
        <v-btn
          icon
          color="grey"
          @click="closeAttachmentsListDrawer()"
        >
          <v-icon>mdi-keyboard-backspace</v-icon>
        </v-btn>
        <span>{{ $t('attachments.list') }}</span>
      </div>
      <v-btn
        icon
        color="primary"
        @click="openAttachmentsAppDrawer()"
      >
        <v-icon size="20">fa-plus</v-icon>
      </v-btn>

    </template>
    <template slot="content">
      <div v-if="attachments.length" class="uploadedFilesItems ml-5">
        <transition-group name="list-complete" tag="div" class="d-flex flex-column">
          <span
            v-for="attachment in attachments"
            :key="attachment"
            class="list-complete-item"
          >
            <attachment-item
              :attachment="attachment"
              :allow-to-preview="true">
            </attachment-item>
          </span>
        </transition-group>
      </div>
      <div v-else class="no-files-attached d-flex flex-column align-center text-sub-title">
        <div class="d-flex pl-6 not-files-icon">
          <i class="uiIconAttach uiIcon64x64"></i>
          <i class="uiIconCloseCircled uiIcon32x32"></i>
        </div>
        <span>{{ $t('no.attachments') }}</span>
      </div>
    </template>
    <template slot="header"></template>
  </exo-drawer>
</template>

<script>
export default {
  props:{
    attachments: {
      type: Array,
      default: () => []
    },
  },
  created() {
    this.$root.$on('open-attachments-list-drawer', () => this.openAttachmentsListDrawer());
  },
  methods:{
    openAttachmentsListDrawer() {
      this.$refs.attachmentsListDrawer.open();
    },
    closeAttachmentsListDrawer() {
      this.$refs.attachmentsListDrawer.close();
    },
    openAttachmentsAppDrawer() {
      this.closeAttachmentsListDrawer();
      this.$root.$emit('open-attachments-app-drawer');
    },
  }
};
</script>