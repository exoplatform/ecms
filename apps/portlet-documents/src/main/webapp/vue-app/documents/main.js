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
//getting language of user
const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';

const resourceBundleName = 'locale.portlet.documents';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${resourceBundleName}-${lang}.json`;

export function init(appId, query, folder, type, limit) {
//getting locale ressources
exoi18n.loadLanguageAsync(lang, url)
  .then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      template: `<documents app-id="${appId}" id="${appId}" query="${query}" folder="${folder}" type="${type}" limit="${limit}"></documents>`,
      i18n,
      vuetify,
    }).$mount(`#${appId}`);
  });
}