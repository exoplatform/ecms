export async function getData(url) {
  try {
    const response = await fetch(url, {
      headers: {
        "Content-Type": "application/json"
      },
      method: "GET"
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
    throw new Error("UnableGetData"); // localized errorCode here
  }
}

export async function postData(url, data) {
  try {
    const response = await fetch(url, {
      headers: {
        "Content-Type": "application/json"
      },
      method: "PUT",
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
    throw new Error("UnablePostData"); // localized errorCode here
  }
}

export function log(msg, err) {
  const logPrefix = "[editorsAdmin] ";
  if (typeof console !== "undefined" && typeof console.log !== "undefined") {
    const isoTime = `--${new Date().toISOString()}`;
    let msgLine = msg;
    if (err) {
      msgLine += ". Error: ";
      if (err.name || err.message) {
        if (err.name) {
          msgLine += `[${err.name}]`;
        }
        if (err.message) {
          msgLine += err.message;
        }
      } else {
        msgLine +=
          typeof err === "string"
            ? err
            : JSON.stringify(err) + (err.toString && typeof err.toString === "function" ? `; ${err.toString()}` : "");
      }

      console.log(logPrefix + msgLine + isoTime);
      if (typeof err.stack !== "undefined") {
        console.log(err.stack);
      }
    } else {
      if (err !== null && typeof err !== "undefined") {
        msgLine += `. Error: ${err}`;
      }
      console.log(logPrefix + msgLine + isoTime);
    }
  }
}
