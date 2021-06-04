export function shareDocumentOnSpaces(spaceId, sharedActivity) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/document/activities/${spaceId}/share`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(sharedActivity),
  }).then(resp => {
    if (!resp || !resp.ok) {
      return resp.text().then((text) => {
        throw new Error(text);
      });
    } else {
      return resp.json();
    }
  });
}