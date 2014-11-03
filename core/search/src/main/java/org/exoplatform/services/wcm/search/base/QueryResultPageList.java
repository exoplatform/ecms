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

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  /** The offset */
  private int offset_;

  /** The user's offset */
  private int realOffset_;
  
  /** The nodes. */
  protected List<E> buffer;
  
  private Set<E> dataSet;
  
  public QueryResultPageList(int pageSize, QueryData queryData, int total, int bufferSize,
                             NodeSearchFilter filter, SearchDataCreator creator) {
    super(pageSize);
    setTotalNodes(total);
    queryData_ = queryData.clone();
    offset_ = 0;
    bufferSize_ = bufferSize;
    this.filter = filter;
    this.searchDataCreator = creator;
    this.setAvailablePage(total);
    removeRedundantPages(Math.min(bufferSize_ / pageSize, 5));
    dataSet = new HashSet<E>();
  }

  public QueryResultPageList(int pageSize, QueryData queryData, int total, int bufferSize,
                             NodeSearchFilter filter, SearchDataCreator creator, int offset) {
    super(pageSize);
    setTotalNodes(total);
    queryData_ = queryData.clone();
    offset_ = 0;
    realOffset_= offset;
    bufferSize_ = bufferSize;
    this.filter = filter;
    this.searchDataCreator = creator;
    this.setAvailablePage(total);
    removeRedundantPages(Math.min(bufferSize_ / pageSize, 5));
    dataSet = new HashSet<E>();
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
      queryDataForBuffer();
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
      queryDataForBuffer();
    }
    
    currentListPage_ = new ArrayList<E>();
    for (int i = getFrom(); i < getTo(); i++) {
      if (i - offset_ < buffer.size()) {
        E data = buffer.get(i - offset_);
        currentListPage_.add(data);
      }
    }
  }
  
  private void queryDataForBuffer() throws Exception {

      /**
       * ECMS-6444 :
       * this make pagination not correct, the first page of results is always displayed when user asks for more results.
       * In fact the first solution is to said : use the offset sent by unifiedSearch portlet and make the query from this offset.
       * BUT : in this function, we can do newNode = filter.filterNodeToDisplay(newNode);
       * This instruction can removes a node if he not match a criteria of the current search.
       * For example : when searching Files (aka nt:file), nt:file can be founded by query
       * because, we found default.html which is nt:file
       * filterNodeToDisplay(newNode) will transform this node to parent webcontent, and remove it form the list
       * because it is not nt:file.
       *
       * So if just use offset, this leads to potential problems :
       * for example :
       * we search for Files (nt:file)
       * if query(offset=0, limit=20) returns results R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 R13 R14 R15 R16 R17 R18 R19 R20
       * and R2 is a webContent, removed by filterNodeToDisplay
       * So first page sent is
       * R1 R3 R4 R5 R6 (assuming pageSize is 5)
       *
       * Now, when querying second page, the query will be done with offset = pageSize (5) and limit=20
       * And list of results will be
       * R6 R7 R8 R9 R10 R11 R12 R13 R14 R15 R16 R17 R18 R19 R20
       * So first page sent is
       * R6 R7 R8 R9 R10 (assuming pageSize is 5)
       * R6 result is displayed twice.
       *
       * So what to do this fix this ?
       * The idea is to redo the query from offset zero, and use the real offset of the user to track results removed.
       * With last example :
       * at first call, realOffset is 0
       * dataBuffer will be filled with result
       * R1 R3 R4 R5 R6 (assuming pageSize is 5)
       *
       * When user choose "More results"
       * realOffset is 5
       * query(offset=0, limit =20) returns
       * R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 R13 R14 R15 R16 R17 R18 R19 R20
       * we loop on theses results :
       * R1 index (1) is before realOffset (5) => so ignore it
       * R2 index (2) is ignored => increase realOffset => realOffset=6
       * R3 index (3) is before realOffset (6) => so ignore it
       * R4 index (4) is before realOffset (6) => so ignore it
       * R5 index (5) is before realOffset (6) => so ignore it
       * R6 index (6) is before realOffset (6) => so ignore it
       * R7 index (7) is greater than realoffset => start to fill the buffer
       * databuffer will be filled with
       * R7 R8 R9 R10 R11
       *
       * This time, results are not displayed twice.
       *
       * Now these approach leads to a potential performances problem
       * even if we want page i, we redo query for all page < i.
       * This is mandatory because else we cannot know which node is removed by filterNodeToDisplay.
       *
       * To prevent this, we have to find a solution to create a query that returns exactly nodes we want to display.
       *
       * Sorry for the long comment, but necessary to understand the problem
       * R. Dénarié
       *
        */

    buffer = new ArrayList<E>();
    dataSet = new HashSet<E>();
    SessionProvider sessionProvider = queryData_.isSystemSession() ? WCMCoreUtils.getSystemSessionProvider() :
                                                                     WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(queryData_.getWorkSpace(), WCMCoreUtils.getRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryData_.getQueryStatement(), queryData_.getLanguage_());
    ((QueryImpl)query).setOffset(offset_);  
    long prevSize = 0;
    int bufSize = bufferSize_;
    int offset = 0;
    int count = 0;
    int currentIndex = 0;
    buffer.clear();
    dataSet.clear();
    while (true) {
      ((QueryImpl)query).setOffset(offset);
      ((QueryImpl)query).setLimit(bufSize);      
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();
      RowIterator rowIter = queryResult.getRows();
      long size = iter.getSize();

      while (iter.hasNext() && count < bufferSize_) {
        currentIndex++;
        Node newNode = iter.nextNode();
        Row newRow = rowIter.nextRow();
        if (filter != null) {
          newNode = filter.filterNodeToDisplay(newNode);
        }
        if (newNode == null) {
          realOffset_++;
        }
        if (newNode != null && searchDataCreator != null) {
          E data = searchDataCreator.createData(newNode, newRow);
          if (data != null && !dataSet.contains(data)) {
            if (currentIndex > realOffset_) {
              buffer.add(data);
              count ++;
            }

            //add the node to dataset each time, to prevent duplication of web content results
            dataSet.add(data);
          } else if (data!=null && dataSet.contains(data)) {
              realOffset_++;
          }
        }
      }
      /* enough data to process*/
      if (count == bufferSize_) break;
      /* already query all data */
      if (size == prevSize) break;
      if (size<bufSize) break; //we found less results than asked => we are at the end of the results list
      offset += bufSize;
      bufSize = 2 * bufSize;
      prevSize = size;
    }
    if (buffer.size() < this.getPageSize()) {
        setTotalNodes(buffer.size());
        this.setAvailablePage(buffer.size());
    }
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
}
