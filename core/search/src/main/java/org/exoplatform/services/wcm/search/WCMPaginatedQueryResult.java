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

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.phamvu@exoplatform.com
 * Oct 21, 2008
 * This class will be removed in the next release
 */
@Deprecated
public class WCMPaginatedQueryResult extends PaginatedQueryResult {

  /** The query time. */
  private long queryTime;

  /** The spell suggestion. */
  private String spellSuggestion;

  /**
   * Instantiates a new wCM paginated query result.
   *
   * @param pageSize the page size
   */
  public WCMPaginatedQueryResult(int pageSize) {
    super(pageSize);
  }

  /**
   * Instantiates a new wCM paginated query result.
   *
   * @param queryResult the query result
   * @param pageSize the page size
   * @param queryCriteria the query criteria
   *
   * @throws Exception the exception
   */
  public WCMPaginatedQueryResult(QueryResult queryResult,
                                 QueryCriteria queryCriteria,
                                 int pageSize,
                                 boolean isSearchContent) throws Exception {
    super(queryResult, pageSize, isSearchContent);
    this.queryCriteria = queryCriteria;
  }

  /**
   * Sets the query time.
   *
   * @param time the new query time
   */
  public void setQueryTime(long time) {
    this.queryTime = time;
  }

  /**
   * Gets the query time in second.
   *
   * @return the query time in second
   */
  public float getQueryTimeInSecond() {
    return (float) this.queryTime / 1000;
  }

  /**
   * Gets the query criteria.
   *
   * @return the query criteria
   */
  public QueryCriteria getQueryCriteria() {
    return this.queryCriteria;
  }

  /**
   * Sets the query criteria.
   *
   * @param queryCriteria the new query criteria
   */
  public void setQueryCriteria(QueryCriteria queryCriteria) {
    this.queryCriteria = queryCriteria;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.exoplatform.wcm.webui.paginator.PaginatedQueryResult#filterNodeToDisplay
   * (javax.jcr.Node)
   */
  protected Node filterNodeToDisplay(Node node, boolean isSearchContent) throws Exception {
    Node displayNode = getNodeToCheckState(node);
    if(displayNode == null) return null;
    if (isSearchContent) return displayNode;
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(displayNode);
    WCMComposer wcmComposer = WCMCoreUtils.getService(WCMComposer.class);
    HashMap<String, String> filters = new HashMap<String, String>();
    filters.put(WCMComposer.FILTER_MODE, WCMComposer.MODE_LIVE);
    return wcmComposer.getContent(nodeLocation.getWorkspace(),
                                  nodeLocation.getPath(),
                                  filters,
                                  WCMCoreUtils.getSystemSessionProvider());
  }

  protected Node getNodeToCheckState(Node node)throws Exception{
    Node displayNode = node;
    if (node.getPath().contains("web contents/site artifacts")) {
      return null;
    }
    if (displayNode.isNodeType("nt:resource")) {
      displayNode = node.getParent();
    }
    if (displayNode.isNodeType("exo:htmlFile")) {
      Node parent = displayNode.getParent();
      if (queryCriteria.isSearchWebContent()) {
        if (parent.isNodeType("exo:webContent")) return parent;
        return null;
      }
      if (parent.isNodeType("exo:webContent")) return null;
      return displayNode;
    }
    /*
    if(queryCriteria.isSearchWebContent()) {
      if(!queryCriteria.isSearchDocument()) {
        if(!displayNode.isNodeType("exo:webContent"))
          return null;
      }
      if(queryCriteria.isSearchWebpage()) {
        if (!displayNode.isNodeType("publication:webpagesPublication"))
          return null;
      }
    } else if(queryCriteria.isSearchWebpage()) {
        if (queryCriteria.isSearchDocument()) {
          return displayNode;
        } else if (!displayNode.isNodeType("publication:webpagesPublication"))
          return null;
    }
    */
    String[] contentTypes = queryCriteria.getContentTypes();
    if(contentTypes != null && contentTypes.length>0) {
      String primaryNodeType = displayNode.getPrimaryNodeType().getName();
      if(!ArrayUtils.contains(contentTypes,primaryNodeType))
        return null;
    }
    return displayNode;
  }

  /**
   * Gets the spell suggestion.
   *
   * @return the spell suggestion
   */
  public String getSpellSuggestion() {
    return spellSuggestion;
  }

  /**
   * Sets the spell suggestion.
   *
   * @param spellSuggestion the new spell suggestion
   */
  public void setSpellSuggestion(String spellSuggestion) {
    this.spellSuggestion = spellSuggestion;
  }
}
