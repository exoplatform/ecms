export function getSharedDocumentStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/getSharedDocumentStatus`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when getting shared documents status');
    }
  });
}

export function saveSharedDocumentStatus(TransferRulesStatusModel) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/saveSharedDocumentStatus`, {
    method: 'PUT',
    credentials: 'include',
    body: JSON.stringify(TransferRulesStatusModel),
    headers: {
      'Content-Type': 'application/json'
    }
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when saving shared documents status');
    }
  });
}
