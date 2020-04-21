import './initComponents.js';

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('documents');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

Vue.use(Vuetify);

const vuetify = new Vuetify({
  dark: true,
  iconfont: '',
});

export function init(appId, query, folder, type, limit) {
// init Vue app when locale ressources are ready
  new Vue({
    template: `<documents app-id="${appId}" id="${appId}" query="${query}" folder="${folder}" type="${type}" limit="${limit}"></documents>`,
    vuetify,
  }).$mount(`#${appId}`);
}