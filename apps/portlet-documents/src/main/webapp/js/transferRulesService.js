export function getTransfertRulesDownloadDocumentStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/getTransfertRulesDocumentStatus`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
    else {
      throw new Error ('Error when getting transfer rules download documents status');
    }
  }).then(data => {
    if (data) {
      return data.downloadDocumentStatus !== 'true';
    }
  });
}

export function getDocumentsTransferRules() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/transferRules/getTransfertRulesDocumentStatus`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error');
    } else {
      return resp.json();
    }
  });
}