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
package org.exoplatform.services.ecm.publication.plugins.webui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 29 mai 08
 */
@ComponentConfig (
    lifecycle = UIApplicationLifecycle.class,
    template = "classpath:resources/templates/staticdirect/publication.gtmpl"
)

public class UIPublicationComponent extends UIComponent {

  private NodeLocation node_;
  private static final Log LOG  = ExoLogger.getLogger("component.UIPublicationComponent");
  public UIPublicationComponent() throws Exception {
  }

  public UIPublicationComponent(Node node) throws Exception {
    this.node_= NodeLocation.getNodeLocationByNode(node);
  }

  public Node getNode() {
    return NodeLocation.getNodeByLocation(this.node_);
  }

  public void setNode(Node node) {
    this.node_= NodeLocation.getNodeLocationByNode(node);
  }

  public String getNodeName() {
    try {
      return getNode().getName();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getNodeName";
    }
  }

  public String getLifeCycleName () {
    try {
      PublicationService service = getApplicationComponent(PublicationService.class) ;
      return service.getNodeLifecycleName(getNode());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getLifeCycleName";
    }
  }

  public String getStateName () {
    try {
      PublicationService service = getApplicationComponent(PublicationService.class) ;
      return service.getCurrentState(getNode());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getStateName";
    }
  }

  public String getLinkStateImage (Locale locale) {
    try {
      DownloadService dS = getApplicationComponent(DownloadService.class);
      PublicationService service = getApplicationComponent(PublicationService.class) ;

      byte[] bytes=service.getStateImage(getNode(),locale);
      InputStream iS = new ByteArrayInputStream(bytes);
      String id = dS.addDownloadResource(new InputStreamDownloadResource(iS, "image/gif"));
      return dS.getDownloadLink(id);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return "Error in getStateImage";
    }
  }

}
