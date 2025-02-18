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

import org.exoplatform.commons.utils.ISO8601;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 5, 2009
 */
public class VersionLog extends VersionData {

  /** The log date. */
  private Calendar logDate;

  /** The description. */
  private String description;

  /**
   * Instantiates a new version log.
   *
   * @param versionName the version name
   * @param state the state
   * @param author the author
   * @param logDate the log date
   * @param description the description
   */
  public VersionLog(String versionName, String state, String author, Calendar logDate, String description) {
    super(null, state, author, null,null);
    this.logDate = logDate;
    this.description = description;
    this.versionName = versionName;
  }

  /**
   * Gets the log date.
   *
   * @return the log date
   */
  public Calendar getLogDate() {
    return logDate;
  }

  /**
   * Sets the log date.
   *
   * @param logDate the new log date
   */
  public void setLogDate(Calendar logDate) {
    this.logDate = logDate;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return new StringBuilder().append(versionName).append(";").append(state).append(";")
    .append(author).append(";").append(ISO8601.format(logDate)).append(";").append(description).toString();
  }

  /**
   * To version log.
   *
   * @param log the log
   *
   * @return the version log
   */
  public static VersionLog toVersionLog(String log) {
    String[] logs = log.split(";");
    return new VersionLog(logs[0],logs[1],logs[2],ISO8601.parse(logs[3]),logs[4]);
  }
}
