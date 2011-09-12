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
package org.exoplatform.services.wcm.search;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.search.base.AbstractPageList;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Oct 7, 2008
 */
public interface SiteSearchService {

  public final static String PAGE_MODE_NONE = "none";
  public final static String PAGE_MODE_MORE = "more";
  public final static String PAGE_MODE_PAGINATION = "pagination";  
  
  /**
   * Adds the exclude include data type plugin.
   *
   * @param plugin the plugin
   */
  public void addExcludeIncludeDataTypePlugin(ExcludeIncludeDataTypePlugin plugin);

  /**
   * Search site contents.<br/>
   * Fill all child nodes of portal which have node type is document, nt:resource or nt:file and check content of them.
   * If node'content have key word, it will be put into list result.<br/>
   * This function have 3 parameters, but the most important is <b>QueryCriteria</b>. With this parameter you only set
   * 5 property: <br/>
   * 1. SiteName: name of portal which is searched. If site name is null then search in all portal of system.<br/>
   * 2. Keyword: key work to search.<br/>
   * 3. SearchDocument and SearchWebContent: two parameter must have save value <code>True</code> or <code>False</code><br/>
   * 4. SearchWebpage: search content of nodes which are added into one or more pages.
   *
   * @param queryCriteria the query criteria
   * @param sessionProvider the session provider
   * @param pageSize the page size
   *
   * @return the wCM paginated query result
   *
   * @throws Exception the exception
   */
  public AbstractPageList<ResultNode> searchSiteContents(SessionProvider sessionProvider,
                                                    QueryCriteria queryCriteria,
                                                    int pageSize,
                                                    boolean isSearchContent) throws Exception;
  
  /**
   * 
   * @param sessionProvider
   * @param queryCriteria
   * @param pageSize
   * @param isSearchContent
   * @return
   * @throws Exception
   */
  public AbstractPageList<ResultNode> searchPageContents(SessionProvider sessionProvider,
                                                      QueryCriteria queryCriteria,
                                                      int pageSize,
                                                      boolean isSearchContent) throws Exception;
}
