/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.base;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryResultImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * This class works as a page list but only load 1 page data at a time.
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 4, 2013  
 */
public class LazyPageList<E> extends PageList{
  
  /** the log */
  private static final Log LOG  = ExoLogger.getLogger(LazyPageList.class.getName());
  /** the query data */
  private QueryData queryData_;
  /** the data creator that creates data from search result */
  private SearchDataCreator<E> dataCreator_;
  /** total size */
  private int total_;
  /** item per page */
  private int pageSize_;
  
  public LazyPageList(QueryData queryData, int pageSize, SearchDataCreator<E> dataCreator) {
    super(pageSize);
    queryData_ = queryData.clone();
    dataCreator_ = dataCreator;
    pageSize_ = pageSize;
    total_ = 0;
    //get total amount
    if (queryData.getQueryStatement() != null) {
      try {
        SessionProvider provider = WCMCoreUtils.getUserSessionProvider();
        Session session = provider.getSession(queryData.getWorkSpace(), WCMCoreUtils.getRepository());
        //In JCR, For performances reason, the permissions are not checked when
        // the order is set in the query. In order to take the permissions in consideration,
        // We need to suppress the orderBy from the Query statement
        String querySizeStatement = queryData.getQueryStatement();
        if (querySizeStatement.indexOf("ORDER BY") > 0) {
          querySizeStatement = querySizeStatement.substring(0,querySizeStatement.indexOf("ORDER BY"));
        }
        total_ = (int)session.getWorkspace().getQueryManager().
        createQuery(querySizeStatement, queryData.getLanguage_()).execute().getRows().getSize();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not execute the query: " + queryData.getQueryStatement(), e);
        }
      }
    }
    setAvailablePage(total_);
  }

  @Override
  protected void populateCurrentPage(int page) throws Exception {
    currentListPage_ = new ArrayList<E>();
    if (queryData_.getQueryStatement() != null) {
      try {
        SessionProvider provider = WCMCoreUtils.getUserSessionProvider();
        Session session = provider.getSession(queryData_.getWorkSpace(), WCMCoreUtils.getRepository());
        Query query = session.getWorkspace().getQueryManager().
        createQuery(queryData_.getQueryStatement(), queryData_.getLanguage_());
        ((QueryImpl)query).setOffset((page-1)*pageSize_);
        ((QueryImpl)query).setLimit(pageSize_);
        ((QueryImpl)query).setCaseInsensitiveOrder(true);
        QueryResult ret = query.execute();
        
        NodeIterator iter = ret.getNodes();
        RowIterator rowIter = ret.getRows();
        while (iter.hasNext()) {
          currentListPage_.add(dataCreator_.createData(iter.nextNode(), rowIter.nextRow()));
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not execute the query: " + queryData_.getQueryStatement(), e);
        }
      }
    }
  }

  @Override
  public List<E> getAll() throws Exception {
    return new ArrayList<E>();
  }

}
