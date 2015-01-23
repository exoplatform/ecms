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
package org.exoplatform.wcm.webui.scv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.NodePresentation;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.ecm.webui.presentation.removecomment.RemoveCommentComponent;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 * dzungdev@gmail.com
 * Jun 9, 2008
 */
@ComponentConfig(
    lifecycle = Lifecycle.class,
    events = {
      @EventConfig(listeners = UIPresentation.DownloadActionListener.class),
      @EventConfig(listeners = UIPresentation.SwitchToAudioDescriptionActionListener.class),
      @EventConfig(listeners = UIPresentation.SwitchToOriginalActionListener.class),
      @EventConfig(listeners = UIBaseNodePresentation.OpenDocInDesktopActionListener.class)    
  }
)

public class UIPresentation extends UIBaseNodePresentation {

  private static final Log LOG  = ExoLogger.getLogger(UIPresentation.class.getName());

  private NodeLocation originalNodeLocation;

  private NodeLocation viewNodeLocation;

  String templatePath = null;

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getOriginalNode()
   */
  public Node getOriginalNode() throws Exception {
    return originalNodeLocation == null ? null :
           Utils.getViewableNodeByComposer(originalNodeLocation.getRepository(),
                                           originalNodeLocation.getWorkspace(),
                                           originalNodeLocation.getPath(),
                                           WCMComposer.BASE_VERSION);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getNode()
   */
  public void setOriginalNode(Node node) throws Exception{
    originalNodeLocation = NodeLocation.getNodeLocationByNode(node);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getNode()
   */
  public Node getNode() throws Exception {
    Node ret = getDisplayNode();
    if (NodePresentation.MEDIA_STATE_DISPLAY.equals(getMediaState()) &&
          (ret.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) ||
              (ret.isNodeType(NodetypeConstant.NT_FROZEN_NODE) && 
               NodetypeConstant.EXO_ACCESSIBLE_MEDIA.equals(ret.getProperty("jcr:frozenPrimaryType").getString())))) {
      Node audioDescription = org.exoplatform.services.cms.impl.Utils.getChildOfType(ret, NodetypeConstant.EXO_AUDIO_DESCRIPTION);
      if (audioDescription != null) {
        return audioDescription;
      }
    }
    return ret;
  }
  
  public String getFastPublicLink(Node viewNode) {
    String fastPublishLink = null;
    try {
      UIPresentationContainer container = (UIPresentationContainer)this.getParent();      
      fastPublishLink = container.event("FastPublish", NodeLocation.getExpressionByNode(viewNode));
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return fastPublishLink;
  }
  
  public Node getDisplayNode() throws Exception {
    if (viewNodeLocation == null) return null;
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
    String sharedCache = preferences.getValue(UISingleContentViewerPortlet.ENABLE_CACHE, "true");
    sharedCache = "true".equals(sharedCache) ? WCMComposer.VISIBILITY_PUBLIC:WCMComposer.VISIBILITY_USER;
    Node ret = Utils.getViewableNodeByComposer(viewNodeLocation.getRepository(),
                                           viewNodeLocation.getWorkspace(),
                                           viewNodeLocation.getPath(),
                                           null,
                                           sharedCache);
    return ret;
  }
  

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#setNode(javax.jcr.Node)
   */
  public void setNode(Node node) {
    viewNodeLocation = NodeLocation.getNodeLocationByNode(node);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getRepositoryName()
   */
  public String getRepositoryName() {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
    return originalNodeLocation != null ? originalNodeLocation.getRepository()
                                       : portletPreferences.getValue(UISingleContentViewerPortlet.REPOSITORY,
                                                                     "repository");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.portal.webui.portal.UIPortalComponent#getTemplate()
   */
  public String getTemplate() {
    return templatePath;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#getTemplatePath()
   */
  public String getTemplatePath() throws Exception {
    return templatePath;
  }

  public void setTemplatePath(String templatePath) {
    this.templatePath = templatePath;
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    return new JCRResourceResolver(workspace);
  }

  public ResourceResolver getTemplateResourceResolver() {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    return new JCRResourceResolver(workspace);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getNodeType()
   */
  public String getNodeType() throws Exception {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#isNodeTypeSupported()
   */
  public boolean isNodeTypeSupported() {
    return false;
  }
  
  /**
   * Checks if allow render fast publish link for the inline editting
   *
   * @return true, if need to render fast publish link
   */
  public boolean isFastPublishLink() { return true ; }
  
  public String getFastPublishLink() throws Exception {
    UIPresentationContainer container = (UIPresentationContainer)getParent();
    return container.event("FastPublish");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getCommentComponent()
   */
  public UIComponent getCommentComponent() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getCommentComponent()
   */
  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null, "PresentationRemoveAttach");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] { UIPresentationContainer.class }));
    return uicomponent;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getCommentComponent()
   */
  public UIComponent getRemoveComment() throws Exception {
    removeChild(RemoveCommentComponent.class);
    UIComponent uicomponent = addChild(RemoveCommentComponent.class, null, "PresentationRemoveComment");
    ((AbstractActionComponent)uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPresentationContainer.class}));
    return uicomponent;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getUIComponent(mimeType, this);
  }

  /**
   * Gets the viewable link (attachment link, relation document link)
   *
   * @param node the node
   * @return the attachment URL
   * @throws Exception the exception
   */
  public String getViewableLink(Node node, Parameter[] params) throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node);
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());
    String basePath = Utils.getPortletPreference(UISingleContentViewerPortlet.PREFERENCE_TARGET_PAGE);
    String scvWith = Utils.getPortletPreference(UISingleContentViewerPortlet.PREFERENCE_SHOW_SCV_WITH);
    if (scvWith == null || scvWith.length() == 0)
        scvWith = UISingleContentViewerPortlet.DEFAULT_SHOW_SCV_WITH;

