/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.base;

import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.exception.ExoMessageException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
@SuppressWarnings("deprecation")
public abstract class AbstractPageList<E> extends PageList<E> {
  
  public static final int DEFAULT_PAGE_SIZE = 10;
  
  public static final int DEAFAULT_BUFFER_SIZE = 100;
  
  public static final int RESULT_SIZE_SEPARATOR = 100;
  
  private static final Log LOG  = ExoLogger.getLogger(AbstractPageList.class.getName());

  /** The spell suggestion. */
  protected String spellSuggestion;
  /** The query time. */
  protected long queryTime;
  /** The node filter */
  protected NodeSearchFilter filter;
  /** The data creator */
  protected SearchDataCreator<E> searchDataCreator;
  /** The comparator for searching */
  protected Comparator<E> comparator;
  /** Sort by */
  protected String sortByField;
  /** oder to sort */
  protected String order;
  /** The offset */
  protected int offset_;
  
  protected boolean loadedAllData_ = true;
  
  public Comparator<E> getComparator() {
    return comparator;
  }

  public void setComparator(Comparator<E> comparator) {
    this.comparator = comparator;
  }

  public String getSortByField() {
    return sortByField;
  }

  public void setSortByField(String sortByField) {
    this.sortByField = sortByField;
  }

  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
  }

  /** The constructor */
  public AbstractPageList(int pageSize) {
    super(pageSize);
  }
  
  /** The constructor */
  public AbstractPageList(int pageSize, NodeSearchFilter filter, SearchDataCreator<E> creator) {
    this(pageSize);
    this.filter = filter;
    this.searchDataCreator = creator;
  }
  
  public String getSpellSuggestion() {
    return spellSuggestion;
  }

  public void setSpellSuggestion(String spellSuggestion) {
    this.spellSuggestion = spellSuggestion;
  }
  
  public long getQueryTime() {
    return queryTime;
  }

  public void setQueryTime(long queryTime) {
    this.queryTime = queryTime;
  }

  public NodeSearchFilter getFilter() {
    return filter;
  }

  public void setFilter(NodeSearchFilter filter) {
    this.filter = filter;
  }

  public SearchDataCreator<E> getSearchDataCreator() {
    return searchDataCreator;
  }
  
  public void setSearchDataCreator(SearchDataCreator<E> searchDataCreator) {
    this.searchDataCreator = searchDataCreator;
  }
  
  @SuppressWarnings("unchecked")
  public List currentPage() throws Exception
  {
    populateCurrentPage(currentPage_);
    return currentListPage_;
  }

  @Override
  protected void checkAndSetPage(int page) throws Exception
  {
     currentPage_ = page;
     if (page < 1 || page > availablePage_ + offset_/getPageSize())
     {
        Object[] args = {Integer.toString(page), Integer.toString(availablePage_)};
        throw new ExoMessageException("PageList.page-out-of-range", args);
     }
  }
  
  
  public abstract void sortData();
  
  public abstract List<E> getPageWithOffsetCare(int page) throws Exception;
  
  protected void removeRedundantPages(int availablePage) {
    for (int i = 1; i < Math.min(availablePage, this.getAvailablePage()); i++) {
      try {
        int currentPageSize = this.getPage(i).size();
        if (currentPageSize == 0) {
          this.setAvailablePage((i-1)*this.getPageSize());
          break;
        }
        if (currentPageSize < this.getPageSize()) {
          this.setAvailablePage(i*this.getPageSize());
          break;
        }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Error in getPage.", e);
        }
      }
    }
    try {
      getPageWithOffsetCare(1);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error in getPageWithOffsetCare.", e);
      }
    }
  }
  
  public boolean loadedAllData() {
    return loadedAllData_;
  }
  
//  @SuppressWarnings("unchecked")
//  protected List<NodeLocation> filterNodes(List<Node> nodes) {
//    if (filter == null) {
//      return NodeLocation.getLocationsByNodeList(nodes);
//    }
//    List<NodeLocation> ret = new ArrayList<NodeLocation>(); 
//    for (Node node : nodes) {
//      Node filteredNode = filter.filterNodeToDisplay(node);
//      if (filteredNode != null) ret.add(NodeLocation.getNodeLocationByNode(filteredNode));
//    }
//    return ret;
//  }
//
//  protected List<NodeLocation> filterNodes(NodeIterator iter) {
//    if (filter == null) {
//      return NodeLocation.getLocationsByIterator(iter);
//    }
//    List<NodeLocation> ret = new ArrayList<NodeLocation>(); 
//    while (iter.hasNext()) {
//      Node filteredNode = filter.filterNodeToDisplay(iter.nextNode());
//      if (filteredNode != null) ret.add(NodeLocation.getNodeLocationByNode(filteredNode));
//    }
//    return ret;
//  }

}
