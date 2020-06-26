import './initComponents.js';

//get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('FileSearch');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

Vue.use(Vuetify);

export function formatSearchResult(result) {
  return result;
}
