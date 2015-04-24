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

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
import org.picocontainer.Startable;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.util.*;

public class QueryServiceImpl implements QueryService, Startable{
  private static final String[] perms = {PermissionType.READ, PermissionType.ADD_NODE,
    PermissionType.SET_PROPERTY, PermissionType.REMOVE };
  private String relativePath_;
  private List<QueryPlugin> queryPlugins_ = new ArrayList<QueryPlugin> ();
  private RepositoryService repositoryService_;
  private ExoCache<String, QueryResult> cache_;
  private PortalContainerInfo containerInfo_;
  private OrganizationService organizationService_;
  private String baseUserPath_;
  private String baseQueriesPath_;
  private String group_;
  private DMSConfiguration dmsConfiguration_;
  private Set<String> configuredQueries_;
  
  private NodeHierarchyCreator nodeHierarchyCreator_;

  private static final Log LOG = ExoLogger.getLogger(QueryServiceImpl.class.getName());

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
    cache_ = cacheService.getCacheInstance(CACHE_NAME);
    organizationService_ = organizationService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
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
    configuredQueries_ = new HashSet<String>();
    for(QueryPlugin queryPlugin : queryPlugins_){
      try{
        queryPlugin.init(baseQueriesPath_);
        configuredQueries_.addAll(queryPlugin.getAllConfiguredQueries());
      }catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not start query plugin '" + queryPlugin.getName() + "'", e);
        }
      }
    }
  }

  /**
   * Implemented method from Startable class
   */
  public void stop() {
  }
  
  /**
   * Init query node with current repository
   */
  public void init() throws Exception {
    configuredQueries_ = new HashSet<String>();
    for(QueryPlugin queryPlugin : queryPlugins_){
      try{
        queryPlugin.init(baseQueriesPath_);
        configuredQueries_.addAll(queryPlugin.getAllConfiguredQueries());
      }catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not init query plugin '" + queryPlugin.getName() + "'", e);
        }
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
  public List<Query> getQueries(String userName, SessionProvider provider) throws Exception {
    List<Query> queries = new ArrayList<Query>();
    if (userName == null) return queries;
    Session session = getSession(provider, true);
    QueryManager manager = session.getWorkspace().getQueryManager();
    Node usersHome;
    try {
      usersHome = (Node)session.getItem(baseUserPath_);
    } catch (PathNotFoundException e) {
      usersHome = (Node)getSession(provider, false).getItem(baseUserPath_);
    }
    Node userHome = null;
    try {
      userHome = nodeHierarchyCreator_.getUserNode(provider, userName);
    } catch(Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    if (userHome == null) {
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
      if(node.isNodeType("nt:query")) queries.add(manager.getQuery(node));
    }
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
    StringBuffer relPath = null;
    Node queriesHome = null;
    for (String path : paths) {
      if (relPath == null)
        relPath = new StringBuffer(path);
      else
        relPath.append("/").append(path);
      if (!userHome.hasNode(relPath.toString()))
        queriesHome = userHome.addNode(relPath.toString());
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
  public void addQuery(String queryName, String statement, String language, String userName) throws Exception {
    if (userName == null)
      return;
    Session session = getSession();
    QueryManager manager = session.getWorkspace().getQueryManager();
    Query query = manager.createQuery(statement, language);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, userName);
    if (!userNode.hasNode(getRelativePath())) {
      getNodeByRelativePath(userNode, relativePath_);
      session.save();
    }
    String absPath = userNode.getPath() + "/" + relativePath_ + "/" + queryName;
    query.storeAsNode(absPath);
    session.save();  
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeQuery(String queryPath, String userName) throws Exception {
    if (userName == null)
      return;
    Session session = getSession();
    
    Node queryNode = null;
    try {
      queryNode = (Node) session.getItem(queryPath);
    } catch (PathNotFoundException pe) {
      queryNode = (Node) getSession(WCMCoreUtils.getSystemSessionProvider(), true).getItem(queryPath);
    }
    Node queriesHome = queryNode.getParent();
    queryNode.remove();
    queriesHome.save();
    removeFromCache(queryPath);
  }  

  /**
   * {@inheritDoc}
   */
  public void addSharedQuery(String queryName,
                             String statement,
                             String language,
                             String[] permissions,
                             boolean cachedResult) throws Exception {
    addSharedQuery(queryName,
                   statement,
                   language,
                   permissions,
                   cachedResult,
                   WCMCoreUtils.getUserSessionProvider());
  }
  
  public void addSharedQuery(String queryName,
                             String statement,
                             String language,
                             String[] permissions,
                             boolean cachedResult,
                             SessionProvider provider) throws Exception {
    Session session = getSession(provider, true);
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
    removeFromCache(queryPath);
  }  
  
  /**
   * {@inheritDoc}
   */
  public Node getSharedQuery(String queryName, SessionProvider provider) throws Exception {
    Session session = getSession(provider, true);
    try {
      Node sharedQueryNode = (Node) session.getItem(baseQueriesPath_ + "/" + queryName);
      return sharedQueryNode;
    } catch (PathNotFoundException e) {
      return null;
    }
  }  

  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(SessionProvider provider) throws Exception {
    Session session = getSession(provider, true);
    List<Node> queries = new ArrayList<Node>();
    Node sharedQueryHome = (Node) session.getItem(baseQueriesPath_);
    NodeIterator iter = sharedQueryHome.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if(node.isNodeType("nt:query")) {
        queries.add(node);
      }
    }
    return queries;
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(String userId, SessionProvider provider) throws Exception {
    List<Node> sharedQueries = new ArrayList<Node>();
    for(Node query : getSharedQueries(provider)) {
      if (canUseQuery(userId, query)) {
        sharedQueries.add(query);
      }
    }
    return sharedQueries;
  }  
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getSharedQueries(String queryType,
                                     String userId,
                                     SessionProvider provider) throws Exception {
    List<Node> resultList = new ArrayList<Node>();
    String language = null;
    for (Node queryNode: getSharedQueries(provider)) {
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
  public Query getQueryByPath(String queryPath, String userName, SessionProvider provider) throws Exception {
    List<Query> queries = getQueries(userName, provider);
    for (Query query : queries) {
      if (query.getStoredQueryPath().equals(queryPath)) return query;
    }
    return null;
  }  
  
  /**
   * {@inheritDoc}
   */
  public void removeSharedQuery(String queryName, SessionProvider provider) throws Exception {
    Session session = getSession(provider, true);
    session.getItem(baseQueriesPath_ + "/" + queryName).remove();
    session.save();
  }  
  
  /**
   * {@inheritDoc}
   */
  public QueryResult execute(String queryPath,
                             String workspace,
                             SessionProvider provider,
                             String userId) throws Exception {
    Session session = getSession(provider, true);
    Session querySession = getSession(workspace, provider);
    Node queryNode = null;
    try {
      queryNode = (Node) session.getItem(queryPath);
    } catch (PathNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not find node by path " + queryPath + " in dms-system workspace");
      }
      queryNode = (Node) querySession.getItem(queryPath);
    }
    if (queryNode != null && queryNode.hasProperty("exo:cachedResult")
        && queryNode.getProperty("exo:cachedResult").getBoolean()) {
      String portalName = containerInfo_.getContainerName();
      String key = portalName + queryPath;
      QueryResult result = cache_.get(key);
      if (result != null) return result;
      result = execute(querySession, queryNode, userId);
      cache_.put(key, result);
      return result;
    }
    QueryResult queryResult = execute(querySession, queryNode, userId);
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
    return createQuery(session, queryNode, userId).execute();
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
    String portalName = containerInfo_.getContainerName();
    String key = portalName + queryPath;
    QueryResult result = cache_.get(key);
    if (result != null) cache_.remove(key);
  }

  /**
   * Get the session with curent repository
   * @return
   * @throws Exception
   */
  private Session getSession() throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    return sessionProvider.getSession(manageableRepository.getConfiguration().getDefaultWorkspaceName(), 
        manageableRepository);

  }

  /**
   * Get the session by specify the repository, sessionprovider and flag params
   * @param provider      The SessionProvider
   * @param flag          The boolean to decide which session will be chosen
   * @return
   * @throws Exception
   */
  private Session getSession(SessionProvider provider, boolean flag) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    if (!flag) {
      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      return provider.getSession(workspace, manageableRepository);
    }
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    return provider.getSession(dmsRepoConfig.getSystemWorkspace(), manageableRepository);
  }

  /**
   * Get the session by specify the repository, workspace and sessionprovider
   * @param workspace     The workspace name
   * @param provider      The SessionProvider
   * @return
   * @throws Exception
   */
  private Session getSession(String workspace, SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
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
    if (userId == null || userId.length() == 0) {
      return false;
    }
    if(roleExpression.equals("*") || roleExpression.equals(IdentityConstants.ANY))
      return true;
    ConversationState conversationState = ConversationState.getCurrent();
    Identity identity = conversationState.getIdentity();
    String membershipType = roleExpression.substring(0, roleExpression.indexOf(":"));
    String groupName = roleExpression.substring(roleExpression.indexOf(":") + 1);
    try {
      MembershipHandler membershipHandler = organizationService_.getMembershipHandler();
      if ("*".equals(membershipType)) {
    	// Determine if there exists at least one membership
        if (userId.equals(ConversationState.getCurrent().getIdentity().getUserId())) {
          return identity.isMemberOf(groupName);
        } else {
          return !membershipHandler.findMembershipsByUserAndGroup( userId,groupName).isEmpty();
        }
      }
      if (userId.equals(ConversationState.getCurrent().getIdentity().getUserId())) {
    	  return identity.isMemberOf(groupName, membershipType);
      } else {
        // Determine if there exists the membership of specified type
        return membershipHandler.findMembershipByUserGroupAndType(userId,groupName,membershipType) != null;
      }
    }
    catch(Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public Query getQuery(String queryPath, String workspace, SessionProvider provider, String userId) throws Exception {
    Session session = getSession(provider, true);
    Session querySession = getSession(workspace, provider);
    Node queryNode = null;
    try {
      queryNode = (Node) session.getItem(queryPath);
    } catch (PathNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not find node by path " + queryPath + " in dms-system workspace");
      }
      queryNode = (Node) querySession.getItem(queryPath);
    }
    return createQuery(querySession, queryNode, userId);
  }
  
  /**
   * Creates the Query object by giving the session, query node and userid
   * @param session     The Session
   * @param queryNode   The node of query
   * @param userId      The userid
   * @return
   * @throws Exception
   */
  private Query createQuery(Session session, Node queryNode, String userId) throws Exception {
    String statement = this.computeStatement(queryNode.getProperty("jcr:statement").getString(), userId);
    String language = queryNode.getProperty("jcr:language").getString();
    Query query = session.getWorkspace().getQueryManager().createQuery(statement,language);
    return query;
  }

  @Override
  public Set<String> getAllConfiguredQueries() {
    return configuredQueries_;
  }
  
}
