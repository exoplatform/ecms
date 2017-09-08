/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.ecm.dms.folksonomy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 19, 2009
 * 9:54:25 AM
 */
public class TestNewFolksonomyService extends BaseWCMTestCase {

  private static final String TEST = "test";
  private static final String TEST2 = "test2";
  private static final String EXO_TOTAL = "exo:total";
  private static final String[] groups = {"/platform/users", "/platform/guests"};

  private NewFolksonomyService newFolksonomyService_;
  private LinkManager linkManager;
  private NodeHierarchyCreator      nodeHierarchyCreator;
  private Node test, test2;
  private Node folksonomyNode;
  private Node groupAFolksonomyNode;
  private Node groupBFolksonomyNode;
  private Node publicFolksonomyNode;
  private Node siteFolksonomyNode;
  private DataDistributionType dataDistributionType;

  public void setUp() throws Exception {
    super.setUp();
    newFolksonomyService_ = (NewFolksonomyService) container.getComponentInstanceOfType(NewFolksonomyService.class);
    this.nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
    applyUserSession("john", "gtn", COLLABORATION_WS);
//    String userName = session.getUserID();
    Node root = session.getRootNode();
    Node applicationData = root.hasNode("Application Data") ?
                           root.getNode("Application Data") :
                           root.addNode("Application Data");
    Node tagsNode = applicationData.hasNode("Tags") ?
                    applicationData.getNode("Tags") :
                    applicationData.addNode("Tags");
    Node rootNode = root.hasNode("Users") ? root.getNode("Users") : root.addNode("Users");
//    Node userNode = rootNode.hasNode(userName) ? rootNode.getNode(userName) :
//                                                  rootNode.addNode(userName);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    Node groupsNode = root.hasNode("Groups") ? root.getNode("Groups") :
                                                    root.addNode("Groups");
    Node platformNode = groupsNode.hasNode("platform") ? groupsNode.getNode("platform") :
                                                          groupsNode.addNode("platform");
    Node privateNode = userNode.hasNode("Private") ? userNode.getNode("Private") :
                                                     userNode.addNode("Private");
//    NodeHierarchyCreator nodehierarchyCreator = (NodeHierarchyCreator) container
//    .getComponentInstanceOfType(NodeHierarchyCreator.class);

    String folksonomyPath = "Folksonomy";
    folksonomyNode = privateNode.hasNode(folksonomyPath) ? privateNode.getNode(folksonomyPath) :
                                                                privateNode.addNode(folksonomyPath);
    rootNode = privateNode;
    test = rootNode.addNode(TEST);
    test2 = rootNode.addNode(TEST2);

    int count = 0;

    String[] groups = {"/users", "/guests"};
    for (String gg : groups) {
      String g = gg.substring(1);
      Node groupNode = platformNode.hasNode(g) ? platformNode.getNode(g) :
                                                   platformNode.addNode(g);
      Node appData = groupNode.hasNode("ApplicationData") ? groupNode.getNode("ApplicationData") :
                                                            groupNode.addNode("ApplicationData");
      Node tag = appData.hasNode("Tags") ? appData.getNode("Tags") : appData.addNode("Tags");
      if (count ++ == 0) groupAFolksonomyNode = tag;
      else groupBFolksonomyNode = tag;
    }

    publicFolksonomyNode = tagsNode;
    session.save();
    dataDistributionType = newFolksonomyService_.getDataDistributionType();
    
    String site = "portal1";
    Node siteTags = root.hasNode("SiteTags") ?
                    root.getNode("SiteTags") :
                    root.addNode("SiteTags");
    siteFolksonomyNode = siteTags.addNode(site);
    session.save();
    
  }

  /**
   * Test Method: addPrivateTag()
   * Input: Node 'test'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test'
   * Expected Result:
   *        in folksonomy node of current user, 2 folder 'sport' and 'weather' appear;
   *        in folder 'sport' there must be a symlink to node 'test'
   *        in folder 'weather' there must be a symlink to node 'test'   *
   *        property 'exo:total' of 'sport' node must be 1
   *        property 'exo:total' of 'weather' node must be 1   *
   */
  
