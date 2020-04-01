const CloudAttachmentsPlugin = {
    key: 'attach-file',
    rank: 20,
    labelKey: 'Connect cloud documents',
    description: 'Connect cloud documents',
    iconClass: 'uiIconGeneralDrive',
    appClass: 'attachmentsSelector',
    html: function() {
        return "Select from drives";
    },
    onExecute() {
        console.log('execute function');
    }
  };
  require(['SHARED/extensionRegistry'], function(extensionRegistry) {
    extensionRegistry.registerExtension('ActivityComposer', 'activity-composer-action', CloudAttachmentsPlugin);
  });
  