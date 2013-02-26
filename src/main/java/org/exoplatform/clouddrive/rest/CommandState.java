/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

package org.exoplatform.clouddrive.rest;

/**
 * Bean used for creation of connect and state request entity in JSON.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CommandState.java 00000 Jan 17, 2013 pnedonosko $
 * 
 */
public class CommandState {
  final String    serviceUrl;

  final DriveInfo drive;

  final String    error;

  final int       progress;

  CommandState(DriveInfo drive, int progress, String serviceUrl) {
    this.drive = drive;
    this.progress = progress;
    this.serviceUrl = serviceUrl;
    this.error = "";
  }

  CommandState(DriveInfo drive, String error, int progress, String serviceUrl) {
    this.drive = drive;
    this.error = error;
    this.progress = progress;
    this.serviceUrl = serviceUrl;
  }

  CommandState(String error, int progress, String serviceUrl) {
    this(null, error, progress, serviceUrl);
  }

  /**
   * @return the drive
   */
  public DriveInfo getDrive() {
    return drive;
  }

  /**
   * @return the progress
   */
  public int getProgress() {
    return progress;
  }

  /**
   * @return the serviceUrl
   */
  public String getServiceUrl() {
    return serviceUrl;
  }

  /**
   * @return the error
   */
  public String getError() {
    return error;
  }
}
