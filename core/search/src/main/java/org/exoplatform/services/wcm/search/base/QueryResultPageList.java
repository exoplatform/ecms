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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 17, 2011  
 */
@SuppressWarnings("unchecked")
public class QueryResultPageList<E> extends AbstractPageList<E> {

  private static final Log LOG  = ExoLogger.getLogger(QueryResultPageList.class.getName());
  
  private static final String ORDER_BY = "ORDER BY";
  
  /** The query data */
  private QueryData queryData_;
  /** The buffer size */
  private int bufferSize_;
  
  /** The nodes. */
  protected List<E> buffer;
  
  private Map<E, Integer> dataSet;
    
  public QueryResultPageList(int pageSize, QueryData queryData, int total, int bufferSize,
                             NodeSearchFilter filter, SearchDataCreator creator) {
    super(pageSize);
    queryData_ = queryData.clone();
    offset_ = (int)queryData.getOffset();
    bufferSize_ = bufferSize;
    this.filter = filter;
    this.searchDataCreator = creator;
    this.setAvailablePage(total);
    removeRedundantPages(Math.min(bufferSize_ / pageSize, 5));
    dataSet = new HashMap<E, Integer>();
    loadedAllData_ = false;
  }
  
  public int getBufferSize() { return bufferSize_; }
  public void setBufferSize(int bufferSize) { bufferSize_ = bufferSize;  }
  
  public int getOffset() { return offset_; }
  public void setOffset(int offset) { offset_ = offset;  }
  
  public QueryData getQueryData() { return queryData_; }
  public void setQueryData(QueryData queryData) { this.queryData_ = queryData; }
  
  /**
   * Updates the page size.
   *
   * @param pageSize the new page size value
   */
  public void setPageSize(int pageSize)
  {
    super.setPageSize(pageSize);
    offset_ = 0;
  }

  @Override
  public List getAll() throws Exception {
    return null;
  }

  @Override
  protected void populateCurrentPage(int page) throws Exception {
    if (buffer == null || buffer.size() == 0) {
      queryDataForBuffer(page);
    }
    int firstBufferPage = offset_ / getPageSize() + 1;
    int lastBufferPage = (offset_ + buffer.size() - 1) / getPageSize() + 1;
    int bufferPage = bufferSize_ / getPageSize();
        
    int offsetPage = firstBufferPage;
    if (page < firstBufferPage || page > lastBufferPage || buffer.size() == 0) {
      if (page < firstBufferPage) {
        offsetPage = Math.max(1, page - (bufferPage / 3 * 2));
      } else if (page > lastBufferPage) {
        offsetPage = page;
      }
      
      offset_ = (offsetPage - 1) * getPageSize();
      queryDataForBuffer(page);
    }
    
    currentListPage_ = new ArrayList<E>();
    for (int i = getFrom(); i < getTo(); i++) {
      if (i - offset_ < buffer.size()) {
        E data = buffer.get(i - offset_);
        currentListPage_.add(data);
      }
    }
    if (currentListPage_.size() < getPageSize()) {
      loadedAllData_ = true;
    }
  }
  
