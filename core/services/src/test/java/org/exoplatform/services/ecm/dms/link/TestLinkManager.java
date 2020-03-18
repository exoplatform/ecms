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
package org.exoplatform.services.ecm.dms.link;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jun 9, 2009
 */

public class TestLinkManager extends BaseWCMTestCase {
  private LinkManager linkManager;
  private Session session;
  private Node rootNode;

  private final static String WORKSPACE = "exo:workspace";
  private final static String UUID = "exo:uuid";
  private final static String PRIMARY_TYPE = "exo:primaryType";
  
  private static final Log LOG = ExoLogger.getLogger(TestLinkManager.class.getName());

  /**
   * Set up for testing
   *
   * In Collaboration workspace
   *
   *  /---TestTreeNode
   *        |
   *        |_____A1
   *        |     |___A1_1
   *        |         |___A1_1_1
   *        |     |___A1_2
   *        |     |___A1_3
   *        |     |___A1_4
   *        |
   *        |_____B1
   *              |___B1_1
   *
   */
  public void setUp() throws Exception {
    super.setUp();
    linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
    applyUserSession("john", "gtn", COLLABORATION_WS);
    createTree();
  }

  public void createTree() throws Exception {
    session = sessionProviderService_.getSystemSessionProvider(null).getSession(COLLABORATION_WS, repository);
    rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("TestTreeNode");
    Node nodeA1 = testNode.addNode("A1");
    nodeA1.addNode("A1_1").addNode("A1_1_1");
    nodeA1.addNode("A1_2");
    nodeA1.addNode("A1_3");
    nodeA1.addNode("A1_4");

    testNode.addNode("B1").addNode("B1_1");
    session.save();
    assertNotNull(nodeA1);
  }

