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
package org.exoplatform.services.wcm.publication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Oct 2, 2008
 */
public class PublicationUtil {

  /** The Constant HISTORY_SEPARATOR. */
  public static final String HISTORY_SEPARATOR = "; ";

  /** The Constant APPLICATION_SEPARATOR. */
  public static final String APPLICATION_SEPARATOR = "@";

  /** The Constant URI_SEPARATOR. */
  public static final String URI_SEPARATOR = "/";

  /**
   * Find user node by page id.
   * @param rootNode
   * @param pageId
   * @return
   * @throws Exception
   */
  public static List<UserNode> findUserNodeByPageId(UserNode rootNode, String pageId) throws Exception {
    List<UserNode> allUserNodes = new ArrayList<>();
    findUserNodeByPageId(rootNode, pageId, allUserNodes);
    return allUserNodes;
  }

  /**
   * Find user node by page id.
   * @param userNode
   * @param pageId
   * @param allUserNodes
   * @throws Exception
   */
  public static void findUserNodeByPageId(UserNode userNode,
                                          String pageId,
                                          List<UserNode> allUserNodes) throws Exception {
    Iterator<UserNode> childrenNodeIter = userNode.getChildren().iterator();
    while (childrenNodeIter.hasNext()) {
      UserNode node = childrenNodeIter.next();
      if (node.getPageRef().equals(pageId)) {
        allUserNodes.add(node);
      } else {
        findUserNodeByPageId(node, pageId, allUserNodes);
      }
    }
  }

  /**
   * Find app instances by name.
   *
   * @param page the page
   * @param applicationName the application name
   *
   * @return the list of app instances
   */
  public static List<String> findAppInstancesByName(Page page, String applicationName) {
    List<String> results = new ArrayList<>();
    findAppInstancesByContainerAndName(page, applicationName, results);
    return results;
  }

  /**
   * Find app instances by container and name.
   *
   * @param container the container
   * @param applicationName the application name
   * @param results the results
   */
  private static void findAppInstancesByContainerAndName(Container container, String applicationName, List<String> results) {
    ArrayList<ModelObject> chidren = container.getChildren();
    if(chidren == null) return ;
    for(ModelObject object: chidren) {
      if(object instanceof Application) {
        Application<?> application = Application.class.cast(object);
        if(application.getId().contains(applicationName)) {
          results.add(application.getId());
        }
      } else if(object instanceof Container) {
        Container child = Container.class.cast(object);
        findAppInstancesByContainerAndName(child, applicationName, results);
      }
    }
  }

  /**
   * Removed app instances in container by names.
   *
   * @param container the container
   * @param removingApplicationIds the removing application ids
   */
  private static void removedAppInstancesInContainerByNames(Container container,
                                                            List<String> removingApplicationIds) {
    ArrayList<ModelObject> childrenTmp = new ArrayList<>();
    ArrayList<ModelObject> chidren = container.getChildren();
    if (chidren == null)
      return;
    for (ModelObject object : chidren) {
      if (object instanceof Application) {
        Application<?> application = Application.class.cast(object);
        if(!removingApplicationIds.contains(application.getId())) {
          childrenTmp.add(object);
        }
      } else if (object instanceof Container) {
        Container child = Container.class.cast(object);
        removedAppInstancesInContainerByNames(child, removingApplicationIds);
        childrenTmp.add(child);
      }
    }
  }

  /**
   * Gets the values as string.
   *
   * @param node the node
   * @param propName the prop name
   *
   * @return the values as string
   *
   * @throws Exception the exception
   */
  public static List<String> getValuesAsString(Node node, String propName) throws Exception {
    if(!node.hasProperty(propName)) return new ArrayList<>();
    List<String> results = new ArrayList<>();
    try{
      for(Value value: node.getProperty(propName).getValues()) {
        results.add(value.getString());
      }
    }catch(ValueFormatException ex){
      results.add(node.getProperty(propName).getValue().getString());
    }
    return results;
  }

  /**
   * To values.
   *
   * @param factory the factory
   * @param values the values
   *
   * @return the value[]
   */
  public static Value[] toValues(ValueFactory factory, List<String> values) {
    List<Value> list = new ArrayList<>();
    for(String value: values) {
      list.add(factory.createValue(value));
    }
    return list.toArray(new Value[list.size()]);
  }

