import './initComponents.js';

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// should expose the locale ressources as REST API
const url = `/portal/rest/i18n/bundle/locale.social.Webui-${lang}.json`;
// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('ShareDocumentActivity');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}
let shareDocumentApp;
export function init(params) {
  const appId = `shareDocumentActivity-${params.activityId}`;

  const appElement = document.createElement('div');
  appElement.id = appId;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    shareDocumentApp = new Vue({
      template: `<share-document-activity
                  v-cacheable="{cacheId: '${appId}'}"
                  id="${appId}"/>`,
      i18n,
      vuetify,
    }).$mount(appElement);
  });
}

export function openShareDocumentActivityDrawer(params) {
  if (shareDocumentApp) {
    shareDocumentApp.$root.$emit('document-share-drawer-open', params);
  }
}

export function destroy() {
  if (shareDocumentApp) {
    shareDocumentApp.$destroy();
  }
}