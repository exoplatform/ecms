export function setTransferRulesStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/documents/transferRules/enabled`, {
    method: 'PATCH',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

export function getTransferRulesStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/documents/transferRules/enabled`,{
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}