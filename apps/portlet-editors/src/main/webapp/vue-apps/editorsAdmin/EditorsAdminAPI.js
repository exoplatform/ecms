export async function getData(url) {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json'
      },
      method: 'GET'
    });
    if (response.ok) {
      return response.json();
    } else {
      return response.json().then(error => {
        log(`Error writing data: ${error.errorMessage ? error.errorMessage : error.errorCode}`);
        throw new Error(error.errorCode);
      });
    }
  } catch (e) {
    // network failure or anything prevented the request from completing.
    log(`Unable to get data: ${e.message}`);
    throw new Error('UnableGetData'); // localized errorCode here
  }
}

export async function postData(url, data) {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json'
      },
      method: 'PUT',
      body: JSON.stringify(data)
    });
    if (response.ok) {
      return response.text();
    } else {
      return response.json().then(error => {
        log(`Error writing data: ${error.errorMessage ? error.errorMessage : error.errorCode}`);
        throw new Error(error.errorCode);
      });
    }
  } catch (e) {
    // network failure or anything prevented the request from completing.
    log(`Unable to post data: ${e.message}`);
    throw new Error('UnablePostData'); // localized errorCode here
  }
}

export function log(msg, err) {
  const logPrefix = '[editorsAdmin] ';
  const isoTime = `--${new Date().toISOString()}`;
  if (err) {
    console.error(logPrefix + msg + isoTime, err);
  } else {
    console.error(logPrefix + msg + isoTime);
  }
}
