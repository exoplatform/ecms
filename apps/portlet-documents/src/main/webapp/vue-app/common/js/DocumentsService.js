export function getDocuments(query, folder, type, limit) {
  let url = '/portal/rest/documents';
  if (query != null && query !== 'null') {
    url += `/query?query=${query}&limit=${limit}`;
  }
  else if (folder != null && folder !== 'null') {
    url += `/folder?folder=${folder}&limit=${limit}`;
  }
  else if (type != null && type !== 'null') {
    url += `/type?type=${type}&limit=${limit}`;
  }
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