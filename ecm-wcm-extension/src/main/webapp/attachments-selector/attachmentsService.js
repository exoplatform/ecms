export function getSpaceById(id) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/spaces/${id}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  }).catch(e => {
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
        return response.json().then(error => {
          throw new Error(error.errorMessage ? error.errorMessage : error.errorCode);
        });
      }})
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
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
        return response.json().then(error => {
          throw new Error(error.errorMessage ? error.errorMessage : error.errorCode);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      throw new Error(`Error getting drivers ${e}`);
    });
}

export function createFolder(currentDrive, workspace, parentPath, newFolderName) {
  return fetch(`/portal/rest/managedocument/createFolder?driveName=${currentDrive}&workspaceName=${workspace}&currentFolder=${parentPath}&folderName=${newFolderName}`, {})
    .then(response => {
      if (response.ok) { 
        return response.text(); 
      } else { 
        return response.json().then(error => {
          throw new Error(error.errorMessage ? error.errorMessage : error.errorCode);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      throw new Error(`Error creating a new folder ${e}`);
    });
}

export function deleteFolderOrFile(currentDrive, workspace, itemPath) {
  return fetch(`/portal/rest/managedocument/deleteFolderOrFile/?driveName=${currentDrive}&workspaceName=${workspace}&itemPath=${itemPath}`, {})
    .then(response => {
      if (response.ok) { 
        return response.text(); 
      } else { 
        return response.json().then(error => {
          throw new Error(error.errorMessage ? error.errorMessage : error.errorCode);
        });
      }
    })
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
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
    throw new Error(`Error rename this folder ${e}`);
  });

}