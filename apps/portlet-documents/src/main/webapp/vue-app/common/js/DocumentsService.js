function getDocuments(url) {
  return fetch(url, {
    method: 'GET',
	credentials: 'include',
  }).then((resp) => {
    if(resp && resp.ok) {
      return resp.json();
	} else {
      throw new Error (`Error when getting documents`);
	}
  })
}

export function getDocumentsByQuery(query, limit) {
  const url = `/portal/rest/documents/query?query=${query}&limit=${limit}`;
  return getDocuments(url);
}

export function getDocumentsByFolder(folder, limit) {
  const url = `/portal/rest/documents/folder?folder=${folder}&limit=${limit}`;
  return getDocuments(url);
}

export function getRecentDocuments(limit) {
  const url = `/portal/rest/search/documents/recent?myWork=true&limit=${limit}`;
  return getDocuments(url);
}

export function getRecentSpacesDocuments(limit) {
  const url = `/portal/rest/search/documents/recentSpaces?limit=${limit}`;
  return getDocuments(url);
}

export function getFavoriteDocuments(limit) {
  const url = `/portal/rest/documents/favorite?limit=${limit}`;
  return getDocuments(url);
}

export function getSharedDocuments(limit) {
  const url = `/portal/rest/documents/shared?limit=${limit}`;
  return getDocuments(url);
}