/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication.listener.post;

import javax.jcr.Node;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.webdav.WebDavService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          hadv@exoplatform.com
 * Sep 6, 2011  
 */
public class PostWebDavUploadEventListener extends Listener<WebDavService, Node> {

  /** The pservice. */
  private WCMPublicationService publicationService;

  private final static Log      LOG = ExoLogger.getLogger(PostWebDavUploadEventListener.class);

  public PostWebDavUploadEventListener(WCMPublicationService publicationService) {
    this.publicationService = publicationService;
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<WebDavService, Node> event) throws Exception {
    Node currentNode = null;

    try {
      currentNode = event.getData();
      LinkManager linkMng = WCMCoreUtils.getService(LinkManager.class);
      if (linkMng.isLink(currentNode)) {
        currentNode = linkMng.getTarget(currentNode);
      }

      if (currentNode == null || currentNode.isNodeType("exo:cssFile")
          || currentNode.isNodeType("exo:template") || currentNode.isNodeType("exo:jsFile")
          || currentNode.isNodeType("exo:action")) {
        return;
      }

      publicationService.updateLifecyleOnChangeContent(currentNode, "", currentNode.getSession()
                                                                                   .getUserID());
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An expected exception has occured: ", ex);
      }
    }
  }

}
