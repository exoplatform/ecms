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
package org.exoplatform.services.ecm.dms.test;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Mar 15, 2009
 */
public class TestSymLink extends BaseDMSTestCase {

  MockNodeFinderImpl nodeFinder;

  /**
   * Set up for testing
   *
   * In Collaboration workspace
   *
   *  /---TestTreeNode
   *        |
   *        |_____A1
   *        |     |___C1___C1_1___C1_2___C1_3(exo:symlink -->C2)
   *        |     |___C2___C2_2___D(exo:symlink -->C3)
   *        |     |___C3___C4
   *        |
   *        |_____A2
   *        |     |___B2(exo:symlink --> C1)
   *        |
   *        |_____A3(exo:symlink --> C2)
   *
   * In System workspace
   *        /------TestTreeNode2
   *                    |
   *                    |_____M1___M2(exo:symlink --> C1)
   *                    |
   *                    |_____N1___N2(exo:symlink --> C2)
   *                    |
   *                    |_____O1(exo:symlink --> no node)
   *                    |
   *                    |_____P1
   *
   */
  public void setUp() throws Exception {
    System.out.println("========== Create root node  ========");
    super.setUp();
    nodeFinder = (MockNodeFinderImpl) container.getComponentInstanceOfType(MockNodeFinderImpl.class);
    createTreeInCollaboration();
    createTreeInSystem();
  }

  /**
   * Create tree in System workspace
   *
   * @throws Exception
   */
  public void createTreeInSystem() throws Exception {
//    Session session = repository.login(credentials, SYSTEM_WS);
    Session sessionSys = sessionProviderService_.getSystemSessionProvider(null).getSession(SYSTEM_WS, repository);
    Node rootNode = sessionSys.getRootNode();
    Node testNode = rootNode.addNode("TestTreeNode2");
    Node nodeM1 = testNode.addNode("M1");
    Node nodeN1 = testNode.addNode("N1");
    testNode.addNode("p1");
//    Session collaboSession = repository.login(credentials, COLLABORATION_WS);
    Node nodeC1 = (Node) session.getItem("/TestTreeNode/A1/C1");
    addSymlink("M2", nodeM1, nodeC1);
    Node nodeC2 = (Node) session.getItem("/TestTreeNode/A1/C2");
    addSymlink("N2", nodeN1, nodeC2);
    Node nodeO1 = testNode.addNode("O1","exo:symlink");
    nodeO1.setProperty("exo:workspace",COLLABORATION_WS);
    nodeO1.setProperty("exo:uuid", "12");
    nodeO1.setProperty("exo:primaryType", "nt:folder");
    /* Set node and properties for Node B1 */
    session.save();
  }

  public void addSymlink(String name, Node src, Node target) throws Exception {
    Node node = src.addNode(name,"exo:symlink");
    if (target.hasProperty("jcr:uuid")) {
      System.out.println("\n\n jcr uuid = " + target.getProperty("jcr:uuid").getValue());
    } else {
      target.addMixin("mix:referenceable");
    }
    node.setProperty("exo:workspace",COLLABORATION_WS);
    node.setProperty("exo:uuid",target.getProperty("jcr:uuid").getString());
    node.setProperty("exo:primaryType",target.getPrimaryNodeType().getName());

  }
  public void createTreeInCollaboration() throws Exception {
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("TestTreeNode");
    Node nodeA1 = testNode.addNode("A1");
    Node nodeA2 = testNode.addNode("A2");
    Node nodeB2 = nodeA2.addNode("B2","exo:symlink");

    Node nodeC1 = nodeA1.addNode("C1");
    Node nodeC1_1 = nodeC1.addNode("C1_1");
    Node nodeC1_2 = nodeC1_1.addNode("C1_2");
    Node nodeC1_3 = nodeC1_2.addNode("C1_3","exo:symlink");
    Node nodeC2 = nodeA1.addNode("C2");
    Node nodeC3 = nodeA1.addNode("C3");
    nodeC3.addNode("C4");
    Node nodeC2_2 = nodeC2.addNode("C2_2");
    Node nodeD = nodeC2_2.addNode("D", "exo:symlink");
    if (nodeC1.hasProperty("jcr:uuid")) {
      System.out.println("\n\n jcr uuid = " +nodeC1.getProperty("jcr:uuid").getValue());
    } else {
      nodeC1.addMixin("mix:referenceable");
      nodeC2.addMixin("mix:referenceable");
      nodeC3.addMixin("mix:referenceable");
    }
    nodeB2.setProperty("exo:workspace",COLLABORATION_WS);
    nodeB2.setProperty("exo:uuid",nodeC1.getProperty("jcr:uuid").getString());
    nodeB2.setProperty("exo:primaryType",nodeC1.getPrimaryNodeType().getName());
    nodeD.setProperty("exo:workspace",COLLABORATION_WS);
    nodeD.setProperty("exo:uuid",nodeC3.getProperty("jcr:uuid").getString());
    nodeD.setProperty("exo:primaryType",nodeC3.getPrimaryNodeType().getName());

    Node nodeA3 = testNode.addNode("A3","exo:symlink");
    nodeA3.setProperty("exo:workspace", COLLABORATION_WS);
    nodeA3.setProperty("exo:uuid", nodeC2.getProperty("jcr:uuid").getString());
    nodeA3.setProperty("exo:primaryType",nodeC2.getPrimaryNodeType().getName());

    nodeC1_3.setProperty("exo:workspace", COLLABORATION_WS);
    nodeC1_3.setProperty("exo:uuid", nodeC2.getProperty("jcr:uuid").getString());
    nodeC1_3.setProperty("exo:primaryType",nodeC2.getPrimaryNodeType().getName());
    System.out.println("Get path = " + nodeA3.getPath());

    /* Set node and properties for Node B1 */
    session.save();
  }

