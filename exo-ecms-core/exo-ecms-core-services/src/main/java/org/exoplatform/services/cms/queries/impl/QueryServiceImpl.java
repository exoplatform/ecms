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
package org.exoplatform.services.cms.queries.impl;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.picocontainer.Startable;

public class QueryServiceImpl implements QueryService, Startable{
  private static final String[] perms = {PermissionType.READ, PermissionType.ADD_NODE, 
    PermissionType.SET_PROPERTY, PermissionType.REMOVE };
  private String relativePath_;  
  private List<QueryPlugin> queryPlugins_ = new ArrayList<QueryPlugin> ();
  private RepositoryService repositoryService_;
  private CacheService cacheService_;  
  private PortalContainerInfo containerInfo_;
  private OrganizationService organizationService_;
  private String baseUserPath_;
  private String baseQueriesPath_;
  private String group_;
  private DMSConfiguration dmsConfiguration_;
  
  private static final Log LOG = ExoLogger.getLogger(QueryServiceImpl.class);

  /**
   * Constructor method
   * @param repositoryService
   * @param nodeHierarchyCreator
   * @param params
   * @param containerInfo
   * @param cacheService
   * @param organizationService
   * @param dmsConfiguration
   * @throws Exception
   */
  public QueryServiceImpl(RepositoryService repositoryService, NodeHierarchyCreator nodeHierarchyCreator, 
      InitParams params, PortalContainerInfo containerInfo, CacheService cacheService, 
      OrganizationService organizationService, DMSConfiguration dmsConfiguration) throws Exception {
    relativePath_ = params.getValueParam("relativePath").getValue();    
    group_ = params.getValueParam("group").getValue();
    repositoryService_ = repositoryService;
    containerInfo_ = containerInfo;
    cacheService_ = cacheService;
    organizationService_ = organizationService;
    baseUserPath_ = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    baseQueriesPath_ = nodeHierarchyCreator.getJcrPath(BasePath.QUERIES_PATH);
    dmsConfiguration_ = dmsConfiguration;
  }
  
  /**
   * Implemented method from Startable class
   * init all ManageDrivePlugin
   * @see QueryPlugin
   */
  public void start() {
    for(QueryPlugin queryPlugin : queryPlugins_){
      try{
        queryPlugin.init(baseQueriesPath_);
      }catch (Exception e) {
        LOG.error("Can not start query plugin '" + queryPlugin.getName() + "'", e);
      }
    }
  }

  /**
   * Implemented method from Startable class
   */
  public void stop() {
    // TODO Auto-generated method stub    
  }

  /**
   * Init query node with specified repository
   */
  public void init(String repository) throws Exception {
    for(QueryPlugin queryPlugin : queryPlugins_){
      try{
        queryPlugin.init(repository,baseQueriesPath_);
      }catch (Exception e) { 
        LOG.error("Can not init query plugin '" + queryPlugin.getName() + "'", e);
      }
    } 
  }
  
  /**
   * Add new QueryPlugin to queryPlugins_
   * @see                   QueryPlugin
   * @param queryPlugin     QueryPlugin
   */
  public void setQueryPlugin(QueryPlugin queryPlugin) {
    queryPlugins_.add(queryPlugin);
  }
  
  /**
   * {@inheritDoc}
   */
  public String getRelativePath() { return relativePath_; }

