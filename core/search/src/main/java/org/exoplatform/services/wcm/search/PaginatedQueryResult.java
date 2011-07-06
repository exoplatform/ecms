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
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.PaginatedNodeIterator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 * 
 * This class will be removed in the next release.
 */
@Deprecated
public class PaginatedQueryResult extends PaginatedNodeIterator {

  /** The query criteria. */
  protected QueryCriteria queryCriteria;

  /** The row iterator. */
  protected QueryResult queryResult;

  private boolean isSearchContent;

  /**
   * Instantiates a new paginated query result.
   *
   * @param pageSize the page size
   */

  public PaginatedQueryResult(int pageSize) {
    super(pageSize);
  }

  /**
   * Instantiates a new paginated query result.
   *
   * @param queryResult the query result
   * @param pageSize the page size
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("deprecation")
  public PaginatedQueryResult(QueryResult queryResult,int pageSize, boolean isSearchContent) throws Exception{
    super(pageSize);
    this.nodeIterator = queryResult.getNodes();
    this.isSearchContent = isSearchContent;
    this.setAvailablePage((int)nodeIterator.getSize());
    this.queryResult = queryResult;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.paginator.PaginatedNodeIterator#populateCurrentPage(int)
   */
  @SuppressWarnings({ "unchecked", "deprecation" })
  protected void populateCurrentPage(int page) throws Exception {
    if(page == currentPage_ && (currentListPage_ != null && !currentListPage_.isEmpty())) {
      return;
    }
    checkAndSetPosition(page);
    currentListPage_ = new CopyOnWriteArrayList<ResultNode>();
    int count = 0;
    RowIterator iterator = queryResult.getRows();
    while(nodeIterator.hasNext()) {
    Node node = nodeIterator.nextNode();
    Node viewNode = filterNodeToDisplay(node);

      if(viewNode != null) {
        //Skip back 1 position to get current row mapping to the node
        long position = nodeIterator.getPosition();
        long rowPosition = iterator.getPosition();
        long skipNum = position - rowPosition;
        iterator.skip(skipNum -1);
        Row row = iterator.nextRow();
        ResultNode resultNode = new ResultNode(viewNode,row);
        currentListPage_.add(resultNode);
        count ++;
        if(count == getPageSize())
          break;
      }
    }
    currentPage_ = page;
  }

  /**
   * Filter node to display.
   *
   * @param node the node
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  protected Node filterNodeToDisplay(Node node) throws Exception {
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
        if (parent.isNodeType("exo:webContent")) displayNode = parent;
    }
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
        String[] contentTypes = queryCriteria.getContentTypes();
        if(contentTypes != null && contentTypes.length>0) {
           String primaryNodeType = displayNode.getPrimaryNodeType().getName();
        if(!ArrayUtils.contains(contentTypes,primaryNodeType))
            return null;
      }
        return displayNode;
    }
}
