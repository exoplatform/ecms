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

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 29 May 2012  
 */
@Path("/content/")
public class NavigationConnector implements ResourceContainer{
  private static ThreadLocal<Boolean> gotNavigationKeeper = new ThreadLocal<Boolean>();
  /**
   * 
   * @param       portalName: Destination portal to get the navigation tree
   * @return
   * @throws      Exception    
   * @Objective : Return a JsonString include all navigation node
   * @Author    : Nguyen The Vinh from ECM of eXoPlatform
   *              vinh.nguyen@exoplatform.com
   */
  @GET
  @Path("/getFullNavigation/")
  public Response getFullNavigation ( @QueryParam("portalName") String portalName) throws Exception {
    String userName = ConversationState.getCurrent().getIdentity().getUserId();
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element element = document.createElement("navigationXML");
    element.setTextContent(getNavigationAsJSON(portalName, userName));
    document.appendChild(element);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML).build();
  }
  private String getNavigationAsJSON(String portalName, String username) throws Exception {

    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
        username,
        PortalRequestContext.USER_PORTAL_CONTEXT);
    UserPortal userPortal = userPortalCfg.getUserPortal();

    //filter nodes
    UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
    filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
    filterConfigBuilder.withTemporalCheck();
    UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

    //get nodes
    UserNavigation navigation = userPortal.getNavigation(SiteKey.portal(portalName));
    UserNode root = userPortal.getNode(navigation, Scope.ALL, filterConfig, null);

    //set gotNavigation=true
    gotNavigationKeeper.set(true);
    return createJsonTree(navigation, root);
  }
  /**
   * 
   * @param           navigation: Navigation information to create Json tree
   * @param           rootNode  : Root node of navigation
   * @return          A String as Json tree
   * @throws Exception
   * @Objective : Return a JsonString include all navigation node, serve for getNavigationAsJSON method
   * @Author    : Nguyen The Vinh from ECM of eXoPlatform
   *              vinh.nguyen@exoplatform.com
   */
  private String createJsonTree(UserNavigation navigation, UserNode rootNode) throws Exception {
    StringBuffer sbJsonTree = new StringBuffer();
    sbJsonTree.append("[");
    sbJsonTree.append("{");
    sbJsonTree.append("\"ownerId\":\"").append(navigation.getKey().getName()).append("\",");
    sbJsonTree.append("\"ownerType\":\"").append(navigation.getKey().getTypeName()).append("\",");
    sbJsonTree.append("\"priority\":\"").append(navigation.getPriority()).append("\",");
    sbJsonTree.append("\"nodes\":").append(addJsonNodes(rootNode.getChildren().iterator()));
    sbJsonTree.append("}");
    sbJsonTree.append("]");
    return sbJsonTree.toString();
  }
  /**
   * 
   * @param           children
   * @return          StringBuffer contain Json tree of children
   * @throws          Exception
   * @Objective :     Build JsonTree for children nodes of navigation
   * @Author    :     Nguyen The Vinh from ECM of eXoPlatform
   *                  vinh.nguyen@exoplatform.com
   */
  private StringBuffer addJsonNodes(Iterator<UserNode> children) {
    StringBuffer sbJsonTree = new StringBuffer();
    String resovleLabel = "";
    sbJsonTree.append("[");
    boolean first = true;

    while (children.hasNext()) {
      UserNode child = children.next();
      if (!first) {
        sbJsonTree.append(",");
      }
      first = false;
      sbJsonTree.append("{");
      sbJsonTree.append("\"icon\":").append(child.getIcon() != null ? "\"" + child.getIcon() + "\""
                                                                   : "null").append(",");
      sbJsonTree.append("\"label\":\"").append(child.getLabel()).append("\",");
      sbJsonTree.append("\"name\":\"").append(child.getName()).append("\",");
      try {
        resovleLabel = child.getResolvedLabel();
      } catch (NullPointerException npe) {
        resovleLabel = "";
      }
      sbJsonTree.append("\"resolvedLabel\":\"").append(resovleLabel).append("\",");
      sbJsonTree.append("\"uri\":\"").append(child.getURI()).append("\",");

      sbJsonTree.append("\"getNodeURL\":\"").append(child.getURI().toString()).append("\",");
      sbJsonTree.append("\"nodes\":").append(addJsonNodes(child.getChildren().iterator()));
      sbJsonTree.append("}");
    }
    sbJsonTree.append("]");
    return sbJsonTree;
  }
}
