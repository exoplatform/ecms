/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.connector.collaboration;

import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.collaboration.FavoriteRESTService.ListResultNode;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 20 Aug 2012  
 */
public class TestFavoriteRESTService extends BaseConnectorTestCase {
  /**
   * Bind FavoriteRESTService REST service
   */
  private FavoriteService        favoriteService;
  private ManageableRepository   manageableRepository;
  private static final String    DATE_MODIFIED   = "exo:dateModified";
  private static final String    restPath        = "/favorite/all/repository/collaboration/john";
  
  public void setUp() throws Exception {
    super.setUp();
    
    // Bind FavoriteRESTService REST service
    FavoriteRESTService restService = (FavoriteRESTService) this.container.getComponentInstanceOfType(FavoriteRESTService.class);
    this.binder.addResource(restService, null);
    favoriteService = WCMCoreUtils.getService(FavoriteService.class);
  }
  
  public void testGetFavoriteByUser() throws Exception{
    applyUserSession("john", "gtn", "collaboration");
    /* Prepare the favourite nodes */
    manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, manageableRepository);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    Node rootNode = session.getRootNode();
    Node testAddFavouriteNode1 = rootNode.addNode("testAddFavorite1");
    Node testAddFavouriteNode2 = rootNode.addNode("testAddFavorite2");
    session.save();
    favoriteService.addFavorite(testAddFavouriteNode1, "john");
    favoriteService.addFavorite(testAddFavouriteNode2, "john");
    List<Node> listNodes = favoriteService.getAllFavoriteNodesByUser(COLLABORATION_WS,
                                              manageableRepository.getConfiguration().getName(),  "john");
    for (Node favorite : listNodes) {
      favorite.addMixin("exo:datetime");
      favorite.setProperty(DATE_MODIFIED, new GregorianCalendar());
      favorite.getProperty(DATE_MODIFIED).setValue(new GregorianCalendar());
    }
    session.save();
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ListResultNode object = (ListResultNode) response.getEntity();
    //Is Rest service properly run
    assertEquals(2, object.getListFavorite().size());
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
