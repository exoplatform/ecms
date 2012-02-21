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
package org.exoplatform.services.ecm.publication.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationPresentationService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 7 mai 08
 */
public class PublicationPresentationServiceImpl implements PublicationPresentationService {

  protected static Log log;
  private Map<String, PublicationPlugin> publicationPlugins_ = new HashMap<String,PublicationPlugin>();

  public PublicationPresentationServiceImpl () {
    log = ExoLogger.getLogger("portal:PublicationPresentationServiceImpl");
    if (log.isInfoEnabled()) {
      log.info("# PublicationPresentationService initialization #");
    }
    this.publicationPlugins_ = new HashMap<String, PublicationPlugin>();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getStateUI(javax.jcr.Node)
   */
  public UIForm getStateUI(Node node, UIComponent component) throws NotInPublicationLifecycleException, Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PublicationService publicationService = (PublicationService) container.getComponentInstanceOfType(PublicationService.class);

    if (!publicationService.isNodeEnrolledInLifecycle(node)) {
      throw new NotInPublicationLifecycleException();
    }
    String lifecycleName=publicationService.getNodeLifecycleName(node);
    PublicationPlugin nodePlugin = this.publicationPlugins_.get(lifecycleName);
    return nodePlugin.getStateUI(node,component);
  }

  /**
   * Add a Publication Plugin to the service.
   * The method caches all added plugins.
   *
   * @param p the plugin to add
   */
  public void addPublicationPlugin(PublicationPlugin p) {
    this.publicationPlugins_.put(p.getLifecycleName(),p);
  }
}
