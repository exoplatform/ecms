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

const components = {
  'attachments-drawer': AttachmentsDrawer,
  'attachment': Attachment,
  'attachment-item': AttachmentItem,
  'attachments-drive-explorer-drawer': AttachmentsDriveExplorerDrawer,
  'attachments-select-from-drive': AttachmentsSelectFromDrive,
  'attachments-drive-explorer-file-item': AttachmentsDriveExplorerFileItem,
  'attachments-folder-actions-menu': AttachmentsFolderActionsMenu,
  'attachments-notification-alert': AttachmentsNotificationAlert,
  'attachments-notification-alerts': AttachmentsNotificationAlerts,
  'attachments-list-drawer': AttachmentsListDrawer,
  'attachments-uploaded-files': AttachmentsUploadedFiles,
  'attachments-upload-input': AttachmentsUploadInput,
  'activity-attachments': ActivityAttachments,
  'activity-attachment': ActivityAttachment,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

import * as attachmentService from '../../js/attachmentService.js';

if (!Vue.prototype.$attachmentService) {
  window.Object.defineProperty(Vue.prototype, '$attachmentService', {
    value: attachmentService,
  });
}