    StringBuffer param = new StringBuffer();
    param.append("/")
         .append(nodeLocation.getRepository())
         .append("/")
         .append(nodeLocation.getWorkspace());

    if (node.isNodeType("nt:frozenNode")) {
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      Node originalNode = node.getSession().getNodeByUUID(uuid);
      param.append(originalNode.getPath());
    } else {
      param.append(node.getPath());
    }

    NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL,
                                                         Util.getPortalRequestContext()
                                                             .getPortalOwner(), basePath);
    nodeURL.setResource(resource).setQueryParameterValue(scvWith, param.toString());
    String link = baseURI + nodeURL.toString();

    FriendlyService friendlyService = getApplicationComponent(FriendlyService.class);
    link = friendlyService.getFriendlyUri(link);

    return link;
  }

  /**
   * Gets the attachment nodes.
   *
   * @return the attachment Nodes
   * @throws Exception the exception
   */
  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    Node parent  = getOriginalNode();
    NodeIterator childrenIterator = parent.getNodes();;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType = org.exoplatform.ecm.webui.utils.Utils.getListAllowedFileType(parent,
                                                                                                        templateService);
      if (listCanCreateNodeType.contains(nodeType)) attachments.add(childNode);
    }
    return attachments;
  }
  
  @Override
  public boolean isDisplayAlternativeText() {
    try {
      Node node = this.getNode();
      return ( node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) || 
               (node.isNodeType(NodetypeConstant.NT_FROZEN_NODE) && 
                   NodetypeConstant.EXO_ACCESSIBLE_MEDIA.equals(node.getProperty("jcr:frozenPrimaryType").getString()))) &&
             node.hasProperty(NodetypeConstant.EXO_ALTERNATIVE_TEXT) &&
             StringUtils.isNotEmpty(node.getProperty(NodetypeConstant.EXO_ALTERNATIVE_TEXT).getString());
    } catch (Exception e) { return false; }
  }
  
  @Override
  public boolean playAudioDescription() {
    try {
      Node node = this.getNode();
      return ( node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) || 
                (node.isNodeType(NodetypeConstant.NT_FROZEN_NODE) && 
                 NodetypeConstant.EXO_ACCESSIBLE_MEDIA.equals(node.getProperty("jcr:frozenPrimaryType").getString()))) &&
                 org.exoplatform.services.cms.impl.Utils.hasChild(node, NodetypeConstant.EXO_AUDIO_DESCRIPTION);
    } catch (Exception e) { return false; }
  }
  
  @Override
  public boolean switchBackAudioDescription() {
    try {
      Node node = this.getNode();
      Node parent = node.getParent();
      return node.isNodeType(NodetypeConstant.EXO_AUDIO_DESCRIPTION) &&
              ( parent.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) || 
                  (parent.isNodeType(NodetypeConstant.NT_FROZEN_NODE) && 
                   NodetypeConstant.EXO_ACCESSIBLE_MEDIA.equals(parent.getProperty("jcr:frozenPrimaryType").getString())));
    } catch (Exception e) { return false; }
  }
  
  @Override  
  public UIPopupContainer getPopupContainer() throws Exception {
    return this.getAncestorOfType(UIPortletApplication.class).getChild(UIPopupContainer.class);
  }
  
  static public class DownloadActionListener extends EventListener<UIPresentation> {
    public void execute(Event<UIPresentation> event) throws Exception {
      UIPresentation uiComp = event.getSource();
      try {
        String downloadLink = Utils.getDownloadLink(Utils.getFileLangNode(uiComp.getNode()));
        RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
        requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + downloadLink + "');");
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
         LOG.error("Repository cannot be found", e);
        }
      }
    }
  }
  
  static public class SwitchToAudioDescriptionActionListener extends EventListener<UIPresentation> {
    public void execute(Event<UIPresentation> event) throws Exception {
      UIPresentation uiPresentation = event.getSource();
      UIPresentationContainer uiContainer = uiPresentation.getAncestorOfType(UIPresentationContainer.class);
      uiPresentation.switchMediaState();        
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
  
  static public class SwitchToOriginalActionListener extends EventListener<UIPresentation> {
    public void execute(Event<UIPresentation> event) throws Exception {
      UIPresentation uiPresentation = event.getSource();
      UIPresentationContainer uiContainer = uiPresentation.getAncestorOfType(UIPresentationContainer.class);
      uiPresentation.switchMediaState();        
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
  
}
