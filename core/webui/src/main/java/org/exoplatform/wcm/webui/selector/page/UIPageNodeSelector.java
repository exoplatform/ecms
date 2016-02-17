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
package org.exoplatform.wcm.webui.selector.page;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.navigation.NavigationUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIDropDownControl;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : chungnv
 * nguyenchung136@yahoo.com
 * Jun 23, 2006
 * 10:07:15 AM
 */
@ComponentConfigs({
  @ComponentConfig(
      template = "classpath:groovy/wcm/webui/selector/page/UIPageNodeSelector.gtmpl" ,
      events = {
        @EventConfig(listeners = UIPageNodeSelector.SelectNavigationActionListener.class, phase=Phase.DECODE)
      }
  ),
  @ComponentConfig (
      type = UIDropDownControl.class ,
      id = "UIDropDown",
      template = "classpath:groovy/wcm/webui/selector/page/UINavigationSelector.gtmpl",
      events = {
        @EventConfig(listeners = UIPageNodeSelector.SelectNavigationActionListener.class)
      }
    )
})
public class UIPageNodeSelector extends UIContainer {

  /** The navigations. */
  private List<UserNavigation> navigations;

  /** The selected node. */
  private SelectedNode selectedNode;

  /** The copy node. */
  private SelectedNode copyNode;

  /** The delete navigations. */
  private List<UserNavigation> deleteNavigations = new ArrayList<UserNavigation>();

  /** the user portal  */
  private UserPortal userPortal;
  
  /**
   * Instantiates a new uI page node selector.
   *
   * @throws Exception the exception
   */
  public UIPageNodeSelector() throws Exception {    
    userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
    
    UIDropDownControl uiDopDownControl = addChild(UIDropDownControl.class, "UIDropDown", "UIDropDown");
    uiDopDownControl.setParent(this);

    UITree uiTree = addChild(UITree.class, null, "TreeNodeSelector");        
    uiTree.setIcon("DefaultPageIcon");
    uiTree.setSelectedIcon("DefaultPageIcon");
    uiTree.setBeanIdField("URI");
    uiTree.setBeanChildCountField("ChildrenCount");
    uiTree.setBeanLabelField("encodedResolvedLabel");
    uiTree.setBeanIconField("icon");

    loadNavigations();
  }

  /**
   * Load navigations.
   *
   * @throws Exception the exception
   */
  public void loadNavigations() throws Exception {
    // get all navigations
    navigations = new ArrayList<UserNavigation>();
    navigations.addAll(userPortal.getNavigations());

    // check navigation list
    if (navigations == null || navigations.size() <= 0) {
      getChild(UIDropDownControl.class).setOptions(null);
      getChild(UITree.class).setSibbling(null);
      return;
    }

    // set option values for navigation selector dropdown
    updateNavigationSelector();

    // choose one navigation and show it on UI
    chooseAndShowNavigation();
  }

  /**
   * Choose one navigation and show it on UI
   *
   * @throws Exception
   */
  private void chooseAndShowNavigation() throws Exception {
    // select the navigation of current portal
    
    String currentPortalName = Util.getPortalRequestContext().getUserPortalConfig().getPortalName();
    UserNavigation portalSelectedNav = NavigationUtils.getUserNavigationOfPortal( userPortal, currentPortalName);

    int portalSelectedNavId = getId(portalSelectedNav);
    if (getUserNavigation(portalSelectedNavId) != null) {
      selectNavigation(portalSelectedNavId);
      UserNode portalSelectedNode = Util.getUIPortal().getSelectedUserNode();
      if (portalSelectedNode != null)
        selectUserNodeByUri(portalSelectedNode.getURI());
      return;
    }

    // select the first navigation
    UserNavigation firstNav = navigations.get(0);
    selectNavigation(getId(firstNav));
    UserNode rootNode = userPortal.getNode(firstNav,
                                           NavigationUtils.ECMS_NAVIGATION_SCOPE,
                                           null, null);
    Iterator<UserNode> childrenIter = rootNode.getChildren().iterator();
    if (childrenIter.hasNext()) {
      selectUserNodeByUri(childrenIter.next().getURI());
    }
  }

  /**
   */
  public int getId(UserNavigation nav) {
    return (nav.getKey().getTypeName() + "::" + nav.getKey().getName()).hashCode();
  }

  /**
   * get index of a navigation in navigation list
   *
   * @param navId the identify of navigation
   * @return the index of the navigation in navigation list
   */
  private int getIndex(int navId) {
    int index = -1;

    if (navigations == null || navigations.size() <= 0) {
      return index;
    }

    for (int i = 0; i < navigations.size(); i++) {
      UserNavigation nav = navigations.get(i);
      if (getId(nav) == navId) {
        index = i;
        break;
      }
    }

    return index;
  }

