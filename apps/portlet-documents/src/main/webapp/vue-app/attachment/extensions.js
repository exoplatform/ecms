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
      const activityOrComment = params && params.activity;
      return activityOrComment && !activityOrComment.activityId && activityOrComment.templateParams && (activityOrComment.templateParams.DOCPATH || activityOrComment.templateParams.nodePath);
    },
    vueComponent: Vue.options.components['activity-attachments'],
    rank: 10,
  });

}
