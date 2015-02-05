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

import org.exoplatform.clouddrive.CloudFile;

import java.util.Calendar;

/**
 * Not yet cloud file. It is a file that accepted to be a cloud file, but currently is creating (uploading) to
 * the cloud and this operation not completed.<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LinkedCloudFile.java 00000 May 26, 2014 pnedonosko $
 */
public class AcceptedCloudFile implements CloudFile {

  private final String path;

  public AcceptedCloudFile(String path) {
    this.path = path;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isSymlink() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getTitle() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getLink() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getEditLink() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getPreviewLink() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getThumbnailLink() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getType() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getTypeMode() {
    return null;
  }

  /**
   * @return the lastUser
   */
  public String getLastUser() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getAuthor() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Calendar getCreatedDate() {
    return null;
  }

  /**
   * @return the modifiedDate
   */
  public Calendar getModifiedDate() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isFolder() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return 0; // zero for accepted
  }

}
