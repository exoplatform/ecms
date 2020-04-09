let attachmentsComposerActions = null;

export function getAttachmentsComposerExtensions() {
  if(attachmentsComposerActions == null) {
    const allExtensions = getExtensionsByType('attachments-composer-action');
    attachmentsComposerActions = allExtensions.filter(extension => isExtensionEnabled(extension));
  }

  return attachmentsComposerActions;
}

export function executeExtensionAction(extension, component) {
  if(extension.hasOwnProperty('onExecute') && isFunction(extension.onExecute)) {
    extension.onExecute(component);
  }
}

function getExtensionsByType(type) {
  return extensionRegistry.loadExtensions('AttachmentsComposer', type);
}

function isExtensionEnabled(extension) {
  if(extension.hasOwnProperty('enabled')) {
    if(typeof extension.enabled === 'boolean') {
      return extension.enabled;
    } else if(isFunction(extension.enabled)) {
      return extension.enabled.call();
    }
  }

  return true;
}

function isFunction(object) {
  return object && {}.toString.call(object) === '[object Function]';
}
