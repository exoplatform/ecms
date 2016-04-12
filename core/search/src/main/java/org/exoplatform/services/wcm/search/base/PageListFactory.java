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
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
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
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
public class PageListFactory {

  private static final Log LOG = ExoLogger.getLogger(PageListFactory.class.getName());
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator,
                                                       int pageSize,
                                                       int bufferSize,
                                                       QueryCriteria criteria) throws LoginException,
                                                                      NoSuchWorkspaceException,
                                                                      RepositoryException {
    if (pageSize == 0) {
      pageSize = AbstractPageList.DEFAULT_PAGE_SIZE;
    }
    if (bufferSize < pageSize) {
      bufferSize = Math.max(pageSize, AbstractPageList.DEAFAULT_BUFFER_SIZE);
    }
    SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider() :
                                                          WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(workspace, WCMCoreUtils.getRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryStatement, language);
    int offset = 0;
    if (criteria != null) {
      if (criteria.getOffset() > 0) { ((QueryImpl)query).setOffset(criteria.getOffset()); }
      else {
        if (criteria.getOffset() < 0) {
          // WCMAdvancedSearch takes the default value (-1) of QueryCriteria's offset
          // reset it to 0 to align with the default value of Unified Search
          criteria.setOffset(0);
        }
        SiteSearchService searchService = WCMCoreUtils.getService(SiteSearchService.class);
        searchService.clearCache(ConversationState.getCurrent().getIdentity().getUserId(), queryStatement);
      }
      offset = (int)criteria.getOffset();
    }  else {
      SiteSearchService searchService = WCMCoreUtils.getService(SiteSearchService.class);
      searchService.clearCache(ConversationState.getCurrent().getIdentity().getUserId(), queryStatement);
    }
    ((QueryImpl)query).setLimit(AbstractPageList.RESULT_SIZE_SEPARATOR + 1);
    QueryResult result = query.execute();
    int totalNodes = (int)result.getNodes().getSize();
    QueryData queryData = new QueryData(queryStatement, workspace, language, isSystemSession, offset);
      return new ArrayNodePageList<E>(result, pageSize, filter, dataCreator, queryData);

  }
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator,
                                                       int pageSize,
                                                       int bufferSize) throws LoginException,
                                                                      NoSuchWorkspaceException,
                                                                      RepositoryException {
    return createPageList(queryStatement, workspace, language,
                           isSystemSession, filter, dataCreator,
                           pageSize, bufferSize, null);
  }
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator) throws LoginException,
                                                                                        NoSuchWorkspaceException,
                                                                                        RepositoryException {
    return createPageList(queryStatement, workspace, language,
                          isSystemSession, filter, dataCreator,
                          AbstractPageList.DEFAULT_PAGE_SIZE, AbstractPageList.DEAFAULT_BUFFER_SIZE);
  }
  
  public static <E> AbstractPageList<E> createPageList(List<Node> nodes, int pageSize, 
                           NodeSearchFilter filter, SearchDataCreator<E> dataCreator) {

    return new ArrayNodePageList<E>(nodes, pageSize, filter, dataCreator);
  }
  
  /**
   * 
   * @param <E>
   * @param queryStatement
   * @param workspace
   * @param language
   * @param isSystemSession
   * @param dataCreator
   * @return
   * @throws LoginException
   * @throws NoSuchWorkspaceException
   * @throws RepositoryException
   */
  public static <E> List<E> createPageList(String queryStatement,
                                           String workspace,
                                           String language,
                                           boolean isSystemSession,
                                           SearchDataCreator<E> dataCreator) throws LoginException,
                                                                            NoSuchWorkspaceException,
                                                                            RepositoryException {
    SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider()
                                                     : WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(workspace, WCMCoreUtils.getRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryStatement, language);
    QueryResult queryResult = query.execute();
    List<E> dataList = new ArrayList<E>();
    try {
      NodeIterator nodeIterator = queryResult.getNodes();
      RowIterator rowIterator = queryResult.getRows();
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
        Row row = rowIterator.nextRow();
        if (dataCreator != null && node != null) { 
          E data = dataCreator.createData(node, row);
          if (data != null) {
            dataList.add(data);
          }
        }
      }
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return dataList;
  }
  
  public static <E> LazyPageList<E> createLazyPageList(QueryData queryData, int nodePerPage, 
                                                       SearchDataCreator<E> dataCreator) {
    return new LazyPageList<E>(queryData, nodePerPage, dataCreator);
  }
  
}
