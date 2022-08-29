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
package org.exoplatform.services.cms.clouddrives.settings;

import org.exoplatform.social.core.identity.model.Identity;

public interface CloudDriveUserSettingsService {

    /**
     * Saves clouddrive connectors settings for authenticated user
     *
     * @param identityId technical identifier of {@link Identity}
     * @param cloudDriveSettingsRestEntity object of {@link CloudDriveSettingsRestEntity}
     */
    void saveCloudDriveUserSettings(long identityId, CloudDriveSettingsRestEntity cloudDriveSettingsRestEntity);

    /**
     * Gets clouddrive connectors settings for authenticated user
     *
     * @param identityId technical identifier of {@link Identity}
     */
    String getCloudDriveUserSettings(long identityId);
}
