/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

import java.util.Calendar;

import org.exoplatform.services.cms.clouddrives.CloudFile;

/**
 * Not yet cloud file. It is a file that accepted to be a cloud file, but currently is creating (uploading) to the cloud and this
 * operation not completed.<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LinkedCloudFile.java 00000 May 26, 2014 pnedonosko $
 */
public class AcceptedCloudFile implements CloudFile {

  /** The path. */
  private final String     path;

  // FYI transient fields will not appear in serialized forms like JSON object
  // on client side
  /** The created date. */
  final transient Calendar createdDate  = null;

  /** The modified date. */
  final transient Calendar modifiedDate = null;

  /**
   * Instantiates a new accepted cloud file.
   *
   * @param path the path
   */
  public AcceptedCloudFile(String path) {
    this.path = path;
  }

  /**
   * Checks if is symlink.
   *
   * @return true, if is symlink
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
   * Gets the last user.
   *
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
   * Gets the modified date.
   *
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

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isConnected() {
    return false;
  }

}
