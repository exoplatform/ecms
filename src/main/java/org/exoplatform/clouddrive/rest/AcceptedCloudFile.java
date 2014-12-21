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
   * @return the isSymlink
   */
  public boolean isSymlink() {
    return false;
  }

  /**
   * @return the id
   */
  public String getId() {
    return null;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return null;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return null;
  }

  /**
   * @return the previewLink
   */
  public String getPreviewLink() {
    return null;
  }

  /**
   * @return the editLink
   */
  public String getEditLink() {
    return null;
  }

  /**
   * @return the thumbnailLink
   */
  public String getThumbnailLink() {
    return null;
  }

  /**
   * @return the type
   */
  public String getType() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
   * @return the author
   */
  public String getAuthor() {
    return null;
  }

  /**
   * @return the createdDate
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
   * @return the isFolder
   */
  public boolean isFolder() {
    return false;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Return <code>true</code> always.
   */
  public boolean isCreating() {
    return true;
  }
}
