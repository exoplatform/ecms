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
package org.exoplatform.services.wcm.search.base;

import org.exoplatform.commons.api.search.data.SearchResult;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 8, 2013  
 */
public class EcmsSearchResult extends SearchResult {

  private String fileType_;
  private String nodePath;
  
  public EcmsSearchResult(String url,
                          String urlOnImage,
                          String title,
                          String excerpt,
                          String detail,
                          String imageUrl,
                          long date,
                          long relevancy,
                          String fileType,
                          String nodePath) {
    super(url, urlOnImage, title, excerpt, detail, imageUrl, date, relevancy);
    this.fileType_ = fileType;
    this.nodePath = nodePath;
  }
  
  public String getFileType() {
    return this.fileType_;
  }
  
  public void setFileType(String fileType) {
    this.fileType_ = fileType;
  }

  public String getNodePath() {
    return nodePath;
  }
    
  public void setNodePath(String nodePath) {
    this.nodePath = nodePath;
  }
  
}
