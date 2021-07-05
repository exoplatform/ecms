export function getSpaceById(id) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/spaces/${id}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  }).catch(e => {
    log(`Error getting space: ${e.errorMessage ? e.errorMessage : e.errorCode}`);
    throw new Error(`Error getting space with id ${e}`);
  });
}

export function fetchFoldersAndFiles(currentDrive, workspace, parentPath) {
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

export function createFolder(currentDrive, workspace, parentPath, newFolderName) {
  return fetch(`/portal/rest/managedocument/createFolder?driveName=${currentDrive}&workspaceName=${workspace}&currentFolder=${parentPath}&folderName=${newFolderName}`, {})
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
    method: 'POST',
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
  let params = {};
  if (attachmentIds) {
    params.attachmentIds = attachmentIds;
  }
  params = $.param(params, true);

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${entityType}/${entityId}?${params}`, {
    credentials: 'include',
    method: 'PUT',
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
  let params = {};
  params.newPath = newPath? newPath: '';
  if (newPathDrive) {
    params.newPathDrive = newPathDrive;
  }
  if (entityType) {
    params.entityType = entityType;
  }
  if (entityId) {
    params.entityId = entityId;
  }
  params = $.param(params, true);

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/attachments/${attachmentId}/move?${params}`, {
    credentials: 'include',
    method: 'POST'
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error moving attachment to the new destination path');
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
