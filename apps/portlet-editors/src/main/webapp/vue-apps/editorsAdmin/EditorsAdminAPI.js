export function getInfo(link) {
  return fetch(link, { method: 'GET' }).then(res => {
    if (res && res.ok) {
      return res.json();
    } else {
      throw new Error('Error when getting info');
    }
  });
}

export function postInfo(link, postData) {
  let requestBody;
  for (const prop in postData) {
    requestBody = encodeURIComponent(prop) + '=' + encodeURIComponent(postData[prop]);
  }
  return fetch(link, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: requestBody
  }).then(res => {
    if (res && res.ok) {
      return res;
    } else {
      throw new Error('Error when posted info');
    }
  });
}
