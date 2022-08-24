/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
export async function getUserDrive() {
  try {
    const response = await fetch(`${cloudDriveUtils.pageBaseUrl()}/portal/rest/clouddrive/document/drive/personal`, {
      credentials: 'include',
      method: 'GET',
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
    throw new Error('CloudFile.msg.ErrorReadingUserDrive'); // localized errorCode here
  }
}

export function isCloudDriveEnabled() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/features/status/enabled`,{
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

export function saveUserSettings(settings) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/settings`,{
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(settings),
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    }
  });
}

export function getUserSettings() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/settings`,{
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }  
  });
}

export function disconnect(workspace, providerId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/clouddrive/disconnect?workspace=${workspace}&providerId=${providerId}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
}

