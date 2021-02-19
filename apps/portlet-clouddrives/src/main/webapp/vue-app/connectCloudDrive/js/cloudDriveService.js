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
      return response.text().then(error => {
        cloudDriveUtils.log(`Error getting user drive: ${error.errorMessage ? error.errorMessage : error}`);
        throw new Error(error);
      });
    }
  } catch (e) {
    // network failure or anything prevented the request from completing.
    cloudDriveUtils.log(`Unable to get data: ${e.message}`);
    throw new Error("CloudFile.msg.ErrorReadingUserDrive"); // localized errorCode here
  }
}

export function isCloudDriveEnabled() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`,{
    credentials: "include",
    method: "GET",
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