  private void queryDataForBuffer(int queryPage) throws Exception {
    buffer = new ArrayList<E>();
    dataSet = new HashMap<E, Integer>();
    SessionProvider sessionProvider = queryData_.isSystemSession() ? WCMCoreUtils.getSystemSessionProvider() :
                                                                     WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(queryData_.getWorkSpace(), WCMCoreUtils.getRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryData_.getQueryStatement(), queryData_.getLanguage_());
    int offset = offset_;
    SiteSearchService siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);
    Map found = siteSearchService.getFoundNodes(ConversationState.getCurrent().getIdentity().getUserId(),
                                                                queryData_.getQueryStatement());
    Map<Integer, Integer> drop = siteSearchService.getDropNodes(ConversationState.getCurrent().getIdentity().getUserId(),
                                                                  queryData_.getQueryStatement());
    for (int i = 0; i < queryPage; i++) {
      if (drop.containsKey(i))
        offset += drop.get(i);
    }
    ((QueryImpl)query).setOffset(offset);  
    long prevSize = 0;
    int bufSize = bufferSize_;
    while (true) {
      int position = offset_;
      int page = position/getPageSize() + 1;
      int prevPage = -1;
      drop.put(page, 0);
      ((QueryImpl)query).setLimit(bufSize);      
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();
      RowIterator rowIter = queryResult.getRows();
      long size = iter.getSize();
      int count = 0;
      buffer.clear();
      dataSet.clear();
      
      while (iter.hasNext() && count < bufferSize_) {
        Node newNode = iter.nextNode();
        Row newRow = rowIter.nextRow();
        if (filter != null) {
          newNode = filter.filterNodeToDisplay(newNode);
        }
        if (newNode != null && searchDataCreator != null) {
          E data = searchDataCreator.createData(newNode, newRow, null);
          if (data != null && !dataSet.containsKey(data) && (found == null || !found.containsKey(data) || ((Integer)found.get(data)) >= page)) {
            buffer.add(data);
            dataSet.put(data, page);
            count ++;
            position++;
            page = (position-1)/getPageSize() + 1;
            if (page != prevPage) {
              prevPage = page;
              drop.put(page, 0);
            }
            // increase page number for the last item
            if (position % getPageSize() == 0) { page++; }
          } else { if (drop.containsKey(page)) drop.put(page, drop.get(page) + 1); }
        } else if (newNode == null) { if (drop.containsKey(page)) drop.put(page, drop.get(page) + 1); }
      }
      /* already query all data */
      if (size == prevSize) {
        loadedAllData_ = true;
        break;
      }
      /* enough data to process*/
      if (count == bufferSize_) break;
      bufSize = 2 * bufSize;
      prevSize = size;
    }
    for (Map.Entry<E, Integer> e : dataSet.entrySet())
      found.put(e.getKey(), e.getValue());
  }
  
  public void sortData() {
    if (sortByField != null) {
      String statement = queryData_.getQueryStatement().toUpperCase(); 
      int orderByIndex = statement.lastIndexOf(ORDER_BY);
      String[] orderStrings = orderByIndex >= 0 ? queryData_.getQueryStatement()
                                                            .substring(orderByIndex
                                                                + ORDER_BY.length())
                                                            .split(",") : new String[] {};
      
      StringBuffer newStatement = orderByIndex >= 0 ?
        new StringBuffer(queryData_.getQueryStatement().substring(0, orderByIndex + ORDER_BY.length())) : 
        new StringBuffer(queryData_.getQueryStatement());
      
      newStatement.append(" ").append(getSortByField(sortByField, queryData_.getLanguage_())).
                   append(" ").append(getOrderForQuery(order, queryData_.getLanguage_()));
      for(String orderString : orderStrings) {
        if (!orderString.toUpperCase().contains(sortByField.toUpperCase())) {
          newStatement.append(", ").append(orderString);
        }
      }
      queryData_.setQueryStatement(newStatement.toString());
      try {
        buffer.clear();
        populateCurrentPage(currentPage_);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
  }
  
  private String getSortByField(String sortField, String queryLanguage) {
    return (Query.SQL.equals(queryLanguage)? sortField : "@" + sortField) +
           ("jcr:score".equals(sortField.toLowerCase()) ? "()" : "");
  }
  
  private String getOrderForQuery(String order, String queryLanguage) {
    if (Query.SQL.equals(queryLanguage)) {
      return order.toUpperCase().startsWith("A") ? "ASC" : "DESC";  
    } else {
      return order.toUpperCase().startsWith("A") ? "ascending" : "descending";
    }
  }

  @Override
  public List<E> getPageWithOffsetCare(int page) throws Exception {
    return getPage(offset_/getPageSize() + page);
  }
  
  /**
   * Returns the to index.
   *
   * @return the to index
   */
  @Override
  public int getTo()
  {
     int to = currentPage_ * getPageSize();
     if (to > available_ + offset_)
        to = available_ + offset_;
     return to;
  }
  
  public boolean loadedAllData() {
    return loadedAllData_;
  }
  
}