  /**
   * Creates a new link
   * Input:
   *    parent = nodeA1(TestTreeNode/A1), target = nodeB1_1(TestTreeNode/B1/B1_1)
   * Expect:
   *    node: name = B1_1, primaryNodeType = "exo:symlink", parent = nodeA1,
   *    value property of this node
   *        exo:workspace = COLLABORATION_WS, exo:uuid = uuid of nodeB1_1,
   *        exo:primaryType = primaryType of node B1_1
   *
   * Input:
   *    parent = nodeA1_1(TestTreeNode/A1/A1_1), linkType = exo:taxonomyLink,
   *    target = nodeB1_1(TestTreeNode/B1/B1_1)
   * Expect:
   *    node: name = "B1_1", primaryNodeType = "exo:taxonomyLink", parent = nodeA1_1,
   *    value property of this node
   *        exo:workspace = COLLABORATION_WS, exo:uuid = uuid of nodeB1_1,
   *        exo:primaryType = primaryType of node B1_1
   *
   * Input:
   *    parent = nodeA1_2(TestTreeNode/A1/A1_2), linkType = null,
   *    target = nodeB1_1(TestTreeNode/B1/B1_1)
   * Expect:
   *    node: name = "B1_1", primaryNodeType = "exo:symlink", parent = nodeA1_1,
   *    value property of this node
   *        exo:workspace = COLLABORATION_WS, exo:uuid = uuid of nodeB1_1,
   *        exo:primaryType = primaryType of node B1_1
   *
   * Input:
   *    parent = nodeA1_3(TestTreeNode/A1/A1_3), linkType = "exo:taxonomyLink", linkName = null,
   *    target = nodeB1_1(TestTreeNode/B1/B1_1)
   * Expect:
   *    node: name = "B1_1", primaryNodeType = "exo:taxonomyLink", parent = nodeA1_1,
   *    value property of this node
   *        exo:workspace = COLLABORATION_WS, exo:uuid = uuid of nodeB1_1,
   *        exo:primaryType = primaryType of node B1_1
   *
   * Input:
   *    parent = nodeA1_4(TestTreeNode/A1/A1_4), linkType = "exo:taxonomyLink",
   *    linkName = "A1_3_To_B1_1", target = nodeB1_1(TestTreeNode/B1/B1_1)
   * Expect:
   *    node: name = "A1_3_To_B1_1", primaryNodeType = "exo:taxonomyLink", parent = nodeA1_1,
   *    value property of this node
   *        exo:workspace = COLLABORATION_WS, exo:uuid = uuid of nodeB1_1,
   *        exo:primaryType = primaryType of node B1_1
   * @throws Exception
   */
  public void testCreateLink() throws Exception {
    System.out.println("================== Test Create Link  ==================");
//    Test method createLink(Node parent, Node target)
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);
    assertEquals(symlinkNodeA1.getName(), nodeB1_1.getName());
    assertEquals(symlinkNodeA1.getProperty(WORKSPACE).getString(), COLLABORATION_WS);
    assertEquals(symlinkNodeA1.getProperty(UUID).getString(), nodeB1_1.getUUID());
    assertEquals(symlinkNodeA1.getProperty(PRIMARY_TYPE).getString(), nodeB1_1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeA1.getPrimaryNodeType().getName(), "exo:symlink");

//    Test method createLink(Node parent, String linkType, Node target)
//    linkType = "exo:taxonomyLink"
    Node nodeA1_1 = rootNode.getNode("TestTreeNode/A1/A1_1");
    Node symlinkNodeA1_1 = linkManager.createLink(nodeA1_1, "exo:taxonomyLink", nodeB1_1);
    assertNotNull(symlinkNodeA1_1);
    assertEquals(symlinkNodeA1_1.getName(), nodeB1_1.getName());
    assertEquals(symlinkNodeA1_1.getProperty(WORKSPACE).getString(), COLLABORATION_WS);
    assertEquals(symlinkNodeA1_1.getProperty(UUID).getString(), nodeB1_1.getUUID());
    assertEquals(symlinkNodeA1_1.getProperty(PRIMARY_TYPE).getString(), nodeB1_1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeA1_1.getPrimaryNodeType().getName(), "exo:taxonomyLink");

//    Test method createLink(Node parent, String linkType, Node target)
//    linkType = null
    Node nodeA1_2 = rootNode.getNode("TestTreeNode/A1/A1_2");
    Node symlinkNodeA1_2 = linkManager.createLink(nodeA1_2, null, nodeB1_1);
    assertNotNull(symlinkNodeA1_2);
    assertEquals(symlinkNodeA1_2.getName(), nodeB1_1.getName());
    assertEquals(symlinkNodeA1_2.getProperty(WORKSPACE).getString(), COLLABORATION_WS);
    assertEquals(symlinkNodeA1_2.getProperty(UUID).getString(), nodeB1_1.getUUID());
    assertEquals(symlinkNodeA1_2.getProperty(PRIMARY_TYPE).getString(), nodeB1_1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeA1_2.getPrimaryNodeType().getName(), "exo:symlink");

//    Test method createLink(Node parent, String linkType, Node target, String linkName)
//    linkType = "exo:taxonomyLink"; linkName = null
    Node nodeA1_3 = rootNode.getNode("TestTreeNode/A1/A1_3");
    Node symlinkNodeA1_3 = linkManager.createLink(nodeA1_3, "exo:taxonomyLink", nodeB1_1, null);
    assertNotNull(symlinkNodeA1_3);
    assertEquals(symlinkNodeA1_3.getName(), nodeB1_1.getName());
    assertEquals(symlinkNodeA1_3.getProperty(WORKSPACE).getString(), COLLABORATION_WS);
    assertEquals(symlinkNodeA1_3.getProperty(UUID).getString(), nodeB1_1.getUUID());
    assertEquals(symlinkNodeA1_3.getProperty(PRIMARY_TYPE).getString(), nodeB1_1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeA1_3.getPrimaryNodeType().getName(), "exo:taxonomyLink");

//    Test method createLink(Node parent, String linkType, Node target, String linkName)
//    linkType = "exo:taxonomyLink"; linkName = "A1_3_To_B1_1"
    Node nodeA1_4 = rootNode.getNode("TestTreeNode/A1/A1_4");
    Node symlinkNodeA1_4 = linkManager.createLink(nodeA1_4, "exo:taxonomyLink", nodeB1_1, "A1_3_To_B1_1");
    assertNotNull(symlinkNodeA1_4);
    assertEquals(symlinkNodeA1_4.getName(), "A1_3_To_B1_1");
    assertEquals(symlinkNodeA1_4.getProperty(WORKSPACE).getString(), COLLABORATION_WS);
    assertEquals(symlinkNodeA1_4.getProperty(UUID).getString(), nodeB1_1.getUUID());
    assertEquals(symlinkNodeA1_4.getProperty(PRIMARY_TYPE).getString(), nodeB1_1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeA1_4.getPrimaryNodeType().getName(), "exo:taxonomyLink");
  }

  public void testisFileOrParentALink() throws Exception {
    System.out.println("================== Test is File Or Parent a Link  ==================");
    String path="";
    assertFalse(linkManager.isFileOrParentALink(session,path));
    path="test/test.link/test";
    assertFalse(linkManager.isFileOrParentALink(session,path));
    path="testA/testB/test.Docx";
    assertFalse(linkManager.isFileOrParentALink(session,path));
    path="/testing/Documents/aaaa/zzz/";
    assertFalse(linkManager.isFileOrParentALink(session,path));
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertTrue(linkManager.isFileOrParentALink(session,symlinkNodeA1.getPath()));
  }

  /**
   * Indicates whether the given item is a link
   * Input: Create a new link
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   * Input:
   *    item = symlinkNodeA1
   * Expect:
   *    result: true
   * Input
   *    item = nodeA1
   * Expect:
   *    result: false
   * @throws Exception
   */
  public void testIsLink() throws Exception {
    System.out.println("================== Test Is Link  ==================");
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);
    assertTrue(linkManager.isLink(symlinkNodeA1));
    assertFalse(linkManager.isLink(nodeA1));
  }

  /**
   * Gets the target node of the given link
   * Input: Create a new link (symlinkNodeA1)
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   * Input:
   *    link = nodeA1
   * Expect:
   *    exception: NodeA1 is not a symlink
   *
   * Input:
   *    link = symlinkNodeA1
   * Expect:
   *    node target: name = "B1_1" is not null,
   *
   * Input:
   *    link = symlinkNodeA1, system = true
   * Expect:
   *    node target: name = "B1_1" is not null,
   * @throws Exception
   */
  public void testGetTarget() throws Exception {
    System.out.println("================== Test Get Target  ==================");
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);

    try {
      linkManager.getTarget(nodeA1);
      fail("\nNode: " + nodeA1.getName() + " is not a symlink");
    } catch (Exception e) {
    }
    assertNotNull(linkManager.getTarget(symlinkNodeA1));
    assertEquals(linkManager.getTarget(symlinkNodeA1).getName(), nodeB1_1.getName());

    assertNotNull(linkManager.getTarget(symlinkNodeA1, true));
    assertEquals(linkManager.getTarget(symlinkNodeA1, true).getName(), nodeB1_1.getName());
  }

  /**
   * Checks if the target node of the given link can be reached using the user session
   * Input: Create a new link (symlinkNodeA1)
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   * Input:
   *    link = nodeA1
   * Expect:
   *    exception: NodeA1 is not a symlink node
   *
   * Input:
   *    link = symlinkNodeA1
   * Expect:
   *    result: true
   * @throws Exception
   */
  public void testIsTargetReachable() throws Exception {
    System.out.println("================== Test IsTargetReachable  ==================");
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);

    try {
      boolean isReachable = linkManager.isTargetReachable(nodeA1);
      assertFalse(isReachable);
      fail("\nNode: " + nodeA1.getName() + " is not a symlink node");
    } catch (Exception e) {
    }
    assertTrue(linkManager.isTargetReachable(symlinkNodeA1));
  }

  /**
   * Gives the primary node type of the target
   * Input: Create a new link (symlinkNodeA1)
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   * Input:
   *    link = symlinkNodeA1
   * Expect:
   *    property exo:primaryType = primaryNodeType of nodeB1_1
   * @throws Exception
   */
  public void testGetTargetPrimaryNodeType() throws Exception {
    System.out.println("================== Test GetTargetPrimaryNodeType  ==================");
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);
    assertEquals(linkManager.getTargetPrimaryNodeType(symlinkNodeA1), nodeB1_1.getPrimaryNodeType().getName());
  }

  /**
   * Updates the target node of the given link
   * Input: Create a new link (symlinkNodeA1)
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   * Input:
   *    link = symlinkNodeA1, target = nodeB1
   * Expect:
   *    node update is not null
   *    value property of this node
   *        exo:workspace = workspace of node B1, exo:uuid = uuid of nodeB1,
   *        exo:primaryType = primaryType of node B1
   * @throws Exception
   */
  public void testUpdateLink() throws Exception {
    System.out.println("================== Test Update Link  ==================");
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1 = rootNode.getNode("TestTreeNode/B1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNodeA1 = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNodeA1);

    Node symlinkNodeUpdate = linkManager.updateLink(symlinkNodeA1, nodeB1);
    assertNotNull(symlinkNodeUpdate);
    assertEquals(symlinkNodeUpdate.getName(), symlinkNodeA1.getName());
    assertEquals(symlinkNodeUpdate.getProperty(WORKSPACE).getString(), nodeB1.getSession().getWorkspace().getName());
    assertEquals(symlinkNodeUpdate.getProperty(UUID).getString(), nodeB1.getUUID());
    assertEquals(symlinkNodeUpdate.getProperty(PRIMARY_TYPE).getString(), nodeB1.getPrimaryNodeType().getName());
    assertEquals(symlinkNodeUpdate.getPrimaryNodeType().getName(), symlinkNodeA1.getPrimaryNodeType().getName());
  }
  
  /**
   * Updates the given symlink
   * Input: Create a new link (symlinkNodeA1)
   *    parent = nodeA1(TestTreeNode/A1), nodeB1_1(TestTreeNode/B1/B1_1)
   *    set nodeB1_1.exo:title = tileB1;
   *        nodeB1_1.exo:dateCreated = current Date;
   *        nodeB1_1.exo:dateModified = current Date + 1 day;
   *        nodeB1_1.publication:liveDate = current Date + 2 days;
   *        nodeB1_1.exo:startEvent = = current Date + 3 days
   *        nodeB1_1.exo:index = 100;
   * Input:
   *    link = symlinkNodeA1, target = nodeB1
   *    
   * Expect:
   *    node update is not null
   *    value property of this node
   *        symlink.exo:title = nodeB1_1.exo:title
   *        symlink.exo:dateCreated = nodeB1_1.exo:dateCreated
   *        symlink.exo:dateModified = nodeB1_1.exo:dateModified
   *        symlink.publication:liveDate = nodeB1_1.exo:title
   *        symlink.exo:index = nodeB1_1.exo:index
   * @throws Exception
   */
  public void testUpdateSymlink() throws Exception {
    LOG.info("==============Test update symlink================");
    
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeB1_1 = rootNode.getNode("TestTreeNode/B1/B1_1");
    Node symlinkNode = linkManager.createLink(nodeA1, nodeB1_1);
    assertNotNull(symlinkNode);
    symlinkNode.addMixin("exo:modify");
    symlinkNode.save();
    
    nodeB1_1.setProperty(NodetypeConstant.EXO_TITLE, "titleB1");
    nodeB1_1.setProperty(NodetypeConstant.EXO_DATE_CREATED, new GregorianCalendar());
    Calendar d = new GregorianCalendar();
    d.add(Calendar.DATE, 1);
    nodeB1_1.setProperty(NodetypeConstant.EXO_DATE_MODIFIED, d);
    nodeB1_1.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, d);
    d = new GregorianCalendar();
    d.add(Calendar.DATE, 2);
    nodeB1_1.setProperty(NodetypeConstant.PUBLICATION_LIVE_DATE, d);
    nodeB1_1.setProperty(NodetypeConstant.EXO_INDEX, 100);
    session.save();

    linkManager.updateSymlink(symlinkNode);

    assertEquals(symlinkNode.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate().getTime(),
                 nodeB1_1.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate().getTime());
    assertEquals(symlinkNode.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate().getTime(),
                 nodeB1_1.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate().getTime());
    assertEquals(symlinkNode.getProperty(NodetypeConstant.PUBLICATION_LIVE_DATE).getDate().getTime(),
                 nodeB1_1.getProperty(NodetypeConstant.PUBLICATION_LIVE_DATE).getDate().getTime());
    assertEquals(symlinkNode.getProperty(NodetypeConstant.EXO_INDEX).getLong(), 
                 nodeB1_1.getProperty(NodetypeConstant.EXO_INDEX).getLong());
  }

  public void tearDown() throws Exception {
    try {
      Node root = session.getRootNode();
      root.getNode("TestTreeNode").remove();
      root.save();
      session.save();
    } catch (PathNotFoundException e) {
    }
    super.tearDown();
  }
}
