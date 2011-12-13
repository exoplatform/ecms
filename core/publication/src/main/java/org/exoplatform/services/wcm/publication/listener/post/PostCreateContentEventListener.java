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
package org.exoplatform.services.wcm.publication.listener.post;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 5, 2009
 */
public class PostCreateContentEventListener extends Listener<CmsService, Node>{

  private static final Log log = ExoLogger.getLogger(PostCreateContentEventListener.class);
  
  public static final String POST_INIT_STATE_EVENT      = "PublicationService.event.postInitState";

  /** The publication service. */
  private WCMPublicationService publicationService;

  /** The publication service. */
  private WCMConfigurationService configurationService;

  /** The web content schema handler. */
  private WebContentSchemaHandler webContentSchemaHandler;

  /**
   * Instantiates a new post create content event listener.
   *
   * @param publicationService the publication service
   * @param configurationService the configuration service
   * @param schemaConfigService the schema config service
   */
  public PostCreateContentEventListener(WCMPublicationService publicationService,
                                        WCMConfigurationService configurationService,
                                        WebSchemaConfigService schemaConfigService) {
    this.publicationService = publicationService;
    this.configurationService = configurationService;
    webContentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<CmsService, Node> event) throws Exception {
    Node currentNode = event.getData();
    if(currentNode.canAddMixin("exo:rss-enable")) {
      currentNode.addMixin("exo:rss-enable");
      if (!currentNode.hasProperty("exo:title")) {
        currentNode.setProperty("exo:title", Text.unescapeIllegalJcrChars(currentNode.getName()));
      }
    }
    if (currentNode.isNodeType("exo:cssFile") || currentNode.isNodeType("exo:jsFile")
        || currentNode.getParent().isNodeType("exo:actionStorage")) {
      if (currentNode.isNodeType("exo:cssFile") || currentNode.isNodeType("exo:jsFile")) {
        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        CmsService cmsService = WCMCoreUtils.getService(CmsService.class);
        listenerService.broadcast(POST_INIT_STATE_EVENT, cmsService, currentNode);
      }
      return;
    }

    Session session = currentNode.getSession();
    String nodePath = currentNode.getPath();
    currentNode.getSession().save();
    currentNode = (Node)session.getItem(nodePath);

    if (currentNode instanceof NodeImpl && !((NodeImpl)currentNode).isValid()) {
      currentNode = (Node)session.getItem(nodePath);
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      LinkManager linkManager = (LinkManager)container.getComponentInstanceOfType(LinkManager.class);
      if (linkManager.isLink(currentNode)) {
        try {
          currentNode = linkManager.getTarget(currentNode, false);
        } catch (Exception ex) {
          currentNode = linkManager.getTarget(currentNode, true);
        }
      }
    }

    String siteName = null, remoteUser = null;
    try {
     siteName = Util.getPortalRequestContext().getPortalOwner();
     remoteUser = Util.getPortalRequestContext().getRemoteUser();
    } catch (NullPointerException npe) {
      if (log.isDebugEnabled()) log.debug("No portal context available");
    }
    if (log.isInfoEnabled()) log.info(currentNode.getPath() + "::" + siteName + "::"+remoteUser);
    if (remoteUser != null) publicationService.updateLifecyleOnChangeContent(currentNode, siteName, remoteUser);
  }
}
