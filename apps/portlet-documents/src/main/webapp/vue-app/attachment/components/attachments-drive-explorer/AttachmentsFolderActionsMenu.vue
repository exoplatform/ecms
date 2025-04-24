<template>
  <v-menu
    v-model="showDropdownMenu"
    :position-x="folderActionsMenuLeft"
    :position-y="folderActionsMenuTop"
    :close-on-content-click="false"
    absolute
    offset-y>
    <v-list class="attachments-folder-action-menu pa-0">
      <v-list-item :class="!selectedFolder.canRemove ? 'not-allowed' : 'clickable'" class="pa-0">
        <v-list-item-title
          class="folder-actions-menu-item no-border px-3 border-bottom-color delete"
          @click="deleteFolder()">
          <i class="uiIconTrash pr-2"></i>{{ $t('attachments.filesFoldersSelector.action.delete') }}
        </v-list-item-title>
      </v-list-item>

      <v-list-item :class="!selectedFolder.canRemove ? 'not-allowed' : 'clickable'" class="pa-0">
        <v-list-item-title
          class="folder-actions-menu-item no-border px-3 rename"
          @click="renameFolder()">
          <i class="uiIconEdit pr-2"></i>{{ $t('attachments.filesFoldersSelector.action.rename') }}
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>
<script>
export default {
  props: {
    folderActionsMenuTop: {
      type: Number,
      default: 0
    },
    folderActionsMenuLeft: {
      type: Number,
      default: 0
    },
    selectedFolder: {
      type: Object,
      required: false,
      default: function () {
        return new Object();
      }
    }
  },
  data() {
    return {
      showDropdownMenu: false,
    };
  },
  methods: {
    openMenu() {
      this.showDropdownMenu = true;
    },
    closeMenu() {
      this.showDropdownMenu = false;
    },
    deleteFolder() {
      this.$emit('deleteFolder');
    },
    renameFolder() {
      this.$emit('renameFolder');
    }
  }
};
</script>
