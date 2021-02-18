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

