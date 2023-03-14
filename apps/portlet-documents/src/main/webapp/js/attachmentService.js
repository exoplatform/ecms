export function fetchFoldersAndFiles(currentDrive, workspace, parentPath) {
  if(parentPath) {
    if (parentPath.startsWith('/')) {
      parentPath = parentPath.substr(1);
    }
    if (parentPath.endsWith('/')) {
      parentPath = parentPath.substr(0, parentPath.length - 1);
    }
  } else {
    parentPath = '';
  }
  return fetch(`/portal/rest/managedocument/getFoldersAndFiles/?driveName=${currentDrive}&workspaceName=${workspace}&currentFolder=${parentPath}`,
    {})
    .then(response => {
      if (response.ok) {
        return response.text();
      } else {
        return response.text().then(error => {
          log(`Error get data: ${error}`);
          throw new Error(error);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      log(`Error get data: ${e.errorMessage ? e.errorMessage : e}`);
      throw new Error(`Error getting folders and files of the current path ${e}`);
    });
}

export function getDrivers() {
  return fetch('/portal/rest/wcmDriver/getDrivers',
    {})
    .then(response => {
      if (response.ok) {
        return response.text();
      } else {
        return response.text().then(error => {
          log(`Error get data: ${error}`);
          throw new Error(error);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      log(`Error get drives data: ${e.errorMessage ? e.errorMessage : e}`);
      throw new Error(`Error getting drivers ${e}`);
    });
}

export function createFolder(currentDrive, workspace, parentPath, newFolderName, folderNodeType, isSystem) {
  const formData = new FormData();
  if (currentDrive) {
    formData.append('driveName', currentDrive);
  }
  if (workspace) {
    formData.append('workspaceName', workspace);
  }
  if (parentPath) {
    formData.append('currentFolder', parentPath);
  }
  if (newFolderName) {
    formData.append('folderName', newFolderName);
  }
  if (folderNodeType) {
    formData.append('folderNodeType', folderNodeType);
  }
  if (isSystem) {
    formData.append('isSystem', isSystem);
  }
  const params = new URLSearchParams(formData).toString();

  return fetch(`/portal/rest/managedocument/createFolder?${params}`, {})
    .then(response => {
      if (response.ok) {
        return response.text();
      } else {
        return response.text().then(error => {
          log(`Error post data: ${error}`);
          throw new Error(error);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      log(`Error creating folder: ${e.errorMessage ? e.errorMessage : e}`);
      throw new Error(`Error creating a new folder ${e}`);
    });
}

export function deleteFolderOrFile(currentDrive, workspace, itemPath) {
  return fetch(`/portal/rest/managedocument/deleteFolderOrFile/?driveName=${currentDrive}&workspaceName=${workspace}&itemPath=${itemPath}`, {})
    .then(response => {
      if (response.ok) {
        return response.text();
      } else {
        return response.text().then(error => {
          log(`Error delete data: ${error}`);
          throw new Error(error);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      log(`Error deleting the folder: ${e.errorMessage ? e.errorMessage : e}`);
      throw new Error(`Error deleting the folder or the file ${e}`);
    });
}

export function renameFolder(pathFolder, newTitle) {
  return fetch(`/portal/rest/contents/rename/rename?oldPath=${pathFolder}&newTitle=${newTitle}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.ok;
    }
  }).catch(e => {
    log(`Error rename folder: ${e.errorMessage ? e.errorMessage : e.errorCode}`);
    throw new Error(`Error rename this folder ${e}`);
  });

}

export function log(msg, err) {
  const logPrefix = '[attachmentsSelector] ';
  if (typeof console !== 'undefined' && typeof console.log !== 'undefined') { // eslint-disable-line no-console
    const isoTime = `--${new Date().toISOString()}`;
    let msgLine = msg;
    if (err) {
      msgLine += '. Error: ';
      if (err.name || err.message) {
        if (err.name) {
          msgLine += `[${err.name}]`;
        }
        if (err.message) {
          msgLine += err.message;
        }
      } else {
        msgLine +=
          typeof err === 'string'
            ? err
            : JSON.stringify(err) + (err.toString && typeof err.toString === 'function' ? `; ${err.toString()}` : '');
      }

      console.log(logPrefix + msgLine + isoTime); // eslint-disable-line no-console
      if (typeof err.stack !== 'undefined') {
        console.log(err.stack); // eslint-disable-line no-console
      }
    } else {
      if (err !== null && typeof err !== 'undefined') {
        msgLine += `. Error: ${err}`;
      }
      console.log(logPrefix + msgLine + isoTime); // eslint-disable-line no-console
    }
  }
}

export function isCloudDriveEnabled() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

export function uploadAttachment(workspaceName, driveName, currentFolder, currentPortal, uploadId, fileName, language,
  existenceAction, action) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/managedocument/uploadFile/control?workspaceName=${workspaceName}&driveName=${driveName}&currentFolder=${currentFolder}&currentPortal=${currentPortal}&uploadId=${uploadId}&fileName=${fileName}&language=${language}&existenceAction=${existenceAction}&action=${action}`, {
    credentials: 'include',
    method: 'GET',
  }).then(response => {
    if (response.ok) {
      return response.text();
    } else {
      return response.text().then(error => {
        log(`Error uploading attachment: ${error}`);
        throw new Error(error);
      });
    }
  })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    });
}

export function linkUploadedAttachmentToEntity(entityId, entityType, attachmentId) {
  if (!attachmentId) {
    throw new Error('Attachment Id can\'t be empty');
  }

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}/${attachmentId}`, {
    credentials: 'include',
    method: 'POST'
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error linking attachments to the entity');
    } else {
      return resp.json();
    }
  });
}

export function updateLinkedAttachmentsToEntity(entityId, entityType, attachmentIds) {
  attachmentIds.forEach(attachmentId => {
    if (!attachmentId) {
      throw new Error('Attachment Id can\'t be empty');
    }
  });

  const formData = new FormData();
  if (attachmentIds) {
    Object.keys(attachmentIds).forEach(attachmentId => {
      formData.append('attachmentId', attachmentIds[attachmentId]);
    });
  }

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error updating entity\'s linked attachments list');
    }
  });
}

export function removeEntityAttachment(entityId, entityType, attachmentId) {
  if (!attachmentId) {
    throw new Error('Attachment Id can\'t be empty');
  }
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}/${attachmentId}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error removing entity\'s linked attachment');
    }
  });
}

export function removeAllAttachmentsFromEntity(entityId, entityType) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error removing entity\'s linked attachment');
    }
  });
}

export function getEntityAttachments(entityType, entityId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp || resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting entity\'s linked attachments');
    }
  });
}

export function getAttachmentByEntityAndId(entityType, entityId, attachmentId) {
  if (!attachmentId) {
    throw new Error('Attachment Id can\'t be empty');
  }
  if (!entityType) {
    throw new Error('Entity Type can\'t be empty');
  }
  if (!entityId) {
    throw new Error('Entity Id can\'t be empty');
  }
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}/${attachmentId}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp || resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting entity\'s linked attachment');
    }
  });
}

export function getAttachmentById(attachmentId) {
  if (!attachmentId) {
    throw new Error('Attachment Id can\'t be empty');
  }
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${attachmentId}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp || resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting entity\'s linked attachment');
    }
  });
}

export function moveAttachmentToNewPath(newPathDrive, newPath, attachmentId, entityType, entityId) {
  if (!attachmentId) {
    throw new Error('Attachment Id can\'t be empty');
  }
  const formData = new FormData();

  formData.append('newPath', newPath ? newPath : '');
  if (newPathDrive) {
    formData.append('newPathDrive', newPathDrive);
  }
  if (entityType) {
    formData.append('entityType', entityType);
  }
  if (entityId) {
    formData.append('entityId', entityId);
  }

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${attachmentId}/move`, {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error moving attachment to the new destination path');
    } else {
      return resp.json();
    }
  });
}
export function createNewDoc(title, templateName, pathDrive, path) {
  const formData = new FormData();

  if (title) {
    formData.append('title', title);
  }
  if (templateName) {
    formData.append('templateName', templateName);
  }
  if (pathDrive) {
    formData.append('pathDrive', pathDrive);
  }
  if (path) {
    formData.append('path', path);
  }

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/newDoc`, {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then((resp) => {
    if (!resp || !resp.ok) {
      if (resp.status === 409) {
        return resp;
      }
      throw new Error('Error creating new document');
    } else {
      return resp.json();
    }
  });
}

export function convertXmlToJson(xml) {
  // Create the return object
  let obj = {}, i, j, attribute, item, nodeName, old;

  if (xml.nodeType === 1) { // element
    // do attributes
    if (xml.attributes.length > 0) {
      obj = {};
      for (j = 0; j < xml.attributes.length; j = j + 1) {
        attribute = xml.attributes.item(j);
        obj[attribute.nodeName] = attribute.nodeValue;
      }
    }
    // eslint-disable-next-line no-magic-numbers
  } else if (xml.nodeType === 3) { // text
    obj = xml.nodeValue;
  }

  // do children
  if (xml.hasChildNodes()) {
    for (i = 0; i < xml.childNodes.length; i = i + 1) {
      item = xml.childNodes.item(i);
      nodeName = item.nodeName;
      // eslint-disable-next-line no-undefined
      if (obj[nodeName] === undefined) {
        obj = convertXmlToJson(item);
      } else {
        // eslint-disable-next-line no-undefined
        if (obj[nodeName].push === undefined) {
          old = obj[nodeName];
          obj[nodeName] = [];
          obj[nodeName].push(old);
        }
        obj[nodeName].push(convertXmlToJson(item));
      }
    }
  }
  return obj;
}

export function downloadFiles(attachments, fileName) {
  const fileNameEncoded = fileName && window.encodeURIComponent(fileName) || '';
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/downloadByPath?fileName=${fileNameEncoded}`, {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(attachments),
  }).then(resp => {
    if (resp && resp.ok) {
      return resp.blob();
    } else {
      throw new Error(`Error downloading file '${fileName}' from server`);
    }
  }).then(blob => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName.replace(/\[[0-9]*\]$/g, '');
    document.body.appendChild(a);
    a.click();
    a.remove();
  });
}

export function checkExistence(driveName, workspaceName, currentFolder, fileName) {
  const formData = new FormData();

  if (workspaceName) {
    formData.append('workspaceName', workspaceName);
  }
  if (driveName) {
    formData.append('driveName', driveName);
  }
  if (currentFolder) {
    formData.append('currentFolder', currentFolder);
  }
  if (fileName) {
    formData.append('fileName', fileName);
  }

  const params = new URLSearchParams(formData).toString();
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/managedocument/uploadFile/exist?${params}`, {
    credentials: 'include',
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    }
  }).then(resp => resp && resp.text())
      .then(text => new DOMParser().parseFromString(text, "text/xml"));
}

