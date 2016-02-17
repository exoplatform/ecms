/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationTree.TreeNode;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com
 * Mar 4, 2009
 */
@ComponentConfig (
                  lifecycle = UIFormLifecycle.class,
                  template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublicationPages.gtmpl",
                  events = @EventConfig(listeners = UIPublicationPages.CloseActionListener.class)
)
public class UIPublicationPages extends UIForm {

  /** The current node. */
  private NodeLocation currentNodeLocation;

  /**
   * Gets the node.
   *
   * @return the node
   */
  public Node getNode() {
    return NodeLocation.getNodeByLocation(currentNodeLocation);
  }

  /**
   * Sets the node.
   *
   * @param node the new node
   */
  public void setNode(Node node) {
    currentNodeLocation = NodeLocation.getNodeLocationByNode(node);
  }

  /**
   * Instantiates a new uI publishing panel.
   *
   * @throws Exception the exception
   */
  public UIPublicationPages() throws Exception {
    addChild(UIPortalNavigationExplorer.class,null,"UIPortalNavigationExplorer");
    addChild(UIPublicationAction.class,null,"UIPublicationAction");
    addChild(UIPublishedPages.class,null,"UIPublishedPages");
    addChild(UIPopupWindow.class, null, "UIClvPopupWindowr");
  }

  /**
   * Inits the panel.
   *
   * @param node the node
   * @param portalName the portal name
   * @param runningPortals the running portals
   *
   * @throws Exception the exception
   */
  public void init(Node node,String portalName,List<String> runningPortals) throws Exception {
    currentNodeLocation = NodeLocation.getNodeLocationByNode(node);
    UIPortalNavigationExplorer poExplorer = getChild(UIPortalNavigationExplorer.class);
    poExplorer.init(portalName,runningPortals);
    UIPublishedPages publishedPages = getChild(UIPublishedPages.class);
    publishedPages.init();
  }

  /**
   * Gets the current portal.
   *
   * @return the current portal
   */
  public String getCurrentPortal() {
    UIPortalNavigationExplorer portalNavigationExplorer = getChild(UIPortalNavigationExplorer.class);
    TreeNode selectedNode = portalNavigationExplorer.getSelectedNode();
    if (selectedNode != null) return selectedNode.getPortalName();
    return null;
  }

  /**
   * Gets the current tree node.
   *
   * @return the current tree node
   */
  public String getCurrentTreeNode() {
    UIPortalNavigationExplorer portalNavigationExplorer = getChild(UIPortalNavigationExplorer.class);
    TreeNode selectedNode = portalNavigationExplorer.getSelectedNode();
    if (selectedNode != null) return selectedNode.getName();
    return null;
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener</code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class CloseActionListener extends EventListener<UIPublicationPages> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPages> event) throws Exception {
      UIPublicationPages publicationPages = event.getSource();
      UIPopupContainer uiPopupContainer = (UIPopupContainer) publicationPages.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
}
