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

import org.exoplatform.commons.exception.ExoMessageException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 */
@SuppressWarnings({ "deprecation", "unchecked" })
public class PaginatedResultIterator extends PageList {

  /** The nodes. */
  protected Result result;

  /**
   * Instantiates a new paginated node iterator.
   *
   * @param pageSize the page size
   */
  public PaginatedResultIterator(int pageSize) {
    super(pageSize);
  }

  public PaginatedResultIterator(Result result, int pageSize) {
     super(pageSize);
     this.result = result;
     this.setAvailablePage((int)result.getNumTotal());
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
    currentListPage_ = new ArrayList();

    /** TODO : TO UPDATE
     *
     */
    WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);
    result.getFiltersDescriber().put(WCMComposer.FILTER_LIMIT, ""+this.getPageSize());
    result.getFiltersDescriber().put(WCMComposer.FILTER_OFFSET, ""+(this.getPageSize()*(page-1)));
    result.getFiltersDescriber().put(WCMComposer.FILTER_TOTAL, ""+this.result.getNumTotal());
    result = composer.getPaginatedContents(result.getNodeLocationDescriber(),
                                           result.getFiltersDescriber(),
                                           WCMCoreUtils.getUserSessionProvider());

    currentListPage_ = result.getNodes();

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
    return result.getNumTotal();
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

  /* (non-Javadoc)
   * @see org.exoplatform.commons.utils.PageList#getAll()
   */
  public List getAll() throws Exception {
    throw new UnsupportedOperationException();
  }
}
