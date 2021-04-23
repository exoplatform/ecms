import AttachmentsDrawer  from './AttachmentsDrawer.vue';
import AttachmentsFoldersFilesSelector  from './AttachmentsFoldersFilesSelector.vue';
import AttachmentItem  from './AttachmentItem.vue';
import ExoDropdownMenu  from './ExoDropdownMenu.vue';
import AttachmentsApp from './AttachmentsApp.vue';
import AttachmentsFileItem from './AttachmentsFileItem.vue';
import AttachmentsNotificationAlert from './snackbar/AttachmentsNotificationAlert.vue';
import AttachmentsNotificationAlerts from './snackbar/AttachmentsNotificationAlerts.vue';

Vue.component('attachments-drawer', AttachmentsDrawer);
Vue.component('attachments-app', AttachmentsApp);
Vue.component('attachments-folders-files-selector', AttachmentsFoldersFilesSelector);
Vue.component('attachment-item', AttachmentItem);
Vue.component('exo-dropdown-menu', ExoDropdownMenu);
Vue.component('attachments-file-item', AttachmentsFileItem);
Vue.component('attachments-notification-alert', AttachmentsNotificationAlert);
Vue.component('attachments-notification-alerts', AttachmentsNotificationAlerts);

import * as attachmentsService from '../js/attachmentsService.js';

if (!Vue.prototype.$attachmentsService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentsService', {
    value: attachmentsService,
  });
}
