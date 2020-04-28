export async function getUserDrive() {
  try {
    const response = await fetch(`${cloudDriveUtils.pageBaseUrl()}/portal/rest/clouddrive/document/drive/personal`, {
      headers: {
        "Content-Type": "application/json",
      },
      method: "GET",
    });
    if (response.ok) {
      return response.json();
    } else {
      return response.json().then(error => {
        cloudDriveUtils.log(`Error getting user drive: ${error.errorMessage ? error.errorMessage : error.errorCode}`);
        throw new Error(error.errorMessage);
      });
    }
  } catch (e) {
    // network failure or anything prevented the request from completing.
    cloudDriveUtils.log(`Unable to get data: ${e.message}`);
    throw new Error("CloudFile.msg.ErrorReadingUserDrive"); // localized errorCode here
  }
}

export function notifyError(errorMsg) {
  $.pnotify({
    title: "User drive error",
    text: errorMsg,
    type: "error",
    icon: "picon picon-process-stop",
    hide: false,
    delay: 0,
    closer: true,
    sticker: false,
    opacity: 1,
    shadow: true,
    nonblock: false,
    width: "380px",
    addclass: "stack-topleft", // This is one of the included default classes.
    stack: { dir1: "down", dir2: "left", firstpos1: 5, firstpos2: 5 }
  });
}
