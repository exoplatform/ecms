import ExoAttachmentsDrawer  from './ExoAttachmentsDrawer.vue';
import ExoFoldersFilesSelector  from './ExoFoldersFilesSelector.vue';
import ExoAttachmentItem  from './ExoAttachmentItem.vue';
import ExoDropdownMenu  from './ExoDropdownMenu.vue';
import ExoAttachmentsApp from './ExoAttachmentsApp.vue';
import AttachmentsNotificationAlert from './snackbar/AttachmentsNotificationAlert.vue';
import AttachmentsNotificationAlerts from './snackbar/AttachmentsNotificationAlerts.vue';

Vue.component('exo-attachments-drawer', ExoAttachmentsDrawer);
Vue.component('exo-attachments-app', ExoAttachmentsApp);
Vue.component('exo-folders-files-selector', ExoFoldersFilesSelector);
Vue.component('exo-attachment-item', ExoAttachmentItem);
Vue.component('exo-dropdown-menu', ExoDropdownMenu);
Vue.component('attachments-notification-alert', AttachmentsNotificationAlert);
Vue.component('attachments-notification-alerts', AttachmentsNotificationAlerts);

import * as attachmentsService from '../js/attachmentsService.js';

if (!Vue.prototype.$attachmentsService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentsService', {
    value: attachmentsService,
  });
}
