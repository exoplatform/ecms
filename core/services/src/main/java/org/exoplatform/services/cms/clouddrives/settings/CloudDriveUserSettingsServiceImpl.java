/*
* Copyright (C) 2022 eXo Platform SAS.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
*/

package org.exoplatform.services.cms.clouddrives.settings;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudDriveUserSettingsServiceImpl implements CloudDriveUserSettingsService {

  private SettingService               settingService;

  private static final Scope           CLOUD_DRIVE_USER_SETTING_SCOPE      = Scope.APPLICATION.id("CloudDrive");

  private static final String          CLOUD_DRIVE_SETTING_KEY             = "CloudDriveSettings";

  public CloudDriveUserSettingsServiceImpl(SettingService settingService) {
      this.settingService = settingService;
  }

  @Override
  public void saveCloudDriveUserSettings(long userIdentityId, CloudDriveSettingsRestEntity cloudDriveSettingsRestEntity) {
    if (userIdentityId <= 0) {
      throw new IllegalArgumentException("User identity id is mandatory");
    }
    if (cloudDriveSettingsRestEntity == null) {
      throw new IllegalArgumentException("CloudDrive user settings are empty");
    }

    SettingValue<?> settingValue = this.settingService.get(Context.USER.id(String.valueOf(userIdentityId)),
            CLOUD_DRIVE_USER_SETTING_SCOPE,
            CLOUD_DRIVE_SETTING_KEY);

    JSONObject existingUserSettings = null;
    String jsonString = null;
    if(settingValue != null && settingValue.getValue() != null) {
      try {
          existingUserSettings = new JSONObject(settingValue.getValue().toString());
          jsonString = existingUserSettings.put(cloudDriveSettingsRestEntity.getConnector(), cloudDriveSettingsRestEntity.getAccount()).toString();
      } catch (JSONException e) {
          throw new IllegalStateException("Error while parsing to jsonObject", e);
      }
    } else {
      try {
          jsonString = new JSONObject().put(cloudDriveSettingsRestEntity.getConnector(), cloudDriveSettingsRestEntity.getAccount()).toString();

      } catch (JSONException e) {
          throw new IllegalStateException("Error creating json object", e);
      }
    }

    this.settingService.set(Context.USER.id(String.valueOf(userIdentityId)),
            CLOUD_DRIVE_USER_SETTING_SCOPE,
            CLOUD_DRIVE_SETTING_KEY,
            SettingValue.create(jsonString));
  }

  @Override
  public String getCloudDriveUserSettings(long userIdentityId) {
    if (userIdentityId <= 0) {
      throw new IllegalArgumentException("User identity id is mandatory");
    }

    SettingValue<?> settingValue = this.settingService.get(Context.USER.id(String.valueOf(userIdentityId)),
            CLOUD_DRIVE_USER_SETTING_SCOPE,
            CLOUD_DRIVE_SETTING_KEY);
    return settingValue != null && settingValue.getValue() != null ? settingValue.getValue().toString() : "{}";
  }

  public void deleteCloudDriveUserSettings(long identityId, String providerId) throws JSONException {

    SettingValue<?> settingValue = this.settingService.get(Context.USER.id(String.valueOf(identityId)),
                                                           CLOUD_DRIVE_USER_SETTING_SCOPE,
                                                           CLOUD_DRIVE_SETTING_KEY);

    JSONObject existingUserSettings = new JSONObject(settingValue.getValue().toString());
    existingUserSettings.remove(providerId);
    this.settingService.set(Context.USER.id(String.valueOf(identityId)),
                            CLOUD_DRIVE_USER_SETTING_SCOPE,
                            CLOUD_DRIVE_SETTING_KEY,
                            SettingValue.create(existingUserSettings.toString()));
  }
}
