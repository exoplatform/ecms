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
package org.exoplatform.wcm.connector.collaboration.editors;

/**
 * The Class IdentityData.
 */
public class IdentityData extends EditorPermission {

  /** The type. */
  private String type;

  
  /**
   * Instantiates a new identity search result.
   *
   * @param id the id
   * @param displayName the display name
   * @param type the type
   * @param avatarUrl the avatar url
   */
  public IdentityData(String id, String displayName, String type, String avatarUrl) {
    super(id, displayName, avatarUrl);
    this.type = type;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

}
