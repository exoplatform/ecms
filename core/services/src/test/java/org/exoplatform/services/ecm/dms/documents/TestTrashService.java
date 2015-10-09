package org.exoplatform.services.ecm.dms.documents;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.gatein.pc.api.PortletInvoker;
import org.mockito.Mockito;

public class TestTrashService extends BaseWCMTestCase {

  final static public String EXO_RESTORE_LOCATION = "exo:restoreLocation";
  final static public String RESTORE_PATH = "exo:restorePath";
  final static public String RESTORE_WORKSPACE = "exo:restoreWorkspace";
  final static public String MIX_REFERENCEABLE = "mix:referenceable";

  private TrashService trashService;

  public void setUp() throws Exception {
    super.setUp();
      ExoContainer manager = ExoContainerContext.getCurrentContainer();
      PortletInvoker portletInvoker = Mockito.mock(PortletInvoker.class);
      manager.addComponentToCtx(portletInvoker.hashCode(), portletInvoker);
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    trashService = (TrashService) container.getComponentInstanceOfType(TrashService.class);
    applySystemSession();
  }
  
  public void tearDown() throws Exception {
//    Node trashNode = session.getRootNode().getNode("TrashNode");
//    trashNode.remove();
    session.logout();
    super.tearDown();
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
    Node trashNode = rootNode.addNode("Trash");    
    Node testNode = rootNode.addNode("TestNode");
   
    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    //Add mix:referenceable into node to coverage code for the case relate to the SEO Service
    node0.addMixin(MIX_REFERENCEABLE);
    node1.addMixin(MIX_REFERENCEABLE);
    node2.addMixin(MIX_REFERENCEABLE);    
    session.save();
    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node1, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider, 0);
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
   * test method testMoveSymlinkToTrash when node workspace and trash workspace are the same
   * input:     /TestNode/node0 and taxonomy link "testSymlink" for node0        
   * tested action: move taxonomy link node to /TrashNode/
   * expectedValue : 0 (number of nodes remain in /TestNode/ )
   *                 2 (number of nodes in /TrashNode/)
   *
   * @throws Exception
   */
  public void testMoveSymlinkToTrash() throws Exception {
  	Node rootNode = session.getRootNode();
  	Node trashNode = rootNode.addNode("Trash");
  	Node testNode = rootNode.addNode("TestNode");
  	Node documentNode = testNode.addNode("Documents");
  	Node taxonomyNode = rootNode.addNode("exo:ecm").addNode("exo:taxonomyTrees").addNode("storage").addNode("System");
  	
  	Node node0 = documentNode.addNode("node0");
  	node0.addMixin(MIX_REFERENCEABLE);
  	LinkManager linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
    Node nodeLink = linkManager.createLink(taxonomyNode, "exo:taxonomyLink", node0, "testSymlink");
    session.save();
    trashService.moveToTrash(nodeLink, sessionProvider, 0);
    session.save();
    long testNodeChild = documentNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();
    assertEquals("testMoveSymlinkToTrash failed!", 0, testNodeChild);
    assertEquals("testMoveSymlinkToTrash failed 2 !", 2, trashNodeChild);  
    
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
    Node tagsNode = rootNode.addNode("tags");
    
    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    
    Session session2 = sessionProvider.getSession(DMSSYSTEM_WS, repository);    
    Node rootNode2 = session2.getRootNode();    
    Node testNode = rootNode2.addNode("TestNode");    
    Node node0 = testNode.addNode("node0");    
    Node node1 = testNode.addNode("node1");
    node0.addMixin(MIX_REFERENCEABLE);
    node1.addMixin(MIX_REFERENCEABLE);
    
    // Add public tag for node
    NewFolksonomyService newFolksonomyService = (NewFolksonomyService)container.getComponentInstanceOfType(NewFolksonomyService.class);
    String[] tags = {"wcm","ecms"};    
    newFolksonomyService.addPublicTag("/tags", tags, node0, COLLABORATION_WS);  
    newFolksonomyService.addPublicTag("/tags", tags, node1, COLLABORATION_WS); 
    session.save();
    session2.save();
    
    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node1, sessionProvider, 0);
    session.save();
    session2.save();
    long testNodeChild = testNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();

