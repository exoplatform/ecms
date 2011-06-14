/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.rightclick.viewinfor;

import javax.jcr.Node;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Nov
 * 3, 2010
 */
@ComponentConfig (
   lifecycle = UIContainerLifecycle.class
)
public class UIViewInfoManager extends UIContainer implements UIPopupComponent {

  /**
   * the node that is selected by right clicking
   */
  private NodeLocation selectedNode;

  /**
   * the constructor
   * @throws Exception
   */
  public UIViewInfoManager() throws Exception {
    addChild(UIViewInfoContainer.class, null, null);
  }

  /**
   * get selected node
   * @return selected node
   */
  public Node getSelectedNode() {
    return NodeLocation.getNodeByLocation(selectedNode);
  }

  /**
   * set value for selected node
   * @param selectedNode
   */
  public void setSelectedNode(Node selectedNode) {
    this.selectedNode = NodeLocation.getNodeLocationByNode(selectedNode);
  }

  public void activate() throws Exception {
    getChild(UIViewInfoContainer.class).readNodeInformation();
  }

  public void deActivate() throws Exception { }

}