  /**
   * Removes the application from page.
   *
   * @param page the page
   * @param removedApplicationIds the removed application ids
   */
  public static void removeApplicationFromPage(Page page, List<String> removedApplicationIds) {
    removedAppInstancesInContainerByNames(page, removedApplicationIds);
  }

  /**
   * Gets the list application id by page.
   *
   * @param page the page
   * @param portletName the portlet name
   *
   * @return the list application id by page
   */
  public static List<String> getListApplicationIdByPage(Page page, String portletName) {
    return PublicationUtil.findAppInstancesByName(page, portletName);
  }

  /**
   * Sets the mixed navigation uri.
   *
   * @param portalName the portal name
   * @param pageNodeUri the page node uri
   *
   * @return the string
   */
  public static String setMixedNavigationUri(String portalName, String pageNodeUri) {
    return URI_SEPARATOR + portalName + URI_SEPARATOR + pageNodeUri;
  }

  /**
   * Parses the mixed navigation uri.
   *
   * @param mixedNavigationUri the mixed navigation uri
   *
   * @return the string[]
   */
  public static String[] parseMixedNavigationUri(String mixedNavigationUri) {
    String[] mixedNavigationUris = new String[2];
    int first = 1;
    int second = mixedNavigationUri.indexOf(URI_SEPARATOR, first);
    mixedNavigationUris[0] = mixedNavigationUri.substring(first, second);
    mixedNavigationUris[1] = mixedNavigationUri.substring(second + URI_SEPARATOR.length(), mixedNavigationUri.length());
    return mixedNavigationUris;
  }

  /**
   * Sets the mixed application id.
   *
   * @param pageId the page id
   * @param applicationId the application id
   *
   * @return the string
   */
  public static String setMixedApplicationId(String pageId, String applicationId) {
    return pageId + APPLICATION_SEPARATOR + applicationId;
  }

  /**
   * Parses the mixed application id.
   *
   * @param mixedApplicationId the mixed application id
   *
   * @return the string[]
   */
  public static String[] parseMixedApplicationId(String mixedApplicationId) {
    return mixedApplicationId.split(APPLICATION_SEPARATOR);
  }

  /**
   * Checks if is node content published to page node.
   *
   * @param contentNode the content node
   * @param navNodeURI the nav node uri
   *
   * @return true, if is node content published to page node
   *
   * @throws Exception the exception
   */
  public static boolean isNodeContentPublishedToPageNode(Node contentNode, String navNodeURI) throws Exception {

    UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();

    // make filter
    UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
    filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
    filterConfigBuilder.withTemporalCheck();
    UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

    // get user node
    String nodeURI = navNodeURI.replace("/" + Util.getPortalRequestContext().getPortalOwner() + "/", "");
    UserNavigation userNav = userPortal.getNavigation(Util.getUIPortal().getSiteKey());
    UserNode userNode = userPortal.resolvePath(userNav, filterConfig, nodeURI);
    
    if (userNode == null || userNode.getPageRef() == null) return false;
    
    return PublicationUtil.getValuesAsString(contentNode, "publication:webPageIDs").contains(userNode.getPageRef());
  }

  public static ArrayList<NodeContext<?>> convertAllNodeContextToList(NodeContext<?> rootNodeContext){
    
    if (rootNodeContext == null || rootNodeContext.getNodes() == null){
      return null;
    }

    ArrayList<NodeContext<?>> nodeContextList = new ArrayList<>();
    Iterator<?> iter = rootNodeContext.getNodes().iterator();
    while (iter.hasNext()){
      NodeContext<?> context = (NodeContext<?>) iter.next();
      nodeContextList.add(context);
      nodeContextList.addAll(convertAllNodeContextToList(context));
    }

    return nodeContextList;
  }

  public static StringBuilder buildUserNodeURI(NodeContext<?> context) {
    NodeContext<?> parent = (NodeContext<?>) context.getParentNode();
     if (parent != null)
     {
        StringBuilder builder = buildUserNodeURI(parent);
        if (builder.length() > 0)
        {
           builder.append('/');
        }
        return builder.append(context.getName());
     }
     else
     {
        return new StringBuilder();
     }
  }
}
