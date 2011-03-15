/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.queries.impl;

import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * mar 02, 2007
 */
public class QueryData{

  private String name ;
  private String language ;
  private String statement ;
  private List<String> permissions ;
  private boolean cachedResult ;

  public  QueryData(){}

  public String getName() { return this.name ; }
  public void setName(String name) { this.name = name ; }

  public String getLanguage() { return this.language ; }
  public void setLanguage(String l) { this.language = l ; }

  public List<String> getPermissions() { return this.permissions ; }
  public void setPermissions(List<String> permission) { this.permissions = permission ; }

  public String getStatement() { return this.statement ; }
  public void setStatement(String s) { this.statement = s ; }

  public boolean getCacheResult() { return this.cachedResult ; }
  public void setCacheResult(boolean r) { this.cachedResult = r ; }
}
