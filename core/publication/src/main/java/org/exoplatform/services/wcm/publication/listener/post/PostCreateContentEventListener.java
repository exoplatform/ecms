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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
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

  private static final Log LOG = ExoLogger.getLogger(PostCreateContentEventListener.class.getName());

  public static final String POST_INIT_STATE_EVENT      = "PublicationService.event.postInitState";

  /** The publication service. */
  private WCMPublicationService publicationService;

  /** The publication service. */
  private WCMConfigurationService configurationService;

  /** The web content schema handler. */
  private WebContentSchemaHandler webContentSchemaHandler;
  
  private ListenerService         listenerService = null;
  private ActivityCommonService   activityService;

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
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<CmsService, Node> event) throws Exception {
    if (listenerService==null) {
      listenerService = WCMCoreUtils.getService(ListenerService.class);
    }
    Node currentNode = event.getData();
    //add mixin exo:webContentChild for default.html/jcr:content of webContent
    try {
      if (currentNode.canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD) && 
          currentNode.isNodeType(NodetypeConstant.NT_RESOURCE) &&
          currentNode.getParent().isNodeType(NodetypeConstant.NT_FILE) && 
          "default.html".equals(currentNode.getParent().getName()) &&
          currentNode.getParent().getParent().isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
        currentNode.addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error, can not add mixin '" + NodetypeConstant.EXO_WEBCONTENT_CHILD + "' to node: " + currentNode.getPath());
      }
    }
    //add mixin exo:webContentChild for css/default.css/jcr:content of webContent
    try {
      if (currentNode.canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD) && 
          currentNode.isNodeType(NodetypeConstant.NT_RESOURCE) &&
          currentNode.getParent().isNodeType(NodetypeConstant.NT_FILE) && 
          "default.css".equals(currentNode.getParent().getName()) &&
          currentNode.getParent().getParent().isNodeType(NodetypeConstant.EXO_CSS_FOLDER) &&
          "css".equals(currentNode.getParent().getParent().getName()) &&
          currentNode.getParent().getParent().getParent().isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
        currentNode.addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error, can not add mixin '" + NodetypeConstant.EXO_WEBCONTENT_CHILD + "' to node: " + currentNode.getPath());
      }
    }
    //add mixin exo:webContentChild for js/default.js/jcr:content of webContent
    try {
      if (currentNode.canAddMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD) && 
          currentNode.isNodeType(NodetypeConstant.NT_RESOURCE) &&
          currentNode.getParent().isNodeType(NodetypeConstant.NT_FILE) && 
          "default.js".equals(currentNode.getParent().getName()) &&
          currentNode.getParent().getParent().isNodeType(NodetypeConstant.EXO_JS_FOLDER) &&
          "js".equals(currentNode.getParent().getParent().getName()) &&
          currentNode.getParent().getParent().getParent().isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
        currentNode.addMixin(NodetypeConstant.EXO_WEBCONTENT_CHILD);
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error, can not add mixin '" + NodetypeConstant.EXO_WEBCONTENT_CHILD + "' to node: " + currentNode.getPath());
      }
    }
    //---------------------------------------------------------------------------------------------------------------------------
    if(currentNode.canAddMixin("exo:rss-enable")) {
      currentNode.addMixin("exo:rss-enable");
    }
    if (currentNode.isNodeType("exo:rss-enable") && !currentNode.hasProperty("exo:title") || 
        currentNode.hasProperty("exo:title") && StringUtils.isEmpty(currentNode.getProperty("exo:title").getString())) {
      currentNode.setProperty("exo:title", Text.unescapeIllegalJcrChars(currentNode.getName()));
    }
    if (currentNode.isNodeType("exo:cssFile") || currentNode.isNodeType("exo:jsFile")
        || currentNode.getParent().isNodeType("exo:actionStorage")) {
      if (currentNode.isNodeType("exo:cssFile") || currentNode.isNodeType("exo:jsFile")) {
        CmsService cmsService = WCMCoreUtils.getService(CmsService.class);
        listenerService.broadcast(POST_INIT_STATE_EVENT, cmsService, currentNode);
      }
      return;
    }

    Session session = currentNode.getSession();
    String nodePath = currentNode.getPath();
    currentNode.getParent().save();
    currentNode = (Node)session.getItem(nodePath);

    if (currentNode instanceof NodeImpl && !((NodeImpl)currentNode).isValid()) {
      currentNode = (Node)session.getItem(nodePath);
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
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
      if (LOG.isDebugEnabled()) LOG.debug("No portal context available");
    }
    if (LOG.isInfoEnabled()) LOG.info(currentNode.getPath() + "::" + siteName + "::"+remoteUser);
    if (remoteUser == null) {
      remoteUser = IdentityConstants.ANONIM;
    }
    publicationService.updateLifecyleOnChangeContent(currentNode, siteName, remoteUser);     
  }
}
