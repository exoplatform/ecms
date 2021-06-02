extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', {
  key: 'file',
  rank: 20,
  resourceBundle: 'locale.portlet.attachments',
  labelKey: 'attachments.composer.app.labelKey',
  description: 'attachments.composer.app.description',
  iconClass: 'addFileComposerIcon',
  appClass: 'attachmentsSelector',
  component: {
    name: 'exo-attachments',
    props: {
      showAttachmentsBackdrop: false,
    },
    model: {
      value: [],
      default: []
    },
    events: [{
      'event': 'attachmentsChanged',
      'listener': 'updateAttachments'
    }]
  },
  onExecute: function (attachmentsComponent) {
    attachmentsComponent.toggleAttachmentsDrawer();
  }
});

const downloadActivityActionBase = {
  click: (activity, activityTypeExtension) => {
    activityTypeExtension.download(activity);
  },
};

extensionRegistry.registerExtension('activity', 'action', Object.assign({
  id: 'download',
  labelKey: 'documents.label.download',
  // TODO use method activityTypeExtension.canDownload to displayed this button
  isEnabled: () => false,
}, downloadActivityActionBase));

extensionRegistry.registerExtension('activity', 'action', Object.assign({
  id: 'downloadAll',
  labelKey: 'documents.label.downloadAll',
  // TODO use method activityTypeExtension.canDownloadAll to displayed this button
  isEnabled: () => false,
}, downloadActivityActionBase));

const attachmentActivityTypeExtensionOptions = {
  canEdit: () => false,
  canDownload: activity => {
    return activity.templateParams && activity.templateParams.WORKSPACE && !activity.templateParams.WORKSPACE.includes('|@|');
  },
  canDownloadAll: activity => {
    return activity.templateParams && activity.templateParams.WORKSPACE && activity.templateParams.WORKSPACE.includes('|@|');
  },
  getBody: activity => activity && activity.title || '',
  click: (activity, activityExtensiontype) => {
    activityExtensiontype.download(activity);
  },
  download: () => {
    // TODO retrieve file to download through Attachment REST Service endpoint
  }
};

extensionRegistry.registerExtension('activity', 'type', {
  type: 'files:spaces',
  options: attachmentActivityTypeExtensionOptions,
});

extensionRegistry.registerExtension('activity', 'type', {
  type: 'sharefiles:spaces',
  options: attachmentActivityTypeExtensionOptions,
});

extensionRegistry.registerExtension('activity', 'type', {
  type: 'SHARED_FILE_ACTIVITY',
  options: attachmentActivityTypeExtensionOptions,
});

extensionRegistry.registerExtension('activity', 'type', {
  type: 'sharecloudfiles:spaces',
  options: attachmentActivityTypeExtensionOptions,
});
