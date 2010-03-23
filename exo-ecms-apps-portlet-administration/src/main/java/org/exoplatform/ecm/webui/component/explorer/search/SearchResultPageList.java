/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.List;

import javax.jcr.query.QueryResult;

import org.exoplatform.ecm.jcr.model.ExtensiblePageList;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 11, 2008  
 */
public class SearchResultPageList extends ExtensiblePageList {
  
  private QueryResult queryResult_ ;
  private List listNodes_  ;
  private boolean isEndOfIterator_ = false ;
  
  public SearchResultPageList(QueryResult queryResult, List listNodes, int pageSize, boolean isEndOfIterator) {
    super(pageSize) ;
    isEndOfIterator_ = isEndOfIterator ;
    queryResult_ = queryResult ;
    listNodes_ = listNodes ;
    try {
      if(isEndOfIterator_) setAvailablePage(listNodes_.size()) ;
      else setAvailablePage(Integer.parseInt(Long.toString(queryResult.getNodes().getSize()))) ;
    } catch(Exception e) {
      setAvailablePage(0) ;
    }
  }
  
  @SuppressWarnings("unused")
  protected void populateCurrentPage(int page) throws Exception  {
    currentListPage_ = listNodes_.subList(getFrom(), getTo()) ;
  }
  
  public List getAll() throws Exception  { return listNodes_ ; }

  @Override
  public long getPageNumberEstimate() {
    try {
      return queryResult_.getNodes().getSize() ;
    } catch(Exception e) {
      return 0 ;
    }
  }

  @Override
  public int getRealNumberNodes() {
    if(isEndOfIterator_) return listNodes_.size() ; 
    return 0;
  }

  @Override
  public boolean isEndOfIterator() {
    return isEndOfIterator_ ;
  }
}
