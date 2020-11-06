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
      }})
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

export function renameFolder(pathFolder,newTitle) {
  return fetch(`/portal/rest/contents/rename/rename?oldPath=${pathFolder}&newTitle=${newTitle}`,{
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
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`,{
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}
