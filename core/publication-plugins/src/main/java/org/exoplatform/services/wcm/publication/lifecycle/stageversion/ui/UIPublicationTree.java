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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UITree;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Sep 23, 2008
 */

@ComponentConfig(
    template = "system:/groovy/webui/core/UITree.gtmpl"
)
public class UIPublicationTree extends UITree {

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#event(java.lang.String, java.lang.String)
   */
  public String event(String name, String beanId) throws Exception {
    UIComponent component = getParent();
    return component.event(name, beanId);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#getActionLink()
   */
  public String getActionLink() throws Exception {
    if(getSelected() == null) return "javascript:void(0)";
    if(getParentSelected() == null) return "javascript:void(0)";
    return event("ChangeNode", (String)getId(getParentSelected()));
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UITree#isSelected(java.lang.Object)
   */
  public boolean isSelected(Object obj) throws Exception{
    TreeNode selected = getSelected();
    if(selected == null) return false;
    TreeNode compared = TreeNode.class.cast(obj);
    return compared.getUri().equals(selected.getUri());
  }

  /**
   * The Class TreeNode.
   */
  public static class TreeNode {

    /** The portal name. */
    private String portalName;

    /** The is user node. */
    private boolean isUserNode;

    /** The user node. */
    private UserNode userNode;

    /** The navigation. */
    private UserNavigation navigation;

    /** The children. */
    private List<TreeNode> children;

    /** The resource bundle. */
    private ResourceBundle resourceBundle;

    /**
     * Instantiates a new tree node.
     *
     * @param portalName the portal name
     * @param navigation the navigation
     * @param res the res
     * @param isUserNode the is user node
     */
    public TreeNode(String portalName, final UserNavigation navigation, final ResourceBundle res, boolean isUserNode) {
      this.portalName = portalName;
      this.navigation = navigation;
      this.resourceBundle = res;
      this.isUserNode = isUserNode;
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public String getUri() {
      if(isUserNode) {
        return "/" + portalName + "/" +userNode.getURI() ;
      }
      return "/" +portalName;
    }

    /**
     * Gets the page node uri.
     *
     * @return the page node uri
     */
    public String getPageNodeUri() {
      if(isUserNode) return userNode.getURI();
      return null;
    }

    /**
     * Gets the icon.
     *
     * @return the icon
     */
    public String getIcon() {
      if(!isUserNode) return "";
      return userNode.getIcon();
    }

    /**
     * Gets the tree node children.
     *
     * @return the tree node children
     */
    public List<TreeNode> getTreeNodeChildren() { return children; }

    /**
     * Sets the tree node children.
     *
     * @param list the new tree node children
     */
    public void setTreeNodeChildren(List<TreeNode> list) { this.children = list; }

    /**
     * Sets the page node.
     *
     * @param userNode the new user node
     */
    public void setUserNode(UserNode userNode) {
      this.userNode = userNode;
      if(userNode.getChildren() == null) {
        children = null;
      }
    }

    /**
     * Checks if is page node.
     *
     * @return true, if is page node
     */
    public boolean isPageNode() {return isUserNode;}

    /**
     * Sets the checks if is page node.
     *
     * @param isPageNode the new checks if is page node
     */
    public void setIsPageNode(boolean isPageNode) {this.isUserNode = isPageNode;}

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
      if(isUserNode) return userNode.getName();
      return portalName;
    }

    /**
     * Gets the resolved label.
     *
     * @return the resolved label
     */
    public String getResolvedLabel() {
      if(isUserNode) return userNode.getEncodedResolvedLabel();
      return portalName;
    }

    /**
     * Sets the portal name.
     *
     * @param s the new portal name
     */
    public void setPortalName(String s) { this.portalName = s; }

    /**
     * Gets the portal name.
     *
     * @return the portal name
     */
    public String getPortalName() {return this.portalName; }

    /**
     * Sets the children by page nodes.
     *
     * @param userNodes the new children by user nodes
     *
     * @throws Exception the exception
     */
    public void setChildrenByUserNodes(Collection<UserNode> userNodes) throws Exception {
      if(userNodes == null) return;
      List<TreeNode> list = new ArrayList<TreeNode>();
      UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
      for(UserNode pNode: userNodes) {
        if (pNode.getPageRef() == null) continue;
        PageContext page = userPortalConfigService.getPage(pNode.getPageRef());
        if (page == null) continue;
        TreeNode treeNode = new TreeNode(portalName, navigation, resourceBundle, true);
        treeNode.setUserNode(pNode);
        treeNode.setChildrenByUserNodes(pNode.getChildren());
        list.add(treeNode);
      }
      setTreeNodeChildren(list);
    }

    /**
     * Search tree node by uri.
     *
     * @param uri the uri
     *
     * @return the tree node
     *
     * @throws Exception the exception
     */
    public TreeNode searchTreeNodeByURI(String uri) throws Exception {

      /**
      * TODO: the API for loading navigations was changed (replaced [PageNavigation, PageNode] by [UserNavigation, UserNode])
      * after refactoring, PageNavigationUtils class was removed from inside API so we can't use this class any more
      *
      * UIPublicationTree class is useless in ECMS project now,
      * so we've temporarily commented some lines below and we will refactor them later
      */
      //      if (uri.equals("/" + portalName)) {
      //        TreeNode treeNode = new TreeNode(portalName, navigation, resourceBundle, false);
      //        treeNode.setChildrenByPageNodes(navigation.getNodes());
      //        return treeNode;
      //      }
      //      String pageNodeURI = StringUtils.substringAfter(uri, "/" + portalName + "/");
      //      PageNode other = PageNavigationUtils.searchPageNodeByUri(this.navigation, pageNodeURI);
      //      if(other == null) return null;
      //      TreeNode treeNode = new TreeNode(portalName,navigation,resourceBundle, true);
      //      treeNode.setPageNode(other);
      //      treeNode.setChildrenByPageNodes(other.getChildren());
      TreeNode treeNode = new TreeNode(portalName,navigation,resourceBundle, true);
      return treeNode;
    }

    /**
     * Gets the children.
     *
     * @return the children
     */
    public List<TreeNode> getChildren() {
      return children;
    }

    /**
     * Gets the navigation.
     *
     * @return the navigation
     */
    public UserNavigation getNavigation() {
      return navigation;
    }

    /**
     * Gets the page node.
     *
     * @return the page node
     */
    public UserNode getUserNode() {
      return userNode;
    }
  }
}
