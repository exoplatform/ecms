/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.ecm.dms.queries;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@gmail.com Jun 12, 2009
 */
public class TestQueryService extends BaseDMSTestCase {
  private QueryService         queryService;

  private NodeHierarchyCreator nodeHierarchyCreator;

  private String               baseUserPath;

  private String               baseQueriesPath;

  private String               relativePath = "Private/Searches";

  public void setUp() throws Exception {
    super.setUp();
    queryService = (QueryService) container.getComponentInstanceOfType(QueryService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator) container
        .getComponentInstanceOfType(NodeHierarchyCreator.class);
    baseUserPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    baseQueriesPath = nodeHierarchyCreator.getJcrPath(BasePath.QUERIES_PATH);
  }

  /**
   * Init all query plugin by giving the following params : repository
   * Input:
   *    Init three param which is configured in test-queries-configuration.xml
   * Expect:
   *    Size of list node = 3, contains CreatedDocuments node, CreatedDocumentsDayBefore node,
   *    AllArticles node
   * @throws Exception
   */
  public void testInit() throws Exception {
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    Node queriesHome = (Node) mySession.getItem(baseQueriesPath);
    assertEquals(queriesHome.getNodes().getSize(), 3);
    assertNotNull(queriesHome.getNode("Created Documents"));
    assertNotNull(queriesHome.getNode("CreatedDocumentDayBefore"));
    assertNotNull(queriesHome.getNode("All Articles"));
  }

  /**
   * Add new query by giving the following params : queryName, statement,
   * language, userName, repository
   * Input:
   *    queryName = "QueryAll"; statement = "Select * from nt:base"; language = "sql";
   *    userName = "root"; repository = "repository";
   * Expect:
   *    node: name = "QueryAll" not null;
   * @throws Exception
   */
  public void testAddQuery() throws Exception {
    queryService.addQuery("QueryAll", "Select * from nt:base", "sql", "root", REPO_NAME);
    String userPath = baseUserPath + "/root/" + relativePath;
    Node nodeSearch = (Node) session.getItem(userPath);
    Node queryAll = nodeSearch.getNode("QueryAll");
    assertNotNull(queryAll);
  }

  /**
   * Get queries by giving the following params : userName, repository, provider
   * Input:
   *    1. Add 2 query node to test
   *      1.1 queryName = "QueryAll1"; statement = "Select * from nt:base"; language = "sql";
   *          userName = "root"; repository = "repository";
   *      1.2 queryName = "QueryAll2"; statement = "//element(*, exo:article)"; language = "xpath";
   *          userName = "root"; repository = "repository";
   * Input: userName = "root", repository = "repository", provider = sessionProvider
   * Expect: Size of list node = 2
   * Input: userName = "marry", repository = "repository", provider = sessionProvider
   * Expect: Size of list node = 0
   * Input: userName = null, repository = "repository", provider = sessionProvider
   * Expect: Size of list node = 0
   * @throws Exception
   */
  public void testGetQueries() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addQuery("QueryAll1", "Select * from nt:base", "sql", "root", REPO_NAME);
    queryService.addQuery("QueryAll2", "//element(*, exo:article)", "xpath", "root", REPO_NAME);
    List<Query> listQueryRoot = queryService.getQueries("root", REPO_NAME, sessionProvider);
    assertEquals(listQueryRoot.size(), 2);
    List<Query> listQueryMarry = queryService.getQueries("marry", REPO_NAME, sessionProvider);
    assertEquals(listQueryMarry.size(), 0);
    List<Query> listQueryNull = queryService.getQueries(null, REPO_NAME, sessionProvider);
    assertEquals(listQueryNull.size(), 0);
  }

  /**
   * Remove query by giving the following params : queryPath, userName, repository
   * Input:
   *    1. Add 2 query node to test
   *      1.1 queryName = "QueryAll1"; statement = "Select * from nt:base"; language = "sql";
   *          userName = "root"; repository = "repository";
   *      1.2 queryName = "QueryAll2"; statement = "//element(*, exo:article)"; language = "xpath";
   *          userName = "root"; repository = "repository";
   * Input:
   *    queryPath = "/Users/root/Private/Searches/QueryAll1", userName = "root",
   *    repository = "repository"
   * Expect:
   *    node: name = "QueryAll1" is removed
   * Input:
   *    queryPath = "/Users/marry/Private/Searches/QueryAll2", userName = "marry",
   *    repository = "repository"
   * Expect:
   *    exception: Query path not found!
   * @throws Exception
   */
  public void testRemoveQuery() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
