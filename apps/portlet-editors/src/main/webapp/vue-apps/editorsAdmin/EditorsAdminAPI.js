export async function getData(url) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json"
    },
    method: "GET"
  });
  if (response && response.ok) {
    return response.json();
  } else {
    log("Unable to get data");
    const errorText = await response.text();
    throw new Error(errorText);
  }
}

export async function postData(url, data) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json"
    },
    method: "POST",
    body: JSON.stringify(data)
  });
  if (response && response.ok) {
    const responseText = await response.text();
    return responseText ? JSON.parse(responseText) : {};
  } else {
    log("Unable to post data");
    const errorText = await response.text();
    throw new Error(errorText);
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

export function parsedErrorMsg(error) {
  try {
    JSON.parse(error.message);
  } catch (e) {
    return error.message;
  }
  return JSON.parse(error.message).message;
}
