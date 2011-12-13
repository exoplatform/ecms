package org.exoplatform.services.ecm.dms.documents;

import javax.jcr.Node;

import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class TestTrashService extends BaseDMSTestCase {

  final static public String EXO_RESTORE_LOCATION = "exo:restoreLocation";
  final static public String RESTORE_PATH = "exo:restorePath";
  final static public String RESTORE_WORKSPACE = "exo:restoreWorkspace";

  private SessionProvider sessionProvider;
  private TrashService trashService;

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    trashService = (TrashService)container.getComponentInstanceOfType(TrashService.class);
  }

  /**
   * {@inheritDoc}
   */
  public void tearDown() throws Exception {
//    Node trashNode = session.getRootNode().getNode("TrashNode");
//    trashNode.remove();
    session.logout();
  }

  /**
   * test method moveToTrash when node workspace and trash workspace are the same
   * input:     /TestNode/node0
   *         /TestNode/node1
   *         /TestNode/node2
   * tested action: move these 3 nodes to /TrashNode/
   * expectedValue : 0 (number of nodes remain in /TestNode/ )
   *            3 (number of nodes in /TrashNode/)
   *
   * @throws Exception
   */
  public void testMoveToTrashSameWorkspace() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    session.save();
    trashService.moveToTrash(node0, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);
    trashService.moveToTrash(node1, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);
    session.save();
    long testNodeChild = testNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();

    assertEquals("testMoveToTrashSameWorkspace failed!", 0, testNodeChild);
    assertEquals("testMoveToTrashSameWorkspace failed 2 !", 3, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
  }

  /**
   * test method moveToTrash when node workspace and trash workspace are different
   * input:     /TestNode/node0
   *         /TestNode/node1
   * tested action: move these 2 nodes to /TrashNode/
   * expectedValue : 0 (number of nodes remain in /TestNode/ )
   *            2 (number of nodes in /TrashNode/)
   *
   * @throws Exception
   */
  public void testMoveToTrashDifferentWorkspaces() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    session.save();
    session.save();
    trashService.moveToTrash(node0, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node1, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    session.save();
    session.save();
    long testNodeChild = testNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();

    assertEquals("testMoveToTrashDifferentWorkspace failed!", 0, testNodeChild);
    assertEquals("testMoveToTrashDefferentWorkspace failed 2 !", 2, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
    session.save();
  }

  /**
   * test method restoreFromTrash when node workspace and trash workspace are the same
   * input:     /TrashNode/node0
   *         /TrashNode/node1
   *         /TrashNode/node2
   *         /TrashNode/node3
   * tested action: restore these 4 nodes from /TrashNode/ to /TestNode
   * expectedValue : 0 (number of nodes remain in /TrashNode/ )
   *            4 (number of nodes in /TestNode/)
   *
   * @throws Exception
   */
  public void testRestoreFromTrashSameWorkspace() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    Node node3 = testNode.addNode("node3");
    session.save();
    trashService.moveToTrash(node0, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node1, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node3, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    session.save();

    System.out.println(trashNode.getNodes().getSize());
    System.out.println("Test restore:");

    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node0", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node1", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node2", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node3", "repository", sessionProvider);

    session.save();
    session.save();


    long testNodeChild =  testNode.getNodes().getSize();
    long trashNodeChild =  trashNode.getNodes().getSize();
    System.out.println(testNodeChild + " " + trashNodeChild);
    assertEquals("testRestoreFromTrashSameWorkspace failed 3!", 4, testNodeChild);
    assertEquals("testRestoreFromTrashSameWorkspace failed 4 !", 0, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
  }

  /**
   * test method restoreFromTrash when node workspace and trash workspace are different
   * input:     /TrashNode/node0
   *         /TrashNode/node1
   *         /TrashNode/node2
   *         /TrashNode/node3
   *           /TrashNode/node4
   * tested action: restore these 4 nodes from /TrashNode/ to /TestNode
   * expectedValue : 1 (number of nodes remain in /TrashNode/ )
   *            4 (number of nodes in /TestNode/)
   *
   * @throws Exception
   */
  public void testRestoreFromTrashDifferentWorkspaces() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    Node node3 = testNode.addNode("node3");
    Node node4 = testNode.addNode("node4");
//    String node0Path = node0.getPath();
//    String node1Path = node1.getPath();
//    String node2Path = node2.getPath();
//    String node3Path = node3.getPath();
//    String node4Path = node4.getPath();

    session.save();
    session.save();
    trashService.moveToTrash(node0, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node1, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node3, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node4, trashNode.getPath(), session.getWorkspace().getName(), "repository", sessionProvider);
    session.save();
    session.save();

    System.out.println(trashNode.getNodes().getSize());
    System.out.println("Test restore:");

    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node0", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node1", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node2", "repository", sessionProvider);
    trashService.restoreFromTrash(trashNode, trashNode.getPath() + "/node3", "repository", sessionProvider);

    session.save();
    session.save();


    long testNodeChild =  testNode.getNodes().getSize();
    long trashNodeChild =  trashNode.getNodes().getSize();
    System.out.println(testNodeChild + " " + trashNodeChild);
    assertEquals("testRestoreFromTrashDifferentWorkspace failed 3!", 4, testNodeChild);
    assertEquals("testRestoreFromTrashDifferentWorkspace failed 4 !", 1, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
    session.save();
  }

  /**
   * test method getAllNodeInTrash
   * input:     /TrashNode/node0
   *         /TrashNode/node0/node1
   *         /TrashNode/node2
   *         /TrashNode/node2/node5
   *           /TrashNode/node4
   * tested action: restore these nodes from /TrashNode/ to /TestNode
   * expectedValue : 0 (number of nodes remain in /TrashNode/)
   *            3 (number of nodes in /TestNode/, node0, node2 and node4)
   *
   * @throws Exception
   */
  public void testGetAllNodeInTrash() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    node0.addNode("node1");
    Node node2 = testNode.addNode("node2");
    node2.addNode("node5");
    Node node4 = testNode.addNode("node4");

    session.save();
    session.save();

    trashService.moveToTrash(node0, trashNode.getPath(), trashRootNode.getSession().getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), trashRootNode.getSession().getWorkspace().getName(), "repository", sessionProvider);
    trashService.moveToTrash(node4, trashNode.getPath(), trashRootNode.getSession().getWorkspace().getName(), "repository", sessionProvider);

    int count =
      trashService.getAllNodeInTrash(trashRootNode.getSession().getWorkspace().getName(), "repository", sessionProvider).size();

    session.save();
    session.save();

    System.out.println("count = " + count);
    assertEquals("testGetAllNodeInTrash failed!", 3, count);
    trashNode.remove();
    testNode.remove();
    session.save();
    session.save();
  }

  /**
   * test method getAllNodeInTrashByUser
   * input:     /TrashNode/node0
   *         /TrashNode/node0/node1
   *         /TrashNode/node2
   *         /TrashNode/node2/node5
   * tested action: restore these nodes from /TrashNode/ to /TestNode
   * expectedValue : 0 (number of nodes remain in /TrashNode/)
   *            2 (number of nodes in /TestNode/, node0 and node2)
   *
   * @throws Exception
   */
  public void testGetAllNodeInTrashByUser() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    node0.addNode("node1");
    Node node2 = testNode.addNode("node2");
    node2.addNode("node5");

    session.save();

    trashService.moveToTrash(node0, trashNode.getPath(), trashRootNode.getSession().getWorkspace().getName(), REPO_NAME, sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), trashRootNode.getSession().getWorkspace().getName(), REPO_NAME, sessionProvider);

    int count =
      trashService.getAllNodeInTrashByUser(trashRootNode.getSession().getWorkspace().getName(),
                  REPO_NAME,
                  sessionProvider, session.getUserID()).size();

    session.save();

