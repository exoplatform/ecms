import CloudStorage from "./components/CloudStorage.vue";

Vue.use(Vuetify);

const vuetify = new Vuetify({
  dark: true,
  iconfont: "mdi",
});

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || "en";
const url = `/portal/rest/i18n/bundle/locale.clouddrive.CloudDrive-${lang}.json`;


export function init() {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    new Vue({
      render: (h) => h(CloudStorage),
      i18n,
      vuetify,
    }).$mount("#cloudStorageApp");
  });
}

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents("cloudStorageApp");
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}