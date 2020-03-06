export function getInfo(link) {
  return fetch(link, { method: 'GET' }).then(res => {
    if (res && res.ok) {
      return res.json();
    } else {
      log('Unable to get data');
    }
  })
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
      log('Unable to post data');
    }
  });
}

function log(msg, err) {
  const logPrefix = "[editorsAdmin] ";
  if (typeof console !== "undefined" && typeof console.log !== "undefined") {
    const isoTime = " -- " + new Date().toISOString();
    let msgLine = msg;
    if (err) {
      msgLine += ". Error: ";
      if (err.name || err.message) {
        if (err.name) {
          msgLine += "[" + err.name + "] ";
        }
        if (err.message) {
          msgLine += err.message;
        }
      } else {
        msgLine += (typeof err === "string" ? err : JSON.stringify(err)
            + (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
      }

      console.log(logPrefix + msgLine + isoTime);
      if (typeof err.stack !== "undefined") {
        console.log(err.stack);
      }
    } else {
      if (err !== null && typeof err !== "undefined") {
        msgLine += ". Error: '" + err + "'";
      }
      console.log(logPrefix + msgLine + isoTime);
    }
  }
}