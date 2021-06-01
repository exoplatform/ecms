import ShareDocumentActivity from './components/ShareDocumentActivity.vue';

const components = {
  'share-document-activity': ShareDocumentActivity,
};
for (const key in components) {
  Vue.component(key, components[key]);
}
import * as  documentServices from './services/documentServices';

if (!Vue.prototype.$documentServices) {
  window.Object.defineProperty(Vue.prototype, '$documentServices', {
    value: documentServices,
  });
}