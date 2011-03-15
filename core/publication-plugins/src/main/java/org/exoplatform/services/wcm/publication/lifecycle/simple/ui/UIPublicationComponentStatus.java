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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 * romain.denarie@exoplatform.com
 * 29 mai 08
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/publication/lifecycle/simple/ui/UIPublicationStatus.gtmpl",
    events = {
      @EventConfig(listeners = UIPublicationComponentStatus.CloseActionListener.class)
    }
)

public class UIPublicationComponentStatus extends UIForm {

  /** The node_. */
  private NodeLocation nodeLocation;

  /**
   * Instantiates a new uI publication component status.
   *
   * @throws Exception the exception
   */
  public UIPublicationComponentStatus() throws Exception {
  }

  /**
   * Instantiates a new uI publication component status.
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  public UIPublicationComponentStatus(Node node) throws Exception {
    nodeLocation = NodeLocation.make(node);
  }

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
    nodeLocation = NodeLocation.make(node);
  }

  /**
   * Gets the node name.
   *
   * @return the node name
   */
  public String getNodeName() {
    try {
      return getNode().getName();
    } catch (Exception e) {
      return "Error in getNodeName";
    }
  }

  /**
   * Gets the life cycle name.
   *
   * @return the life cycle name
   */
  public String getLifeCycleName () {
    try {
      PublicationService service = getApplicationComponent(PublicationService.class) ;
      return service.getNodeLifecycleName(getNode());
    } catch (Exception e) {
      return "Error in getLifeCycleName";
    }
  }

  /**
   * Gets the state name.
   *
   * @return the state name
   */
  public String getStateName () {
    try {
      PublicationService service = getApplicationComponent(PublicationService.class) ;
      return service.getCurrentState(getNode());
    } catch (Exception e) {
      return "Error in getStateName";
    }
  }

  /**
   * Gets the link state image.
   *
   * @param locale the locale
   *
   * @return the link state image
   */
  public String getLinkStateImage (Locale locale) {
    try {
      DownloadService dS = getApplicationComponent(DownloadService.class);
      PublicationService service = getApplicationComponent(PublicationService.class) ;

      byte[] bytes=service.getStateImage(getNode(),locale);
      InputStream iS = new ByteArrayInputStream(bytes);
      String id = dS.addDownloadResource(new InputStreamDownloadResource(iS, "image/gif"));
      return dS.getDownloadLink(id);
    } catch (Exception e) {
      return "Error in getStateImage";
    }
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  public static class CloseActionListener extends EventListener<UIPublicationComponentStatus> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationComponentStatus> event) throws Exception {
      UIPublicationComponentStatus publicationComponentStatus = event.getSource();
      UIPublishingPanel publishingPanel = publicationComponentStatus.getAncestorOfType(UIPublishingPanel.class);
      UIPopupWindow popupAction = publishingPanel.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false);
      popupAction.setRendered(false);
    }
  }
}
