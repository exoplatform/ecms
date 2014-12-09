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
package org.exoplatform.services.cms.documents.impl;

import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 20, 2009
 * 9:05:49 AM
 */
public class DocumentType {

  private List<String> mimeTypes;
  private String contentsType;
  private String displayInFilter;

  private String resourceBundleKey;
  private String iconClass;

  public DocumentType(){}

  public DocumentType(List<String> mimeTypes, String resourceBundleKey, String iconClass) {
    this.mimeTypes = mimeTypes;
    this.resourceBundleKey = resourceBundleKey;
    this.iconClass = iconClass;
  }

  public String getResourceBundleKey() {
    return resourceBundleKey;
  }

  public String getIconClass() {
    return iconClass;
  }

  public void setMimeTypes(List<String> mimeTypes) { this.mimeTypes = mimeTypes; }

  public List<String> getMimeTypes() { return this.mimeTypes; }

  public void setContentsType(String contentTypes) { this.contentsType = contentTypes; }

  public String getContentsType() { return this.contentsType; }
  
  public String getDisplayInFilter() { return displayInFilter; }
  
  public void setDisplayInFilter(String displayInFilter) { this.displayInFilter = displayInFilter; }
}
