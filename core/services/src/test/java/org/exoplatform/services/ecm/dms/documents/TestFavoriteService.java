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
package org.exoplatform.services.ecm.dms.documents;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL Author : Nguyen Anh Vu anhvurz90@gmail.com
 * Nov 17, 2009 11:14:48 AM
 */
public class TestFavoriteService extends BaseDMSTestCase {

  // final static public String EXO_FAVOURITE_NODE = "exo:favourite";
  // final static public String EXO_FAVOURITER_PROPERTY = "exo:favouriter";

  private FavoriteService favoriteService;

  public void setUp() throws Exception {
    super.setUp();
    super.applyUserSession("john", "exo");
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    favoriteService = (FavoriteService)myContainer.getComponentInstanceOfType(FavoriteService.class);


    SessionProviderService sessionProviderService
    =	(SessionProviderService) myContainer.getComponentInstanceOfType(SessionProviderService.class);

    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);

    ManageableRepository manageableRepository = repositoryService.getRepository("repository");

    Session session = sessionProvider.getSession(COLLABORATION_WS, manageableRepository);
    Node rootNode = session.getRootNode();
    String[] names = new String[] {"root", "demo", "james", "john", "marry"};
    for (String name : names)
      if (rootNode.hasNode(name)) {
        rootNode.getNode(name).remove();
        rootNode.save();
      }

  }

  /**
   * test method addFavourite. Input: /testAddFavourite1, /testAddFavorite2 nodes 
   * tested action: add favorite for users 'john' to the nodes
   * above. Expected value : 2 for favorite node list size
   *
   * @throws Exception
   */
  public void testAddFavorite() throws Exception {
    Node rootNode = session.getRootNode();
    Node testAddFavouriteNode1 = rootNode.addNode("testAddFavorite1");
    Node testAddFavouriteNode2 = rootNode.addNode("testAddFavorite2");
    session.save();
    favoriteService.addFavorite(testAddFavouriteNode1, "john");
    favoriteService.addFavorite(testAddFavouriteNode2, "john");

    int johnFav = favoriteService.
    getAllFavoriteNodesByUser(session.getWorkspace().getName(), REPO_NAME, "john").size();

    assertEquals("testAddFavorite failed!", 2, johnFav);
    
    // Add favorite to an un-existed user
    favoriteService.addFavorite(testAddFavouriteNode2, "unknown");
    assertEquals(0, favoriteService.getAllFavoriteNodesByUser(session.getWorkspace().getName(), REPO_NAME, "unknown").size());

    testAddFavouriteNode1.remove();
    testAddFavouriteNode2.remove();
    session.save();
  }

  /**
   * test method removeFavourite. Input: /test1, /test2, /test3 nodes  .Tested action:
   * add favorite for 'john' to the nodes above,
   * remove favorite for node /test2. Expected value :
   * 2 for favorite node list size of 'john'
   *
   * @throws Exception
   */
  public void testRemoveFavorite() throws Exception {
    Node rootNode = session.getRootNode();
    Node test1Remove = rootNode.addNode("test1");
    Node test2Remove = rootNode.addNode("test2");
    Node test3Remove = rootNode.addNode("test3");
    session.save();
    favoriteService.addFavorite(test1Remove, "john");
    favoriteService.addFavorite(test2Remove, "john");
    favoriteService.addFavorite(test3Remove, "john");

    favoriteService.removeFavorite(test2Remove, "john");

    int johnFav = favoriteService.
        getAllFavoriteNodesByUser(session.getWorkspace().getName(), REPO_NAME, "john").size();

    assertEquals("testRemoveFavorite failed!", 2, johnFav);

    test1Remove.remove();
    test2Remove.remove();
    test3Remove.remove();

    session.save();
  }

  /**
   * test method getAllFavouriteNodesByUser. Input: /node0 /node1 /node2 /node3
   * /node4. Tested action: add favorite for 'john' to node0, node2, node3, node4;
   *
   * expectedValue : 4 ( number of favorite nodes by 'john')
   *
   * @throws Exception
   */
  public void testGetAllFavouriteNodesByUser() throws Exception {
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");
    session.save();

    Node node0 = testNode.addNode("node0");
    Node node1 = testNode.addNode("node1");
    Node node2 = testNode.addNode("node2");
    Node node3 = testNode.addNode("node3");
    Node node4 = node3.addNode("node4");

    favoriteService.addFavorite(node0, "john");
    favoriteService.addFavorite(node2, "john");
    favoriteService.addFavorite(node3, "john");
    favoriteService.addFavorite(node4, "john");

    assertEquals("testGetAllFavouriteNodesByUser failed!", 4, favoriteService
        .getAllFavoriteNodesByUser(
            rootNode.getSession().getWorkspace().getName(), "repository",
            "john").size());
    testNode.remove();
    session.save();
  }
}