  /**
   * Browser tree of one node
   *
   * @param node
   */

  public void browserTree(Node node, int iLevel) throws RepositoryException {
    if (iLevel != 0) {
      for (int j = 0; j < iLevel; j++) {
        System.out.print("\t");
        System.out.print("|");
      }
      System.out.println("-------" + node.getName());
    } else
      System.out.println(node.getName());

    for (int j = 0; j < iLevel; j++) {
      System.out.print("\t");
      System.out.print("|");
    }
    System.out.print("\t");
    System.out.println("|");
    for (int j = 0; j < iLevel; j++) {
      System.out.print("\t");
      System.out.print("|");
    }
    System.out.print("\t");
    System.out.println("|");
    /* Get all nodes */
    NodeIterator iterNode = node.getNodes();
    /* Browser node */
    Node tempNode;
    /*
     * for(int j = 0; j < iLevel + 1; j++){ System.out.print("\t"); }
     */
    while (iterNode.hasNext()) {
      for (int j = 0; j < iLevel; j++) {
        System.out.print("\t");
        System.out.print("|");
      }
      System.out.print("\t");
      System.out.println("|");
      for (int j = 0; j < iLevel; j++) {
        System.out.print("\t");
        System.out.print("|");
      }
      System.out.print("\t");
      System.out.println("|");

      tempNode = iterNode.nextNode();
      this.browserTree(tempNode, iLevel + 1);
    }
  }

  public void testGetPath() throws Exception {
    String path = "/";
    String expectedPath = "/";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path out put: "+ node.getPath());
    assertEquals(expectedPath,node.getPath());
  }

  public void testGetPath1() throws Exception {
    String path = "/TestTreeNode";
    String expectedPath = "/TestTreeNode";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path out put: "+ node.getPath());
    assertEquals(expectedPath,node.getPath());
  }

  public void testGetPath2() throws Exception {
    String path = "/TestTreeNode/A2/B2";
    String expectedPath = "/TestTreeNode/A2/B2";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Item item = nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path output: "+ item.getPath());
    assertEquals(expectedPath,item.getPath());
  }

  public void testGetPath3() throws Exception {
    String path = "/TestTreeNode/A3/C2_2/D/C4";
    String expectedPath = "/TestTreeNode/A1/C3/C4";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path output: "+ node.getPath());
    assertEquals(expectedPath,node.getPath());
  }

  public void testGetPath4() throws Exception {
    String path = "/TestTreeNode/A2/B2/C1_1/C1_2/C1_3/C2_2/D";
    String expectedPath = "/TestTreeNode/A1/C2/C2_2/D";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path output: "+ node.getPath());
    assertEquals(expectedPath,node.getPath());
  }

