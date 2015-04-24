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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
public class ArrayNodePageList<E> extends AbstractPageList<E> {
  
  private static final Log LOG = ExoLogger.getLogger(ArrayNodePageList.class.getName());

  /** The nodes. */
  protected List<E> dataList;
  
  /** Constructor */
  public ArrayNodePageList(int pageSize) {
    super(pageSize);
    removeRedundantPages(0);
  }

  /** Constructor */
  public ArrayNodePageList(List<E> nodes, int pageSize) {
    super(pageSize);
    setTotalNodes(nodes.size());
    dataList = nodes;
    setAvailablePage(dataList.size());
    removeRedundantPages(dataList.size() / pageSize);
    currentListPage_ = null;
    loadedAllData_ = true;
  }
  
  /** Constructor */
  public ArrayNodePageList(List<Node> nodes, int pageSize, 
                           NodeSearchFilter filter, SearchDataCreator<E> dataCreator) {
    super(pageSize, filter, dataCreator);
    setTotalNodes(nodes.size());
    dataList = new ArrayList<E>();
    try {
      for (Node node : nodes) {
        if (filter != null) {
          node = filter.filterNodeToDisplay(node);
        }
        if (searchDataCreator != null && node != null) { 
          E data = searchDataCreator.createData(node, null);
          if (data != null) {
            dataList.add(data);
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    setAvailablePage(dataList.size());
    removeRedundantPages(dataList.size() / pageSize);
    currentListPage_ = null;
  }  
  
  /** Constructor */
  public ArrayNodePageList(QueryResult queryResult, int pageSize, 
                           NodeSearchFilter filter, SearchDataCreator<E> dataCreator, QueryData queryData) {
    super(pageSize, filter, dataCreator);
    dataList = new ArrayList<E>();
    SiteSearchService siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);
    Map found = siteSearchService.getFoundNodes(ConversationState.getCurrent().getIdentity().getUserId(),
                                                                queryData.getQueryStatement());
    int offset = (int)queryData.getOffset();
    int position = offset;
    int page = (position)/getPageSize() + 1;    
    try {
      setTotalNodes(queryResult.getNodes().getSize());
      NodeIterator nodeIterator = queryResult.getNodes();
      RowIterator rowIterator = queryResult.getRows();
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
//        if (page != prevPage) {
//          prevPage = page;
//        }
        if (filter != null) {
          node = filter.filterNodeToDisplay(node);
        }
        Row row = rowIterator.nextRow();
        if (searchDataCreator != null && node != null) { 
          E data = searchDataCreator.createData(node, row);
          if (data != null && (found == null || !found.containsKey(data) || ((Integer)found.get(data)) >= page)) {
            position++;
            page = (position-1)/getPageSize() + 1;
            dataList.add(data);
            found.put(data, page);
            // increase page number for the last item
            if (position % getPageSize() == 0) { page++; }
          }
        }
      }
      dataList = removeDuplication(dataList);
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    setAvailablePage(dataList.size());
    removeRedundantPages(dataList.size() / pageSize);
    currentListPage_ = null;
  }
  
  /** Nodes getter */
  public List<E> getDataList() {
    return dataList;
  }
  
  @Override
  public List<E> getAll() throws Exception {
    return getDataList();
  }

  @Override
  protected void populateCurrentPage(int page) throws Exception {
    currentListPage_ = new ArrayList<E>();
    int count = 0;
    if (dataList != null) {
        for(int i = ((page - 1)*this.getPageSize()); i < dataList.size(); i++) {
          currentListPage_.add(dataList.get(i));
          count++;
          if(count == getPageSize()) {
             break;
          }
      }
    }
    currentPage_ = page;
  }

  @Override
  public void sortData() {
    //sort by given comparator
    if (comparator != null) {
      Collections.sort(dataList, comparator);
      try {
        populateCurrentPage(currentPage_);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }  
  }
  
  private List<E> removeDuplication(List<E> srcList) {
    List<E> ret = new ArrayList<E>();
    Set<E> set = new HashSet<E>();
    if (srcList != null) {
      for (E elem : srcList) {
        if (!set.contains(elem)) {
          set.add(elem);
          ret.add(elem);
        }
      }
    }
    return ret;
  }

  @Override
  public List<E> getPageWithOffsetCare(int page) throws Exception {
    return getPage(page);
  }
}
