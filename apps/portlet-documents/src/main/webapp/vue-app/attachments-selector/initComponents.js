import ExoAttachments  from './components/ExoAttachments.vue';
import ExoFoldersFilesSelector  from './components/ExoFoldersFilesSelector.vue';
import ExoAttachmentItem  from './components/ExoAttachmentItem.vue';
import ExoDropdownMenu  from './components/ExoDropdownMenu.vue';

const components = {
  'exo-attachments': ExoAttachments,
  'exo-folders-files-selector': ExoFoldersFilesSelector,
  'exo-attachment-item': ExoAttachmentItem,
  'exo-dropdown-menu': ExoDropdownMenu,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

