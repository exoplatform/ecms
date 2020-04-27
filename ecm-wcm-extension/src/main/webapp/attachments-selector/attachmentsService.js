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
    .then(response => response.text())
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
    .then(response => response.text())
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
    .then(response => response.text())
    .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
    .then(xml => {
      if (xml) {
        return xml;
      }
    }).catch(e => {
      throw new Error(`Error creating a new folder ${e}`);
    });
}