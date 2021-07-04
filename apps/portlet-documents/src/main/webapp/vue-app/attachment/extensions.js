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

  const downloadHandlerExtension = {
    click: (activity) => {
      const repositories = activity.templateParams.REPOSITORY && activity.templateParams.REPOSITORY.split('|@|')
                        || (activity.templateParams.repository && [activity.templateParams.repository]);
      const workspaces = activity.templateParams.WORKSPACE && activity.templateParams.WORKSPACE.split('|@|')
                        || (activity.templateParams.workspace && [activity.templateParams.workspace]);
      const docPaths = activity.templateParams.DOCPATH && activity.templateParams.DOCPATH.split('|@|')
                        || (activity.templateParams.nodePath && [activity.templateParams.nodePath]);
      const attachments = docPaths.map((docPath, i) => ({
        docPath,
        repository: repositories[i],
        workspace: workspaces[i],
      }));
      const fileName = attachments.length === 1 ? attachments[0].docPath.replaceAll(/(.*)\//g, '') : `Activity_${activity.id}_${activity.createDate.substring(0, 10)}.zip`;
      return Vue.prototype.$attachmentService.downloadFiles(attachments, fileName);
    },
  };

  extensionRegistry.registerExtension('activity', 'action', Object.assign({
    id: 'download',
    labelKey: 'documents.label.download',
    isEnabled: activity => {
      if (activity.templateParams) {
        const docPaths = activity.templateParams.DOCPATH && activity.templateParams.DOCPATH.split('|@|')
                          || (activity.templateParams.nodePath && [activity.templateParams.nodePath]);
        return docPaths && docPaths.length === 1;
      }
    },
    rank: 0,
  }, downloadHandlerExtension));

  extensionRegistry.registerExtension('activity', 'action', Object.assign({
    id: 'downloadAll',
    labelKey: 'documents.label.downloadAll',
    isEnabled: activity => {
      if (activity.templateParams) {
        const docPaths = activity.templateParams.DOCPATH && activity.templateParams.DOCPATH.split('|@|')
                          || (activity.templateParams.nodePath && [activity.templateParams.nodePath]);
        return docPaths && docPaths.length > 1;
      }
    },
    rank: 0,
  }, downloadHandlerExtension));

}
