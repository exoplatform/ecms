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

import javax.jcr.Node;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com
 * Mar 19, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublishClvChooser.gtmpl",
                 events = {
    @EventConfig(listeners = UIPublishClvChooser.CloseActionListener.class) })
public class UIPublishClvChooser extends UIForm implements UIPopupComponent {

  /** The page. */
  private Page page;

  /** The node. */
  private NodeLocation nodeLocation;

  /**
   * Gets the page.
   *
   * @return the page
   */
  public Page getPage() {return page;}

  /**
   * Sets the page.
   *
   * @param page the new page
   */
  public void setPage(Page page) {this.page = page;}

  /**
   * Gets the node.
   *
   * @return the node
   */
  public Node getNode() {
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  /**
   * Sets the node.
   *
   * @param node the new node
   */
  public void setNode(Node node) {
    nodeLocation = NodeLocation.getNodeLocationByNode(node);
  }

  /**
   * Instantiates a new uI publish clv chooser.
   */
  public UIPublishClvChooser() {
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
  public static class CloseActionListener extends EventListener<UIPublishClvChooser> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublishClvChooser> event) throws Exception {
      UIPublishClvChooser clvChooser = event.getSource();
      UIPopupWindow popupWindow = clvChooser.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setShow(false);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#activate()
   */
  public void activate() {
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#deActivate()
   */
  public void deActivate() {
  }
}
