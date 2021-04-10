import ConnectCloudDrive from "./components/ConnectCloudDrive.vue";

Vue.use(Vuetify);
Vue.component("connect-cloud-drive", ConnectCloudDrive);

const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// getting language of the PLF
const lang = typeof eXo !== "undefined" ? eXo.env.portal.language : "en";

// should expose the locale resources as REST API
const url = `/portal/rest/i18n/bundle/locale.clouddrive.CloudDrive-${lang}.json`;

// getting locale resources
exoi18n.loadLanguageAsync(lang, url).then(i18n => {
  // init Vue app when locale resources are ready
  new Vue({
    render: function(createElement) {
      return createElement(ConnectCloudDrive, { props: { showCloudDrawer: true }});
    },
    i18n,
    vuetify
  }).$mount("#connectCloudApp");
});