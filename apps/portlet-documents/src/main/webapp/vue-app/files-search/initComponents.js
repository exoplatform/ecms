import fileSearchCard from './components/FileSearchCard.vue';
import fileFavoriteAction from './components/FileFavoriteAction.vue';


const components = {
  'file-search-card': fileSearchCard,
  'file-favorite-action': fileFavoriteAction
};

for (const key in components) {
  Vue.component(key, components[key]);
}