    assertEquals("testMoveToTrashDifferentWorkspace failed!", 0, testNodeChild);
    assertEquals("testMoveToTrashDefferentWorkspace failed 2 !", 2, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    tagsNode.remove();
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
    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node1, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider, 0);
    trashService.moveToTrash(node3, sessionProvider, 0);
    session.save();

    trashService.restoreFromTrash(trashNode.getPath() + "/node0", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node1", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node2", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node3", sessionProvider);

    session.save();
    long testNodeChild =  testNode.getNodes().getSize();
    long trashNodeChild =  trashNode.getNodes().getSize();
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
    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    
    Session session2 = sessionProvider.getSession(DMSSYSTEM_WS, repository);
    Node rootNode2 = session2.getRootNode();
    Node testNode = rootNode2.addNode("TestNode");

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    Node node3 = testNode.addNode("node3");
    Node node4 = testNode.addNode("node4");
    node0.addMixin(MIX_REFERENCEABLE);
    node1.addMixin(MIX_REFERENCEABLE);
    node2.addMixin(MIX_REFERENCEABLE);
    node3.addMixin(MIX_REFERENCEABLE);
    node4.addMixin(MIX_REFERENCEABLE);

    session.save();
    session2.save();
    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node1, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider, 0);
    trashService.moveToTrash(node3, sessionProvider, 0);
    trashService.moveToTrash(node4, sessionProvider, 0);
    session.save();
    session2.save();
    
    trashService.restoreFromTrash(trashNode.getPath() + "/node0", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node1", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node2", sessionProvider);
    trashService.restoreFromTrash(trashNode.getPath() + "/node3", sessionProvider);

    session.save();
    session2.save();
    long testNodeChild =  testNode.getNodes().getSize();
    long trashNodeChild =  trashNode.getNodes().getSize();
    assertEquals("testRestoreFromTrashDifferentWorkspace failed 3!", 4, testNodeChild);
    assertEquals("testRestoreFromTrashDifferentWorkspace failed 4 !", 1, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
    session.save();
  }
  /**
   * test method testRestoreSymlinkFromTrash when node workspace and trash workspace are same
   * input:  /TestNode/node0
   *         /TrashNode/testSymlink with testSymlink is taxonomy link of node0   *         
   * tested action: restore taxonomy node from /TrashNode/
   * expectedValue : 1 (number of nodes remain in /TestNode/ )
   *                 0 (number of nodes in /TrashNode/)
   *
   * @throws Exception
   */
  public void testRestoreSymlinkFromTrash() throws Exception {
  	Node rootNode = session.getRootNode();
  	Node trashNode = rootNode.addNode("Trash");
  	Node testNode = rootNode.addNode("TestNode");
  	Node documentNode = testNode.addNode("Documents");
  	Node taxonomyNode = rootNode.addNode("exo:ecm").addNode("exo:taxonomyTrees").addNode("storage").addNode("System");
  	
  	Node node0 = documentNode.addNode("node0");
  	node0.addMixin(MIX_REFERENCEABLE);
  	LinkManager linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
    Node nodeLink = linkManager.createLink(taxonomyNode, "exo:taxonomyLink", node0, "testSymlink");
    session.save();
    trashService.moveToTrash(nodeLink, sessionProvider, 0);
    session.save();    
    trashService.restoreFromTrash(trashNode.getPath() + "/testSymlink", sessionProvider);
    session.save();    
    long testNodeChild = documentNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();    
    assertEquals("testMoveSymlinkToTrash failed!", 1, testNodeChild);
    assertEquals("testMoveSymlinkToTrash failed 2 !", 0, trashNodeChild);    
  	trashNode.remove();    
    testNode.remove();
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

    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider, 0);
    trashService.moveToTrash(node4, sessionProvider, 0);

    int count =
      trashService.getAllNodeInTrash(sessionProvider).size();

    session.save();
    session.save();
    assertEquals("testGetAllNodeInTrash failed!", 3, count);
    trashNode.remove();
    testNode.remove();
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

    trashService.moveToTrash(node0, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider);

    session.save();

    trashNode.remove();
    testNode.remove();
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

    trashService.moveToTrash(node1, sessionProvider, 0);
    trashService.moveToTrash(node2, sessionProvider, 0);

    session.save();

