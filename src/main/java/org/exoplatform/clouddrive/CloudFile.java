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
 *  General abstraction for cloud file.
 *
 */
public interface CloudFile {
  
  String getId();
  
  String getTitle();
  
  String getLink();
  
  String getPreviewLink();
  
  String getEditLink();
  
  String getThumbnailLink();
  
  String getType();

  String getLastUser();

  String getAuthor();

  Calendar getCreatedDate();
  
  Calendar getModifiedDate();
  
  boolean isFolder();
  
  /**
   * Path to the cloud file in local storage.
   * 
   * @return {@link String}
   */
  String getPath();
  
  /**
   * Tell if this file currently synchronizing. 
   * 
   * @return <code>true</code> if file is synchronizing currently, <code>false</code> otherwise. 
   */
  boolean isSyncing();
}
