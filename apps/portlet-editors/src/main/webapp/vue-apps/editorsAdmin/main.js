import adminApp from "./components/AdminApp.vue";
import EditDialog from "./components/EditDialog.vue";

import "../../css/main.less";

Vue.use(Vuetify);
Vue.component("edit-dialog", EditDialog);

const vuetify = new Vuetify({
  dark: true,
  iconfont: "",
});

// getting language of user
const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || "en";
const resourceBundleName = "locale.portlet.EditorsAdmin";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${resourceBundleName}-${lang}.json`;

export function init(settings) {
  // getting locale ressources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      data: {
        message : "Hello Vue!"
      },
      render: h => h(adminApp, { props: { services: settings.services }}),
      i18n,
      vuetify
    }).$mount("#editors-admin");
  });
}
