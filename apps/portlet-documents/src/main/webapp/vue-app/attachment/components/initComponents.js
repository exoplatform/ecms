import AttachmentsDrawer  from './AttachmentsDrawer.vue';
import AttachmentItem  from './attachments-upload-components/AttachmentItem.vue';
import AttachmentsFolderActionsMenu  from './attachments-drive-explorer/AttachmentsFolderActionsMenu.vue';
import Attachment from './Attachment.vue';
import AttachmentsDriveExplorerFileItem from './attachments-drive-explorer/AttachmentsDriveExplorerFileItem.vue';
import AttachmentsNotificationAlert from './snackbar/AttachmentsNotificationAlert.vue';
import AttachmentsNotificationAlerts from './snackbar/AttachmentsNotificationAlerts.vue';
import AttachmentsListDrawer from './AttachmentsListDrawer.vue';
import AttachmentsUploadInput from './attachments-upload-components/AttachmentsUploadInput.vue';
import AttachmentsUploadedFiles from './attachments-upload-components/AttachmentsUploadedFiles.vue';
import AttachmentsDriveExplorerDrawer from './attachments-drive-explorer/AttachmentsDriveExplorerDrawer.vue';
import AttachmentsSelectFromDrive from './attachments-drive-explorer/AttachmentsSelectFromDrive.vue';

Vue.component('AttachmentsDrawer', AttachmentsDrawer);
Vue.component('Attachment', Attachment);
Vue.component('AttachmentItem', AttachmentItem);
Vue.component('AttachmentsDriveExplorerDrawer', AttachmentsDriveExplorerDrawer);
Vue.component('AttachmentsSelectFromDrive', AttachmentsSelectFromDrive);
Vue.component('AttachmentsDriveExplorerFileItem', AttachmentsDriveExplorerFileItem);
Vue.component('AttachmentsFolderActionsMenu', AttachmentsFolderActionsMenu);
Vue.component('AttachmentsNotificationAlert', AttachmentsNotificationAlert);
Vue.component('AttachmentsNotificationAlerts', AttachmentsNotificationAlerts);
Vue.component('AttachmentsListDrawer', AttachmentsListDrawer);
Vue.component('AttachmentsUploadedFiles', AttachmentsUploadedFiles);
Vue.component('AttachmentsUploadInput', AttachmentsUploadInput);

import * as attachmentService from '../../../js/attachmentService.js';

if (!Vue.prototype.$attachmentService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentService', {
    value: attachmentService,
  });
}