  /**
   * Set option values for navigation selector dropdown
   */
  private void updateNavigationSelector() {

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    for (UserNavigation navigation : navigations) {
      options.add(new SelectItemOption<String>(navigation.getKey().getTypeName() + ":"
          + navigation.getKey().getName(), String.valueOf(getId(navigation))));
    }
    UIDropDownControl uiNavigationSelector = getChild(UIDropDownControl.class);
    uiNavigationSelector.setOptions(options);
    if (options.size() > 0)
      uiNavigationSelector.setValue(0);
  }

  /**
   * Select navigation.
   *
   * @param id the id
   */
  public void selectNavigation(int id) throws Exception {
    UserNavigation selectedNav = getUserNavigation(id);
    if (selectedNav == null) {
      return;
    }

    UserNode rootNode = userPortal.getNode(selectedNav,
                                           NavigationUtils.ECMS_NAVIGATION_SCOPE,
                                           null,
                                           null);
    selectedNode = new SelectedNode(selectedNav, rootNode, null, null);
    selectUserNodeByUri(null);

    // update tree
    UITree uiTree = getChild(UITree.class);
    uiTree.setSibbling(rootNode.getChildren());

    // update dropdown
    UIDropDownControl uiDropDownSelector = getChild(UIDropDownControl.class);
    uiDropDownSelector.setValue(getIndex(id));
  }

  /**
   * Select page node by uri.
   *
   * @param uri the uri
   */
  public void selectUserNodeByUri(String uri) throws Exception {
    if (selectedNode == null || uri == null)
      return;
    UITree tree = getChild(UITree.class);
    Collection<?> sibbling = tree.getSibbling();
    tree.setSibbling(null);
    tree.setParentSelected(null);

    UserNavigation selectedNav = selectedNode.getUserNavigation();
    UserNode userNode = userPortal.resolvePath(selectedNav, null, uri);

    if (userNode != null) {
      userPortal.updateNode(userNode, NavigationUtils.ECMS_NAVIGATION_SCOPE, null);
      if (userNode != null) {
        // selectedNode.setNode(searchUserNodeByUri(selectedNode.getRootNode(), uri));
        selectedNode.setNode(userNode);
        selectedNode.setParentNode(userNode.getParent());

        tree.setParentSelected(selectedNode.getParentNode());
        tree.setSibbling(selectedNode.getParentNode().getChildren());
        tree.setSelected(selectedNode.getNode());
        tree.setChildren(selectedNode.getNode().getChildren());
        return;
      }
    }

    tree.setSelected(null);
    tree.setChildren(null);
    tree.setSibbling(sibbling);
  }

  /**
   * Gets the user navigations.
   *
   * @return the page navigations
   */
  public List<UserNavigation> getUserNavigations() {
    if(navigations == null) navigations = new ArrayList<UserNavigation>();
    return navigations;
  }

