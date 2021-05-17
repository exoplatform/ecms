import './components/initComponents.js';

Vue.use(Vuetify);

const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale resources as REST API

const url = [`/portal/rest/i18n/bundle/locale.attachmentsSelector.attachments-${lang}.json`,
  `/portal/rest/i18n/bundle/locale.documents.documents-${lang}.json`];

// get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('attachmentsApp');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

exoi18n.loadLanguageAsync(lang, url).then(i18n => {
  if (Vue.prototype.$i18n) {
    Vue.prototype.$i18n = i18n;
  }
});

const appId = 'attachmentsApp';
let attachmentsApp;
export function init(entityId, entityType, defaultDrive, defaultFolder, spaceId) {
  // getting locale resources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    attachmentsApp = new Vue({
      data: () => ({
        entityId: entityId || '',
        entityType: entityType || '',
        defaultDrive: defaultDrive || null,
        defaultFolder: defaultFolder || '',
        spaceId: spaceId || '',
      }),
      template: `<attachments-app
                  :entity-id=entityId
                  :entity-type=entityType
                  :default-drive=defaultDrive
                  :default-folder=defaultFolder
                  :space-id=spaceId
                  id="${appId}" />`,
      vuetify,
      i18n
    }).$mount(`#${appId}`);
  });
}

export function openAttachmentsDrawer() {
  if (attachmentsApp) {
    attachmentsApp.$root.$emit('open-attachments-app-drawer');
  }
}

export function destroy() {
  if (attachmentsApp) {
    attachmentsApp.$destroy();
  }
}

document.dispatchEvent(new CustomEvent('exo-attachment-app-refresh'));