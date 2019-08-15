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
package org.exoplatform.clouddrive;

import java.util.Calendar;

/**
 * General abstraction of a cloud file.
 */
public interface CloudFile {

  /**
   * File ID as in cloud provider API.
   * 
   * @return {@link String}
   */
  String getId();

  /**
   * File title (can be also its name) as in cloud provider API.
   * 
   * @return {@link String}
   */
  String getTitle();

  /**
   * Link to a file on cloud provider. This link can be used for opening a file in new window or access it via the provider API.
   * 
   * @return {@link String}
   */
  String getLink();

  /**
   * Link for editing a file on cloud provider. This link can be used for opening a file in new window or embedding.
   * 
   * @return {@link String} remote editor link or <code>null</code> if edit not supported
   */
  String getEditLink();

  /**
   * Preview link of a file if cloud provider supports such feature.
   * 
   * @return {@link String} a preview link or <code>null</code> if preview not offered
   */
  String getPreviewLink();

  /**
   * File thumbnail link if cloud provider supports such feature.
   *
   * @return {@link String} a thumbnail link or <code>null</code> if thumbnail support not offered
   */
  String getThumbnailLink();

  /**
   * File type as in cloud provider API.
   * 
   * @return {@link String}
   */
  String getType();

  /**
   * Optional representation (UI) mode associated with the file type. Can be <code>null</code>.
   * 
   * @return {@link String} a type mode or <code>null</code> if not available.
   */
  String getTypeMode();

  /**
   * Last user changed the file as in cloud provider API.
   * 
   * @return {@link String}
   */
  String getLastUser();

  /**
   * File author as in cloud provider API.
   * 
   * @return {@link String}
   */
  String getAuthor();

  /**
   * File creation date as in cloud provider API.
   * 
   * @return {@link Calendar}
   */
  Calendar getCreatedDate();

  /**
   * File modification date as in cloud provider API.
   * 
   * @return {@link Calendar}
   */
  Calendar getModifiedDate();

  /**
   * Path to the cloud file in local storage.
   * 
   * @return {@link String}
   */
  String getPath();

  /**
   * File size in bytes. It is an actual file size from cloud side. If size not available or it is a folder then size is -1.
   * 
   * @return {@link Long} file size in bytes (-1 for folder or when cannot be determined)
   */
  long getSize();

  /**
   * Return <code>true</code> if this cloud file represent a folder object.
   * 
   * @return <code>true</code> if it is a folder, <code>false</code> otherwise
   */
  boolean isFolder();

  /**
   * Tells is this file a connected one (i.e. a part of connected cloud drive).
   *
   * @return true, if is connected, false otherwise
   */
  boolean isConnected();
  
}
