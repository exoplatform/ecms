import "./css/cloud-drive.less";
import ConnectCloud from "./components/ConnectCloud.vue";

Vue.use(Vuetify);
Vue.component("connect-cloud", ConnectCloud);

const vuetify = new Vuetify({
  dark: true,
  iconfont: ""
});

// getting language of the PLF
const lang = typeof eXo !== "undefined" ? eXo.env.portal.language : "en";

// should expose the locale resources as REST API

const url = `/portal/rest/i18n/bundle/locale.clouddrive.CloudDrive-${lang}.json`;

// get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents("cloud-drive");
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

let connectCloudApp;
export function init() {
  // getting locale resources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    connectCloudApp = new Vue({
      render: function(createElement) {
        return createElement(ConnectCloud);
      },
      i18n,
      vuetify
    }).$mount("#connectCloudApp");
  });
}

export function destroy() {
  if (connectCloudApp) {
    connectCloudApp.$destroy();
  }
}
