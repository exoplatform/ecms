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
        color="grey"
        @click="openAttachmentsAppDrawer()"
      >
        <i class="uiIconAttach" ></i>
      </v-btn>

    </template>
    <template slot="content">
      <v-list v-if="attachments.length" dense>
        <v-list-item-group>
          <attachment-item
            v-for="attachment in attachments"
            :key="attachment.id"
            :file="attachment">
          </attachment-item>
        </v-list-item-group>
      </v-list>
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