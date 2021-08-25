<template>
  <ul
    v-show="showDropdownMenu"
    id="folderActionsMenu"
    :style="{top:folderActionsMenuTop, left:folderActionsMenuLeft}"
    class="dropdown-menu"
    tabindex="-1"
    @blur="closeMenu">
    <li
      :class="!selectedFolder.canRemove ? 'disabled' : ''"
      class="folderActionsMenuItem  delete"
      @click="deleteFolder()">
      <i class="uiIconTrash"></i>
      <span>{{ $t('attachments.filesFoldersSelector.action.delete') }}</span>
    </li>
    <li
      :class="!selectedFolder.canRemove ? 'disabled' : ''"
      class="folderActionsMenuItem  rename"
      @click="renameFolder()">
      <i
        class="uiIconEdit"></i>
      <span>{{ $t('attachments.filesFoldersSelector.action.rename') }}</span>
    </li>
  </ul>
</template>

<script>
export default {
  props: {
    showDropdownMenu: {
      type: Boolean,
      default: false
    },
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
  methods: {
    deleteFolder() {
      this.$emit('deleteFolder');
    },
    closeMenu() {
      this.$emit('closeMenu');
    },
    renameFolder(){
      this.$emit('renameFolder');
    }
  }
};
</script>