  /**
   * Gets the user navigation.
   *
   * @param id the id
   * @return the page navigation
   */
  public UserNavigation getUserNavigation(int id) {
    for (UserNavigation nav : getUserNavigations()) {
      if (getId(nav) == id)
        return nav;
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    UIRightClickPopupMenu uiPopupMenu = getChild(UIRightClickPopupMenu.class);
    if(uiPopupMenu != null) {
      if(navigations == null || navigations.size() < 1) uiPopupMenu.setRendered(false) ;
      else uiPopupMenu.setRendered(true) ;
    }
    super.processRender(context) ;
  }

  /**
   * Gets the copy node.
   *
   * @return the copy node
   */
  public SelectedNode getCopyNode() { return copyNode; }

  /**
   * Sets the copy node.
   *
   * @param copyNode the new copy node
   */
  public void setCopyNode(SelectedNode copyNode) { this.copyNode = copyNode; }

  /**
   * The listener interface for receiving selectNavigationAction events.
   * The class that is interested in processing a selectNavigationAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectNavigationActionListener</code> method. When
   * the selectNavigationAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class SelectNavigationActionListener  extends EventListener<UIDropDownControl> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIDropDownControl> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDropDownControl uiDropDownControl = event.getSource();
      UIPageNodeSelector uiPageNodeSelector = uiDropDownControl.getAncestorOfType(UIPageNodeSelector.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPageNodeSelector.getParent()) ;
      if(id != null) uiPageNodeSelector.selectNavigation(Integer.parseInt(id));
      try {
        UIPageSelector pageSelector = uiPageNodeSelector.getAncestorOfType(UIPageSelector.class);
        UIPageSelectorPanel pageSelectorPanel = pageSelector.getChild(UIPageSelectorPanel.class);
        pageSelectorPanel.setSelectedNode(uiPageNodeSelector.getSelectedNode().getNode());
        pageSelectorPanel.updateGrid();

        event.getRequestContext().addUIComponentToUpdateByAjax(pageSelector);
      } catch (Exception ex) {
        org.exoplatform.wcm.webui.Utils.createPopupMessage(uiPageNodeSelector,
                                                           "UIMessageBoard.msg.select-navigation",
                                                           null,
                                                           ApplicationMessage.ERROR);
      }
      uiPageNodeSelector.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());

    }
  }

  /**
   * The Class SelectedNode.
   */
  public static class SelectedNode {

    /** The nav. */
    private UserNavigation nav;

    /** The parent node. */
    private UserNode       parentNode;

    /** The node. */
    private UserNode       node;

    private UserNode       rootNode;

    /** The delete node. */
    private boolean        deleteNode = false;

    /** The clone node. */
    private boolean        cloneNode  = false;

    /**
     * Instantiates a new selected node.
     *
     * @param nav the nav
     * @param parentNode the parent node
     * @param node the node
     */
    public SelectedNode(UserNavigation nav, UserNode rootNode, UserNode parentNode, UserNode node) {
      this.nav = nav;
      this.rootNode = rootNode;
      this.parentNode = parentNode;
      this.node = node;
    }

    /**
     * Gets the user navigation.
     *
     * @return the user navigation
     */
    public UserNavigation getUserNavigation() {
      return nav;
    }

    /**
     * Sets the page navigation.
     *
     * @param nav the new page navigation
     */
    public void setUserNavigation(UserNavigation nav) {
      this.nav = nav;
    }

    /**
     * Gets the root node
     *
     * @return the root node
     */
    public UserNode getRootNode() {
      return rootNode;
    }

    /**
     * Sets the root node
     *
     * @param rootNode the root node
     */
    public void setRootNode(UserNode rootNode) {
      this.rootNode = rootNode;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent node
     */
    public UserNode getParentNode() {
      return parentNode;
    }

    /**
     * Sets the parent node.
     *
     * @param parentNode the new parent node
     */
    public void setParentNode(UserNode parentNode) {
      this.parentNode = parentNode;
    }

    /**
     * Gets the node.
     *
     * @return the node
     */
    public UserNode getNode() {
      return node;
    }

    /**
     * Sets the node.
     *
     * @param node the new node
     */
    public void setNode(UserNode node) {
      this.node = node;
    }

    /**
     * Checks if is delete node.
     *
     * @return true, if is delete node
     */
    public boolean isDeleteNode() {
      return deleteNode;
    }

    /**
     * Sets the delete node.
     *
     * @param deleteNode the new delete node
     */
    public void setDeleteNode(boolean deleteNode) {
      this.deleteNode = deleteNode;
    }

    /**
     * Checks if is clone node.
     *
     * @return true, if is clone node
     */
    public boolean isCloneNode() {
      return cloneNode;
    }

    /**
     * Sets the clone node.
     *
     * @param b the new clone node
     */
    public void setCloneNode(boolean b) {
      cloneNode = b;
    }
  }

  /**
   * Gets the selected node.
   *
   * @return the selected node
   */
  public SelectedNode getSelectedNode() {
    return selectedNode;
  }

  /**
   * Gets the selected navigation.
   *
   * @return the selected navigation
   */
  public UserNavigation getSelectedNavigation() {
    return selectedNode == null ? null : selectedNode.getUserNavigation();
  }

  /**
   * Gets the root node of the selected navigation.
   *
   * @return the root node of the selected navigation.
   */
  public UserNode getRootNodeOfSelectedNav() {
    return selectedNode == null ? null : selectedNode.getRootNode();
  }

  /**
   * Gets the selected page node.
   *
   * @return the selected page node
   */
  public UserNode getSelectedUserNode() {
    return selectedNode == null ? null : selectedNode.getNode();
  }

  /**
   * Gets the up level uri.
   *
   * @return the up level uri
   */
  public String getUpLevelUri() {
    return selectedNode.getParentNode().getURI();
  }

  /**
   * Gets the delete navigations.
   *
   * @return the delete navigations
   */
  public List<UserNavigation> getDeleteNavigations() {
    return deleteNavigations;
  }
}