    assertEquals(true, trashService.isInTrash(trashNode.getNode("node1")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node2")));
    assertEquals(false, trashService.isInTrash(testNode.getNode("node3")));

    trashNode.remove();
    testNode.remove();
    session.save();
  }
  /**
   * test method testRemoveRelations()
   * input: /TestNode/node0
   *        Add a relation node to node0
   *        /TestNode/node3
   * test action: remove all relation nodes of node0
   * expectedValue : 0 (number of relation nodes of /testNode/node0 )   
   * @throws Exception
   */
  public void testRemoveRelations() throws Exception {
  	Node rootNode = session.getRootNode();
  	Node testNode = rootNode.addNode("testNode");
  	Node test0 = rootNode.addNode("test0");
  	testNode.addMixin(MIX_REFERENCEABLE);
  	test0.addMixin(MIX_REFERENCEABLE);
  	session.save();
  	RelationsService relationService = (RelationsService) container.getComponentInstanceOfType(RelationsService.class);
  	relationService.addRelation(testNode, "/test0", session.getWorkspace().getName());
  	session.save();
  	trashService.removeRelations(testNode, sessionProvider);
  	session.save();
  	List<Node> nodesRelation = relationService.getRelations(testNode, sessionProvider);
  	int count = nodesRelation.size();
  	assertEquals("testRemoveRelations failed!", 0, count);
  	testNode.remove();
  	test0.remove();
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

    trashService.moveToTrash(node0, sessionProvider, 0);

    session.save();

    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0/node1")));
    assertEquals(true, trashService.isInTrash(trashNode.getNode("node0/node2")));

    trashNode.remove();
    testNode.remove();
    session.save();
  } 
  
  /**
   * test method GetNodeByTrashId
   * input:     /TrashNode/node0

   * tested action: move node to Trash and get by returned trashId, get random trashId
   * expectedValue : 1 node
   *            0 node with random trashId
   *
   * @throws Exception
   */
  public void testGetNodeByTrashId() throws Exception {
    Node rootNode = session.getRootNode();

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node0");


    session.save();

    String trashId = trashService.moveToTrash(node0, sessionProvider, 0);

    session.save();
    assertNotNull(trashService.getNodeByTrashId(trashId));
    assertNull(trashService.getNodeByTrashId(trashId + "qqqqqqqqqqqqqqq"));
    trashNode.remove();
    testNode.remove();
    session.save();
  }
  
  public void testGetFileByTrashId() throws Exception {

    Node trashRootNode = session.getRootNode();
    Node trashNode = trashRootNode.addNode("Trash");
    Node test = session.getRootNode().addNode("test1", "nt:file");
    if(test.getPrimaryNodeType().getName().equals("nt:file")){
      test.addNode("jcr:content", "nt:base");
    }

    session.save();

    String trashId = trashService.moveToTrash(test, sessionProvider, 0);

    session.save();
    assertNotNull(trashService.getNodeByTrashId(trashId));
    assertNull(trashService.getNodeByTrashId(trashId + "qqqqqqqqqqqqqqq"));
    trashNode.remove();
    session.save();
  }
  
  public void testMoveSameNameToTrashSameWorkspace() throws Exception {
    Node rootNode = session.getRootNode();
    Node trashNode = rootNode.addNode("Trash");
    Node testNode = rootNode.addNode("TestNode");

    Node node0 = testNode.addNode("node");
    Node node1 = testNode.addNode("node");
    Node node2 = testNode.addNode("node");
    Node node3 = testNode.addNode("node");
    //Add mix:referenceable into node to coverage code for the case relate to the SEO Service
    node0.addMixin(MIX_REFERENCEABLE);
    node1.addMixin(MIX_REFERENCEABLE);
    node2.addMixin(MIX_REFERENCEABLE);
    node3.addMixin(MIX_REFERENCEABLE);
    session.save();
    trashService.moveToTrash(node0, sessionProvider);
    trashService.moveToTrash(node1, sessionProvider);
    trashService.moveToTrash(node2, sessionProvider);
    session.save();

    long testNodeChild = testNode.getNodes().getSize();
    long trashNodeChild = trashService.getTrashHomeNode().getNodes().getSize();
    assertEquals("testMoveToTrashSameWorkspace failed!", 1, testNodeChild);
    assertEquals("testMoveToTrashSameWorkspace failed 2 !", 3, trashNodeChild);

    trashNode.remove();
    testNode.remove();
    session.save();
  }
  

}
