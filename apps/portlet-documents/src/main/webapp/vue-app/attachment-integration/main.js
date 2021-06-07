import './initComponents.js';

Vue.use(Vuetify);

const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale resources as REST API

const url = `/portal/rest/i18n/bundle/locale.portlet.attachments-${lang}.json`;

// get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('attachmentIntegration');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

exoi18n.loadLanguageAsync(lang, url).then(i18n => {
  new Vue({i18n});
});

const appId = 'attachmentIntegration';
let attachmentIntegration;
export function init(entityId, entityType, defaultDrive, defaultFolder, spaceId) {
  // getting locale resources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    attachmentIntegration = new Vue({
      data: () => ({
        entityId: entityId || '',
        entityType: entityType || '',
        defaultDrive: defaultDrive || null,
        defaultFolder: defaultFolder || '',
        spaceId: spaceId || '',
      }),
      template: `<attachment-app
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
  if (attachmentIntegration) {
    attachmentIntegration.$root.$emit('open-attachments-app-drawer');
  }
}

export function destroy() {
  if (attachmentIntegration) {
    attachmentIntegration.$destroy();
  }
}