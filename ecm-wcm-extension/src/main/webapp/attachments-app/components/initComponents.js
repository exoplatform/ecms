import AttachmentsDrawer  from './AttachmentsDrawer.vue';
import AttachmentItem  from './attachments-upload-components/AttachmentItem.vue';
import AttachmentsFolderActionsMenu  from './attachments-drive-explorer/AttachmentsFolderActionsMenu.vue';
import AttachmentsApp from './AttachmentsApp.vue';
import AttachmentsDriveExplorerFileItem from './attachments-drive-explorer/AttachmentsDriveExplorerFileItem.vue';
import AttachmentsNotificationAlert from './snackbar/AttachmentsNotificationAlert.vue';
import AttachmentsNotificationAlerts from './snackbar/AttachmentsNotificationAlerts.vue';
import AttachmentsListDrawer from './AttachmentsListDrawer.vue';
import AttachmentsUploadInput from './attachments-upload-components/AttachmentsUploadInput.vue';
import AttachmentsUploadedFiles from './attachments-upload-components/AttachmentsUploadedFiles.vue';
import AttachmentsDriveExplorerDrawer from './attachments-drive-explorer/AttachmentsDriveExplorerDrawer.vue';

Vue.component('attachments-drawer', AttachmentsDrawer);
Vue.component('attachments-app', AttachmentsApp);
Vue.component('attachment-item', AttachmentItem);
Vue.component('attachments-drive-explorer-drawer', AttachmentsDriveExplorerDrawer);
Vue.component('attachments-drive-explorer-file-item', AttachmentsDriveExplorerFileItem);
Vue.component('attachments-folder-actions-menu', AttachmentsFolderActionsMenu);
Vue.component('attachments-notification-alert', AttachmentsNotificationAlert);
Vue.component('attachments-notification-alerts', AttachmentsNotificationAlerts);
Vue.component('attachments-list-drawer', AttachmentsListDrawer);
Vue.component('attachments-uploaded-files', AttachmentsUploadedFiles);
Vue.component('attachments-upload-input', AttachmentsUploadInput);

import * as attachmentService from '../js/attachmentService.js';

if (!Vue.prototype.$attachmentService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentService', {
    value: attachmentService,
  });
}
