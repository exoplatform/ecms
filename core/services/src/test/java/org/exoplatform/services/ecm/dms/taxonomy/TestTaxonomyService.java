/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.taxonomy;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyNodeAlreadyExistsException;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 14, 2009
 */
public class TestTaxonomyService extends BaseDMSTestCase {

  private TaxonomyService      taxonomyService;

  private String                       definitionPath;

  private String                       storagePath;

  private LinkManager                  linkManage;

  private Session                      dmsSesssion;

  private NodeHierarchyCreator nodeHierarchyCreator;
  
  private MockTaxonomyService mockTaxonomyService;

  public void setUp() throws Exception {
    super.setUp();
    taxonomyService = (TaxonomyService) container.getComponentInstanceOfType(TaxonomyService.class);
    mockTaxonomyService = (MockTaxonomyService) container.getComponentInstanceOfType(MockTaxonomyService.class);
    dmsSesssion = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
    nodeHierarchyCreator = (NodeHierarchyCreator) container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    linkManage = (LinkManager)container.getComponentInstanceOfType(LinkManager.class);
    definitionPath =  nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_DEFINITION_PATH);
    storagePath =  nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
  }


  /**
   *  Test method TaxonomyService.addTaxonomyPlugin()
   *  @see {@link # testInit()}
   */
  public void testAddTaxonomyPlugin() throws Exception {
  }

  /**
   *  Test method TaxonomyService.init()
   *  Expect: Create system taxonomy tree in dms-system
   *  @see {@link # testInit()}
   */
  public void testInit() throws Exception {
    Node systemTreeDef = (Node) dmsSesssion.getItem(definitionPath + "/System");
    Node systemTreeStorage = (Node) dmsSesssion.getItem(storagePath + "/System");
    assertNotNull(systemTreeDef);
    assertNotNull(systemTreeStorage);
    assertEquals(systemTreeStorage, linkManage.getTarget(systemTreeDef, true));
  }

  /**
   *  Test method TaxonomyService.getTaxonomyTree(String repository, String taxonomyName, boolean system)
   *  Expect: return System tree
   * @throws Exception
   */
  public void testGetTaxonomyTree1() throws Exception {
    Node systemTree = taxonomyService.getTaxonomyTree(REPO_NAME, "System");
    assertNotNull(systemTree);
  }

  /**
   *  Test method TaxonomyService.addTaxonomyTree(Node taxonomyTree)
   *  Input: add Doc as root tree node, get taxonomy tree with name = Doc
   *  Expect: return Doc node
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testAddTaxonomyTree1() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Doc", "root");
    Node docTree = (Node)session.getItem("/MyDocuments/Doc");
    taxonomyService.addTaxonomyTree(docTree);
    assertTrue(dmsSesssion.itemExists(definitionPath + "/Doc"));
    Node definitionDocTree = (Node)dmsSesssion.getItem(definitionPath + "/Doc");
    assertEquals(docTree, linkManage.getTarget(definitionDocTree, true));
  }

  /**
   * Test method TaxonomyService.addTaxonomyTree(Node taxonomyTree) with one tree has already existed
   * Input: add 2 tree node with same name
   * Expect: Fire TaxonomyAlreadyExistsException
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   */
  public void testAddTaxonomyTree2() throws RepositoryException, TaxonomyNodeAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Doc", "root");
    Node docTree = (Node)session.getItem("/MyDocuments/Doc");
    try {
      taxonomyService.addTaxonomyTree(docTree);
      taxonomyService.addTaxonomyTree(docTree);
    } catch(TaxonomyAlreadyExistsException e) {
    }
  }

  /**
   *  Test method TaxonomyService.getTaxonomyTree(String repository, String taxonomyName)
   *  Input: Create taxonomy tree Music
   *  Expect: Node Music
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testGetTaxonomyTree2() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Music", "root");
    Node musicTree = (Node)session.getItem("/MyDocuments/Music");
    taxonomyService.addTaxonomyTree(musicTree);
    assertTrue(dmsSesssion.itemExists(definitionPath + "/Music"));
    Node musicTreeDefinition = (Node)dmsSesssion.getItem(definitionPath + "/Music");
    assertEquals(musicTree, linkManage.getTarget(musicTreeDefinition, true));
  }

  /**
   *  Test method TaxonomyService.getAllTaxonomyTrees(String repository)
   *  Expect: return one tree in repository
   * @throws Exception
   */
  public void testGetAllTaxonomyTrees2() throws Exception {
    assertEquals(1, taxonomyService.getAllTaxonomyTrees(REPO_NAME).size());
  }

  /**
   *  Test method TaxonomyService.getTaxonomyTree(String repository, String taxonomyName)
   *  Input: Add 2 tree Miscellaneous and Shoes, remove Miscellaneous tree
   *  Expect: Node Miscellaneous is remove in definition and real path
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testRemoveTaxonomyTree() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Miscellaneous", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Miscellaneous", "Shoes", "root");
    Node miscellaneous = (Node)session.getItem("/MyDocuments/Miscellaneous");
    taxonomyService.addTaxonomyTree(miscellaneous);
    assertTrue(dmsSesssion.itemExists(definitionPath + "/Miscellaneous"));
    taxonomyService.removeTaxonomyTree("Miscellaneous");
    assertFalse(dmsSesssion.itemExists(definitionPath + "/Miscellaneous"));
    assertFalse(dmsSesssion.itemExists("/MyDocuments/Miscellaneous"));
  }

  /**
   *  Test method TaxonomyService.getAllTaxonomyTrees(String repository, boolean system)
   *  Input: Add 2 taxonomy tree
   *  Output: size of tree increase 2 trees
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testGetAllTaxonomyTrees1() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Champion Leage", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Europa", "root");
    Node championLeague = (Node)session.getItem("/MyDocuments/Europa");
    Node europa = (Node)session.getItem("/MyDocuments/Champion Leage");
    int totalTree1 = taxonomyService.getAllTaxonomyTrees(REPO_NAME).size();
    taxonomyService.addTaxonomyTree(championLeague);
    taxonomyService.addTaxonomyTree(europa);
    int totalTree2 = taxonomyService.getAllTaxonomyTrees(true).size();
    assertEquals(2, totalTree2 - totalTree1);
  }

  /**
   *  Test method TaxonomyService.hasTaxonomyTree(String repository, String taxonomyName)
   *  Input: Add one tree: Primera Liga
   *  Expect: Return 2 taxonomy tree: System and Primera Liga tree
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testHasTaxonomyTree() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Primera Liga", "root");
    taxonomyService.addTaxonomyTree((Node)session.getItem("/MyDocuments/Primera Liga"));
    assertTrue(taxonomyService.hasTaxonomyTree("System"));
    assertTrue(taxonomyService.hasTaxonomyTree("Primera Liga"));
  }

  /**
   * Test method TaxonomyService.addTaxonomyNode()
   * Input: Add one taxonomy node below MyDocument
   * Expect: One node with primary node type = exo:taxonomy in path MyDocument/
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws RepositoryException
   */
  public void testAddTaxonomyNode1() throws TaxonomyNodeAlreadyExistsException, RepositoryException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Sport", "root");
    Node taxonomyNode = (Node)session.getItem("/MyDocuments/Sport");
    assertTrue(taxonomyNode.isNodeType("exo:taxonomy"));
  }

  /**
   *  Test method TaxonomyService.addTaxonomyNode() throws TaxonomyNodeAlreadyExistsException when Already exist node
   *  Input: Add 2 taxonomy node below MyDocument path
   *  Ouput: TaxonomyNodeAlreadyExistsException
   * @throws RepositoryException
   */
  public void testAddTaxonomyNode2() throws RepositoryException {
    try {
      session.getRootNode().addNode("MyDocuments");
      session.save();
      taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Sport", "root");
      taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Sport", "root");
    } catch (TaxonomyNodeAlreadyExistsException e) {
    }
  }

  /**
   *  Test method TaxonomyService.removeTaxonomyNode()
   *  Input: Add and remove Tennis node
   *  Expect: Node Tennis has not existed
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   */
  public void testRemoveTaxonomyNode() throws RepositoryException, TaxonomyNodeAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Tennis", "root");
    taxonomyService.removeTaxonomyNode(REPO_NAME, COLLABORATION_WS, "/MyDocuments/Tennis");
    assertFalse(session.itemExists("/MyDocuments/Tennis"));
  }

  /**
   *  Test method TaxonomyService.moveTaxonomyNode()
   *  Input: Move node to other place
   *  Output: Node is moved
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   */
  public void testMoveTaxonomyNode() throws RepositoryException, TaxonomyNodeAlreadyExistsException  {
    session.getRootNode().addNode("MyDocuments");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Serie", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Budesliga", "root");
    taxonomyService.moveTaxonomyNode(REPO_NAME, COLLABORATION_WS, "/MyDocuments/Serie", "/Serie", "cut");
    taxonomyService.moveTaxonomyNode(REPO_NAME, COLLABORATION_WS, "/MyDocuments/Budesliga", "/Budesliga", "copy");
    assertFalse(session.itemExists("/MyDocuments/Serie"));
    assertTrue(session.itemExists("/Serie"));
    assertTrue(session.itemExists("/Budesliga"));
    assertTrue(session.itemExists("/MyDocuments/Budesliga"));
  }

  /**
   *  Test method TaxonomyService.addCategory()
   *  Input: Add category for article node
   *  Expect: one node with primary type exo:taxonomyLink is create in taxonomy tree
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */

  public void testAddCategory() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Serie", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Serie", "A", "root");
    Node rootTree = (Node)session.getItem("/MyDocuments/Serie");
    session.save();
    taxonomyService.addTaxonomyTree(rootTree);
    taxonomyService.addCategory(article, "Serie", "A", true);
    Node link = (Node)session.getItem("/MyDocuments/Serie/A/Article");
    assertTrue(link.isNodeType("exo:taxonomyLink"));
    assertEquals(article, linkManage.getTarget(link));
  }

  /**
   *  Test method TaxonomyService.addCategories()
   *  Input: add 2 categories in article node
   *  Output: create 2 exo:taxonomyLink in each category
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testAddCategories() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Serie", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Serie", "A", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Serie", "B", "root");
    Node rootTree = (Node)session.getItem("/MyDocuments/Serie");
    taxonomyService.addTaxonomyTree(rootTree);
    taxonomyService.addCategories(article, "Serie", new String[] {"A", "B"}, true);
    Node link1 = (Node)session.getItem("/MyDocuments/Serie/A/Article");
    Node link2 = (Node)session.getItem("/MyDocuments/Serie/B/Article");
    assertTrue(link1.isNodeType("exo:taxonomyLink"));
    assertEquals(article, linkManage.getTarget(link1));
    assertTrue(link2.isNodeType("exo:taxonomyLink"));
    assertEquals(article, linkManage.getTarget(link2));
  }

  /**
   *  Test method TaxonomyService.hasCategories()
   *  Input: Add categories for article node
   *  Expect: return true with category is added
   *          return false with category that is not added
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testHasCategories() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Budesliga", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Serie", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Serie", "A", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Serie", "B", "root");
    Node rootTree1 = (Node)session.getItem("/MyDocuments/Serie");
    Node rootTree2 = (Node)session.getItem("/MyDocuments/Budesliga");
    taxonomyService.addTaxonomyTree(rootTree1);
    taxonomyService.addTaxonomyTree(rootTree2);
    taxonomyService.addCategories(article, "Serie", new String[] {"A", "B"}, true);
    assertTrue(mockTaxonomyService.hasCategories(article, "Serie", true));
    assertFalse(mockTaxonomyService.hasCategories(article, "Budesliga", true));
  }

  /**
   * Test method TaxonomyService.getCategories()
   * Input: Add 2 categories to article node
   * Expect: Return 2 node of categories
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testGetCategories() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Stories", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Stories", "Homorous", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Stories", "Fairy", "root");
    Node rootTree = (Node)session.getItem("/MyDocuments/Stories");
    taxonomyService.addTaxonomyTree(rootTree);
    taxonomyService.addCategories(article, "Stories", new String[] {"Homorous", "Fairy"}, true);
    List<Node> lstNode = mockTaxonomyService.getCategories(article, "Stories", true);
    Node taxoLink1 = (Node)session.getItem("/MyDocuments/Stories/Homorous");
    Node taxoLink2 = (Node)session.getItem("/MyDocuments/Stories/Fairy");
    assertEquals(2, lstNode.size());
    assertTrue(lstNode.contains(taxoLink1));
    assertTrue(lstNode.contains(taxoLink2));
  }

  /**
   * Test method TaxonomyService.getAllCategories()
   * Input: Add 3 categories to node
   * Expect: return 3 categories
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testGetAllCategories() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Culture", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "News", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/News", "Politics", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Culture", "Foods", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Culture", "Art", "root");
    Node rootTree1 = (Node)session.getItem("/MyDocuments/Culture");
    Node rootTree2 = (Node)session.getItem("/MyDocuments/News");
    taxonomyService.addTaxonomyTree(rootTree1);
    taxonomyService.addTaxonomyTree(rootTree2);
    taxonomyService.addCategories(article, "Culture", new String[] {"Foods", "Art"}, true);
    taxonomyService.addCategory(article, "News", "Politics", true);
    List<Node> lstNode = mockTaxonomyService.getAllCategories(article, true);
    Node taxoLink1 = (Node)session.getItem("/MyDocuments/Culture/Foods");
    Node taxoLink2 = (Node)session.getItem("/MyDocuments/Culture/Art");
    Node taxoLink3 = (Node)session.getItem("/MyDocuments/News/Politics");
    assertEquals(3, lstNode.size());
    assertTrue(lstNode.contains(taxoLink1));
    assertTrue(lstNode.contains(taxoLink2));
    assertTrue(lstNode.contains(taxoLink3));
  }

  /**
   *  Test method TaxonomyService.removeCategory()
   *  Input: Remove categories of Article node
   *  Expect: Return empty list
   * @throws RepositoryException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws TaxonomyAlreadyExistsException
   */
  public void testRemoveCategory() throws RepositoryException, TaxonomyNodeAlreadyExistsException, TaxonomyAlreadyExistsException {
    session.getRootNode().addNode("MyDocuments");
    Node article = session.getRootNode().addNode("Article");
    session.save();
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "Education", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments", "News", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/Education", "Language", "root");
    taxonomyService.addTaxonomyNode(COLLABORATION_WS, "/MyDocuments/News", "Weather", "root");
    Node rootTree1 = (Node)session.getItem("/MyDocuments/Education");
    taxonomyService.addTaxonomyTree(rootTree1);
    Node rootTree2 = (Node)session.getItem("/MyDocuments/News");
    taxonomyService.addTaxonomyTree(rootTree2);
    taxonomyService.addCategory(article, "Education", "Language", true);
    taxonomyService.addCategory(article, "News", "Weather", true);
    List<Node> lstNode = mockTaxonomyService.getAllCategories(article, true);
    assertEquals(2, lstNode.size());
    taxonomyService.removeCategory(article, "Education", "Language", true);
    lstNode = mockTaxonomyService.getAllCategories(article, true);
    assertEquals(1, lstNode.size());
    taxonomyService.removeCategory(article, "News", "Weather", true);
    lstNode = mockTaxonomyService.getAllCategories(article, true);
    assertEquals(0, lstNode.size());
  }

  public void tearDown() throws Exception {
    List<Node> lstNode = taxonomyService.getAllTaxonomyTrees(true);
    for(Node tree : lstNode) {
      if (!tree.getName().equals("System"))
        taxonomyService.removeTaxonomyTree(tree.getName());
    }
    for (String s : new String[] {"/Article","/MyDocuments"})
      if (session.itemExists(s)) {
      session.getItem(s).remove();
      session.save();
    }
    super.tearDown();
  }
}



