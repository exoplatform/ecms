import './components/initComponents.js';

Vue.use(Vuetify);

const vuetify = new Vuetify({
  dark: true,
  iconfont: '',
});

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale resources as REST API

const url = `/portal/rest/i18n/bundle/locale.attachmentsSelector.attachments-${lang}.json`;

// get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('attachmentsSelector');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

let exoAttachmentsApp;
export function init() {
  // getting locale resources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    exoAttachmentsApp = new Vue({
      el: '#exoAttachmentsApp',
      template: '<exo-attachments></exo-attachments>',
      i18n,
      vuetify
    });
  });
}

export function destroy() {
  if(exoAttachmentsApp) {
    exoAttachmentsApp.$destroy();
  }
}