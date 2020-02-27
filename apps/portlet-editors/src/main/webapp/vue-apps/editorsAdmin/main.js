import adminApp from './components/AdminApp.vue';

import '../../css/main.less';

Vue.use(Vuetify);
//Vue.component('task-drawer', TaskDrawer);
//Vue.component('task-details', TaskDetails);

const vuetify = new Vuetify({
    dark: true,
    iconfont: '',
});

// getting language of user
const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';

const resourceBundleName = 'locale.portal.EditorsAdmin';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${resourceBundleName}-${lang}.json`;

export function init() {
//getting locale ressources
exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
        // init Vue app when locale ressources are ready
        new Vue({
            render: h => h(adminApp),
            i18n,
            vuetify,
        }).$mount('#digital-workplace-tasks');
    });
}
