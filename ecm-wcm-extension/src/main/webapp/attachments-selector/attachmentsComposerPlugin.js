const ActivityComposerAttachmentsPlugin = [
  {
    key: 'file',
    rank: 20,
    resourceBundle: 'locale.attachmentsSelector.attachments',
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
      events: [
        {
          'event': 'attachmentsChanged',
          'listener': 'updateAttachments'
        }
      ]
    },
    onExecute: function (attachmentsComponent) {
      attachmentsComponent.toggleAttachmentsDrawer();
    }
  }];

require(['SHARED/extensionRegistry'], function (extensionRegistry) {
  for (const extension of ActivityComposerAttachmentsPlugin) {
    extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', extension);
  }
});
