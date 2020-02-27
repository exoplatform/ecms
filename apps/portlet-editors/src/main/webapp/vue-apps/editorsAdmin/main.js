import adminApp from "./components/AdminApp.vue";

import "../../css/main.less";

Vue.use(Vuetify);
// Vue.component("task-drawer", TaskDrawer);
// Vue.component("task-details", TaskDetails);

const vuetify = new Vuetify({
  dark: true,
  iconfont: "",
});

// getting language of user
const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || "en";
const resourceBundleName = "locale.portlet.EditorsAdmin";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${resourceBundleName}-${lang}.json`;

export function init(settings) {
  // TODO settings object contains starting info such as
  // { 
  //   "services" : {
  //     "providers": "https://..." <<< // an URL to providers REST service
  //   },
  //  // Other required data, e.g. current user
  //   "user": {
  //     "id": "john",
  //     "full_name": "John Smith"
  //   }
  // }
  
  // getting locale ressources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      data: {
        message : "Hello Vue!",
        settings: settings // TODO pass settings via data here?
      },
      render: h => h(adminApp),
      i18n,
      vuetify,
    }).$mount("#editors-admin");
  });
}
