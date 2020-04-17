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
        throw new Error(error.errorCode);
      });
    }
  } catch (e) {
    // network failure or anything prevented the request from completing.
    cloudDriveUtils.log(`Unable to get data: ${e.message}`);
    throw new Error("UnableGetData"); // localized errorCode here
  }
}
