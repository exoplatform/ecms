import DlpQuarantineApp from './components/DlpQuarantineApp.vue';

const components = {
  'dlp-quarantine-app': DlpQuarantineApp,
};

for (const key in components) {
  Vue.component(key, components[key]);
}