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
package org.exoplatform.services.wcm.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.exception.ExoMessageException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.jcr.impl.core.query.lucene.TwoWayRangeIterator;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 */
@SuppressWarnings({ "deprecation", "unchecked" })
public class PaginatedNodeIterator  extends PageList {

  /** The nodes. */
  protected List<Node> nodes;

  /**
   * Gets the nodes.
   * 
   * @return the nodes
   */
  public List<Node> getNodes() {
    return nodes;
  }

  /**
   * Sets the nodes.
   * 
   * @param nodes the nodes to set
   */
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  /**
   * Instantiates a new paginated node iterator.
   * 
   * @param pageSize the page size
   */
  public PaginatedNodeIterator(int pageSize) {
    super(pageSize);
  }

  /**
   * Instantiates a new paginated node iterator.
   * 
   * @param pageSize the page size
   * @param nodes the nodes
   */
  public PaginatedNodeIterator(List<Node> nodes, int pageSize) {
    super(pageSize);
    this.nodes = nodes;
    this.setAvailablePage(nodes.size());
    this.currentListPage_ = null;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.commons.utils.PageList#populateCurrentPage(int)
   */
  protected void populateCurrentPage(int page) throws Exception {
    if(page == currentPage_) {
      if(currentListPage_ != null)
        return;
    }
    checkAndSetPosition(page);
    currentListPage_ = new ArrayList();
    int count = 0;
    for(int i = ((page - 1)*this.getPageSize()); i < nodes.size(); i++) {
      currentListPage_.add(nodes.get(i));
      count++;
      if(count == getPageSize()) {
        break;
      }  
    }
    currentPage_ = page;
  }

  /**
   * Retrieve the total pages.
   * 
   * @return the total pages
   */
  public int getTotalPages() { return getAvailablePage(); }  

  /**
   * Retrieve the nodes per page.
   * 
   * @return the nodes per page
   */
  public int getNodesPerPage() { return getPageSize(); }    

  /**
   * Retrieve the total nodes.
   * 
   * @return the total nodes
   */
  public long getTotalNodes() { 
    return nodes.size();  
  }

  /**
   * Retrieve the nodes of current page.
   * 
   * @return the current page data
   * 
   * @throws Exception the exception
   */
  public List getCurrentPageData() throws Exception {
    return currentPage();
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.commons.utils.PageList#getPage(int)
   */
  public List getPage(int page) throws Exception {
    if (page < 1 || page > availablePage_) {
      Object[] args = { Integer.toString(page), Integer.toString(availablePage_) };
      throw new ExoMessageException("PageList.page-out-of-range", args);
    }
    populateCurrentPage(page);
    return currentListPage_;
  }

  /**
   * Change page.
   * 
   * @param page the page
   * 
   * @throws Exception the exception
   */
  public void changePage(int page) throws Exception {
    populateCurrentPage(page);
  }

  /**
   * Check and set current cursor position in iterator.
   * 
   * @param page the page
   */
  protected void checkAndSetPosition(int page) {
    try {
      //Iterate next
      if (page > currentPage_) {
        long skipNextNum = (page - (currentPage_ + 1)) * getPageSize();
        ((TwoWayRangeIterator)nodes).skip(skipNextNum);
      } else if(page <currentPage_) {
        //Iterate back
        int currentPageSize = currentListPage_.size();
        long skipBackNum = ((currentPage_ - page) * getPageSize()) + currentPageSize;      
        ((TwoWayRangeIterator)nodes).skipBack(skipBackNum);
      }
    }catch(Exception e) {}
  }

  /* (non-Javadoc)
   * @see org.exoplatform.commons.utils.PageList#getAll()
   */
  public List getAll() throws Exception {
    throw new UnsupportedOperationException();
  }
}