//    assertEquals("testGetAllNodeInTrash failed!", 2, count);
    trashNode.remove();
    testNode.remove();
    session.save();
    session.save();
  }

  /**
   * test method isInTrash()
   * input:     /TestNode/node1
   *        /TestNode/node2
   *        /TestNode/node3
   * test action: move two nodes, node1 and node2, to /Trash/
   * expectedValue: node1 and node2 is in trash, <code>isInTrash()</code> return <code>true</code>.
   * And node3 is NOT in trash, <code>isInTrash()</code> return <code>false</code>
   * @throws Exception
   */
  public void testIsInTrash1() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();

    Node trashNode = trashRootNode.addNode("Trash");

    Node testNode = rootNode.addNode("testNode");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    testNode.addNode("node3");

    session.save();

    trashService.moveToTrash(node1, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);
    trashService.moveToTrash(node2, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);

    session.save();

    assertEquals(true, trashService.isInTrash(trashNode.getNode("node1")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node2")));
    assertEquals(false, trashService.isInTrash(testNode.getNode("node3")));

    trashNode.remove();
    testNode.remove();
    session.save();
  }

  /**
   * test method isInTrash()
   * input:     /TestNode/node0
   *        /TestNode/node0/node1
   *        /TestNode/node0/node2
   * test action: move the node0 to /Trash/
   * expectedValue: node0 is in /Trash/ and all child nodes of node0 (node1 and node2) are also in /Trash/
   * @throws Exception
   */
  public void testIsInTrash2() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();

    Node trashNode = trashRootNode.addNode("Trash");

    Node testNode = rootNode.addNode("testNode");
    Node node0 = testNode.addNode("node0");
    node0.addNode("node1");
    node0.addNode("node2");

    session.save();

    trashService.moveToTrash(node0, trashNode.getPath(), session.getWorkspace().getName(), REPO_NAME, sessionProvider);

    session.save();

    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0/node1")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0/node2")));

    trashNode.remove();
    testNode.remove();
    session.save();
  }

}
