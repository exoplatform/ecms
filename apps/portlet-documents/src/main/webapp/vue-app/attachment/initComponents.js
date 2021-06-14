import AttachmentsDrawer  from './components/AttachmentsDrawer.vue';
import AttachmentItem  from './components/attachments-upload-components/AttachmentItem.vue';
import AttachmentsFolderActionsMenu  from './components/attachments-drive-explorer/AttachmentsFolderActionsMenu.vue';
import Attachment from './components/Attachment.vue';
import AttachmentsDriveExplorerFileItem from './components/attachments-drive-explorer/AttachmentsDriveExplorerFileItem.vue';
import AttachmentsNotificationAlert from './components/snackbar/AttachmentsNotificationAlert.vue';
import AttachmentsNotificationAlerts from './components/snackbar/AttachmentsNotificationAlerts.vue';
import AttachmentsListDrawer from './components/AttachmentsListDrawer.vue';
import AttachmentsUploadInput from './components/attachments-upload-components/AttachmentsUploadInput.vue';
import AttachmentsUploadedFiles from './components/attachments-upload-components/AttachmentsUploadedFiles.vue';
import AttachmentsDriveExplorerDrawer from './components/attachments-drive-explorer/AttachmentsDriveExplorerDrawer.vue';
import AttachmentsSelectFromDrive from './components/attachments-drive-explorer/AttachmentsSelectFromDrive.vue';
import ActivityAttachments from './components/activity/ActivityAttachments.vue';
import ActivityAttachment from './components/activity/ActivityAttachment.vue';

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

Vue.component('activity-attachments', ActivityAttachments);
Vue.component('activity-attachment', ActivityAttachment);

import * as attachmentService from '../../js/attachmentService.js';

if (!Vue.prototype.$attachmentService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentService', {
    value: attachmentService,
  });
}
