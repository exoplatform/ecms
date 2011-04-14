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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.NodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.ecm.webui.presentation.removecomment.RemoveCommentComponent;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

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

public class UIViewVersion extends UIContainer implements NodePresentation {
  private Node node_ ;
  protected Node originalNode_ ;
  private String language_ ;
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIViewVersion");
  final private static String COMMENT_COMPONENT = "Comment".intern();

  public UIViewVersion() throws Exception {
    addChild(UINodeInfo.class, null, null) ;
    addChild(UINodeProperty.class, null, null).setRendered(false) ;
  }

  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    try {
      Node node = getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ;
      originalNode_ = node ;
      String nodeType = node.getPrimaryNodeType().getName();
      if(isNodeTypeSupported(node)) return templateService.getTemplatePathByUser(false, nodeType, userName) ;
    } catch (Exception e) {
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


  @SuppressWarnings("unused")
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

  public Node getNode() throws RepositoryException { return node_; }

  public Node getOriginalNode() throws Exception {return  originalNode_ ;}

  public void setNode(Node node) {node_ = node ;}

  public Node getNodeByUUID(String uuid) throws Exception{
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    for(String ws : workspaces) {
      try{
        return SessionProviderFactory.createSystemProvider().getSession(ws, manageRepo).getNodeByUUID(uuid) ;
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }

  public List<Node> getRelations() throws Exception {
    List<Node> relations = new ArrayList<Node>() ;
    if (node_.hasProperty(Utils.EXO_RELATION)) {
      Value[] vals = node_.getProperty(Utils.EXO_RELATION).getValues();
      for (int i = 0; i < vals.length; i++) {
        String uuid = vals[i].getString();
        Node node = getNodeByUUID(uuid);
        relations.add(node);
      }
    }
    return relations;
  }

  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    NodeIterator childrenIterator = node_.getNodes();
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    int attachData = 0 ;
    while(childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType =
        Utils.getListAllowedFileType(node_, templateService) ;
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

  @Override
  public String getAttachmentURL(Node attNode, Parameter[] params)
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
      LOG.error("Unexpected error", e);
    }
    return false ;
  }

  public boolean isRssLink() { return false ; }
  public String getRssLink() { return null ; }

  public void update() throws Exception {
    getChild(UINodeInfo.class).update();
  }

  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(node_, getLanguage()) ;
  }

  @SuppressWarnings("unchecked")
  public Object getComponentInstanceOfType(String className) {
    Object service = null;
    try {
      ClassLoader loader =  Thread.currentThread().getContextClassLoader();
      Class object = loader.loadClass(className);
      service = getApplicationComponent(object);
    } catch (ClassNotFoundException ex) {
      LOG.error("Unexpected error", ex);
    }
    return service;
  }

  public String getDownloadLink(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) node = originalNode_;
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
    return node_.getPrimaryNodeType().getName() ;
  }

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public List getSupportedLocalise() throws Exception {
    MultiLanguageService multiLanguageService = getApplicationComponent(MultiLanguageService.class) ;
    return multiLanguageService.getSupportedLanguages(node_) ;
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
    return node_.getSession().getWorkspace().getName();
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

  private Node getFileLangNode(Node currentNode) throws Exception {
    if(currentNode.getNodes().getSize() > 0) {
      NodeIterator nodeIter = currentNode.getNodes() ;
      while(nodeIter.hasNext()) {
        Node ntFile = nodeIter.nextNode() ;
        if(ntFile.getPrimaryNodeType().getName().equals("nt:file")) {
          return ntFile ;
        }
      }
      return currentNode ;
    }
    return currentNode ;
  }

  static public class ChangeLanguageActionListener extends EventListener<UIViewVersion>{
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiViewVersion = event.getSource() ;
      UIApplication uiApp = uiViewVersion.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIViewVersion.msg.not-supported", null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      return ;
    }
  }

  static  public class DownloadActionListener extends EventListener<UIViewVersion> {
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiComp = event.getSource() ;
      String downloadLink = uiComp.getDownloadLink(uiComp.getFileLangNode(uiComp.getNode()));
      event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
    }
  }

  static  public class ChangeNodeActionListener extends EventListener<UIViewVersion> {
    public void execute(Event<UIViewVersion> event) throws Exception {
      UIViewVersion uiViewVersion =  event.getSource() ;
      UIApplication uiApp = uiViewVersion.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIViewVersion.msg.not-supported", null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      return ;
    }
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(Utils.MIME_TYPE, mimeType);
    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, this);
      if(uiComponent != null) return uiComponent;
    }
    return null;
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

}
