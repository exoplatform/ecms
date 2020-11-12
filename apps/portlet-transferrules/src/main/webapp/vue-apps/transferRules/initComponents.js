import TransferRulesApp from './components/TransferRulesApp.vue';

const components = {
  'transfer-rules-app': TransferRulesApp,
};

for (const key in components) {
  Vue.component(key, components[key]);
}