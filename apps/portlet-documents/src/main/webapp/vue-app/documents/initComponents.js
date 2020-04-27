import Documents from './components/Documents.vue';

const components = {
  'documents': Documents,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
