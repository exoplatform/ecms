import "./css/cloud-drive.less";
import ConnectCloudDrive from "./components/ConnectCloudDrive.vue";

Vue.use(Vuetify);
Vue.component("connect-cloud-drive", ConnectCloudDrive);

const vuetify = new Vuetify({
  dark: true,
  iconfont: ""
});

// getting language of the PLF
const lang = typeof eXo !== "undefined" ? eXo.env.portal.language : "en";

// should expose the locale resources as REST API
const url = `/portal/rest/i18n/bundle/locale.clouddrive.CloudDrive-${lang}.json`;

let connectCloudApp;
export function init() {
  // should get workspace and path from Personal Documents
  cloudDrive.init("collaboration", "/Users/r___/ro___/roo___/root/Private");
  // getting locale resources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    connectCloudApp = new Vue({
      render: function(createElement) {
        return createElement(ConnectCloudDrive);
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
