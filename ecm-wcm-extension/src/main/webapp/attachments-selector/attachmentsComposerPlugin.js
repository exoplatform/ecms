const ActivityComposerAttachmentsPlugin = {
  key: 'file',
  rank: 20,
  labelKey: 'attachments.composer.app.labelKey',
  description: 'attachments.composer.app.description',
  iconClass: 'addFileComposerIcon',
  appClass: 'attachmentsSelector',
  component: 'exo-attachments',
  onExecute() {
    document.getElementsByClassName('attachments drawer')[0].className += ' open';
  },
};
require(['SHARED/extensionRegistry'], function(extensionRegistry) {
  extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', ActivityComposerAttachmentsPlugin);
});
