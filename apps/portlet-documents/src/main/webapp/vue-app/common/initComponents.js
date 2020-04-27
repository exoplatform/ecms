import ExoDocuments from './components/ExoDocuments.vue';
import ExoDocument from './components/ExoDocument.vue';

const components = {
  'exo-documents': ExoDocuments,
  'exo-document': ExoDocument
};

for (const key in components) {
  Vue.component(key, components[key]);
}
