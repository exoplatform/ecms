// configuration here uses configuretion file (in case of cloudDrive - cloudDrivePlugin.js) from another vue app,
// extensions are registered there
// (extensionRegistry.registerExtension("AttachmentsComposer", "attachments-composer-action", extension))
// if composer name or extension type changes it should be changed here and in configuration file

let attachmentsComposerActions = null;

export function getAttachmentsComposerExtensions() {
  if (attachmentsComposerActions == null || !attachmentsComposerActions.length) {
    const allExtensions = getExtensionsByType('attachments-composer-action');
    // if some extension registered but has flag 'enabled' set to false, we don't add this extension
    attachmentsComposerActions = allExtensions.filter(extension => isExtensionEnabled(extension));
  }

  return attachmentsComposerActions;
}

export function executeExtensionAction(extension, component) {
  if (Object.prototype.hasOwnProperty.call(extension, 'onExecute') && isFunction(extension.onExecute)) {
    extension.onExecute.apply(component, [component]); // it will execute code inside onExecute() defined in configuration file
  }
}

function getExtensionsByType(type) {
  return extensionRegistry.loadExtensions('AttachmentsComposer', type);
}

function isExtensionEnabled(extension) {
  if (Object.prototype.hasOwnProperty.call(extension, 'enabled')) {
    if (typeof extension.enabled === 'boolean') {
      return extension.enabled;
    } else if (isFunction(extension.enabled)) {
      return extension.enabled.call();
    }
  }

  return true;
}

function isFunction(object) {
  return object && {}.toString.call(object) === '[object Function]';
}
