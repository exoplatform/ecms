export function installExtensions() {
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

  extensionRegistry.registerComponent('ActivityContent', 'activity-content-extensions', {
    id: 'attachments',
    isEnabled: (params) => {
      const activity = params && params.activity;
      return activity && activity.templateParams && (activity.templateParams.DOCPATH || activity.templateParams.nodePath);
    },
    vueComponent: Vue.options.components['activity-attachments'],
    rank: 10,
  });

}
