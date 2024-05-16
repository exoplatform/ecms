import AttachmentsDrawer  from './components/AttachmentsDrawer.vue';
import AttachmentItem  from './components/attachments-upload-components/AttachmentItem.vue';
import AttachmentsFolderActionsMenu  from './components/attachments-drive-explorer/AttachmentsFolderActionsMenu.vue';
import Attachment from './components/Attachment.vue';
import AttachmentsDriveExplorerFileItem from './components/attachments-drive-explorer/AttachmentsDriveExplorerFileItem.vue';
import AttachmentsListDrawer from './components/AttachmentsListDrawer.vue';
import AttachmentsUploadInput from './components/attachments-upload-components/AttachmentsUploadInput.vue';
import AttachmentsUploadedFiles from './components/attachments-upload-components/AttachmentsUploadedFiles.vue';
import AttachmentsDriveExplorerDrawer from './components/attachments-drive-explorer/AttachmentsDriveExplorerDrawer.vue';
import AttachmentsSelectFromDrive from './components/attachments-drive-explorer/AttachmentsSelectFromDrive.vue';
import ActivityAttachments from './components/activity/ActivityAttachments.vue';
import ActivityAttachment from './components/activity/ActivityAttachment.vue';
import ActivityComposerAttachments from './components/activity/ActivityComposerAttachments.vue';
import AttachmentCreateDocumentInput from './components/attachment-document-creator/AttachmentCreateDocumentInput.vue';
import TaskAttachment from './components/task/TaskAttachment.vue';
import AnalyticsTableCellDocumentTitleValue from './components/analytics/AnalyticsTableCellDocumentTitleValue.vue';
import AnalyticsTableCellDocumentSizeValue from './components/analytics/AnalyticsTableCellDocumentSizeValue.vue';
import AnalyticsTableCellDocumentOriginValue from './components/analytics/AnalyticsTableCellDocumentOriginValue.vue';
import ContentAttachmentList from './components/content/ContentAttachmentList.vue';

const components = {
  'attachments-drawer': AttachmentsDrawer,
  'attachment': Attachment,
  'attachment-item': AttachmentItem,
  'attachments-drive-explorer-drawer': AttachmentsDriveExplorerDrawer,
  'attachments-select-from-drive': AttachmentsSelectFromDrive,
  'attachments-drive-explorer-file-item': AttachmentsDriveExplorerFileItem,
  'attachments-folder-actions-menu': AttachmentsFolderActionsMenu,
  'attachments-list-drawer': AttachmentsListDrawer,
  'attachments-uploaded-files': AttachmentsUploadedFiles,
  'attachments-upload-input': AttachmentsUploadInput,
  'activity-attachments': ActivityAttachments,
  'activity-attachment': ActivityAttachment,
  'activity-composer-attachments': ActivityComposerAttachments,
  'attachment-create-document-input': AttachmentCreateDocumentInput,
  'task-attachment': TaskAttachment,
  'analytics-table-cell-document-title-value': AnalyticsTableCellDocumentTitleValue,
  'analytics-table-cell-document-size-value': AnalyticsTableCellDocumentSizeValue,
  'analytics-table-cell-document-origin-value': AnalyticsTableCellDocumentOriginValue,
  'content-attachment-list': ContentAttachmentList
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

import * as transferRulesService from '../../js//transferRulesService.js';

if (!Vue.prototype.$transferRulesService) {
  window.Object.defineProperty(Vue.prototype, '$transferRulesService', {
    value: transferRulesService,
  });
}
