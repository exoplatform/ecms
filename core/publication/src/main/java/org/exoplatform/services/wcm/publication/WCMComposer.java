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
package org.exoplatform.services.wcm.publication;

import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodeLocation;

/**
 * This class is responsible of getting contents inside the WCM product.
 * We shouldn't access directly contents from the jcr on front side.
 *
 * In a general manner, this service stands between publication and cache.
 *
 * @author Benjamin Paillereau - benjamin.paillereau@exoplatform.com
 * @version 1.0
 */
public interface WCMComposer {

  /** Filter parameter to filter results by state. ex : draft, staged, published */
  public final static String FILTER_STATE = "filter-state";

  /** Filter parameter to filter results by primary type. ex: exo:article, exo:webContent */
  public final static String FILTER_PRIMARY_TYPE = "filter-primary-type";

  /** Filter parameter to order results. ex: exo:title, dc:title */
  public final static String FILTER_ORDER_BY = "filter-order-by";

  /** Filter parameter to order results in ascendant order or descendant order. values : ASC, DESC */
  public final static String FILTER_ORDER_TYPE = "filter-order-type";

  /** Filter parameter to filter results by target mode. ex: editing, approving, live */
  public final static String FILTER_MODE = "filter-mode";

  /** Filter parameter to search recursively or not. ex: recursive*/
  public final static String FILTER_RECURSIVE = "filter-recursive";

  /** Filter parameter to filter result by a decicated version. ex: base, 1, 2, 3, etc */
  public final static String FILTER_VERSION = "filter-version";

  /** Filter parameter to filter results by site. ex: classic */
  public final static String FILTER_SITE_NAME = "filter-site";

  /** Filter parameter to filter results by user. We will return only contents authored by this user. ex: */
  public final static String FILTER_REMOTE_USER = "filter-remote-user";

  /** Filter parameter to filter results by language. We will return only contents in this language. ex: fr, en, de */
  public final static String FILTER_LANGUAGE = "filter-language";

  /** Filter parameter to add a parameter in the executed query. ex: "AND exo:myproperty like 'cat1%'"*/
  public final static String FILTER_QUERY = "filter-query";

  /** Filter parameter to execute a specific query. ex: "SELECT * from nt:base"*/
  public final static String FILTER_QUERY_FULL = "filter-query-full";

  /** Filter parameter to limit the result size */
  public final static String FILTER_LIMIT = "filter-limit";

  /** Filter parameter to return the result with an offset delimiter */
  public final static String FILTER_OFFSET = "filter-offset";

  /** Total number of contents */
  public final static String FILTER_TOTAL = "filter-total-number";

  /** Filter parameter to filter results by visibility. ex: public, user*/
  public final static String FILTER_VISIBILITY = "filter-visibility";

  /** mode of portlet **/
  public final static String PORTLET_MODE = "portlet-mode";

  /** The Constant MODE_EDIT. */
  public final static String MODE_EDIT = "Edit";

  /** The Constant MODE_LIVE. */
  public final static String MODE_LIVE = "Live";

  /** The Constant IS_RECURSIVE. */
  public final static String IS_RECURSIVE = "rec";

  /** The Constant for base version. */
  public final static String BASE_VERSION = "base";
  
  /** The Constant VISIBILITY PUBLIC. */
  public final static String VISIBILITY_PUBLIC = "public";
  
  /** The Constant VISIBILITY USER. */
  public final static String VISIBILITY_USER = "user";

  /**
   * returns content at the specified path based on filters.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param nodeIdentifier the path
   * @param filters the filters
   * @param sessionProvider the session provider
   *
   * @return a jcr node
   *
   * @throws Exception the exception
   */
  @Deprecated
  public Node getContent(String repository,
                         String workspace,
                         String nodeIdentifier,
                         HashMap<String, String> filters,
                         SessionProvider sessionProvider) throws Exception;
  
  /**
   * returns content at the specified path based on filters.
   *
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   * @param sessionProvider the session provider
   *
   * @return a jcr node
   *
   * @throws Exception the exception
   */
  public Node getContent(String workspace,
                         String nodeIdentifier,
                         HashMap<String, String> filters,
                         SessionProvider sessionProvider) throws Exception;  

  /**
   * returns contents at the specified path based on filters.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   * @param sessionProvider the session provider
   *
   * @return a jcr node
   *
   * @throws Exception the exception
   */
  @Deprecated
  public List<Node> getContents(String repository,
                                String workspace,
                                String path,
                                HashMap<String, String> filters,
                                SessionProvider sessionProvider) throws Exception;
  
  /**
   * returns contents at the specified path based on filters.
   * 
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   * @param sessionProvider the session provider
   * @return a jcr node
   * @throws Exception the exception
   */
  public List<Node> getContents(String workspace,
                                String path,
                                HashMap<String, String> filters,
                                SessionProvider sessionProvider) throws Exception;
  
 /**
  * returns contents at the specified path based on filters.
  *
  * @param nodeLocation the content location
  * @param filters the filters
  * @param sessionProvider the session provider
  *
  * @return a jcr node
  *
  * @throws Exception the exception
  */
 public Result getPaginatedContents(NodeLocation nodeLocation, 
                                                             HashMap<String, String> filters, 
                                                             SessionProvider sessionProvider) throws Exception ;
  
  
  /**
   * Update content.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param nodeIdentifier the path 
   * @param filters the filters
   *
   * @return true, if successful
   */
  @Deprecated
  public boolean updateContent(String repository,
                               String workspace,
                               String nodeIdentifier,
                               HashMap<String, String> filters) throws Exception;
  
  /**
   * Update content.
   * 
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   * @return true, if successful
   */
  public boolean updateContent(String workspace,
                               String nodeIdentifier,
                               HashMap<String, String> filters) throws Exception;

  /**
   * Update contents.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   *
   * @return true, if successful
   */
  @Deprecated
  public boolean updateContents(String repository,
                                String workspace,
                                String path,
                                HashMap<String, String> filters) throws Exception;
  
  /**
   * Update contents.
   * 
   * @param workspace the workspace
   * @param path the path
   * @param filters the filters
   * @return true, if successful
   */
  public boolean updateContents(String workspace, String path, HashMap<String, String> filters) throws Exception;

  /**
   * returns allowed states for a specified mode.
   *
   * @param mode the mode
   *
   * @return a jcr node
   *
   * @throws Exception the exception
   */
  public List<String> getAllowedStates(String mode) throws Exception ;

  /**
   * initialize the templates hashmap
   *
   * @throws Exception the exception
   */
  public void cleanTemplates() throws Exception ;

  /**
   * Check isCache or not
   * @return 
   * @throws Exception the exception
   */
  public boolean isCached() throws Exception;
  /**
   * Update all document nodetypes and write a query statement   
   * @return a part of the query allow search all document node and taxonomy link also. Return null if there is any exception.
   * @throws Exception the exception
   */
  public String updateTemplatesSQLFilter() throws Exception; 
}
