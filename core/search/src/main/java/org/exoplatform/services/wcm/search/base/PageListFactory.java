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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.search.QueryCriteria;
import org.exoplatform.services.wcm.search.SiteSearchService;
import org.exoplatform.services.wcm.search.connector.FileApplicationSearchServiceConnector;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.*;
import javax.jcr.query.*;
import java.util.*;

import static org.exoplatform.services.cms.folksonomy.NewFolksonomyService.EXO_TAGGED;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
public class PageListFactory {

  private static final Log LOG = ExoLogger.getLogger(PageListFactory.class.getName());
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       Locale locale,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator,
                                                       int pageSize,
                                                       int bufferSize,
                                                       QueryCriteria criteria) throws RepositoryException {
    SiteSearchService siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);

    if (pageSize == 0) {
      pageSize = AbstractPageList.DEFAULT_PAGE_SIZE;
    }

    if (criteria != null) {
      if (criteria.getOffset() <= 0) {
        // WCMAdvancedSearch takes the default value (-1) of QueryCriteria's offset
        // reset it to 0 to align with the default value of Unified Search
        criteria.setOffset(0);
        siteSearchService.clearCache(ConversationState.getCurrent().getIdentity().getUserId(), queryStatement);
      }
    } else {
      siteSearchService.clearCache(ConversationState.getCurrent().getIdentity().getUserId(), queryStatement);
    }

    // search in JCR
    List<E> results = searchInJCR(queryStatement, workspace, language, isSystemSession, filter, dataCreator, criteria);

    if(criteria != null && !criteria.isSearchWebpage()) {
      // search in ES
      results.addAll(searchInES(workspace, locale, isSystemSession, filter, dataCreator, criteria));
    }

    // remove duplications
    results = new ArrayList<>(new LinkedHashSet<>(results));

    if(criteria != null && criteria.getOffset() > 0) {
      if(criteria.getOffset() >= results.size()) {
        return new ArrayNodePageList<>(pageSize);
      }
      results = results.subList((int) criteria.getOffset(), results.size());
    }

    return new ArrayNodePageList<>(results, pageSize);
  }
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       Locale locale,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator,
                                                       int pageSize,
                                                       int bufferSize) throws LoginException,
                                                                      NoSuchWorkspaceException,
                                                                      RepositoryException {
    return createPageList(queryStatement, locale, workspace, language,
                           isSystemSession, filter, dataCreator,
                           pageSize, bufferSize, null);
  }
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                       Locale locale,
                                                       String workspace,
                                                       String language,
                                                       boolean isSystemSession,
                                                       NodeSearchFilter filter,
                                                       SearchDataCreator<E> dataCreator) throws LoginException,
                                                                                        NoSuchWorkspaceException,
                                                                                        RepositoryException {
    return createPageList(queryStatement, locale, workspace, language,
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
          E data = dataCreator.createData(node, row, null);
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

  /**
   * Search in JCR
   * @param queryStatement
   * @param workspace
   * @param language
   * @param isSystemSession
   * @param filter
   * @param dataCreator
   * @param criteria
   * @param <E>
   * @return
   * @throws RepositoryException
   */
  protected static <E> List<E> searchInJCR(String queryStatement,
                                    String workspace,
                                    String language,
                                    boolean isSystemSession,
                                    NodeSearchFilter filter,
                                    SearchDataCreator<E> dataCreator,
                                    QueryCriteria criteria) throws RepositoryException {
    SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider() :
            WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(workspace, WCMCoreUtils.getRepository());
    NewFolksonomyService newFolksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryStatement, language);

    long offset = criteria != null ? criteria.getOffset() : 0;
    ((QueryImpl) query).setOffset(0);
    ((QueryImpl) query).setLimit(offset + AbstractPageList.RESULT_SIZE_SEPARATOR + 1);
    QueryResult result = query.execute();

    List<E> dataList = new ArrayList<>();

    try {
      NodeIterator nodeIterator = result.getNodes();
      RowIterator rowIterator = result.getRows();
      while (nodeIterator.hasNext()) {
        Row row = rowIterator.nextRow();
        Node node = nodeIterator.nextNode();
        if(node.isNodeType(EXO_TAGGED)) {
            List<Node> taggedNode = newFolksonomyService.getAllDocumentsByTag(node.getPath(), session.getWorkspace().getName(), sessionProvider);
            for (Node item : taggedNode) {
              if (filter != null) {
                item = filter.filterNodeToDisplay(item);
              }
              if (dataCreator != null && item != null) {
                E data = dataCreator.createData(item, row, null);
                if (data != null) {
                  dataList.add(data);
                }
              }
            }
          } else {
          if (filter != null) {
            node = filter.filterNodeToDisplay(node);
          }
          if (dataCreator != null && node != null) {
            E data = dataCreator.createData(node, row, null);
            if (data != null) {
              dataList.add(data);
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }

    return dataList;
  }

  /**
   *
   * @param workspace
   * @param isSystemSession
   * @param filter
   * @param dataCreator
   * @param criteria
   * @param <E>
   * @return
   * @throws RepositoryException
   */
  protected static <E> List<E> searchInES(String workspace,
                                   Locale locale,
                                   boolean isSystemSession,
                                   NodeSearchFilter filter,
                                   SearchDataCreator<E> dataCreator,
                                   QueryCriteria criteria) throws RepositoryException {
    SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider() :
            WCMCoreUtils.getUserSessionProvider();
    Session session = sessionProvider.getSession(workspace, WCMCoreUtils.getRepository());

    FileApplicationSearchServiceConnector fileSearchInternalServiceConnector = CommonsUtils.getService(FileApplicationSearchServiceConnector.class);
    int offset = criteria != null ? (int) criteria.getOffset() : 0;
    Collection<SearchResult> results = fileSearchInternalServiceConnector.appSearch(workspace,
            criteria != null ? criteria.getSearchPath() : "",
            locale,
            criteria != null ? criteria.getKeyword() : "",
            0,
            offset + AbstractPageList.RESULT_SIZE_SEPARATOR + 1,
            null,
            null);

    List<E> filteredResults = new ArrayList<>();

    results.forEach(result -> {
      EcmsSearchResult searchResult = (EcmsSearchResult) result;
      // JCR score is calculated as (lucene score)*1000 , so we do the same thing for ES results to have a more accurate comparison
      searchResult.setRelevancy(searchResult.getRelevancy() * 1000);
      String nodePath = searchResult.getNodePath();
      try {
        if(StringUtils.isNotBlank(nodePath) && session.itemExists(nodePath)) {
          Node node = (Node) session.getItem(nodePath);
          if (filter != null) {
            node = filter.filterNodeToDisplay(node);
          }
          if (node != null) {
            E nodeData = dataCreator.createData(node, null, searchResult);
            if (nodeData != null) {
              filteredResults.add(nodeData);
            }
          }
        }
      } catch (RepositoryException e) {
        LOG.error("Cannot get node " + nodePath, e);
      }
    });

    return filteredResults;
  }

}