  public void testAddPrivateTag() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    //-------get sportTagNode and weatherTagNode----------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    Node weatherTagNode = null;
    try {
      weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //----------------------------------------------------------
    assertNotNull("testAddPrivateTag failed! ", sportTagNode);
    assertNotNull("testAddPrivateTag failed! ", weatherTagNode);

    Node link = sportTagNode.getNodes().nextNode();
    Node targetNode = linkManager.getTarget(link);
    assertTrue("testAddPrivateTag failed! ", test.isSame(targetNode));

    link = weatherTagNode.getNodes().nextNode();
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddPrivateTag failed! ", test.isSame(targetNode));

    assertEquals("testAddPrivateTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
    assertEquals("testAddPrivateTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());
  }

  /**
   * Test Method: addGroupsTag()
   * Input: Node 'test'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in group 'a' and 'b'
   * Expected Result:
   *        in folksonomy node of group 'a' user, 2 folder 'sport' and 'weather' appear;
   *        in folksonomy node of group 'b' user, 2 folder 'sport' and 'weather' appear;
   *        in folder 'sport' there must be a symlink to node 'test'
   *        in folder 'weather' there must be a symlink to node 'test'   *
   *        property 'exo:total' of 'sport' node must be 1
   *        property 'exo:total' of 'weather' node must be 1   *
   */
  public void testAddGroupsTag() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);

    //--------------------------TEST FOR GROUP A----------------------------------
    //-------get sportTagNode and weatherTagNode----------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(groupAFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    Node weatherTagNode = null;
    try {
      weatherTagNode = dataDistributionType.getDataNode(groupAFolksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //----------------------------------------------------------
    
    assertNotNull("testAddGroupsTag failed! ", sportTagNode);
    assertNotNull("testAddGroupsTag failed! ", weatherTagNode);
    
    Node link = sportTagNode.getNodes().nextNode();
    Node targetNode = linkManager.getTarget(link);
    assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));

    link = weatherTagNode.getNodes().nextNode();
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));

    assertEquals("testAddGroupsTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
    assertEquals("testAddGroupsTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());
    //--------------------------TEST FOR GROUP B----------------------------------
    //-------get sportTagNode and weatherTagNode----------------
    try {
      sportTagNode = dataDistributionType.getDataNode(groupBFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    try {
      weatherTagNode = dataDistributionType.getDataNode(groupBFolksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //----------------------------------------------------------
    
    assertNotNull("testAddGroupsTag failed! ", sportTagNode);
    assertNotNull("testAddGroupsTag failed! ", weatherTagNode);

    link = sportTagNode.getNodes().nextNode();
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));

    link = weatherTagNode.getNodes().nextNode();
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));

    assertEquals("testAddGroupsTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
    assertEquals("testAddGroupsTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());
  }

  /**
   * Test Method: addPublicTag()
   * Input: Node 'test'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in public
   * Expected Result:
   *        in public folksonomy node of group 'a' user, 2 folder 'sport' and 'weather' appear;
   *        in folder 'sport' there must be a symlink to node 'test'
   *        in folder 'weather' there must be a symlink to node 'test'   *
   *        property 'exo:total' of 'sport' node must be 1
   *        property 'exo:total' of 'weather' node must be 1   *
   */
  public void testAddPublicTag() throws Exception {
    String[] tags = { "sport", "weather" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    
    //---------------get sportTagNode and weatherTagNode---------------------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    Node weatherTagNode = null;
    try {
      weatherTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //-------------------------------------------------------------------------------
    assertNotNull("testAddPublicTag failed! ", sportTagNode);
    assertNotNull("testAddPublicTag failed! ", weatherTagNode);

    Node link = sportTagNode.getNodes().nextNode();
    Node targetNode = linkManager.getTarget(link);
    assertTrue("testAddPublicTag failed! ", test.isSame(targetNode));

    link = weatherTagNode.getNodes().nextNode();
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddPublicTag failed! ", test.isSame(targetNode));

    assertEquals("testAddPublicTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
    assertEquals("testAddPublicTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());
  }

  /**
   * Test Method: addSiteTag()
   * Input: Node 'test'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in site 'portal1'
   * Expected Result:
   *        in folksonomy node of site 'portal1' user, 2 folder 'sport' and 'weather' appear;
   *        in folder 'sport' there must be a symlink to node 'test'
   *        in folder 'weather' there must be a symlink to node 'test'   *
   *        property 'exo:total' of 'sport' node must be 1
   *        property 'exo:total' of 'weather' node must be 1   *
   */
  public void testAddSiteTag() throws Exception {
    String[] tags = { "sport", "weather" };
    String site = "portal1";
    Node root = session.getRootNode();
    Node siteTags = root.hasNode("SiteTags") ?
                    root.getNode("SiteTags") :
                    root.addNode("SiteTags");
    Node test = nodeHierarchyCreator.getUserNode(sessionProvider, "john").getNode("Private").addNode("a");
    session.save();
    siteFolksonomyNode = siteTags.addNode(site);
    session.save();
    newFolksonomyService_.addSiteTag(site,
                                     tags,
                                     test,
                                     COLLABORATION_WS);
    siteFolksonomyNode = siteTags.getNode(site);
    //----------------------get sportTagNode and weatherTagNode------------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(siteFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    Node weatherTagNode = null;
    try {
      weatherTagNode = dataDistributionType.getDataNode(siteFolksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //---------------------------------------------------------------------------
    assertNotNull("testAddSiteTag failed! ", sportTagNode);
    assertNotNull("testAddSiteTag failed! ", weatherTagNode);

    Node link = sportTagNode.getNode("a");
    Node targetNode = linkManager.getTarget(link);
    assertTrue("testAddSiteTag failed! ", test.isSame(targetNode));

    link = weatherTagNode.getNode("a");
    targetNode = linkManager.getTarget(link);
    assertTrue("testAddSiteTag failed! ", test.isSame(targetNode));

    assertEquals("testAddSiteTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
    assertEquals("testAddSiteTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());
  }

  public void testGetAllDocumentsByOneTagAndPath() throws Exception {
    String[] twoTags = { "sport", "weather" };
    String[] seaTag = { "sea" };
    String user = session.getUserID();

    newFolksonomyService_.addPrivateTag(twoTags, test, COLLABORATION_WS, user);
    newFolksonomyService_.addPrivateTag(twoTags, test2, COLLABORATION_WS, user);
    Node test3 = session.getRootNode().addNode("test3");
    newFolksonomyService_.addPrivateTag(seaTag, test3, COLLABORATION_WS, user);

    Node seaTagNode = dataDistributionType.getDataNode(folksonomyNode, "sea");
    Node weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");

    assertNotNull("'sea' tag node is null", seaTagNode);
    assertNotNull("'weather' tag node is null", weatherTagNode);

    String seaTagPath = seaTagNode.getPath();
    String weatherTagPath = weatherTagNode.getPath();

    Set<String> tagPaths = new HashSet<>();
    tagPaths.add(seaTagPath);
    List<Node> docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test3.isSame(node));
    }
    assertEquals("Documents count linked to tag 'sea' is wrong", 1, docs.size());

    tagPaths.clear();
    tagPaths.add(weatherTagPath);

    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test.isSame(node) || test2.isSame(node));
    }
    assertEquals("Documents count linked to tag 'weather' is wrong", 2, docs.size());
  }

  public void testGetAllDocumentsByTwoTagsAndPath() throws Exception {
    String[] twoTags = { "sport", "weather" };
    String[] seaTag = { "sea", "sport" };

    String user = session.getUserID();

    newFolksonomyService_.addPrivateTag(twoTags, test, COLLABORATION_WS, user);
    newFolksonomyService_.addPrivateTag(twoTags, test2, COLLABORATION_WS, user);
    Node test3 = session.getRootNode().addNode("test3");
    newFolksonomyService_.addPrivateTag(seaTag, test3, COLLABORATION_WS, user);

    Node seaTagNode = dataDistributionType.getDataNode(folksonomyNode, "sea");
    Node sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    Node weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");

    assertNotNull("'sea' tag node is empty ", seaTagNode);
    assertNotNull("'sport' tag node is empty ", sportTagNode);
    assertNotNull("'weather' tag node is empty", weatherTagNode);

    String seaTagPath = seaTagNode.getPath();
    String weatherTagPath = weatherTagNode.getPath();
    String sportTagPath = sportTagNode.getPath();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Set<String> tagPaths = new HashSet<>();
    tagPaths.add(sportTagPath);
    tagPaths.add(weatherTagPath);

    List<Node> docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test.isSame(node) || test2.isSame(node));
    }
    assertEquals("Returned documents count by method getAllDocumentsByTagsAndPath is wrong", 2, docs.size());

    tagPaths.clear();
    tagPaths.add(sportTagPath);
    tagPaths.add(seaTagPath);
    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test3.isSame(node));
    }
    assertEquals("Returned documents count by method getAllDocumentsByTagsAndPath is wrong ", 1, docs.size());

    tagPaths.clear();
    tagPaths.add(seaTagPath);
    tagPaths.add(weatherTagPath);
    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test3.isSame(node));
    }
    assertEquals("Returned documents count by method getAllDocumentsByTagsAndPath is wrong ", 0, docs.size());
  }

  public void testGetAllDocumentsByTagsAndDifferentPaths() throws Exception {
    String[] twoTags = { "sport", "weather" };

    String user = session.getUserID();

    newFolksonomyService_.addPrivateTag(twoTags, test, COLLABORATION_WS, user);
    newFolksonomyService_.addPrivateTag(twoTags, test2, COLLABORATION_WS, user);

    Node test3 = session.getRootNode().addNode("test3");
    newFolksonomyService_.addPrivateTag(twoTags, test3, COLLABORATION_WS, user);

    Node sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    Node weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");

    assertNotNull("'sport' tag node is empty ", sportTagNode);
    assertNotNull("'weather' tag node is empty", weatherTagNode);

    String weatherTagPath = weatherTagNode.getPath();
    String sportTagPath = sportTagNode.getPath();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Set<String> tagPaths = new HashSet<>();
    tagPaths.add(sportTagPath);
    tagPaths.add(weatherTagPath);

    List<Node> docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test.isSame(node) || test2.isSame(node) || test3.isSame(node));
    }
    assertEquals("Returned documents count by method getAllDocumentsByTagsAndPath is wrong", 3, docs.size());

    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/Users", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test.isSame(node) || test2.isSame(node));
    }
    assertEquals("Returned documents count by method getAllDocumentsByTagsAndPath is wrong ", 2, docs.size());

    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/Users/notexistingpath",
                                                              tagPaths,
                                                              COLLABORATION_WS,
                                                              sessionProvider);
    assertEquals("No document should be returned by using a non existing path parameter", 0, docs.size());
  }

  public void testGetAllDocumentsByTagsAndPathWithThreeTags() throws Exception {
    String[] twoTags = { "sport", "weather" };
    String[] twoOtherTags = { "sport", "sea" };
    String user = session.getUserID();

    newFolksonomyService_.addPrivateTag(twoTags, test, COLLABORATION_WS, user);
    newFolksonomyService_.addPrivateTag(twoTags, test2, COLLABORATION_WS, user);
    Node test3 = session.getRootNode().addNode("test3");
    newFolksonomyService_.addPrivateTag(twoOtherTags, test3, COLLABORATION_WS, user);

    Node weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");
    Node seaTagNode = dataDistributionType.getDataNode(folksonomyNode, "sea");

    assertNotNull("'sea' tag node is null", seaTagNode);
    assertNotNull("'weather' tag node is null", weatherTagNode);

    String seaTagPath = seaTagNode.getPath();
    String weatherTagPath = weatherTagNode.getPath();

    Set<String> tagPaths = new HashSet<>();
    tagPaths.add(seaTagPath);

    List<Node> docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    for (Node node : docs) {
      assertTrue(test3.isSame(node));
    }
    assertEquals("No document is linked to tag 'sea'", 1, docs.size());

    // Test with tags 'sea' and 'weather'
    tagPaths.add(weatherTagPath);
    docs = newFolksonomyService_.getAllDocumentsByTagsAndPath("/", tagPaths, COLLABORATION_WS, sessionProvider);
    assertEquals("No document should be linked to tags 'sea' and 'weather' at the same time", 0, docs.size());
  }

  /**
   * Test Method: getAllDocumentsByTag()
   * Input: Nodes 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' and 'test2' of current user
   *               get all documents by tag 'sport'
   * Expected Result:
   *              'test' and 'test2'
   */
  
  public void testGetAllDocumentsByTag() throws Exception {
    String[] tags = { "sport", "weather" };
    String user = session.getUserID();

    newFolksonomyService_.addPrivateTag(tags,
                                       test,
                                       COLLABORATION_WS,
                                       user);
    newFolksonomyService_.addPrivateTag(tags,
                                       test2,
                                       COLLABORATION_WS,
                                       user);
    //----------------------get sportTagNode and weatherTagNode------------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    Node weatherTagNode = null;
    try {
      weatherTagNode = dataDistributionType.getDataNode(folksonomyNode, "weather");
    } catch (PathNotFoundException e) {
      weatherTagNode = null;
    }
    //---------------------------------------------------------------------------
    
    assertNotNull("testGetAllDocumentsByTag failed! ", sportTagNode);
    assertNotNull("testGetAllDocumentsByTag failed! ", weatherTagNode);

    int count = 0;
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    List<Node> docs = newFolksonomyService_.getAllDocumentsByTag(sportTagNode.getPath(), COLLABORATION_WS, sessionProvider);
    for(Node node : docs) {
      if (test.isSame(node)) count ++;
      else if (test2.isSame(node)) count ++;
    }
    assertEquals("testGetAllDocumentsByTag failed! ", 2, count);
    assertEquals("testGetAllDocumentsByTag failed! ", 2L, sportTagNode.getProperty(EXO_TOTAL).getLong());
  }

  /**
   * Test Method: getAllGroupTagsOfManyRoles()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in group 'a' and 'b'
   *               add 2 tags 'sport' and 'music' for node 'test2' in group 'a' and 'b'
   *               get all tags of groups 'a' and 'b'
   * Expected Result:
   *               'sport', 'weather', 'music' of a
   *               'sport', 'weather', 'music' of b
   *               total 6
   */
  public void testGetAllGroupTagsOfManyRoles() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "sport", "music" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<Node> groupTags = newFolksonomyService_.getAllGroupTags(groups, COLLABORATION_WS);
    int count = 0;
    for (Node tag : groupTags) {
      if ("sport".equals(tag.getName())) count ++;
      if ("weather".equals(tag.getName())) count ++;
      if ("music".equals(tag.getName())) count ++;
    }

    assertEquals("testGetAllGroupTagsOfManyRoles failed! ", 6, count);
  }

  /**
   * Test Method: getAllGroupTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in group 'a' and 'b'
   *               add 2 tags 'sport' and 'music' for node 'test2' in group 'a' and 'b'
   *               get all tags of groups 'a'
   * Expected Result:
   *               'sport', 'weather', 'music'
   */
  
  public void testGetAllGroupTags() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "sport", "music" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<Node> groupTags = newFolksonomyService_.getAllGroupTags("/platform/users", COLLABORATION_WS);
    int count = 0;
    for (Node tag : groupTags) {
      if ("sport".equals(tag.getName())) count ++;
      if ("weather".equals(tag.getName())) count ++;
      if ("music".equals(tag.getName())) count ++;
    }

    assertEquals("testGetAllGroupTags failed! ", 3, count);
  }

  /**
   * Test Method: testGetAllPrivateTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' of current user
   *               add 2 tags 'sport' and 'xyz' for node 'test2' of current user
   *               get all private tags of current user
   * Expected Result:
   *               'sport', 'weather', 'xyz'
   */
  
  public void testGetAllPrivateTags() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    String[] tags2 = {"sport", "xyz"};
    newFolksonomyService_.addPrivateTag(tags2, test2, COLLABORATION_WS, session.getUserID());

    List<Node> tagList = newFolksonomyService_.getAllPrivateTags(session.getUserID());
    int count = 0;
    for (Node node : tagList) {
      if ("sport".equals(node.getName())) count ++;
      if ("xyz".equals(node.getName())) count ++;
      if ("weather".equals(node.getName())) count ++;
    }
    assertEquals("testGetAllPrivateTags failed! ", 3, count);
  }

  /**
   * Test Method : getAllPublicTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in public
   *               add 3 tags 'sport', 'boy', 'girl' for node 'test2' in public
   *               get all public tags
   * Expected Result:
   *               sport, weather, boy, girl
   */
  public void testGetAllPublicTags() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);
    List<Node> tagList = newFolksonomyService_.getAllPublicTags(publicFolksonomyTreePath, COLLABORATION_WS);
    int count = 0;
    for (Node tag : tagList) {
      if ("sport".equals(tag.getName())) count ++;
      if ("weather".equals(tag.getName())) count ++;
      if ("boy".equals(tag.getName())) count ++;
      if ("girl".equals(tag.getName())) count ++;
    }

    assertEquals("testGetAllPublicTags failed!", 4, count);
  }

  /**
   * Test Method : getAllSiteTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in site 'portal1'
   *               add 3 tags 'sport', 'boy', 'girl' for node 'test2' in site 'portal1'
   *               get all tags of site 'portal1'
   * Expected Result:
   *               sport, weather, boy, girl
   */
  public void testGetAllSiteTags() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String site = "portal1";
    Node root = session.getRootNode();
    Node siteTags = root.hasNode("SiteTags") ?
                    root.getNode("SiteTags") :
                    root.addNode("SiteTags");

    siteFolksonomyNode = siteTags.addNode(site);
    session.save();
    
    newFolksonomyService_.addSiteTag(site,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addSiteTag(site,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);
    List<Node> tagList = newFolksonomyService_.getAllSiteTags(site, COLLABORATION_WS);
    int count = 0;
    for (Node tag : tagList) {
      if ("sport".equals(tag.getName())) count ++;
      if ("weather".equals(tag.getName())) count ++;
      if ("boy".equals(tag.getName())) count ++;
      if ("girl".equals(tag.getName())) count ++;
    }
    assertEquals("testGetAllSiteTags failed!", 4, count);
  }

  /**
   * Test Method : modifyTagName()
   * Input: Node 'test',
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in public
   *               rename tag 'sport' to 'football'
   *               get all public tags
   * Expected Result:
   *               get all public tags -> football, weather
   *               node 'football' must have a symlink child which points to 'test' node
   */
  public void testModifyTagName() throws Exception {
    String[] tags = { "sport", "weather" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    Node sportNode = dataDistributionType.getDataNode(publicFolksonomyNode, "sport");
    Node football = newFolksonomyService_.modifyPublicTagName(sportNode.getPath()  , "football", 
                                                              COLLABORATION_WS, publicFolksonomyTreePath);
    //----------------------get footballTagNode, newSportTagNode--------------------------
    Node footballTagNode = null;
    try {
      footballTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "football");
    } catch (PathNotFoundException e) {
      footballTagNode = null;
    }    
    Node newSportTagNode = null;
    try {
      newSportTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      newSportTagNode = null;
    }
    //------------------------------------------------------------------------------------
    assertNotNull("testModifyTagName failed! ", footballTagNode);
    assertTrue("testModifyTagName failed! ", footballTagNode.isSame(football));
    assertNull("testModifyTagName failed! ", newSportTagNode);

    assertTrue("testModifyTagName failed! ", test.isSame(
        linkManager.getTarget(football.getNodes().nextNode())));
  }

  /**
   * Test Method : removeTag()
   * Input: Node 'test',
   * Test action: add 3 tags 'sport', 'nobita' and 'weather' for node 'test' in public
   *               remove  tag 'sport'
   *               get all public tags
   * Expected Result:
   *               'nobita', 'weather'
   */
  public void testRemoveTag() throws Exception {
    String[] tags = { "sport", "weather", "nobita"};
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    //-------------------------get sportTagNode-------------------------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    //------------------------------------------------------------------------

    newFolksonomyService_.removeTag(sportTagNode.getPath(), COLLABORATION_WS);

    //-------------------------get sportTagNode-------------------------------
    sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(publicFolksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    //------------------------------------------------------------------------
    assertNull("testRemoveTag failed! ", sportTagNode);

    List<Node> tagList = newFolksonomyService_.getAllPublicTags(publicFolksonomyTreePath, COLLABORATION_WS);
    int count = 0;
    for (Node tag : tagList) {
      if ("nobita".equals(tag.getName())) count ++;
      if ("weather".equals(tag.getName())) count ++;
    }
    assertEquals("testRemoveTag failed! ", 2, count);
  }

  /**
   * Test Method: removeTagOfDocument()
   * Input: Node 'test'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' of current user
   *               remove tag 'sport' of current user
   *               get all symlinks of tags 'sport'
   * Expected Result:
   *               none
   */
  public void testRemoveTagOfDocument() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());

    //--------------get sportTagNode----------------
    Node sportTagNode = null;
    try {
      sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    //----------------------------------------------
    newFolksonomyService_.removeTagOfDocument(sportTagNode.getPath(), test, COLLABORATION_WS);

    //--------------get sportTagNode----------------
    try {
      sportTagNode = dataDistributionType.getDataNode(folksonomyNode, "sport");
    } catch (PathNotFoundException e) {
      sportTagNode = null;
    }
    //----------------------------------------------
    assertNull("test removeTagOfDocument failed!", sportTagNode);
  }
  /**
   * Clean data test
   */

  public void testGetAllTagNamesWithPublicScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);

    List<String> tagListPublic = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, NewFolksonomyService.PUBLIC, publicFolksonomyTreePath);

    assertTrue(tagListPublic.contains("sport"));
    assertTrue(tagListPublic.contains("weather"));
    assertTrue(tagListPublic.contains("boy"));
    assertTrue(tagListPublic.contains("girl"));

    assertEquals("getAllTagNames failed!", 4, tagListPublic.size());
  }

  public void testGetAllTagNamesWithPrivateScope() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    String[] tags2 = {"sport", "xyz"};
    newFolksonomyService_.addPrivateTag(tags2, test2, COLLABORATION_WS, session.getUserID());

    List<String> tagList = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, NewFolksonomyService.PRIVATE, session.getUserID());

    assertTrue(tagList.contains("sport"));
    assertTrue(tagList.contains("weather"));
    assertTrue(tagList.contains("xyz"));
    assertEquals("getAllTagNames failed! ", 3, tagList.size());
  }

  public void testGetAllTagNamesWithGroupScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "sport", "music" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<String> groupTags = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, NewFolksonomyService.GROUP, "/platform/users;/platform/guests");

    assertTrue(groupTags.contains("sport"));
    assertTrue(groupTags.contains("weather"));
    assertTrue(groupTags.contains("music"));
    assertEquals("getAllTagNames failed! ", 6, groupTags.size());
  }

  public void testGetAllTagNamesWithGroupScope2() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "sport", "music" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<String> groupTags = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, NewFolksonomyService.GROUP, "/platform/users");

    assertTrue(groupTags.contains("sport"));
    assertTrue(groupTags.contains("weather"));
    assertTrue(groupTags.contains("music"));
    assertEquals("getAllTagNames failed! ", 3, groupTags.size());
  }

  public void testGetAllTagNamesWithSiteScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String site = "portal1";
    newFolksonomyService_.addSiteTag(site,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addSiteTag(site,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);
    List<String> tagList = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, NewFolksonomyService.SITE, site);

    assertTrue(tagList.contains("sport"));
    assertTrue(tagList.contains("weather"));
    assertTrue(tagList.contains("boy"));
    assertTrue(tagList.contains("girl"));
    assertEquals("getAllTagNames failed!", 4, tagList.size());
  }

  public void testGetAllTagNamesWithNoneExistingScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String site = "portal1";
    newFolksonomyService_.addSiteTag(site,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addSiteTag(site,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);
    List<String> tagList = newFolksonomyService_.getAllTagNames(COLLABORATION_WS, 10, site);

    assertFalse(tagList.contains("sport"));
    assertFalse(tagList.contains("weather"));
    assertFalse(tagList.contains("boy"));
    assertFalse(tagList.contains("girl"));
    assertEquals("getAllTagNames failed!", 0, tagList.size());
  }

  public void testCanEditTag1() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);

    List<String> memberships = new ArrayList<String>();
    memberships.add("/platform/users");
    memberships.add("/platform/guests");
    newFolksonomyService_.initTagPermissionListCache();
    assertFalse(newFolksonomyService_.canEditTag(0, memberships));

    memberships.clear();

    memberships.add("*:/platform/administrators");
    newFolksonomyService_.initTagPermissionListCache();
    assertTrue(newFolksonomyService_.canEditTag(0, memberships));

    memberships.clear();
    memberships.add("manager:/platform/administrators");
    newFolksonomyService_.initTagPermissionListCache();
    assertTrue(newFolksonomyService_.canEditTag(0, memberships));

    memberships.clear();
    memberships.add("/platform/administrators");
    assertTrue(newFolksonomyService_.canEditTag(1, memberships));
  }

  public void testCanEditTag2() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);

    List<String> memberships = new ArrayList<String>();
    memberships.add("/platform/administrators");

    String tagPermission = "*:/platform/administrators";

    newFolksonomyService_.initTagPermissionListCache();
    // 1 item from getTagPermissionList
    assertTrue("Wrong initial number of item in tag permission list", newFolksonomyService_.getTagPermissionList().size() == 1);

    newFolksonomyService_.removeTagPermission(tagPermission);
    assertTrue("Wrong number of item in tag permission list after removing " + tagPermission, newFolksonomyService_.getTagPermissionList() == null || newFolksonomyService_.getTagPermissionList().size() == 0);

    assertFalse(newFolksonomyService_.canEditTag(0, memberships));

    newFolksonomyService_.addTagPermission(tagPermission);
    assertTrue("Wrong tag permission list after restoring the configuration", tagPermission.equals(newFolksonomyService_.getTagPermissionList().get(0)));
  }

  
  public void testGetLinkedTagsOfDocumentByScopeWithPrivateScope() throws Exception {
    String[] tags = { "sport", "weather" };
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    String[] tags2 = {"sport", "xyz"};
    newFolksonomyService_.addPrivateTag(tags2, test2, COLLABORATION_WS, session.getUserID());

    List<Node> linkedTags = newFolksonomyService_
        .getLinkedTagsOfDocumentByScope(NewFolksonomyService.PRIVATE, session.getUserID(), test, COLLABORATION_WS);
    List<String> tagNames = getNodeNames(linkedTags);

    assertTrue(tagNames.contains("sport"));
    assertTrue(tagNames.contains("weather"));
    assertFalse(tagNames.contains("xyz"));
    assertEquals("getLinkedTagsOfDocumentByScope failed! ", 2, linkedTags.size());
  }

  public void testGetLinkedTagsOfDocumentByScopeWithPublicScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags,
                                       test,
                                       COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath,
                                       tags2,
                                       test2,
                                       COLLABORATION_WS);

    List<Node> linkedTags = newFolksonomyService_
        .getLinkedTagsOfDocumentByScope(NewFolksonomyService.PUBLIC, null, test, COLLABORATION_WS);
    List<String> tagNames = getNodeNames(linkedTags);

    assertTrue(tagNames.contains("sport"));
    assertTrue(tagNames.contains("weather"));
    assertFalse(tagNames.contains("boy"));
    assertFalse(tagNames.contains("girl"));

    assertEquals("getLinkedTagsOfDocumentByScope failed! ", 2, linkedTags.size());
  }
  
  public void testGetLinkedTagsOfDocumentByScopeWithGroupScope() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "sport", "music" };
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<Node> linkedTags = newFolksonomyService_
        .getLinkedTagsOfDocumentByScope(NewFolksonomyService.GROUP, "/platform/users", test, COLLABORATION_WS);
    List<String> tagNames = getNodeNames(linkedTags);

    assertTrue(tagNames.contains("sport"));
    assertTrue(tagNames.contains("weather"));
    assertFalse(tagNames.contains("music"));
    assertEquals("getLinkedTagsOfDocumentByScope failed! ", 2, tagNames.size());
  }

  public void testGetLinkedTagsOfDocument() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, tags, test, COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, tags2, test2, COLLABORATION_WS);
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    newFolksonomyService_.addPrivateTag(tags2, test2, COLLABORATION_WS, session.getUserID());
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    List<Node> linkedTags = newFolksonomyService_.getLinkedTagsOfDocument(test, COLLABORATION_WS);
    List<String> tagNames = getNodeNames(linkedTags);

    assertTrue(tagNames.contains("sport"));
    assertTrue(tagNames.contains("weather"));
    assertFalse(tagNames.contains("boy"));
    assertFalse(tagNames.contains("girl"));
    assertFalse(tagNames.contains("music"));
    assertEquals("getLinkedTagsOfDocument failed! ", 8, tagNames.size());
  }
  
  public void testRemoveTagsOfNodeRecursively() throws Exception {
    String[] tags = { "sport", "weather" };
    String[] tags2 = { "boy", "girl", "sport" };
    String publicFolksonomyTreePath = "/Application Data/Tags";
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, tags, test, COLLABORATION_WS);
    newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, tags2, test2, COLLABORATION_WS);
    newFolksonomyService_.addPrivateTag(tags, test, COLLABORATION_WS, session.getUserID());
    newFolksonomyService_.addPrivateTag(tags2, test2, COLLABORATION_WS, session.getUserID());
    newFolksonomyService_.addGroupsTag(tags, test, COLLABORATION_WS, groups);
    newFolksonomyService_.addGroupsTag(tags2, test2, COLLABORATION_WS, groups);

    newFolksonomyService_.removeTagsOfNodeRecursively(test, COLLABORATION_WS, session.getUserID(), "/platform/users;/platform/guests");

    List<Node> linkedTags = newFolksonomyService_.getLinkedTagsOfDocument(test, COLLABORATION_WS);
    List<String> tagNames = getNodeNames(linkedTags);
    assertEquals("removeTagsOfNodeRecursively failed! ", 0, tagNames.size());
  }

  public void testManipulateTagStyle() throws Exception {
    String normal = "normal";
    String normalTagRange = "0..2";
    String normalHtmlStyle = "font-size: 12px; font-weight: bold; color: #6b6b6b; font-family: verdana; text-decoration:none;";
    String interesting = "interesting";
    String interestingTagRange = "2..5";
    String interestingHtmlStyle = "font-size: 13px; font-weight: bold; color: #5a66ce; font-family: verdana; text-decoration:none;";
    String tagStylePath = "/jcr:system/exo:ecm/exo:folksonomies/exo:tagStyle";

    // Test addTagStyle
    newFolksonomyService_.addTagStyle(normal, normalTagRange, normalHtmlStyle, DMSSYSTEM_WS);
    assertEquals(normalHtmlStyle, newFolksonomyService_.getTagStyle(tagStylePath + "/" + normal, DMSSYSTEM_WS));

    // Test updateTagStyle
    newFolksonomyService_.updateTagStyle(normal, interestingTagRange, interestingHtmlStyle, DMSSYSTEM_WS);
    assertEquals(interestingHtmlStyle, newFolksonomyService_.getTagStyle(tagStylePath + "/" + normal, DMSSYSTEM_WS));

    // Test addTagStyle
    newFolksonomyService_.addTagStyle(interesting, interestingTagRange, interestingHtmlStyle, DMSSYSTEM_WS);
    List<String> tagStyleNames = getNodeNames(newFolksonomyService_.getAllTagStyle(DMSSYSTEM_WS));
    assertTrue(tagStyleNames.contains(normal));
    assertTrue(tagStyleNames.contains(interesting));
    assertFalse(tagStyleNames.contains("boy"));
    assertEquals("testManipulateTagStyle failed! ", 2, tagStyleNames.size());
  }

  private List<String> getNodeNames(List<Node> nodes) throws RepositoryException {
    List<String> nodeNames = new ArrayList<String>();
    for (Node node : nodes) {
      nodeNames.add(node.getName());
    }
    return nodeNames;
  }

  public void tearDown() throws Exception {
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, "john");
    String[] nodes = {"/Application Data/Tags",
                      "/Groups/platform/users/ApplicationData/Tags",
                      "/Groups/platform/guests/ApplicationData/Tags", "/SiteTags",
                      "/test","/test2", userNode.getNode("Private/" + TEST).getPath(), userNode.getNode("Private/" + TEST2).getPath(),
                      userNode.getNode("Private/Folksonomy").getPath(),
                      session.getUserID()};
    for (String node : nodes)
      if (session.itemExists(node)) {
        //System.out.println("Delete: -----------------------------" + node);
        Node n = (Node)session.getItem(node);
        n.remove();
        session.save();
      }

    Session dmsSession = sessionProvider.getSession(DMSSYSTEM_WS, repository);
    String tagStylesPath = "/jcr:system/exo:ecm/exo:folksonomies/exo:tagStyle";
    if (dmsSession.itemExists(tagStylesPath)) {
      Node tagStylesNode = (Node)dmsSession.getItem(tagStylesPath);
      for (NodeIterator iter = tagStylesNode.getNodes(); iter.hasNext();) {
        iter.nextNode().remove();
      }
    }
    dmsSession.save();
    dmsSession.logout();
    super.tearDown();
  }
}
