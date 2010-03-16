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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 19, 2009  
 * 9:54:25 AM
 */
public class TestNewFolksonomyService extends BaseDMSTestCase {
	
	private static final String TEST = "test";
	private static final String TEST2 = "test2";
	private static final String USER_FOLKSONOMY_ALIAS = "userPrivateFolksonomy".intern();
  private static final String EXO_TOTAL = "exo:total";
  private static final String[] groups = {"/platform/users", "/platform/guests"};
	
	private NewFolksonomyService newFolksonomyService_;
	private LinkManager linkManager;
	private Node test, test2;
	private Node folksonomyNode;
	private Node groupAFolksonomyNode;
	private Node groupBFolksonomyNode;
	private Node publicFolksonomyNode;
	private Node siteFolksonomyNode;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		newFolksonomyService_ = (NewFolksonomyService)
				container.getComponentInstanceOfType(NewFolksonomyService.class);
		linkManager 
		= (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
		
//		String userName = session.getUserID();
		String userName = session.getUserID();
		Node root = session.getRootNode();
		Node applicationData = root.hasNode("Application Data") ? 
													 root.getNode("Application Data") :
													 root.addNode("Application Data");
		Node tagsNode = applicationData.hasNode("Tags") ? 
										applicationData.getNode("Tags") :
										applicationData.addNode("Tags");
		Node rootNode = root.hasNode("Users") ? root.getNode("Users") : root.addNode("Users");
		Node userNode = rootNode.hasNode(userName) ? rootNode.getNode(userName) :
																									rootNode.addNode(userName);
		Node groupsNode = root.hasNode("Groups") ? root.getNode("Groups") :
																										root.addNode("Groups");
		Node platformNode = groupsNode.hasNode("platform") ? groupsNode.getNode("platform") :
																													groupsNode.addNode("platform");
		Node privateNode = userNode.hasNode("Private") ? userNode.getNode("Private") :
																										 userNode.addNode("Private");
//    NodeHierarchyCreator nodehierarchyCreator = (NodeHierarchyCreator) container
//    .getComponentInstanceOfType(NodeHierarchyCreator.class);

    String folksonomyPath = "Folksonomy";
		System.out.println(folksonomyPath);
		folksonomyNode = privateNode.hasNode(folksonomyPath) ? privateNode.getNode(folksonomyPath) :
																																privateNode.addNode(folksonomyPath);
		System.out.println(folksonomyNode.getPath());

		rootNode = privateNode;
		System.out.println(userName);
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
		newFolksonomyService_.addPrivateTag(tags, test, REPO_NAME, COLLABORATION_WS, session.getUserID());
		assertTrue("testAddPrivateTag failed! ", folksonomyNode.hasNode("sport"));
		assertTrue("testAddPrivateTag failed! ", folksonomyNode.hasNode("weather"));
		
		Node sportTagNode = folksonomyNode.getNode("sport");
		Node link = sportTagNode.getNodes().nextNode();
		Node targetNode = linkManager.getTarget(link);
		assertTrue("testAddPrivateTag failed! ", test.isSame(targetNode));
		
		Node weatherTagNode = folksonomyNode.getNode("weather");
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
		newFolksonomyService_.addGroupsTag(tags, test, REPO_NAME, COLLABORATION_WS, groups);
		
		System.out.println("Group A:" + groupAFolksonomyNode.getPath());
		
		assertTrue("testAddGroupsTag failed! ", groupAFolksonomyNode.hasNode("sport"));
		assertTrue("testAddGroupsTag failed! ", groupAFolksonomyNode.hasNode("weather"));
		
		Node sportTagNode = groupAFolksonomyNode.getNode("sport");
		Node link = sportTagNode.getNodes().nextNode();
		Node targetNode = linkManager.getTarget(link);
		assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));
		
		Node weatherTagNode = groupAFolksonomyNode.getNode("weather");
		link = weatherTagNode.getNodes().nextNode();
		targetNode = linkManager.getTarget(link);
		assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));
		
		assertEquals("testAddGroupsTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
		assertEquals("testAddGroupsTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());		
		//--------------------------
		assertTrue("testAddGroupsTag failed! ", groupBFolksonomyNode.hasNode("sport"));
		assertTrue("testAddGroupsTag failed! ", groupBFolksonomyNode.hasNode("weather"));
		
		sportTagNode = groupBFolksonomyNode.getNode("sport");
		link = sportTagNode.getNodes().nextNode();
		targetNode = linkManager.getTarget(link);
		assertTrue("testAddGroupsTag failed! ", test.isSame(targetNode));
		
		weatherTagNode = groupBFolksonomyNode.getNode("weather");
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
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		assertTrue("testAddPublicTag failed! ", publicFolksonomyNode.hasNode("sport"));
		assertTrue("testAddPublicTag failed! ", publicFolksonomyNode.hasNode("weather"));
		
		Node sportTagNode = publicFolksonomyNode.getNode("sport");
		Node link = sportTagNode.getNodes().nextNode();
		Node targetNode = linkManager.getTarget(link);
		assertTrue("testAddPublicTag failed! ", test.isSame(targetNode));
		
		Node weatherTagNode = publicFolksonomyNode.getNode("weather");
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
		
		siteFolksonomyNode = siteTags.addNode(site);
		session.save();
		newFolksonomyService_.addSiteTag(site, 
																		 tags, 
																		 test, 
																		 REPO_NAME, 
																		 COLLABORATION_WS);
		assertTrue("testAddSiteTag failed! ", siteFolksonomyNode.hasNode("sport"));
		assertTrue("testAddSiteTag failed! ", siteFolksonomyNode.hasNode("weather"));
		
		Node sportTagNode = siteFolksonomyNode.getNode("sport");
		Node link = sportTagNode.getNodes().nextNode();
		Node targetNode = linkManager.getTarget(link);
		assertTrue("testAddSiteTag failed! ", test.isSame(targetNode));
		
		Node weatherTagNode = siteFolksonomyNode.getNode("weather");
		link = weatherTagNode.getNodes().nextNode();
		targetNode = linkManager.getTarget(link);
		assertTrue("testAddSiteTag failed! ", test.isSame(targetNode));
		
		assertEquals("testAddSiteTag failed! ", 1L, sportTagNode.getProperty(EXO_TOTAL).getLong());
		assertEquals("testAddSiteTag failed! ", 1L, weatherTagNode.getProperty(EXO_TOTAL).getLong());		
	}
	
	
  /**
   * Test Method: getAllDocumentsByTag()
   * Input: Nodes 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' and 'test2' of current user
   * 							get all documents by tag 'sport'
   * Expected Result: 
   *							'test' and 'test2'        
   */
	public void testGetAllDocumentsByTag() throws Exception {
		String[] tags = { "sport", "weather" };
		String user = session.getUserID();
//		Node sportTagNode = folksonomyNode.getNode("sport");
//		System.out.println(sportTagNode.getProperty(EXO_TOTAL).getLong());
//		NodeIterator iter = sportTagNode.getNodes();
//		while (iter.hasNext()) {
//			System.out.println(iter.nextNode().getPath());
//		}
		newFolksonomyService_.addPrivateTag(tags, 
																			 test, 
																			 REPO_NAME, 
																			 COLLABORATION_WS,
																			 user);
		newFolksonomyService_.addPrivateTag(tags, 
																			 test2, 
																			 REPO_NAME, 
																			 COLLABORATION_WS,
																			 user); 
		assertTrue("testGetAllDocumentsByTag failed! ", folksonomyNode.hasNode("sport"));
		assertTrue("testGetAllDocumentsByTag failed! ", folksonomyNode.hasNode("weather"));
		
		Node sportTagNode = folksonomyNode.getNode("sport");
		
		int count = 0;
		SessionProvider sessionProvider = SessionProviderFactory.createSystemProvider();
		List<Node> docs = newFolksonomyService_.getAllDocumentsByTag(sportTagNode.getPath(), REPO_NAME, COLLABORATION_WS, sessionProvider);
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
   * 							add 2 tags 'sport' and 'music' for node 'test2' in group 'a' and 'b'
   * 							get all tags of groups 'a' and 'b' 
   * Expected Result:
   * 							'sport', 'weather', 'music' of a 
   * 							'sport', 'weather', 'music' of b
   * 							total 6
   */
	public void testGetAllGroupTagsOfManyRoles() throws Exception {
		String[] tags = { "sport", "weather" };
		String[] tags2 = { "sport", "music" };
		newFolksonomyService_.addGroupsTag(tags, test, REPO_NAME, COLLABORATION_WS, groups);
		newFolksonomyService_.addGroupsTag(tags2, test2, REPO_NAME, COLLABORATION_WS, groups);
		
		List<Node> groupTags = newFolksonomyService_.getAllGroupTags(groups, REPO_NAME, COLLABORATION_WS);
		int count = 0;
		for (Node tag : groupTags) {
			if ("sport".equals(tag.getName())) count ++;
			if ("weather".equals(tag.getName())) count ++;
			if ("music".equals(tag.getName())) count ++;
			System.out.println(tag.getPath());
		}
		
		assertEquals("testGetAllGroupTagsOfManyRoles failed! ", 6, count);		
	}
	
  /**
   * Test Method: getAllGroupTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' in group 'a' and 'b'
   * 							add 2 tags 'sport' and 'music' for node 'test2' in group 'a' and 'b'
   * 							get all tags of groups 'a'
   * Expected Result:
   * 							'sport', 'weather', 'music' 
   */
	public void testGetAllGroupTags() throws Exception {
		String[] tags = { "sport", "weather" };
		String[] tags2 = { "sport", "music" };
		newFolksonomyService_.addGroupsTag(tags, test, REPO_NAME, COLLABORATION_WS, groups);
		newFolksonomyService_.addGroupsTag(tags2, test2, REPO_NAME, COLLABORATION_WS, groups);
		
		List<Node> groupTags = newFolksonomyService_.getAllGroupTags("/platform/users", REPO_NAME, COLLABORATION_WS);
		int count = 0;
		for (Node tag : groupTags) {
			if ("sport".equals(tag.getName())) count ++;
			if ("weather".equals(tag.getName())) count ++;
			if ("music".equals(tag.getName())) count ++;
			System.out.println(tag.getPath());
		}
		
		assertEquals("testGetAllGroupTags failed! ", 3, count);		
	}

  /**
   * Test Method: testGetAllPrivateTags()
   * Input: Node 'test', 'test2'
   * Test action: add 2 tags 'sport' and 'weather' for node 'test' of current user
   * 							add 2 tags 'sport' and 'xyz' for node 'test2' of current user
   * 							get all private tags of current user
   * Expected Result:
   * 							'sport', 'weather', 'xyz' 
   */
  public void testGetAllPrivateTags() throws Exception {
		String[] tags = { "sport", "weather" };
		newFolksonomyService_.addPrivateTag(tags, test, REPO_NAME, COLLABORATION_WS, session.getUserID());
		String[] tags2 = {"sport", "xyz"};
		newFolksonomyService_.addPrivateTag(tags2, test2, REPO_NAME, COLLABORATION_WS, session.getUserID());
		
		List<Node> tagList = newFolksonomyService_.getAllPrivateTags(session.getUserID(), REPO_NAME, COLLABORATION_WS);
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
	 * 							add 3 tags 'sport', 'boy', 'girl' for node 'test2' in public
	 * 							get all public tags
	 * Expected Result:
	 * 							sport, weather, boy, girl 
	 */
  public void testGetAllPublicTags() throws Exception {
		String[] tags = { "sport", "weather" };
		String[] tags2 = { "boy", "girl", "sport" };
		String publicFolksonomyTreePath = "/Application Data/Tags";
		newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, 
																			 tags, 
																			 test, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, 
																			 tags2, 
																			 test2, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		List<Node> tagList = newFolksonomyService_.getAllPublicTags(publicFolksonomyTreePath, REPO_NAME, COLLABORATION_WS);
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
	 * 							add 3 tags 'sport', 'boy', 'girl' for node 'test2' in site 'portal1'
	 * 							get all tags of site 'portal1'
	 * Expected Result:
	 * 							sport, weather, boy, girl 
	 */
  public void testGetAllSiteTags() throws Exception {
		String[] tags = { "sport", "weather" };
		String[] tags2 = { "boy", "girl", "sport" };
		String site = "portal1";
		newFolksonomyService_.addSiteTag(site, 
																			 tags, 
																			 test, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		newFolksonomyService_.addSiteTag(site, 
																			 tags2, 
																			 test2, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		List<Node> tagList = newFolksonomyService_.getAllSiteTags(site, REPO_NAME, COLLABORATION_WS);
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
	 * 							rename tag 'sport' to 'football'
	 * 							get all public tags
	 * Expected Result:
	 * 							get all public tags -> football, weather
	 * 							node 'football' must have a symlink child which points to 'test' node
	 */ 
  public void testModifyTagName() throws Exception {
		String[] tags = { "sport", "weather" };
		String publicFolksonomyTreePath = "/Application Data/Tags";
		newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, 
																			 tags, 
																			 test, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		Node sportNode = publicFolksonomyNode.getNode("sport");
		Node football = newFolksonomyService_.modifyTagName(sportNode.getPath()	, "football", REPO_NAME, COLLABORATION_WS);
		
		assertTrue("testModifyTagName failed! ", publicFolksonomyNode.hasNode("football"));
		assertTrue("testModifyTagName failed! ", publicFolksonomyNode.getNode("football").isSame(football));
		assertFalse("testModifyTagName failed! ", publicFolksonomyNode.hasNode("sport"));
		
		assertTrue("testModifyTagName failed! ", test.isSame(
				linkManager.getTarget(football.getNodes().nextNode())));
  }
  
	/**
	 * Test Method : removeTag() 	
	 * Input: Node 'test',
	 * Test action: add 3 tags 'sport', 'nobita' and 'weather' for node 'test' in public
	 * 							remove  tag 'sport'
	 * 							get all public tags
	 * Expected Result:
	 * 							'nobita', 'weather'
	 */ 
  public void testRemoveTag() throws Exception {
		String[] tags = { "sport", "weather", "nobita"};
		String publicFolksonomyTreePath = "/Application Data/Tags";
		newFolksonomyService_.addPublicTag(publicFolksonomyTreePath, 
																			 tags, 
																			 test, 
																			 REPO_NAME, 
																			 COLLABORATION_WS);
		Node sportNode = publicFolksonomyNode.getNode("sport");
		newFolksonomyService_.removeTag(sportNode.getPath(), REPO_NAME, COLLABORATION_WS);
		
		assertFalse("testRemoveTag failed! ", publicFolksonomyNode.hasNode("sport"));

		List<Node> tagList = newFolksonomyService_.getAllPublicTags(publicFolksonomyTreePath, REPO_NAME, COLLABORATION_WS);
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
   * 							remove tag 'sport' of current user
   * 							get all symlinks of tags 'sport'
   * Expected Result:
   * 							none 
   */
	public void testRemoveTagOfDocument() throws Exception {
		String[] tags = { "sport", "weather" };
		newFolksonomyService_.addPrivateTag(tags, test, REPO_NAME, COLLABORATION_WS, session.getUserID());
		
		Node sportTagNode = folksonomyNode.getNode("sport");
		newFolksonomyService_.removeTagOfDocument(sportTagNode.getPath(), test, REPO_NAME, COLLABORATION_WS);

		assertEquals("testRemoveTagOfDocument failed! ", 0, sportTagNode.getNodes().getSize());
	}
	/**
	 * Clean data test
	 */
	public void tearDown() throws Exception {
		String[] nodes = {"/Application Data/Tags",
											"/Users/" + session.getUserID() + "/Private/Folksonomy", 
											"Groups/platform/users/ApplicationData/Tags",
											"Groups/platform/guests/ApplicationData/Tags",
											TEST, TEST2, 
											session.getUserID()};
		for (String node : nodes)
			if (session.itemExists(node)) {
				Node n = (Node)session.getItem(node);
				n.remove();
				session.save();
			}
		super.tearDown();
		NodeIterator iter = session.getRootNode().getNodes();
		System.out.println("TearDown:......................");
		while (iter.hasNext())
			System.out.println(iter.nextNode().getPath());
	}
}
