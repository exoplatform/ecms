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
package org.exoplatform.services.wcm.newsletter.config;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 5, 2009
 */
public class NewsletterUserConfig {

  /** The mail. */
  private String mail;

  /** The is banned. */
  private boolean isBanned;

  /**
   * Gets the mail.
   *
   * @return the mail
   */
  public String getMail() {
    return mail;
  }

  /**
   * Sets the mail.
   *
   * @param mail the new mail
   */
  public void setMail(String mail) {
    this.mail = mail;
  }

  /**
   * Gets the checks if is banned.
   *
   * @return the checks if is banned
   */
  public boolean getIsBanned() {
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
