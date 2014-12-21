/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.clouddrive;

import java.util.Calendar;

/**
 * General abstraction of a cloud file.
 * 
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
   * Link to a file on cloud provider. This link can be used for opening a file in new window or access it via
   * the provider API.
   * 
   * @return {@link String}
   */
  String getLink();

  /**
   * Preview link of a file if cloud provider supports such feature.
   * 
   * @return {@link String} a preview link or <code>null</code>
   */
  String getPreviewLink();

  /**
   * File editing link if cloud provider supports such feature.
   * 
   * @return {@link String} a editing link or <code>null</code>
   */
  String getEditLink();

  /**
   * File thumbnail link if cloud provider supports such feature.
   * 
   * @return {@link String} a thumbnail link or <code>null</code>
   * 
   * @return
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
   * @return
   */
  boolean isFolder();

  /**
   * Path to the cloud file in local storage.
   * 
   * @return {@link String}
   */
  String getPath();

}
