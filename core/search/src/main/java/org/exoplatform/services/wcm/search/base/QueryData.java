/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.base;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
public class QueryData {
  
  private String queryStatement_;
  private String workSpace_;
  private String language_;
  private boolean isSystemSession_;
  
  public QueryData(String queryStatement, String workspace, String language, boolean isSystemSession) {
    queryStatement_ = queryStatement;
    workSpace_ = workspace;
    language_ = language;
    isSystemSession_ = isSystemSession;
  }
  
  public String getQueryStatement() {
    return queryStatement_;
  }
  public void setQueryStatement(String queryStatement) {
    queryStatement_ = queryStatement;
  }
  public String getWorkSpace() {
    return workSpace_;
  }
  public void setWorkSpace(String workSpace) {
    workSpace_ = workSpace;
  }
  public boolean isSystemSession() {
    return isSystemSession_;
  }
  public void setSystemSession(boolean isSystemSession) {
    isSystemSession_ = isSystemSession;
  }
  public String getLanguage_() {
    return language_;
  }
  public void setLanguage_(String language) {
    language_ = language;
  }
  
  public QueryData clone() {
    return new QueryData(queryStatement_, workSpace_, language_, isSystemSession_);
  }
  
}
