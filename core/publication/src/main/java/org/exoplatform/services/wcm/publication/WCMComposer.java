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
 * Gets content inside WCM.
 * In general, this service stands between publication and cache,
 * so you should not access content directly from the JCR on the front side.
 *
 * @LevelAPI Experimental
 */
public interface WCMComposer {

  /** Filter parameter to filter results by state. For example: draft, staged, published. */
  public final static String FILTER_STATE = "filter-state";

  /** Filter parameter to filter results by primary type. For example: exo:webContent. */
  public final static String FILTER_PRIMARY_TYPE = "filter-primary-type";

  /** Filter parameter to order results. For example: exo:title, dc:title. */
  public final static String FILTER_ORDER_BY = "filter-order-by";

  /** Filter parameter to choice translation results. */
  public final static String FILTER_TRANSLATION = "filter-translation";

  /** Filter parameter to order results in ascending or descending order. Its values are: ASC and DESC. */
  public final static String FILTER_ORDER_TYPE = "filter-order-type";

  /** Filter parameter to filter results by target mode. For example: editing, approving, live. */
  public final static String FILTER_MODE = "filter-mode";

  /** Filter parameter to search recursively or not. For example: recursive.*/
  public final static String FILTER_RECURSIVE = "filter-recursive";

  /** Filter parameter to filter results by a dedicated version. For example: base, 1, 2, 3, and more.*/
  public final static String FILTER_VERSION = "filter-version";

  /** Filter parameter to filter results by site. For example: ACME. */
  public final static String FILTER_SITE_NAME = "filter-site";

  /** Filter parameter to filter results by user. This means only content authored by this user is returned. */
  public final static String FILTER_REMOTE_USER = "filter-remote-user";

  /** Filter parameter to filter results by language. This means only content in this language is returned. For example: fr, en, de. */
  public final static String FILTER_LANGUAGE = "filter-language";

  /** Filter parameter to add a parameter to the executed query. For example: "AND exo:myproperty like 'cat1%'".*/
  public final static String FILTER_QUERY = "filter-query";

  /** Filter parameter to execute a specific query. For example: "SELECT * from nt:base".*/
  public final static String FILTER_QUERY_FULL = "filter-query-full";

  /** Filter parameter to limit the result size. */
  public final static String FILTER_LIMIT = "filter-limit";

  /** Filter parameter to return results with an offset delimiter. */
  public final static String FILTER_OFFSET = "filter-offset";

  /** Total number of content. */
  public final static String FILTER_TOTAL = "filter-total-number";

  /** Filter parameter to filter results by visibility. For example: public, user. */
  public final static String FILTER_VISIBILITY = "filter-visibility";

  /** Mode of portlet. **/
  public final static String PORTLET_MODE = "portlet-mode";

  /** The constant MODE_EDIT. */
  public final static String MODE_EDIT = "Edit";

  /** The constant MODE_LIVE. */
  public final static String MODE_LIVE = "Live";

  /** The constant IS_RECURSIVE. */
  public final static String IS_RECURSIVE = "rec";

  /** The constant for base version. */
  public final static String BASE_VERSION = "base";

  /** The constant VISIBILITY PUBLIC. */
  public final static String VISIBILITY_PUBLIC = "public";

  /** The constant VISIBILITY USER. */
  public final static String VISIBILITY_USER = "user";

  /**
   * Gets a content node at a specified path based on given filters.
   *
   * @param workspace The workspace that includes the content node.
   * @param nodeIdentifier Identifier of the content node.
   * @param filters The given filters.
   * @param sessionProvider The session provider.
   * @return The content node.
   * @throws Exception The exception
   */
  public Node getContent(String workspace,
                         String nodeIdentifier,
                         HashMap<String, String> filters,
                         SessionProvider sessionProvider) throws Exception;

  /**
   * Gets content nodes at a specified path based on given filters.
   * 
   * @param workspace The workspace that includes the content nodes.
   * @param path The path.
   * @param filters The given filters.
   * @param sessionProvider The session provider.
   * @return The list of content nodes.
   * @throws Exception The exception
   */
  public List<Node> getContents(String workspace,
                                String path,
                                HashMap<String, String> filters,
                                SessionProvider sessionProvider) throws Exception;

  /**
   * Gets content nodes that are paginated at a specified path based on given filters.
   *
   * @param nodeLocation Location of the content nodes.
   * @param filters The given filters.
   * @param sessionProvider The session provider.
   * @return The list of content nodes.
   * @throws Exception The exception
   */
 public Result getPaginatedContents(NodeLocation nodeLocation,
                                                             HashMap<String, String> filters,
                                                             SessionProvider sessionProvider) throws Exception ;
															 
  /**
   * Gets the allowed states for a given mode.
   *
   * @param mode The given mode.
   * @return The list of states.
   * @throws Exception The exception
   */
  public List<String> getAllowedStates(String mode) throws Exception ;

  /**
   * Resets the template filters.
   *
   * @throws Exception the exception
   */
  public void cleanTemplates() throws Exception ;

  /**
   * Updates the SQL filter of templates.
   *
   * @return A part of the query that allows to search all document nodes and taxonomy links. It returns "null" if there is any exception.
   * @throws Exception the exception
   */
  public String updateTemplatesSQLFilter() throws Exception;
}
