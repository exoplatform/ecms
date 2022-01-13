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

import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Created by The eXo Platform SARL Author : Nguyen Anh Vu anhvurz90@gmail.com
 * Nov 17, 2009 11:14:48 AM
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFavoriteService extends BaseWCMTestCase {

  // final static public String EXO_FAVOURITE_NODE = "exo:favourite";
  // final static public String EXO_FAVOURITER_PROPERTY = "exo:favouriter";

  private FavoriteService favoriteService;
  private Node            rootNode;
  
  public void setUp() throws Exception {
    super.setUp();
    favoriteService = (FavoriteService) container.getComponentInstanceOfType(FavoriteService.class);
    applyUserSession("john", "gtn",COLLABORATION_WS);
    SessionProviderService sessionProviderService = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    ManageableRepository manageableRepository = repositoryService.getRepository("repository");
    Session session = sessionProvider.getSession(COLLABORATION_WS, manageableRepository);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    rootNode = session.getRootNode();
    String[] names = new String[] {"root", "demo", "james", "john", "marry"};
    for (String name : names)
      if (rootNode.hasNode(name)) {
        rootNode.getNode(name).remove();
        rootNode.save();
      }
  }
  
  public void tearDown() throws Exception {
    List<Node> favoritesOfJohn = favoriteService.getAllFavoriteNodesByUser("john",0);
    for (Node node : favoritesOfJohn) {
      favoriteService.removeFavorite(node, "john");
    }
    session.save();
    super.tearDown();
  }

  /**
   * test method addFavourite. Input: /testAddFavourite1, /testAddFavorite2 nodes 
   * tested action: add favorite for users 'john' to the nodes
   * above. Expected value : 2 for favorite node list size
   *
   * @throws Exception
   */
  public void testAddFavorite() throws Exception {
    Node testAddFavouriteNode1 = rootNode.addNode("testAddFavorite1");
    testAddFavouriteNode1.addMixin("mix:referenceable");
    testAddFavouriteNode1.addMixin("exo:datetime");
    testAddFavouriteNode1.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node testAddFavouriteNode2 = rootNode.addNode("testAddFavorite2");
    testAddFavouriteNode2.addMixin("mix:referenceable");
    testAddFavouriteNode2.addMixin("exo:datetime");
    testAddFavouriteNode2.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    session.save();
    favoriteService.addFavorite(testAddFavouriteNode1, "john");
    favoriteService.addFavorite(testAddFavouriteNode2, "john");

    int johnFav = favoriteService.
    getAllFavoriteNodesByUser( "john",0).size();

    assertEquals("testAddFavorite failed!", 2, johnFav);
    
    // Add favorite to an un-existed user
    favoriteService.addFavorite(testAddFavouriteNode2, "unknown");
    assertEquals(0, favoriteService.getAllFavoriteNodesByUser("unknown",0).size());

    favoriteService.removeFavorite(testAddFavouriteNode1, "john");
    favoriteService.removeFavorite(testAddFavouriteNode2, "john");

    testAddFavouriteNode1.remove();
    testAddFavouriteNode2.remove();
    
    session.save();
  }
  /**
   * test method addFavourite. Input: /testAddFavourite1, /testAddFavorite2 nodes 
   * tested action: add favorite for users 'john' to the nodes
   * above. Expected value : 2 for favorite node list size
   * @throws Exception
   */
  public void testIsFavoriter() throws Exception {
    Node testAddFavouriteNode1 = rootNode.addNode("testAddFavorite1");
    testAddFavouriteNode1.addMixin("mix:referenceable");
    testAddFavouriteNode1.addMixin("exo:datetime");
    testAddFavouriteNode1.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    favoriteService.addFavorite(testAddFavouriteNode1, "john");
    assertEquals(true, favoriteService.isFavoriter("john", testAddFavouriteNode1));
    favoriteService.removeFavorite(testAddFavouriteNode1, "john");
    testAddFavouriteNode1.remove();
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
    Node test1Remove = rootNode.addNode("test1");
    test1Remove.addMixin("mix:referenceable");
    test1Remove.addMixin("exo:datetime");
    test1Remove.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node test2Remove = rootNode.addNode("test2");
    test2Remove.addMixin("mix:referenceable");
    test2Remove.addMixin("exo:datetime");
    test2Remove.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node test3Remove = rootNode.addNode("test3");
    test3Remove.addMixin("mix:referenceable");
    test3Remove.addMixin("exo:datetime");
    test3Remove.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    session.save();
    favoriteService.addFavorite(test1Remove, "john");
    favoriteService.addFavorite(test2Remove, "john");
    favoriteService.addFavorite(test3Remove, "john");

    favoriteService.removeFavorite(test2Remove, "john");

    int johnFav = favoriteService.
        getAllFavoriteNodesByUser("john",0).size();

    assertEquals("testRemoveFavorite failed!", 2, johnFav);

    favoriteService.removeFavorite(test1Remove, "john");
    favoriteService.removeFavorite(test3Remove, "john");
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

    Node node0 = rootNode.addNode("node0");
    node0.addMixin("mix:referenceable");
    node0.addMixin("exo:datetime");
    node0.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node node1 = rootNode.addNode("node1");
    node1.addMixin("mix:referenceable");
    node1.addMixin("exo:datetime");
    node1.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node node2 = rootNode.addNode("node2");
    node2.addMixin("mix:referenceable");
    node2.addMixin("exo:datetime");
    node2.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node node3 = rootNode.addNode("node3");
    node3.addMixin("mix:referenceable");
    node3.addMixin("exo:datetime");
    node3.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    Node node4 = rootNode.addNode("node4");
    node4.addMixin("mix:referenceable");
    node4.addMixin("exo:datetime");
    node4.setProperty(NodetypeConstant.EXO_DATE_CREATED, Calendar.getInstance());
    session.save();


    favoriteService.addFavorite(node0, "john");
    favoriteService.addFavorite(node1, "john");
    favoriteService.addFavorite(node2, "john");
    favoriteService.addFavorite(node3, "john");
    favoriteService.addFavorite(node4, "john");

    assertEquals("testGetAllFavouriteNodesByUser failed!", 5, favoriteService
        .getAllFavoriteNodesByUser(
            "john",0).size());
    session.save();
  }
}