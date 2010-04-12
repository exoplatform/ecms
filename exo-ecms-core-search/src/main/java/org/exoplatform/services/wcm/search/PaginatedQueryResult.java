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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.PaginatedNodeIterator;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 */
public class PaginatedQueryResult extends PaginatedNodeIterator {
  
  /** The query criteria. */
  protected QueryCriteria queryCriteria; 

  /** The row iterator. */  
  protected QueryResult queryResult;
  
  /** The iterator. */
  private NodeIterator iterator;
  
  /**
   * Gets the iterator.
   * 
   * @return the iterator
   */
  public NodeIterator getIterator() {
    return iterator;
  }

  /**
   * Sets the iterator.
   * 
   * @param iterator the iterator to set
   */
  public void setIterator(NodeIterator iterator) {
    this.iterator = iterator;
  }

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
  public PaginatedQueryResult(QueryResult queryResult,int pageSize) throws Exception{
    super(pageSize);         
    NodeIterator nodeIterator = queryResult.getNodes();
    this.setIterator(nodeIterator);
    Node node = null;
    nodes = new ArrayList<Node>();
    while(nodeIterator.hasNext()) {
      node = nodeIterator.nextNode();
      nodes.add(node);
    }
    this.setAvailablePage(nodes.size());
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
    NodeIterator nodeIterator = this.getIterator();
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
    Node displayNode = null;
    if(node.isNodeType("nt:resource")) displayNode = node.getParent();
    PublicationService publicationService = (PublicationService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PublicationService.class);
    String lifecycleName = publicationService.getNodeLifecycleName(node);
    if (lifecycleName == null) return node;
    PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins().get(lifecycleName);
    HashMap<String, Object> context = new HashMap<String, Object>();
    context.put(WCMComposer.FILTER_MODE, queryCriteria.isLiveMode() ? WCMComposer.MODE_LIVE : WCMComposer.MODE_EDIT);
    displayNode = publicationPlugin.getNodeView(node, context);
    return displayNode;
  }
}
