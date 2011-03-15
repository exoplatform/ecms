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
package org.exoplatform.services.wcm.publication.lifecycle.simple.ui;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 9, 2008
 */
@ComponentConfig (
    lifecycle = UIApplicationLifecycle.class,
    template = "classpath:groovy/wcm/webui/publication/lifecycle/simple/ui/UIPublishingPanel.gtmpl"
)
public class UIPublishingPanel extends UIForm implements UIPopupComponent {

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
    currentNodeLocation = NodeLocation.make(node);
  }

  /**
   * Instantiates a new uI publishing panel.
   *
   * @throws Exception the exception
   */
  public UIPublishingPanel() throws Exception {
    addChild(UIPublicationAction.class,null,"UIPublicationAction");
    addChild(UIPublicationComponentStatus.class, null, "UIPublicationComponentStatus");
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
  public void initPanel(Node node,String portalName,List<String> runningPortals) throws Exception {
    currentNodeLocation = NodeLocation.make(node);
    UIPublicationComponentStatus publicationComponentStatus = getChild(UIPublicationComponentStatus.class);
    publicationComponentStatus.setNode(getNode());
  }

  public void activate() throws Exception {

  }

  public void deActivate() throws Exception {

  }
}