//    queryService.addQuery("QueryAll1", "Select * from nt:base", "sql", "root", REPO_NAME);
//    queryService.addQuery("QueryAll2", "//element(*, exo:article)", "xpath", "root", REPO_NAME);
//    String queryPathRoot = baseUserPath + "/root/" + relativePath + "/QueryAll1";
//    queryService.removeQuery(queryPathRoot, "root", REPO_NAME);
//    List<Query> listQuery = queryService.getQueries("root", REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 1);

    try {
//      String queryPathMarry = baseUserPath + "/marry/" + relativePath + "/QueryAll2";
      queryService.removeSharedQuery("Created Documents", REPO_NAME);
      List<Node> listQuery = queryService.getSharedQueries(sessionProvider);
      assertEquals(listQuery.size(), 2);
      fail("Query Path not found!");
    } catch (PathNotFoundException e) {
    }
  }

  /**
   * Get query with path by giving the following params : queryPath, userName, repository, provider
   * Input:
   *    1. Add 2 query node to test
   *      1.1 queryName = "QueryAll1"; statement = "Select * from nt:base"; language = "sql";
   *          userName = "root"; repository = "repository";
   *      1.2 queryName = "QueryAll2"; statement = "//element(*, exo:article)"; language = "xpath";
   *          userName = "root"; repository = "repository";
   * Input:
   *    queryPath = "/Users/root/Private/Searches/QueryAll1", userName = "root",
   *    repository = "repository"
   * Expect:
   *    node: name = "QueryAll1" is not null
   * Input:
   *    queryPath = "/Users/root/Private/Searches/QueryAll3", userName = "root",
   *    repository = "repository"
   * Expect:
   *    node: query node is null
   * @throws Exception
   */
  public void testGetQueryByPath() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addQuery("QueryAll1", "Select * from nt:base", "sql", "root", REPO_NAME);
    queryService.addQuery("QueryAll2", "//element(*, exo:article)", "xpath", "root", REPO_NAME);

    String queryPath1 = baseUserPath + "/root/" + relativePath + "/QueryAll1";
    Query query = queryService.getQueryByPath(queryPath1, "root", REPO_NAME, sessionProvider);
    assertNotNull(query);
    assertEquals(query.getStatement(), "Select * from nt:base");

    String queryPath2 = baseUserPath + "/root/" + relativePath + "/QueryAll3";
    query = queryService.getQueryByPath(queryPath2, "root", REPO_NAME, sessionProvider);
    assertNull(query);
  }

  /**
   * Add new shared query by giving the following params: queryName, statement, language,
   * permissions, cachedResult, repository
   * Input:
   *    queryName = "QueryAll1", statement = "Select * from nt:base", language = "sql",
   *    permissions = { "*:/platform/administrators" }, cachedResult = false, repository = "repository"
   * Expect:
   *    node: name = "QueryAll1" is not null
   *    Value of property
   *      jcr:language = sql, jcr:statement = Select * from nt:base, exo:cachedResult = false,
   *      exo:accessPermissions = *:/platform/administrators
   * @throws Exception
   */
  public void testAddSharedQuery() throws Exception {
    Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    queryService.addSharedQuery("QueryAll1", "Select * from nt:base", "sql",
        new String[] { "*:/platform/administrators" }, false, REPO_NAME, sessionProviderService_.getSystemSessionProvider(null));
    Node queriesHome = (Node) mySession.getItem(baseQueriesPath);
    Node queryAll1 = queriesHome.getNode("QueryAll1");
    assertNotNull(queryAll1);
    assertEquals(queryAll1.getProperty("jcr:language").getString(), "sql");
    assertEquals(queryAll1.getProperty("jcr:statement").getString(), "Select * from nt:base");
    assertEquals(queryAll1.getProperty("exo:cachedResult").getBoolean(), false);
    assertEquals(queryAll1.getProperty("exo:accessPermissions").getValues()[0].getString(),
        "*:/platform/administrators");
    queryAll1.remove();
    queriesHome.save();
  }

  /**
   * Get shared queries by giving the following params : userId, repository, provider
   * Input:
   *    1. Add a shared query node
   *       queryName = "QueryAll1", statement = "Select * from nt:base", language = "sql",
   *       permissions = { "*:/platform/administrators" }, cachedResult = false,
   *       repository = "repository"
   * Expect:
   *    node: name = "QueryAll1" is not null
   *    Value of property
   *      jcr:language = sql, jcr:statement = Select * from nt:base, exo:cachedResult = false,
   *      exo:accessPermissions = *:/platform/administrators
   * @throws Exception
   */
  public void testGetSharedQuery() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addSharedQuery("QueryAll1", "Select * from nt:base", "sql",
        new String[] { "*:/platform/administrators" }, false, REPO_NAME, sessionProvider);
    Node nodeQuery = queryService.getSharedQuery("QueryAll1", REPO_NAME, sessionProvider);
    assertNotNull(nodeQuery);
    assertEquals(nodeQuery.getProperty("jcr:language").getString(), "sql");
    assertEquals(nodeQuery.getProperty("jcr:statement").getString(), "Select * from nt:base");
    assertEquals(nodeQuery.getProperty("exo:cachedResult").getBoolean(), false);
    assertEquals(nodeQuery.getProperty("exo:accessPermissions").getValues()[0].getString(),
        "*:/platform/administrators");
    Node queriesHome = (Node) sessionProvider.getSession(DMSSYSTEM_WS, repository).getItem(baseQueriesPath);
    queriesHome.getNode("QueryAll1").remove();
    queriesHome.save();
  }

  /**
   * Remove share query by giving the following params : queryName, repository
   * Input:
   *    1. Add a shared query node
   *       queryName = "QueryAll1", statement = "Select * from nt:base", language = "sql",
   *       permissions = { "*:/platform/administrators" }, cachedResult = false,
   *       repository = "repository"
   * Input:
   *    queryName = "QueryAll2", repository = "repository"
   * Expect:
   *    exception: Query Path not found!
   * Input:
   *    queryName = "QueryAll1", repository = "repository"
   * Expect:
   *    node: name = "QueryAll1" is removed
   * @throws Exception
   */
  public void testRemoveSharedQuery() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addSharedQuery("QueryAll1", "Select * from nt:base", "sql",
        new String[] { "*:/platform/administrators" }, false, REPO_NAME, sessionProvider);
    try {
      queryService.removeSharedQuery("QueryAll2", REPO_NAME);
      fail("Query Path not found!");
      queryService.removeSharedQuery("QueryAll1", REPO_NAME);
      Node nodeQuery = queryService.getSharedQuery("QueryAll1", REPO_NAME, sessionProvider);
      assertNull(nodeQuery);
    } catch (PathNotFoundException e) {
    }
  }

  /**
   * Get shared queries
   * Input:
   *    1. Add two shared query node
   *       1.1 queryName = "QueryAll1", statement = "Select * from nt:base", language = "sql",
   *           permissions = { "*:/platform/administrators" }, cachedResult = false,
   *           repository = "repository"
   *       1.2 queryName = "QueryAll2", statement = "//element(*, exo:article)", language = "xpath",
   *           permissions = { "*:/platform/users" }, cachedResult = true, repository = "repository"
   * Input:
   *    repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 2, contains QueryAll1 and QueryAll2 node
   * Input:
   *    userId = "root", repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 2, contains QueryAll1 and QueryAll2 node
   * Input:
   *    userId = "marry", repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 1, contains QueryAll2
   * Input:
   *    queryType = "sql", userId = "root", repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 1, contains QueryAll1
   * Input:
   *    queryType = "sql", userId = "marry", repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 0
   * Input:
   *    queryType = "xpath", userId = "marry", repository = "repository", provider = sessionProvider
   * Expect:
   *    Size of listNode = 1, contains QueryAll2
   * @throws Exception
   */
  public void testGetSharedQueries() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addSharedQuery("QueryAll1", "Select * from nt:base", "sql",
        new String[] { "*:/platform/administrators" }, false, REPO_NAME, sessionProvider);
    queryService.addSharedQuery("QueryAll2", "//element(*, exo:article)", "xpath",
        new String[] { "*:/platform/users" }, true, REPO_NAME, sessionProvider);
    // Test getSharedQueries(String repository, SessionProvider provider)
    List<Node> listQuery = queryService.getSharedQueries(sessionProvider);
    assertEquals(listQuery.size(), 5);
