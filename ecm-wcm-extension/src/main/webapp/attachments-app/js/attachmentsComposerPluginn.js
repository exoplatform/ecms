const ActivityComposerAttachmentsPluginn = [
  {
    key: 'filee',
    rank: 10,
    resourceBundle: 'locale.attachmentsSelector.attachments',
    labelKey: 'TESSSST',
    description: 'TESSSST description',
    iconClass: 'addFileComposerIcon',
    appClass: 'attachmentsApp',
    component: {
      name: 'exo-attachments-app',
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
      attachmentsComponent.openAttachmentsAppDrawer();
    }
  }];

require(['SHARED/extensionRegistry'], function (extensionRegistry) {
  for (const extension of ActivityComposerAttachmentsPluginn) {
    extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', extension);
  }
});
