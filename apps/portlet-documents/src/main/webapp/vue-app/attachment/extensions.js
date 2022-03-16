export function installExtensions() {
  CKEDITOR.plugins.addExternal('attachFile', '/eXoWCMResources/eXoPlugins/attachFile/', 'plugin.js');

  extensionRegistry.registerComponent('ActivityContent', 'activity-content-extensions', {
    id: 'attachments',
    isEnabled: (params) => {
      const activityOrComment = params && params.activity;
      return activityOrComment && !activityOrComment.activityId && activityOrComment.templateParams && (activityOrComment.templateParams.DOCPATH || activityOrComment.templateParams.nodePath);
    },
    vueComponent: Vue.options.components['activity-attachments'],
    rank: 10,
  });

  extensionRegistry.registerComponent('CommentContent', 'comment-content-extensions', {
    id: 'attachments',
    isEnabled: (params) => {
      const activityOrComment = params && params.activity;
      return activityOrComment && activityOrComment.templateParams && (activityOrComment.templateParams.DOCPATH || activityOrComment.templateParams.nodePath);
    },
    vueComponent: Vue.options.components['activity-attachments'],
    rank: 20,
  });

  extensionRegistry.registerComponent('TaskDrawer', 'task-drawer-action', {
    id: 'attachments',
    vueComponent: Vue.options.components['task-attachment'],
    rank: 10,
  });

  extensionRegistry.registerComponent('ActivityComposerAction', 'activity-composer-action', {
    id: 'activityComposerAttachments',
    vueComponent: Vue.options.components['activity-composer-attachments'],
    rank: 10,
  });

  extensionRegistry.registerExtension('ActivityComposer', 'ckeditor-extensions', {
    id: 'attachFile',
    extraPlugin: 'attachFile',
    extraToolbarItem: 'attachFile',
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

  Vue.prototype.$transferRulesService.getTransfertRulesDownloadDocumentStatus()
    .then(data => {
      if (data) {
        extensionRegistry.registerExtension('activity', 'action', Object.assign({
          id: 'download',
          icon: 'fa-download',
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
          icon: 'fa-download',
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
    });

  extensionRegistry.registerExtension('AnalyticsTable', 'CellValue', {
    type: 'document-title',
    options: {
      // Rank of executing 'match' method
      rank: 10,
      // Used Vue component to display cell value
      vueComponent: Vue.options.components['analytics-table-cell-document-title-value'],
      // Method complete signature : match: (fieldName, aggregationType, fieldDataType, item) => { ... }
      match: fieldName => fieldName === 'documentId.keyword',
    },
  });
  extensionRegistry.registerExtension('AnalyticsTable', 'CellValue', {
    type: 'document-size',
    options: {
      // Rank of executing 'match' method
      rank: 20,
      // Used Vue component to display cell value
      vueComponent: Vue.options.components['analytics-table-cell-document-size-value'],
      // Method complete signature : match: (fieldName, aggregationType, fieldDataType, item) => { ... }
      match: fieldName => fieldName === 'documentSize',
    },
  });
  extensionRegistry.registerExtension('AnalyticsTable', 'CellValue', {
    type: 'document-origin',
    options: {
      // Rank of executing 'match' method
      rank: 30,
      // Used Vue component to display cell value
      vueComponent: Vue.options.components['analytics-table-cell-document-origin-value'],
      // Method complete signature : match: (fieldName, aggregationType, fieldDataType, item) => { ... }
      match: fieldName => fieldName === 'origin.keyword',
    },
  });

}