//    Node queryNode1 = listQuery.get(1);
//    assertEquals(queryNode1.getName(), "QueryAll1");
//    assertEquals(queryNode1.getProperty("jcr:language").getString(), "sql");
//    assertEquals(queryNode1.getProperty("jcr:statement").getString(), "Select * from nt:base");
//    assertEquals(queryNode1.getProperty("exo:cachedResult").getBoolean(), false);
//    assertEquals(queryNode1.getProperty("exo:accessPermissions").getValues()[0].getString(),
//        "*:/platform/administrators");
//
//    Node queryNode2 = listQuery.get(0);
//    assertEquals(queryNode2.getName(), "QueryAll2");
//    assertEquals(queryNode2.getProperty("jcr:language").getString(), "xpath");
//    assertEquals(queryNode2.getProperty("jcr:statement").getString(), "//element(*, exo:article)");
//    assertEquals(queryNode2.getProperty("exo:cachedResult").getBoolean(), true);
//    assertEquals(queryNode2.getProperty("exo:accessPermissions").getValues()[0].getString(),
//        "*:/platform/users");

    // Test getSharedQueries(String userId, String repository, SessionProvider
    // provider)
//    listQuery = queryService.getSharedQueries(REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 5);

//    listQuery = queryService.getSharedQueries("marry", REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 1);
//    assertEquals(listQuery.get(0).getName(), "QueryAll2");
//    assertEquals(listQuery.get(0).getProperty("jcr:language").getString(), "xpath");
//    assertEquals(listQuery.get(0).getProperty("jcr:statement").getString(),
//        "//element(*, exo:article)");
//    assertEquals(listQuery.get(0).getProperty("exo:cachedResult").getBoolean(), true);
//    assertEquals(listQuery.get(0).getProperty("exo:accessPermissions").getValues()[0].getString(),
//        "*:/platform/users");
    // Test getSharedQueries(String queryType, String userId, String repository,
    // SessionProvider provider)
