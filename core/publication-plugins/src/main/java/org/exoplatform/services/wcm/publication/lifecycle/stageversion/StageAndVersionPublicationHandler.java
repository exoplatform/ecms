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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 5, 2009
 */
public class StageAndVersionPublicationHandler extends BaseWebSchemaHandler {

  /** The Logger **/
  private static final Log LOG = ExoLogger.getLogger(StageAndVersionPublicationHandler.class);
  
  /** The template service. */
  private TemplateService templateService;

  /** The publication service. */
  private WCMPublicationService publicationService;

  /**
   * Instantiates a new stage and version publication handler.
   *
   * @param templateService the template service
   * @param publicationService the publication service
   */
  public StageAndVersionPublicationHandler(TemplateService templateService,
                                           WCMPublicationService publicationService) {
    this.templateService = templateService;
    this.publicationService = publicationService;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getHandlerNodeType()
   */
  protected String getHandlerNodeType() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getParentNodeType()
   */
  @Override
  protected String getParentNodeType() {
    return null;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#matchHandler(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public boolean matchHandler(Node node, SessionProvider sessionProvider) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WebSchemaConfigService schemaConfigService = (WebSchemaConfigService) container.
        getComponentInstanceOfType(WebSchemaConfigService.class);
    WebContentSchemaHandler webContentSchemaHandler = schemaConfigService.
        getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    if(webContentSchemaHandler.isWebcontentChildNode(node))
      return false;
    if(node.isNodeType("exo:cssFile") || node.isNodeType("exo:jsFile"))
      return false;
    String primaryNodeType = node.getPrimaryNodeType().getName();
    return templateService.isManagedNodeType(primaryNodeType);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onCreateNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onCreateNode(Node node, SessionProvider sessionProvider) throws Exception {
    Node checkNode = node;
    if(node.isNodeType("nt:file")) {
      if(node.canAddMixin("exo:rss-enable")) {
        node.addMixin("exo:rss-enable");
        if(!node.hasProperty("exo:title")) {
          node.setProperty("exo:title",node.getName());
        }
      }
      Node parentNode = node.getParent();
      if(parentNode.isNodeType("exo:webContent")) {
        checkNode = parentNode;
      }
    }

    String siteName = null, remoteUser = null;
    try {
      siteName = Util.getPortalRequestContext().getPortalOwner();
      remoteUser = Util.getPortalRequestContext().getRemoteUser();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    publicationService.updateLifecyleOnChangeContent(checkNode, siteName, remoteUser);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onModifyNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onModifyNode(Node node, SessionProvider sessionProvider) throws Exception {
    if(node.isNew())
      return;
    Node checkNode = node;
    if(node.isNodeType("nt:file")) {
      Node parentNode = node.getParent();
      if(parentNode.isNodeType("exo:webContent")) {
        checkNode = parentNode;
      }
    }
    String siteName = Util.getPortalRequestContext().getPortalOwner();
    String remoteUser = Util.getPortalRequestContext().getRemoteUser();
    publicationService.updateLifecyleOnChangeContent(checkNode, siteName, remoteUser);
  }
}
