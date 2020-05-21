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
 * The Class ProfileInfo.
 */
public class IdentityProfile {
  
  /** The avatar url. */
  private final String avatarUrl;
  
  /** The display name. */
  private final String displayName;
  
  
  /**
   * Instantiates a new profile info.
   *
   * @param avatarUrl the avatar url
   * @param displayName the display name
   */
  public IdentityProfile(String avatarUrl, String displayName) {
    this.avatarUrl = avatarUrl;
    this.displayName = displayName;
  }

  /**
   * Gets the avatar url.
   *
   * @return the avatar url
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }

  /**
   * Gets the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }
}
