/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.config;

import java.util.Calendar;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 4, 2009
 */
public class VersionData {

  /** The UUID. */
  protected String UUID;

  /** The version name. */
  protected String versionName;

  /** The state. */
  protected String state;

  /** The author. */
  protected String author;

  /** The start publication date. */
  protected Calendar startPublicationDate;

  /** The end publication date. */
  protected Calendar endPublicationDate;

  /**
   * Instantiates a new version data.
   *
   * @param uuid the uuid
   * @param state the state
   * @param author the author
   * @param startPublicationDate the start publication date
   * @param endPublicationDate the end publication date
   */
  public VersionData(String uuid, String state, String author, Calendar startPublicationDate, Calendar endPublicationDate) {
    this.UUID = uuid;
    this.state = state;
    this.author = author;
    this.startPublicationDate = startPublicationDate;
    this.endPublicationDate = endPublicationDate;
  }

  /**
   * Instantiates a new version data.
   *
   * @param uuid the uuid
   * @param state the state
   * @param author the author
   */
  public VersionData(String uuid, String state, String author) {
    this.UUID = uuid;
    this.state = state;
    this.author = author;
    this.startPublicationDate = null;
    this.endPublicationDate = null;
  }

  /**
   * Gets the version name.
   *
   * @return the version name
   */
  public String getVersionName() {
    return versionName;
  }

  /**
   * Sets the version name.
   *
   * @param versionName the new version name
   */
  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  /**
   * Gets the state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state the new state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Gets the author.
   *
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Sets the author.
   *
   * @param author the new author
   */
  public void setAuthor(String author) {
    this.author = author;
  }

  /**
   * Gets the start publication date.
   *
   * @return the start publication date
   */
  public Calendar getStartPublicationDate() {
    return startPublicationDate;
  }

  /**
   * Sets the start publication date.
   *
   * @param startPublicationDate the new start publication date
   */
  public void setStartPublicationDate(Calendar startPublicationDate) {
    this.startPublicationDate = startPublicationDate;
  }

  /**
   * Gets the end publication date.
   *
   * @return the end publication date
   */
  public Calendar getEndPublicationDate() {
    return endPublicationDate;
  }

  /**
   * Sets the end publication date.
   *
   * @param endPublicationDate the new end publication date
   */
  public void setEndPublicationDate(Calendar endPublicationDate) {
    this.endPublicationDate = endPublicationDate;
  }

  /**
   * To string values.
   *
   * @return the string[]
   */
  public String[] toStringValues() {
    return new String[] { versionName, state, author} ;
  }

  /**
   * To string value.
   *
   * @return the string
   */
  public String toStringValue() {
    StringBuilder builder = new StringBuilder();
    builder.append(UUID).append(",").append(state).append(",").append(author);
    return builder.toString();
  }

  public String toString() {
    return toStringValue();
  }

  /**
   * To version data.
   *
   * @param s the s
   *
   * @return the version data
   */
  public static VersionData toVersionData(String s) {
    String[] info = s.split(",");
    return new VersionData(info[0],info[1],info[2]);
  }

  /**
   * Gets the uUID.
   *
   * @return the uUID
   */
  public String getUUID() {
    return UUID;
  }

  /**
   * Sets the uUID.
   *
   * @param uuid the new uUID
   */
  public void setUUID(String uuid) {
    UUID = uuid;
  }
}