//    listQuery = queryService.getSharedQueries("sql", "root", REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 1);
//    assertEquals(listQuery.get(0).getName(), "QueryAll1");
//    assertEquals(listQuery.get(0).getProperty("jcr:language").getString(), "sql");
//    assertEquals(listQuery.get(0).getProperty("jcr:statement").getString(), "Select * from nt:base");
//    assertEquals(listQuery.get(0).getProperty("exo:cachedResult").getBoolean(), false);
//    assertEquals(listQuery.get(0).getProperty("exo:accessPermissions").getValues()[0].getString(),
//        "*:/platform/administrators");
//    listQuery = queryService.getSharedQueries("sql", "marry", REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 0);
//    listQuery = queryService.getSharedQueries("xpath", "marry", REPO_NAME, sessionProvider);
//    assertEquals(listQuery.size(), 1);
//    assertEquals(listQuery.get(0).getName(), "QueryAll2");
//    assertEquals(listQuery.get(0).getProperty("jcr:language").getString(), "xpath");
//    assertEquals(listQuery.get(0).getProperty("jcr:statement").getString(),
//        "//element(*, exo:article)");
//    assertEquals(listQuery.get(0).getProperty("exo:cachedResult").getBoolean(), true);
//    assertEquals(listQuery.get(0).getProperty("exo:accessPermissions").getValues()[0].getString(),
//        "*:/platform/users");
    Node queriesHome = (Node) sessionProvider.getSession(DMSSYSTEM_WS, repository).getItem(baseQueriesPath);
    queriesHome.getNode("QueryAll1").remove();
    queriesHome.getNode("QueryAll2").remove();
    queriesHome.save();
  }

  /**
   * Execute query by giving the following params : queryPath, workspace, repository,
   * provider, userId
   * Input:
   *    1. Add two shared query node
   *       1.1 queryName = "QueryAll1", statement =
   *           "Select * from nt:base where jcr:path like '/exo:ecm/queries/%'", language = "sql",
   *           permissions = { "*:/platform/administrators" }, cachedResult = false,
   *           repository = "repository"
   *       1.2 queryName = "QueryAll2", statement = "//element(*, exo:article)", language = "xpath",
   *           permissions = { "*:/platform/users" }, cachedResult = true, repository = "repository"
   * Input:
   *    queryPath = "/exo:ecm/queries/QueryAll1", workspace = DMSSYSTEM_WS,
   *    repository = "repository", provider = sessionProvider, userId = "root"
   * Expect:
   *    Size of list node = 2
   * Input:
   *    queryPath = "/exo:ecm/queries/QueryAll2", workspace = DMSSYSTEM_WS,
   *    repository = "repository", provider = sessionProvider, userId = "root"
   * Expect:
   *    exception: Query Path not found!
   * @throws Exception
   */
  public void testExecute() throws Exception {
    SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    queryService.addSharedQuery("QueryAll1",
        "Select * from nt:base where jcr:path like '/exo:ecm/queries/%'", "sql",
        new String[] { "*:/platform/administrators" }, false, REPO_NAME, sessionProvider);
    queryService.addSharedQuery("QueryAll2", "//element(*, exo:article)", "xpath",
        new String[] { "*:/platform/users" }, true, REPO_NAME, sessionProvider);
    String queryPath = baseQueriesPath + "/QueryAll1";
    QueryResult queryResult = queryService.execute(queryPath, DMSSYSTEM_WS, REPO_NAME,
        sessionProvider, "root");
    assertEquals(queryResult.getNodes().getSize(), 5);

    Node queriesHome = (Node) sessionProvider.getSession(DMSSYSTEM_WS, repository).getItem(baseQueriesPath);
    queriesHome.getNode("QueryAll1").remove();
    queriesHome.getNode("QueryAll2").remove();
    queriesHome.save();
  }

  public void tearDown() throws Exception {
    try {
      Session mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(COLLABORATION_WS, repository);
      Node nodeUser = (Node) mySession.getItem(baseUserPath);
      NodeIterator iter = nodeUser.getNodes();
      while (iter.hasNext()) {
        try {
          Node node = iter.nextNode().getNode(relativePath);
          node.remove();
        } catch (Exception ex) {}
      }
      mySession.save();

//      mySession = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
//      Node nodeQueryHome = (Node) mySession.getItem(baseQueriesPath);
//      iter = nodeQueryHome.getNodes();
//      while (iter.hasNext()) {
//      	iter.nextNode().remove();
//      }
//      mySession.save();
    } catch (PathNotFoundException e) {
    }
    super.tearDown();
  }
}
