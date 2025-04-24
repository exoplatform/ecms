import AttachmentApp from './components/AttachmentApp.vue';

Vue.component('AttachmentApp', AttachmentApp);

import * as attachmentService from '../../js/attachmentService.js';

if (!Vue.prototype.$attachmentService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentService', {
    value: attachmentService,
  });
}
