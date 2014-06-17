/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.versions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.ecm.webui.presentation.removecomment.RemoveCommentComponent;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : lxchiati
 *          lebienthuy@gmail.com
 * Oct 19, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
    type     = UIViewVersion.class,
    template = "system:/groovy/webui/core/UITabPane.gtmpl",
    events = {
      @EventConfig(listeners = UIViewVersion.ChangeLanguageActionListener.class),
      @EventConfig(listeners = UIViewVersion.ChangeNodeActionListener.class),
      @EventConfig(listeners = UIViewVersion.DownloadActionListener.class)
    }
)

public class UIViewVersion extends UIBaseNodePresentation {
  private NodeLocation node_ ;
  protected NodeLocation originalNode_ ;
  private String language_ ;
  private static final Log LOG  = ExoLogger.getLogger(UIViewVersion.class.getName());
  final private static String COMMENT_COMPONENT = "Comment";

  public UIViewVersion() throws Exception {
    addChild(UINodeInfo.class, null, null) ;
    addChild(UINodeProperty.class, null, null).setRendered(false) ;
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    try {
      Node node = getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ;
      originalNode_ = NodeLocation.getNodeLocationByNode(node);
      String nodeType = node.getPrimaryNodeType().getName();
      if(isNodeTypeSupported(node)) return templateService.getTemplatePathByUser(false, nodeType, userName) ;
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return null ;
  }

  public UIComponent getCommentComponent() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIActionBar uiActionBar = uiExplorer.findFirstComponentOfType(UIActionBar.class);
    UIComponent uicomponent = uiActionBar.getUIAction(COMMENT_COMPONENT);
    return (uicomponent != null ? uicomponent : this);
  }

  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null, "UIViewVersionRemoveAttach");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPopupWindow.class}));
    return uicomponent;
  }

  public UIComponent getRemoveComment() throws Exception {
    removeChild(RemoveCommentComponent.class);
    UIComponent uicomponent = addChild(RemoveCommentComponent.class, null, "UIViewVersionRemoveComment");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPopupWindow.class}));
    return uicomponent;
  }


  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver() ;
  }

  public boolean isNodeTypeSupported(Node node) {
    try {
      TemplateService templateService = getApplicationComponent(TemplateService.class) ;
      String nodeTypeName = node.getPrimaryNodeType().getName();
      return templateService.isManagedNodeType(nodeTypeName);
    } catch (Exception e) {
      return false;
    }
  }

  public Node getNode() throws RepositoryException {
    return NodeLocation.getNodeByLocation(node_);
  }

  public Node getOriginalNode() throws Exception {
    return  NodeLocation.getNodeByLocation(originalNode_);
  }

  public void setNode(Node node) {
    node_ = NodeLocation.getNodeLocationByNode(node);
  }

  public Node getNodeByUUID(String uuid) {
    ManageableRepository manageRepo = WCMCoreUtils.getRepository();
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    for(String ws : workspaces) {
      try{
        return WCMCoreUtils.getSystemSessionProvider().getSession(ws, manageRepo).getNodeByUUID(uuid) ;
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }

  public List<Node> getRelations() throws Exception {
    List<Node> relations = new ArrayList<Node>() ;
    Node node = getNode();
    if (node.hasProperty(Utils.EXO_RELATION)) {
      Value[] vals = node.getProperty(Utils.EXO_RELATION).getValues();
      for (int i = 0; i < vals.length; i++) {
        String uuid = vals[i].getString();
        Node nodeToAdd = getNodeByUUID(uuid);
        relations.add(nodeToAdd);
      }
    }
    return relations;
  }

  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    Node node = getNode();
    NodeIterator childrenIterator = node.getNodes();
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    int attachData = 0 ;
    while(childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType =
        Utils.getListAllowedFileType(node, templateService) ;
      if(listCanCreateNodeType.contains(nodeType)) {

        // Case of childNode has jcr:data property
        if (childNode.hasProperty(Utils.JCR_DATA)) {
          attachData = childNode.getProperty(Utils.JCR_DATA).getStream().available();

          // Case of jcr:data has content
          if (attachData > 0)
            attachments.add(childNode);
        } else {
          attachments.add(childNode);
        }
      }
    }
    return attachments;
  }

  public String getViewableLink(Node attNode, Parameter[] params)
      throws Exception {
    return this.event("ChangeNode", Utils.formatNodeName(attNode.getPath()), params);
  }

  public String getIcons(Node node, String type) throws Exception {
    return Utils.getNodeTypeIcon(node, type) ;
  }
  public boolean hasPropertyContent(Node node, String property){
    try {
      String value = node.getProperty(property).getString() ;
      if(value.length() > 0) return true ;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return false ;
  }

  public boolean isRssLink() { return false ; }
  public String getRssLink() { return null ; }

  public void update() throws Exception {
    getChild(UINodeInfo.class).update();
  }

  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(getNode(), getLanguage()) ;
  }

  @SuppressWarnings("unchecked")
  public Object getComponentInstanceOfType(String className) {
    Object service = null;
    try {
      ClassLoader loader =  Thread.currentThread().getContextClassLoader();
      Class object = loader.loadClass(className);
      service = getApplicationComponent(object);
    } catch (ClassNotFoundException ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", ex);
      }
    }
    return service;
  }

  public String getDownloadLink(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) {
      node = NodeLocation.getNodeByLocation(originalNode_);
    }
    Node jcrContentNode = node.getNode(Utils.JCR_CONTENT) ;
    InputStream input = jcrContentNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  public String getImage(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    Node imageNode = node.getNode(Utils.EXO_IMAGE) ;
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  public void setLanguage(String language) { language_ = language ; }
  public String getLanguage() { return language_ ; }

  public String getNodeType() throws Exception {
    return getNode().getPrimaryNodeType().getName() ;
  }

  public String getPortalName() {
    PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public List<String> getSupportedLocalise() throws Exception {
    MultiLanguageService multiLanguageService = getApplicationComponent(MultiLanguageService.class) ;
    return multiLanguageService.getSupportedLanguages(getNode()) ;
  }

  public String getTemplatePath() throws Exception {
    return null;
  }

  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getSkinPath(nodeTypeName, skinName, getLanguage()) ;
  }

  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
    portletRequestContext.getRequest().getServerName() + ":" +
    String.format("%s",portletRequestContext.getRequest().getServerPort()) ;
    return prefixWebDAV ;
  }

  public String getWorkspaceName() throws Exception {
    return getNode().getSession().getWorkspace().getName();
  }

  public boolean isNodeTypeSupported() {
    try {
      TemplateService templateService = getApplicationComponent(TemplateService.class);
      return templateService.isManagedNodeType(getNodeType());
    } catch (Exception e) {
      return false;
    }
  }

  public String getRepository() throws Exception{
    return getAncestorOfType(UIJCRExplorer.class).getRepositoryName() ;
  }

  public String encodeHTML(String text) throws Exception {
    return Utils.encodeHTML(text) ;
  }

  static public class ChangeLanguageActionListener extends EventListener<UIViewVersion>{
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiViewVersion = event.getSource() ;
      UIApplication uiApp = uiViewVersion.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIViewVersion.msg.not-supported", null)) ;

      return ;
    }
  }

  static  public class DownloadActionListener extends EventListener<UIViewVersion> {
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiComp = event.getSource() ;
      String downloadLink = uiComp.getDownloadLink(org.exoplatform.wcm.webui.Utils.getFileLangNode(uiComp.getNode()));
      RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
      requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + downloadLink + "');");
    }
  }

  static  public class ChangeNodeActionListener extends EventListener<UIViewVersion> {
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiViewVersion =  event.getSource() ;
      UIApplication uiApp = uiViewVersion.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIViewVersion.msg.not-supported", null)) ;

      return ;
    }
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return Utils.getUIComponent(mimeType, this);
  }

  public boolean isEnableComment() {
    return false;
  }

  public boolean isEnableVote() {
    return false;
  }

  public void setEnableComment(boolean value) {
  }

  public void setEnableVote(boolean value) {
  }

  public String getInlineEditingField(Node orgNode, String propertyName, String defaultValue,
      String inputType, String idGenerator, String cssClass, boolean isGenericProperty, String... arguments) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName, defaultValue, inputType,
                                                                       idGenerator, cssClass, isGenericProperty, arguments);
  }

  public String getInlineEditingField(Node orgNode, String propertyName) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName);
  }
}
