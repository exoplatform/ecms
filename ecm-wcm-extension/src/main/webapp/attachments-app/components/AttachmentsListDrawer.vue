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
      <v-list v-if="!$scopedSlots.attachmentsList" dense>
        <v-list-item-group>
          <attachment-item
            v-for="(attachment, i) in attachments"
            :key="i"
            :file="attachment">
          </attachment-item>
        </v-list-item-group>
      </v-list>
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