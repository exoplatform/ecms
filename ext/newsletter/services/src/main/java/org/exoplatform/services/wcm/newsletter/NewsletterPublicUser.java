/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.newsletter;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * ha.mai@exoplatform.com
 * Jun 22, 2009
 */
public class NewsletterPublicUser {

  /** The email. */
  String email;

  /** The user code. */
  String userCode;

  /** The is banned. */
  boolean isBanned;

  /**
   * Gets the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email the new email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the user code.
   *
   * @return the user code
   */
  public String getUserCode() {
    return userCode;
  }

  /**
   * Sets the user code.
   *
   * @param userCode the new user code
   */
  public void setUserCode(String userCode) {
    this.userCode = userCode;
  }

  /**
   * Checks if is banned.
   *
   * @return true, if is banned
   */
  public boolean isBanned() {
    return isBanned;
  }

  /**
   * Sets the banned.
   *
   * @param isBanned the new banned
   */
  public void setBanned(boolean isBanned) {
    this.isBanned = isBanned;
  }
}
