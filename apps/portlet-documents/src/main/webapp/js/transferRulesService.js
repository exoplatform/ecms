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
  });
}