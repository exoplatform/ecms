<template>
  <ul v-show="showDropdownMenu" id="folderActionsMenu"
      :style="{top:folderActionsMenuTop, left:folderActionsMenuLeft}"
      class="dropdown-menu" tabindex="-1" @blur="closeMenu">
    <li :class="!selectedFolder.canRemove ? 'disabled' : ''" class="folderActionsMenuItem btn delete"
        @click="deleteFolder()"><i class="uiIconTrash"></i>{{ $t('attachments.filesFoldersSelector.action.delete') }}
    </li>
    <li :class="!selectedFolder.canRemove ? 'disabled' : ''" class="folderActionsMenuItem btn rename"><i
      class="uiIconEdit"></i>{{ $t('attachments.filesFoldersSelector.action.rename') }}
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
  }
};
</script>
