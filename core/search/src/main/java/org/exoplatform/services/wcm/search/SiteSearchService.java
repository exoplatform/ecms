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

import java.util.Map;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.search.base.AbstractPageList;

/**
 * Is used in the Search portlet that allows
 * users to find all information matching with your given keyword.
 * <p></p>
 * It is configured in the core/core-configuration/src/main/webapp/WEB-INF/conf/configuration.xml file as follows:<br>
 * {@code <import>war:/conf/wcm-core/core-search-configuration.xml</import>}<br>
 * <p></p>
 * The component configuration maps the SiteSearchService component with its own implementation: SiteSearchServiceImpl.
 * <p></p>
 * <pre>
 * &lt;component&gt;
 *     &lt;key&gt;org.exoplatform.services.wcm.search.SiteSearchService&lt;/key&gt;
 *     &lt;type&gt;org.exoplatform.services.wcm.search.SiteSearchServiceImpl&lt;/type&gt;
 *     &lt;component-plugins&gt;
 *         ...
 *     &lt;/component-plugins&gt;
 *     &lt;init-params&gt;
 *         &lt;value-param&gt;
 *             &lt;name&gt;isEnabledFuzzySearch&lt;/name&gt;
 *             &lt;value&gt;true&lt;/value&gt;
 *         &lt;/value-param&gt;
 *         &lt;value-param&gt;
 *             &lt;name&gt;fuzzySearchIndex&lt;/name&gt;
 *             &lt;value/&gt;
 *         &lt;/value-param&gt;
 *     &lt;/init-params&gt;
 * &lt;/component&gt;
 * </pre>
 *
 * @LevelAPI Experimental
 */
public interface SiteSearchService {

  public final static String PAGE_MODE_NONE = "none";
  public final static String PAGE_MODE_MORE = "more";
  public final static String PAGE_MODE_PAGINATION = "pagination";
  public final static String PATH_PORTAL_SITES = "/production/mop:workspace/mop:portalsites";
  
  /**
   * Adds the exclude/include data type plugin.
   *
   * @param plugin The plugin to be added.
   */
  public void addExcludeIncludeDataTypePlugin(ExcludeIncludeDataTypePlugin plugin);

  /**
   * Searches for content nodes of a site.
   *
   * This method finds all nodes of a site with nodetype that is document, nt:resource or nt:file, then checks their content.
   * If the node content has key word, it will be put into the list result.<br>
   * This function has 3 parameters, but the most important is {@link QueryCriteria}. With this parameter, you only set
   * 5 properties:<br>
   * <ul>
   * <li>SiteName: Name of the site which is searched. If the site name is "null", the search will be performed in all sites of system.</li>
   * <li>Keyword: The key word to search.</li>
   * <li>SearchDocument and SearchWebContent: The values of these two parameters must be set to "true" or "false".</li>
   * <li>SearchWebpage: Searches for content of nodes which are added to one or more pages.</li>
   * </ul>
   *
   * @param sessionProvider The session provider.
   * @param queryCriteria The query criteria.
   * @param pageSize The page size.
   * @param isSearchContent If "true", search is performed by content. If "false", search is performed by page.
   * @return The list of paginated content based on the query.
   * @throws Exception The exception
   */
  public AbstractPageList<ResultNode> searchSiteContents(SessionProvider sessionProvider,
                                                    QueryCriteria queryCriteria,
                                                    int pageSize,
                                                    boolean isSearchContent) throws Exception;

  /**
   * Searches for pages.
   * 
   * @param sessionProvider The session provider.
   * @param queryCriteria The query criteria.
   * @param pageSize The page size.
   * @return The list of pages.
   * @throws Exception The exception
   */
  public AbstractPageList<ResultNode> searchPageContents(SessionProvider sessionProvider,
                                                      QueryCriteria queryCriteria,
                                                      int pageSize,
                                                      boolean isSearchContent) throws Exception;
  
  /**
   * Gets map containing list of found nodes
   * @param userId user name
   * @param queryStatement the query statement
   * @return
   */
  public Map<?, Integer> getFoundNodes(String userId, String queryStatement);
  
  /**
   * Gets map containing list of dropped nodes
   * @param userId user name
   * @param queryStatement the query statement
   * @return
   */
  public Map<Integer, Integer> getDropNodes(String userId, String queryStatement);
  
  public void clearCache(String userId, String queryStatement);
  
}