  /**
   * {@inheritDoc}
   */
  public List<Query> getQueries(String userName, String repository, SessionProvider provider) throws Exception {
    List<Query> queries = new ArrayList<Query>();        
    if (userName == null) return queries;    
    Session session = getSession(repository, provider, true);
    QueryManager manager = session.getWorkspace().getQueryManager();
    Node usersHome;
    try {
      usersHome = (Node)session.getItem(baseUserPath_);
    } catch (PathNotFoundException e) {
      usersHome = (Node)getSession(repository, provider, false).getItem(baseUserPath_);
    }
    Node userHome = null;
    if(usersHome.hasNode(userName)) {
      userHome = usersHome.getNode(userName);
    } else{
      userHome = usersHome.addNode(userName);
      if(userHome.canAddMixin("exo:privilegeable")){
        userHome.addMixin("exo:privilegeable");
      }
      ((ExtendedNode)userHome).setPermissions(getPermissions(userName));
      Node query = null;
      if(userHome.hasNode(relativePath_)) {
        query = userHome.getNode(relativePath_);
      } else {
        query = getNodeByRelativePath(userHome, relativePath_);
      }
      if (query.canAddMixin("exo:privilegeable")){
        query.addMixin("exo:privilegeable");
      }
      ((ExtendedNode)query).setPermissions(getPermissions(userName));
      usersHome.save();
    }
    Node queriesHome = null;
    if(userHome.hasNode(relativePath_)) {
      queriesHome = userHome.getNode(relativePath_);
    } else {
      queriesHome = getNodeByRelativePath(userHome, relativePath_);
    }
    NodeIterator iter = queriesHome.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if("nt:query".equals(node.getPrimaryNodeType().getName())) queries.add(manager.getQuery(node));
    }    
    session.logout();
    return queries;
  }
  
  /**
   * Get node by giving the node user and the relative path to its 
   * @param userHome      Node user
   * @param relativePath  The relative path to its
   * @return
   * @throws Exception
   */
  private Node getNodeByRelativePath(Node userHome, String relativePath) throws Exception {
    String[] paths = relativePath.split("/");
    String relPath = null;
    Node queriesHome = null;
    for(String path : paths) {
      if(relPath == null) relPath = path;
      else relPath = relPath + "/" + path;
      if(!userHome.hasNode(relPath)) queriesHome = userHome.addNode(relPath);
    }
    return queriesHome;
  }

  /**
   * Get all permission of the giving owner
   * @param owner
   * @return
   */
  private Map<String,String[]> getPermissions(String owner) {
    Map<String, String[]> permissions = new HashMap<String, String[]>();
    permissions.put(owner, perms);         
    permissions.put(group_, perms);
    return permissions;
  } 

  /**
   * {@inheritDoc}
   */
  public void addQuery(String queryName, String statement, String language, 
      String userName, String repository) throws Exception {
    if (userName == null) return;
    Session session = getSession(repository);
    QueryManager manager = session.getWorkspace().getQueryManager();    
    Query query = manager.createQuery(statement, language);    
    Node usersNode = (Node) session.getItem(baseUserPath_);
    if (!usersNode.hasNode(userName)) {
      usersNode.addNode(userName);
      usersNode.save();
    }
    Node userNode = usersNode.getNode(userName);
    if (!userNode.hasNode(relativePath_)) {
      getNodeByRelativePath(userNode, relativePath_);
      userNode.save();
      session.save();
    }
    String absPath = baseUserPath_ + "/" + userName + "/" + relativePath_ + "/" + queryName;
    query.storeAsNode(absPath);
    session.refresh(true);
    session.getItem(baseUserPath_).save();
    session.save();
    session.logout();
  }

  /**
   * {@inheritDoc}
   */
  public void removeQuery(String queryPath, String userName, String repository) throws Exception {
    if(userName == null) return;    
    Session session = getSession(repository);
    
    Node queryNode = null;
    try {
      queryNode = (Node) session.getItem(queryPath);
    } catch (PathNotFoundException pe) {
      queryNode = (Node) getSession(repository, SessionProviderFactory.createSessionProvider(), true).getItem(queryPath);
    }
    Node queriesHome = queryNode.getParent();
    queryNode.remove();
    queriesHome.save();
    session.save();
    session.logout();
    removeFromCache(queryPath);
  }

  /**
   * {@inheritDoc}
   */
  public void addSharedQuery(String queryName, String statement, String language, 
      String[] permissions, boolean cachedResult, String repository) throws Exception {
    addSharedQuery(queryName, statement, language, 
        permissions, cachedResult, repository, SessionProviderFactory.createSessionProvider());
  }
  
  public void addSharedQuery(String queryName, String statement, String language, 
      String[] permissions, boolean cachedResult, String repository, SessionProvider provider) throws Exception {
    Session session = getSession(repository, provider, true);
    ValueFactory vt = session.getValueFactory();
    String queryPath;
    List<Value> perm = new ArrayList<Value>();                 
    for (String permission : permissions) {
      Value vl = vt.createValue(permission);
      perm.add(vl);
    }
    Value[] vls = perm.toArray(new Value[] {});

    String queriesPath = baseQueriesPath_;
    Node queryHome = (Node)session.getItem(baseQueriesPath_);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    queryManager.createQuery(statement, language);
    if (queryHome.hasNode(queryName)) {
      Node query = queryHome.getNode(queryName);
      query.setProperty("jcr:language", language);
      query.setProperty("jcr:statement", statement);
      query.setProperty("exo:accessPermissions", vls);
      query.setProperty("exo:cachedResult", cachedResult);
      query.save();
      session.save();
      queryPath = query.getPath();
    } else {
      QueryManager manager = session.getWorkspace().getQueryManager();    
      Query query = manager.createQuery(statement, language);      
      Node newQuery = query.storeAsNode(baseQueriesPath_ + "/" + queryName);
      newQuery.addMixin("mix:sharedQuery");
      newQuery.setProperty("exo:accessPermissions", vls);
      newQuery.setProperty("exo:cachedResult", cachedResult);
      session.getItem(queriesPath).save();
      queryPath = queriesPath;
    }
    session.logout();
    removeFromCache(queryPath);
  }  

  /**
   * {@inheritDoc}
   */
  public Node getSharedQuery(String queryName, String repository, SessionProvider provider) throws Exception {
    Session session = getSession(repository, provider, true);    
    Node sharedQueryNode = (Node)session.getItem(baseQueriesPath_ + "/" + queryName); 
    session.logout();
    return sharedQueryNode;
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(String repository, SessionProvider provider) throws Exception {
    Session session = getSession(repository, provider, true);
    List<Node> queries = new ArrayList<Node>();
    Node sharedQueryHome = (Node) session.getItem(baseQueriesPath_);
    NodeIterator iter = sharedQueryHome.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if("nt:query".equals(node.getPrimaryNodeType().getName()))
        queries.add(node);
    }
    return queries;
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(String userId, String repository, SessionProvider provider) throws Exception {
    List<Node> sharedQueries = new ArrayList<Node>();
    for(Node query : getSharedQueries(repository, provider)) {
      if (canUseQuery(userId, query)) {
        sharedQueries.add(query);
      }
    }
    return sharedQueries;
  }  
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(String queryType, String userId, String repository, SessionProvider provider) throws Exception {
    List<Node> resultList = new ArrayList<Node>();
    String language = null;    
    for (Node queryNode: getSharedQueries(repository,provider)) {
      language = queryNode.getProperty("jcr:language").getString();
      if (!queryType.equalsIgnoreCase(language)) continue;
      if (canUseQuery(userId,queryNode)) {
        resultList.add(queryNode);
      }
    }
    return resultList;
  }

  /**
   * {@inheritDoc}
   */
  public Query getQueryByPath(String queryPath, String userName, String repository, SessionProvider provider) throws Exception {
    List<Query> queries = getQueries(userName, repository, provider);
    for (Query query : queries) {
      if (query.getStoredQueryPath().equals(queryPath)) return query;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void removeSharedQuery(String queryName, String repository) throws Exception {
    Session session = getSession(repository);    
    session.getItem(baseQueriesPath_ + "/" + queryName).remove();
    session.save();
    session.logout();
  }
  
  /**
   * {@inheritDoc}
   */
  public QueryResult execute(String queryPath, String workspace, String repository, SessionProvider provider, String userId) throws Exception {
    Session session = getSession(repository, provider, true);    
    Session querySession = getSession(repository, workspace, provider);
    Node queryNode = null;
    try {
      queryNode = (Node) session.getItem(queryPath);
    } catch (PathNotFoundException e) {
      LOG.warn("Can not find node by path " + queryPath + " in dms-system workspace");
      queryNode = (Node) querySession.getItem(queryPath);
    }
    if (queryNode != null && queryNode.hasProperty("exo:cachedResult")
        && queryNode.getProperty("exo:cachedResult").getBoolean()) {
      ExoCache queryCache = cacheService_.getCacheInstance(QueryServiceImpl.class.getName());
      String portalName = containerInfo_.getContainerName();
      String key = portalName + queryPath;
      QueryResult result = (QueryResult) queryCache.get(key);
      if (result != null) return result;
      result = execute(querySession, queryNode, userId);
      queryCache.put(key, result);
      return result;
    }
    QueryResult queryResult = execute(querySession, queryNode, userId);
    session.logout();
    querySession.logout();
    return queryResult;
  }
  
  /**
   * Execute the query by giving the session, query node and userid
   * @param session     The Session
   * @param queryNode   The node of query
   * @param userId      The userid
   * @return  
   * @throws Exception
   */
  private QueryResult execute(Session session, Node queryNode, String userId) throws Exception {
    String statement = this.computeStatement(queryNode.getProperty("jcr:statement").getString(), userId);
    String language = queryNode.getProperty("jcr:language").getString();
    Query query = session.getWorkspace().getQueryManager().createQuery(statement,language);
    return query.execute();
  }    
  
  /**
   * This method replaces tokens in the statement by their actual values
   * Current supported tokens are :
   * ${UserId}$ corresponds to the current user
   * ${Date}$   corresponds to the current date
   * That way, predefined queries can be equipped with dynamic values. This is
   * useful when querying for documents made by the current user, or documents
   * in publication state.
   * 
   * @param session reference to the JCR Session
   * @return the processed String, with replaced tokens
   */
  private String computeStatement(String statement, String userId) {

    // The returned computed statement
    String ret = statement;
      
    // Replace ${UserId}$
    ret = ret.replace("${UserId}$",userId);
    
    // Replace ${Date}$
    String currentDate = ISO8601.format(new GregorianCalendar());
    ret = ret.replace("${Date}$",currentDate);
    
    return ret;
  }
  
  /**
   * Remove query from cache by giving the query path
   * @param queryPath   The path to query
   * @throws Exception
   */
  private void removeFromCache(String queryPath) throws Exception {
    ExoCache queryCache = cacheService_.getCacheInstance(QueryServiceImpl.class.getName());
    String portalName = containerInfo_.getContainerName();
    String key = portalName + queryPath;
    QueryResult result = (QueryResult)queryCache.get(key);
    if (result != null) queryCache.remove(key);
  }

  /**
   * Get the session with specified repository
   * @param repository
   * @return
   * @throws Exception
   */
  private Session getSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    return manageableRepository.getSystemSession(manageableRepository.getConfiguration().getDefaultWorkspaceName());
        
  }

  /**
   * Get the session by specify the repository, sessionprovider and flag params
   * @param repository    The repository name
   * @param provider      The SessionProvider
   * @param flag          The boolean to decide which session will be chosen
   * @return
   * @throws Exception
   */
  private Session getSession(String repository,SessionProvider provider, boolean flag) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    if (!flag) {
      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      return provider.getSession(workspace, manageableRepository);
    }
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return provider.getSession(dmsRepoConfig.getSystemWorkspace(), manageableRepository);
  }

  /**
   * Get the session by specify the repository, workspace and sessionprovider
   * @param repository    The repository name
   * @param workspace     The workspace name
   * @param provider      The SessionProvider
   * @return
   * @throws Exception
   */
  private Session getSession(String repository, String workspace, SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    return provider.getSession(workspace,manageableRepository);
  }
  
  /**
   * Check the given user can use this query
   * @param userId      The user id
   * @param queryNode   The node of query
   * @return
   * @throws Exception
   */
  private boolean canUseQuery(String userId, Node queryNode) throws Exception{    
    Value[] values = queryNode.getProperty("exo:accessPermissions").getValues();
    for(Value value : values) {
      String accessPermission = value.getString();
      if (hasMembership(userId,accessPermission)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check the user which has a given membership
   * @param userId          The user id
   * @param roleExpression  The expression of membership
   * @return
   */
  private boolean hasMembership(String userId, String roleExpression) {
    if("*".equals(roleExpression))
      return true;
    String membershipType = roleExpression.substring(0, roleExpression.indexOf(":"));
    String groupName = roleExpression.substring(roleExpression.indexOf(":") + 1);
    try {
      MembershipHandler membershipHandler = organizationService_.getMembershipHandler();
      if ("*".equals(membershipType)) {
        // Determine if there exists at least one membership
        return !membershipHandler.findMembershipsByUserAndGroup( userId,groupName).isEmpty();
      } 
      // Determine if there exists the membership of specified type
      return membershipHandler.findMembershipByUserGroupAndType(userId,groupName,membershipType) != null;      
    }
    catch(Exception e) {            
    }  
    return false;
  }   
}
