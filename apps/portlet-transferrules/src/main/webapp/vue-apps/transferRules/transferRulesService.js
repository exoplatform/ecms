export function getTransfertRulesDocumentStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/getTransfertRulesDocumentStatus`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when getting shared documents status');
    }
  });
}

export function saveSharedDocumentStatus(saveSharedDocumentStatus) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/saveSharedDocumentStatus`, {
    method: 'PUT',
    credentials: 'include',
    body: JSON.stringify(saveSharedDocumentStatus),
    headers: {
      'Content-Type': 'application/json'
    }
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when saving shared documents status');
    }
  });
}

export function saveDownloadDocumentStatus(downloadDocumentStatus) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/saveDownloadDocumentStatus`, {
    method: 'PUT',
    credentials: 'include',
    body: JSON.stringify(downloadDocumentStatus),
    headers: {
      'Content-Type': 'application/json'
    }
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when saving download documents status');
    }
  });
}
