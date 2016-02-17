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
package org.exoplatform.services.cms.queries;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

public interface QueryService {

  public static final String CACHE_NAME = "ecm.query";
  
  /**
   * Get the relative path
   *
   * @return
   */
  public String getRelativePath();
  
  /**
   * Get queries by giving the following params : userName, repository, provider
   *
   * @param userName String Can be <code>null</code>
   * @param provider SessionProvider
   * @return queries
   * @see Query
   * @see SessionProvider
   * @throws Exception
   */
  public List<Query> getQueries(String userName, SessionProvider provider) throws Exception;
  
  /**
   * Execute query by giving the following params : queryPath, workspace,
   * provider, userId
   *
   * @param queryPath String The path of query
   * @param workspace String The name of workspace
   * @param provider SessionProvider
   * @param userId String The id of current user
   * @return queries QueryResult
   * @see QueryResult
   * @see SessionProvider
   * @throws Exception
   */
  public QueryResult execute(String queryPath,
                             String workspace,
                             SessionProvider provider,
                             String userId) throws Exception;
  
  
  /**
   * Add new query by giving the following params : queryName, statement,
   * language, userName
   *
   * @param queryName String The name of query
   * @param statement String The statement query
   * @param language String The language is requested
   * @param userName String Can be <code>null</code>
   * @throws Exception
   */
  public void addQuery(String queryName, String statement, String language, String userName) throws Exception;
  
  /**
   * Remove query by giving the following params : queryPath, userName
   *
   * @param queryPath String The path of query
   * @param userName String Can be <code>null</code>
   * @throws Exception
   */
  public void removeQuery(String queryPath, String userName) throws Exception;  
  
  /**
   * Add new shared query by giving the following params: queryName, statement,
   * language, permissions, cachedResult
   * 
   * @param queryName String The name of query
   * @param statement String The statement query
   * @param language String The language is requested
   * @param permissions String[]
   * @param cachedResult boolean Choosen for caching results
   * @throws Exception
   */
  public void addSharedQuery(String queryName,
                             String statement,
                             String language,
                             String[] permissions,
                             boolean cachedResult) throws Exception;
  
  /**
   * Add new shared query by giving the following params: queryName, statement,
   * language, permissions, cachedResult, provider
   * 
   * @param queryName String The name of query
   * @param statement String The statement query
   * @param language String The language is requested
   * @param permissions String[]
   * @param cachedResult boolean Choosen for caching results
   * @param provider Session provider
   * @throws Exception
   */
  public void addSharedQuery(String queryName,
                             String statement,
                             String language,
                             String[] permissions,
                             boolean cachedResult,
                             SessionProvider provider) throws Exception;

  
  /**
   * Get shared queries by giving the following params : queryName, provider
   *
   * @param queryName the name of query 
   * @param provider SessionProvider
   * @return sharedQueries
   * @see Node
   * @see SessionProvider
   * @throws Exception
   */
  public Node getSharedQuery(String queryName, SessionProvider provider) throws Exception;
  
  /**
   * Remove share query by giving the following params : queryName
   *
   * @param queryName String The name of query  
   * @param provider SessionProvider
   * @throws Exception
   */
  public void removeSharedQuery(String queryName, SessionProvider provider) throws Exception;  

  /**
   * Get shared queries by giving the following params : provider
   *
   * @param provider SessionProvider
   * @return sharedQueries
   * @see Node
   * @see SessionProvider
   * @throws Exception
   */
  public List<Node> getSharedQueries(SessionProvider provider) throws Exception;
  
  /**
   * Get query with path by giving the following params : queryPath, userName,
   * provider
   * 
   * @param queryPath String The path of query
   * @param userName String The name of current user
   * @param provider SessionProvider
   * @return query Query
   * @see Node
   * @see Query
   * @see SessionProvider
   * @throws Exception
   */
  public Query getQueryByPath(String queryPath, String userName, SessionProvider provider) throws Exception;

  
  /**
   * Get shared queries by giving the following params : userId, provider
   *
   * @param userId String The id of current user
   * @param provider SessionProvider
   * @return sharedQueries
   * @see Node
   * @see SessionProvider
   * @throws Exception
   */
  public List<Node> getSharedQueries(String userId, SessionProvider provider) throws Exception;  
  
  /**
   * Get shared queries by giving the following params : queryType, userId,
   * provider
   *
   * @param queryType String The type of query
   * @param userId String The id of current user
   * @param provider SessionProvider
   * @return sharedQueries
   * @see Node
   * @see SessionProvider
   * @throws Exception
   */
  public List<Node> getSharedQueries(String queryType,
                                     String userId,
                                     SessionProvider provider) throws Exception;

  /**
   * Init all query plugin in the current repository
   * @see org.exoplatform.services.cms.queries.impl.QueryPlugin
   * @throws Exception
   */
  public void init() throws Exception;

  /**
   * Returns Query object by giving the following params : queryPath, workspace,
   * provider, userId
   *
   * @param queryPath String The path of query
   * @param workspace String The name of workspace
   * @param provider SessionProvider
   * @param userId String The id of current user
   * @return queries QueryResult
   * @see QueryResult
   * @see SessionProvider
   * @throws Exception
   */  
  public Query getQuery(String queryPath,
                         String workspace,
                         SessionProvider provider,
                         String userId) throws Exception;
  
  /**
   * gets all configured queries
   * @return
   */
  public Set<String> getAllConfiguredQueries();

}
