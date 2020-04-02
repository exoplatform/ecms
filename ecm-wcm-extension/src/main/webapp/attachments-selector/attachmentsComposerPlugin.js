const ActivityComposerAttachmentsPlugin = {
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
      showAttachmentsDrawer: false,
      maxFilesCount: 22,
      maxFileSize: 333
    },
    model: {
      value: []
    },
    events: [
      {
        'event': 'uploadingFileFinished',
        'listener': 'setUploadingCount',
        'listenerParam': 'add'
      },
      {
        'event': 'removingFileFinished',
        'listener': 'setUploadingCount',
        'listenerParam': 'remove'
      }
    ],
    show: false
  },
  onExecute: function () {
    document.getElementsByClassName('attachments drawer')[0].className += ' open';
  }
};
require(['SHARED/extensionRegistry'], function (extensionRegistry) {
  extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', ActivityComposerAttachmentsPlugin);
});
