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
 * The SiteSearchService component is used in the Search portlet that allows
 * users to find all information matching with your given keyword.
 * It is configured in the core/core-configuration/src/main/webapp/WEB-INF/conf/configuration.xml file as follows:
 *
 * {@code <import>war:/conf/wcm-core/core-search-configuration.xml</import>}
 *
 * The component configuration maps the SiteSearchService component with its own implementation: SiteSearchServiceImpl.
 * {@code
 *
 * <component>
 *     <key>org.exoplatform.services.wcm.search.SiteSearchService</key>
 *     <type>org.exoplatform.services.wcm.search.SiteSearchServiceImpl</type>
 *     <component-plugins>
 *         ...
 *     </component-plugins>
 *     <init-params>
 *         <value-param>
 *             <name>isEnabledFuzzySearch</name>
 *             <value>true</value>
 *         </value-param>
 *         <value-param>
 *             <name>fuzzySearchIndex</name>
 *             <value/>
 *         </value-param>
 *     </init-params>
 * </component>
 *}
 *
 * @LevelAPI Experimental
 */
public interface SiteSearchService {

  public final static String PAGE_MODE_NONE = "none";
  public final static String PAGE_MODE_MORE = "more";
  public final static String PAGE_MODE_PAGINATION = "pagination";  
  
  /**
   * Add the exclude/include data type plugin.
   *
   * @param plugin The plugin
   */
  public void addExcludeIncludeDataTypePlugin(ExcludeIncludeDataTypePlugin plugin);

  /**
   * Search site contents.
   *
   * Fill all child nodes of portal which have node type is document, nt:resource or nt:file and check content of them.
   * If node's content have key word, it will be put into the list result.
   * This function have 3 parameters, but the most important is {@link QueryCriteria}. With this parameter you only set
   * 5 property:
   * 1. SiteName: name of portal which is searched. If site name is null then search in all portal of system.
   * 2. Keyword: key work to search.
   * 3. SearchDocument and SearchWebContent: two parameter must have save value True or False
   * 4. SearchWebpage: search content of nodes which are added into one or more pages.
   *
   * @param sessionProvider The session provider
   * @param queryCriteria The query criteria
   * @param pageSize The page size
   * @param isSearchContent
   * @return The List of content  paginated based on the query.
   * @throws Exception The exception
   */
  public AbstractPageList<ResultNode> searchSiteContents(SessionProvider sessionProvider,
                                                    QueryCriteria queryCriteria,
                                                    int pageSize,
                                                    boolean isSearchContent) throws Exception;

  /**
   * Search site contents.
   *
   * Fill all child nodes of portal which have node type is document, nt:resource or nt:file and check content of them.
   * If node's content have key word, it will be put into the list result.
   * This function have 3 parameters, but the most important is {@link QueryCriteria}. With this parameter you only set
   * 5 property:
   * 1. SiteName: name of portal which is searched. If site name is null then search in all portal of system.
   * 2. Keyword: key work to search.
   * 3. SearchDocument and SearchWebContent: two parameter must have save value True or False
   * 4. SearchWebpage: search content of nodes which are added into one or more pages.
   *
   * @param sessionProvider The session provider
   * @param queryCriteria The query criteria
   * @param pageSize The page size
   * @param isSearchContent
   * @return The List of content  paginated based on the query.
   * @throws Exception The exception
   */
  public AbstractPageList<ResultNode> searchPageContents(SessionProvider sessionProvider,
                                                      QueryCriteria queryCriteria,
                                                      int pageSize,
                                                      boolean isSearchContent) throws Exception;
}
