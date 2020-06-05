/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.ecm.connector.clouddrives;

/**
 * Bean used for creation of connect and state request entity in JSON.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CommandState.java 00000 Jan 17, 2013 pnedonosko $
 */
public class CommandState {

  /** The service url. */
  final String    serviceUrl;

  /** The drive. */
  final DriveInfo drive;

  /** The error. */
  final String    error;

  /** The progress. */
  final int       progress;

  /**
   * Instantiates a new command state.
   *
   * @param drive the drive
   * @param progress the progress
   * @param serviceUrl the service url
   */
  CommandState(DriveInfo drive, int progress, String serviceUrl) {
    this.drive = drive;
    this.progress = progress;
    this.serviceUrl = serviceUrl;
    this.error = "";
  }

  /**
   * Instantiates a new command state.
   *
   * @param drive the drive
   * @param error the error
   * @param progress the progress
   * @param serviceUrl the service url
   */
  CommandState(DriveInfo drive, String error, int progress, String serviceUrl) {
    this.drive = drive;
    this.error = error;
    this.progress = progress;
    this.serviceUrl = serviceUrl;
  }

  /**
   * Instantiates a new command state.
   *
   * @param error the error
   * @param progress the progress
   * @param serviceUrl the service url
   */
  CommandState(String error, int progress, String serviceUrl) {
    this(null, error, progress, serviceUrl);
  }

  /**
   * Gets the drive.
   *
   * @return the drive
   */
  public DriveInfo getDrive() {
    return drive;
  }

  /**
   * Gets the progress.
   *
   * @return the progress
   */
  public int getProgress() {
    return progress;
  }

  /**
   * Gets the service url.
   *
   * @return the serviceUrl
   */
  public String getServiceUrl() {
    return serviceUrl;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }
}