  public void testGetPath5() throws Exception {
    String path = "/TestTreeNode/A1/C1/C1_1/C1_2/C1_3/C2_2/D/C4";
    String expectedPath = "/TestTreeNode/A1/C3/C4";
    System.out.println("\n\n Path input : " + path);
    System.out.println("\n\n expected Path : " + expectedPath);
    Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
    System.out.println("Path output: "+ node.getPath());
    assertEquals(expectedPath,node.getPath());
  }


//  public void testGetPathInOtherWorkspace1() throws Exception {
//      String path = "/TestTreeNode2/M1/M2/jcr:uuid";
//      String expectedPath = "/TestTreeNode/A1/C1/jcr:uuid";
//      System.out.println("\n\n Path input : " + path);
//      System.out.println("\n\n expected Path : " + expectedPath);
//      Item item = nodeFinder.getItemSys(REPO_NAME, SYSTEM_WS, path, true);
//      System.out.println("Path output: "+ item.getPath());
//      assertEquals(expectedPath, item.getPath());
//  }
//
//  public void testGetPathInOtherWorkspace2() throws Exception {
//      String path = "/TestTreeNode2/N1/N2/C2_2/D/C4";
//      String expectedPath = "/TestTreeNode/A1/C3/C4";
//      System.out.println("\n\n Path input : " + path);
//      System.out.println("\n\n expected Path : " + expectedPath);
//      Node node = (Node)nodeFinder.getItemSys(REPO_NAME, SYSTEM_WS, path, true);
//      System.out.println("Path output: "+ node.getPath());
//      assertEquals(expectedPath,node.getPath());
//  }
//
//  /**
//   * Test get path with target node is in other workspace
//   *
//   */
//
//  public void testGetPathInOtherWorkspace3() throws Exception {
//      String path = "/TestTreeNode2/M1/M2/C1_1/C1_2/C1_3/C2_2/D/C4";
//      String expectedPath = "/TestTreeNode/A1/C3/C4";
//      System.out.println("\n\n Path input : " + path);
//      System.out.println("\n\n expected Path : " + expectedPath);
//      Node node = (Node)nodeFinder.getItemSys(REPO_NAME, SYSTEM_WS, path, true);
//      System.out.println("Path output: "+ node.getPath());
//      assertEquals(expectedPath,node.getPath());
//  }

  public void testGetInvalidPath1() throws Exception {
    String path = "/TestTreeNode/A2/D";
    System.out.println("\n\n Path input : " + path);
    try {
        Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
        System.out.println("Path output: "+ node.getPath());
    } catch (PathNotFoundException e) {}
  }


  public void testGetInvalidPath2() throws Exception {
    String path = "/TestTreeNode/A2/B2/C2";
    System.out.println("\n\n Path input : " + path);
    try {
        Node node = (Node)nodeFinder.getItem(REPO_NAME, COLLABORATION_WS, path);
        System.out.println("Path output: "+ node.getPath());
    } catch (PathNotFoundException e) {}
  }

  /**
   * Test with target Node is remove: Throws PathNotFoundException
   */

  public void testGetInvalidPath3() throws Exception {
    String path = "/TestTreeNode2/O1";
    System.out.println("\n\n Path input : " + path);
    try {
        Node node = (Node) nodeFinder.getItem(REPO_NAME, SYSTEM_WS, path);
        System.out.println("Path output: "+ node.getPath());
    } catch (PathNotFoundException e) {}
  }

  public void testGetInvalidPath4() throws Exception {
  String path = "/TestTreeNode2/O1/C2_2";
  System.out.println("\n\n Path input : " + path);
    try {
      Node node = (Node) nodeFinder.getItemSys(REPO_NAME, SYSTEM_WS, path, true);
      System.out.println("Path output: " + node.getPath());
    } catch (PathNotFoundException e) {}
  }

  public void testGetInvalidWorkspace() throws Exception {
    try {
      Node node = (Node) nodeFinder.getItem(REPO_NAME, SYSTEM_WS + "12", "/");
      System.out.println("Path output: " + node.getPath());
    } catch (RepositoryException e) {}
  }

  public void testGetInvalidRepository() throws Exception {
    try {
      Node node = (Node) nodeFinder.getItem(REPO_NAME + "12", SYSTEM_WS, "/");
      System.out.println("Path output: " + node.getPath());
    } catch (RepositoryException e) {}
  }

  public void tearDown() throws Exception {
    Node root;
    try {
      System.out.println("\n\n -----------Teadown-----------------");
      root = session.getRootNode();
      root.getNode("TestTreeNode").remove();
      root.save();
      root = session.getRootNode();
      root.getNode("TestTreeNode2").remove();
      root.save();
    } catch (PathNotFoundException e) {
    }
    super.tearDown();
  }

}
