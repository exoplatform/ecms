/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.documents;

/**
 * The Interface IdentityProfileService is a facade to Social.
 */
public interface IdentityProfileService {

  /**
   * Checks if profile exists.
   *
   * @param userId the user id
   * @return true, if successful
   */
  boolean hasProfile(String userId);
  
  /**
   * Gets the user profile.
   *
   * @param userId the user id
   * @return the user profile
   */
  IdentityProfile getUserProfile(String userId);
  
  /**
   * Gets the space profile.
   *
   * @param groupId the group id
   * @return the space profile
   */
  IdentityProfile getSpaceProfile(String groupId);
  
}
