/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.contentvalidation.webui;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.ecm.webui.presentation.removecomment.RemoveCommentComponent;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.workflow.webui.component.controller.UITaskManager;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 16, 2009
 */
@ComponentConfig(
    template = "classpath:resources/templates/controller/UIDocumentContent.gtmpl",
    events = {
        @EventConfig(listeners = UIDocumentContent.ChangeLanguageActionListener.class),
        @EventConfig(listeners = UIDocumentContent.DownloadActionListener.class),
        @EventConfig(listeners = UIDocumentContent.ChangeNodeActionListener.class)
    }
)
public class UIDocumentContent extends UIBaseNodePresentation {
  private NodeLocation node_ ;
  public static final String DEFAULT_LANGUAGE = "default" ;
  private String language_ = DEFAULT_LANGUAGE ;
  private static final Log LOG  = ExoLogger.getLogger(UIDocumentContent.class);
  public UIDocumentContent() throws Exception {}

  public void setNode(Node node)  {
    this.node_ = NodeLocation.getNodeLocationByNode(node);
  }

  public Node getNode() throws Exception {
    Node node = getOriginalNode();
    if(node.hasProperty(Utils.EXO_LANGUAGE)) {
      String defaultLang = node.getProperty(Utils.EXO_LANGUAGE).getString() ;
      if(!language_.equals(DEFAULT_LANGUAGE) && !language_.equals(defaultLang)) {
        Node curNode = node.getNode(Utils.LANGUAGES + "/" + language_) ;
        language_ = defaultLang ;
        return curNode ;
      }
    }
    return node;
  }
  public Node getOriginalNode() throws Exception {
    return NodeLocation.getNodeByLocation(node_);
  }

  public String getNodeType() throws Exception { return getOriginalNode().getPrimaryNodeType().getName() ; }

  public String getTemplate() {
    try {
      if(isNodeTypeSupported()) return getTemplatePath() ;
      return super.getTemplate() ;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return null ;
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    try {
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      String wsName = dmsConfiguration.getConfig().getSystemWorkspace();
      return new JCRResourceResolver(wsName);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return super.getTemplateResourceResolver(context, template);
  }

  public boolean isNodeTypeSupported() {
    try {
      TemplateService templateService = getApplicationComponent(TemplateService.class) ;
      String nodeTypeName = getOriginalNode().getPrimaryNodeType().getName();

      return templateService.isManagedNodeType(nodeTypeName);
    } catch (Exception e) {
      return false;
    }
  }

  public String getImage(Node node, String nodeTypeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    Node imageNode = node.getNode(nodeTypeName) ;
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  public Node getNodeByPath(String nodePath, String workspace) throws Exception {
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, manageRepo) ;
    return (Node) session.getItem(nodePath) ;
  }

  public String getCapacityOfFile(Node file) throws Exception {
    Node contentNode = file.getNode(Utils.JCR_CONTENT) ;
    InputStream in = contentNode.getProperty(Utils.JCR_DATA).getStream() ;
    float capacity = in.available()/1024 ;
    String strCapacity = Float.toString(capacity) ;
    if(strCapacity.indexOf(".") > -1) return strCapacity.substring(0, strCapacity.lastIndexOf(".")) ;
    return strCapacity ;
  }

  public List<Node> getRelations() throws Exception {
    List<Node> relations = new ArrayList<Node>();
    try {
      Value[] vals = getOriginalNode().getProperty(Utils.EXO_RELATION).getValues();
      for (Value val : vals) {
        String uuid = val.getString();
        Node relationNode = getNodeByUUID(uuid);
        relations.add(relationNode);
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return relations;
  }

  public Node getNodeByUUID(String uuid) throws Exception{
    ManageableRepository manageRepo = (ManageableRepository)getOriginalNode().getSession().getRepository();
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    for(String ws : manageRepo.getWorkspaceNames()) {
      try{
        return sessionProvider.getSession(ws,manageRepo).getNodeByUUID(uuid) ;
      }catch(Exception e) {
        continue ;
      }
    }
    return null;
  }

  private List<String> getListAllowedFileType(Node currentNode) throws Exception {
    List<String> nodeTypes = new ArrayList<String>() ;
    NodeTypeManager ntManager = currentNode.getSession().getWorkspace().getNodeTypeManager() ;
    NodeType currentNodeType = currentNode.getPrimaryNodeType() ;
    NodeDefinition[] childDefs = currentNodeType.getChildNodeDefinitions() ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    List templates = templateService.getDocumentTemplates() ;
    try {
      for(int i = 0; i < templates.size(); i ++){
        String nodeTypeName = templates.get(i).toString() ;
        NodeType nodeType = ntManager.getNodeType(nodeTypeName) ;
        NodeType[] superTypes = nodeType.getSupertypes() ;
        boolean isCanCreateDocument = false ;
        for(NodeDefinition childDef : childDefs){
          NodeType[] requiredChilds = childDef.getRequiredPrimaryTypes() ;
          for(NodeType requiredChild : requiredChilds) {
            if(nodeTypeName.equals(requiredChild.getName())){
              isCanCreateDocument = true ;
              break ;
            }
          }
          if(nodeTypeName.equals(childDef.getName()) || isCanCreateDocument) {
            if(!nodeTypes.contains(nodeTypeName)) nodeTypes.add(nodeTypeName) ;
            isCanCreateDocument = true ;
          }
        }
        if(!isCanCreateDocument){
          for(NodeType superType:superTypes) {
            for(NodeDefinition childDef : childDefs){
              for(NodeType requiredType : childDef.getRequiredPrimaryTypes()) {
                if (superType.getName().equals(requiredType.getName())) {
                  if(!nodeTypes.contains(nodeTypeName)) nodeTypes.add(nodeTypeName) ;
                  isCanCreateDocument = true ;
                  break;
                }
              }
              if(isCanCreateDocument) break ;
            }
            if(isCanCreateDocument) break ;
          }
        }
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return nodeTypes ;
  }

  public UIComponent getCommentComponent() {
    return this;
  }

  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null, "DocumentContentRemoveAttach");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPopupWindow.class}));
    return uicomponent;
  }

  public UIComponent getRemoveComment() throws Exception {
    removeChild(RemoveCommentComponent.class);
    UIComponent uicomponent = addChild(RemoveCommentComponent.class, null, "DocumentContentRemoveComment");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIPopupWindow.class}));
    return uicomponent;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return Utils.getUIComponent(mimeType, this);
  }

  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>();
    String nodeType = "";
    NodeIterator childrenIterator;
    Node node = getOriginalNode();
    childrenIterator = node.getNodes();
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      try {
        nodeType = childNode.getPrimaryNodeType().getName();
        List<String> listCanCreateNodeType = getListAllowedFileType(node) ;
        if(listCanCreateNodeType.contains(nodeType)) attachments.add(childNode);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    return attachments;
  }

  /**
   * use getViewableLink(Node attNode, Parameter[] params) instead
   * @param attNode
   * @param params
   * @return
   * @throws Exception
   */
  @Deprecated
  public String getAttachmentURL(Node attNode, Parameter[] params) throws Exception {
    return getViewableLink(attNode, params);
  }

  public String getViewableLink(Node attNode, Parameter[] params)
      throws Exception {
    return this.event("ChangeNode", Utils.formatNodeName(attNode.getPath()), params);
  }

  public String getRssLink() { return null ; }
  public boolean isRssLink() { return false ; }

  public List<String> getSupportedLocalise() throws Exception {
    List<String> local = new ArrayList<String>();
    Node node = getOriginalNode();
    if(node.hasNode(Utils.LANGUAGES)){
      Node languages = node.getNode(Utils.LANGUAGES) ;
      NodeIterator iter = languages.getNodes() ;
      while(iter.hasNext()) {
        local.add(iter.nextNode().getName()) ;
      }
      local.add(node.getProperty(Utils.EXO_LANGUAGE).getString()) ;
    }
    return local ;
  }

  public String getTemplatePath() throws Exception {
    String nodeTypeName = getOriginalNode().getPrimaryNodeType().getName();
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.getTemplatePathByUser(false, nodeTypeName, userName);
  }

  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getSkinPath(nodeTypeName, skinName, getLanguage()) ;
  }

  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(getOriginalNode(), "default") ;
  }

  public String getIcons(Node node, String appended) throws Exception {
    String nodeType = node.getPrimaryNodeType().getName().replaceAll(":", "_") + appended ;
    StringBuilder str = new StringBuilder(nodeType) ;
    if(node.isNodeType(Utils.NT_FILE)) {
      Node jcrContentNode = node.getNode(Utils.JCR_CONTENT) ;
      str.append(" ").append(jcrContentNode.getProperty(Utils.JCR_MIMETYPE).getString().replaceFirst("/", "_")).append(appended);
    }
    return str.toString() ;
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

  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
    portletRequestContext.getRequest().getServerName() + ":" +
    String.format("%s",portletRequestContext.getRequest().getServerPort()) ;
    return prefixWebDAV ;
  }

  public String getLanguage() { return language_ ; }
  public void setLanguage(String language) { language_ = language ; }

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
    return org.exoplatform.wcm.webui.Utils.getDownloadLink(node);
  }

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public String getWorkspaceName() throws Exception {
    return getOriginalNode().getSession().getWorkspace().getName();
  }

  public String getRepository() throws Exception {
    ManageableRepository manaRepo = (ManageableRepository)getOriginalNode().getSession().getRepository() ;
    return manaRepo.getConfiguration().getName() ;
  }

  static public class ChangeLanguageActionListener extends EventListener<UIDocumentContent> {
    public void execute(Event<UIDocumentContent> event) throws Exception {
      UIDocumentContent uiDocContent = event.getSource() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocContent.getAncestorOfType(UITaskManager.class)) ;
      String selectedLanguage = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiDocContent.setRenderSibling(UIDocumentContent.class) ;
      uiDocContent.setLanguage(selectedLanguage) ;
    }
  }

  public String encodeHTML(String text) throws Exception {
    return text.replaceAll("&", "&amp;").replaceAll("\"", "&quot;")
    .replaceAll("<", "&lt;").replaceAll(">", "&gt;") ;
  }

  static  public class DownloadActionListener extends EventListener<UIDocumentContent> {
    public void execute(Event<UIDocumentContent> event) throws Exception {
      UIDocumentContent uiComp = event.getSource() ;
      String downloadLink = uiComp.getDownloadLink(org.exoplatform.wcm.webui.Utils.getFileLangNode(uiComp.getNode()));
      event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
    }
  }

  static  public class ChangeNodeActionListener extends EventListener<UIDocumentContent> {
    public void execute(Event<UIDocumentContent> event) throws Exception {
      UIDocumentContent uiComp = event.getSource() ;
      RepositoryService repositoryService  = uiComp.getApplicationComponent(RepositoryService.class) ;
      ManageableRepository repository = repositoryService.getCurrentRepository();
      String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName") ;
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, repository) ;
      Node selectedNode = (Node) session.getItem(uri) ;
      uiComp.setNode(selectedNode) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiComp.getParent()) ;
    }
  }

  public boolean isEnableComment() {
    return false;
  }

  public boolean isEnableVote() {
    return false;
  }

  @SuppressWarnings("unused")
  public void setEnableComment(boolean value) {
  }

  @SuppressWarnings("unused")
  public void setEnableVote(boolean value) {
  }

  public String getInlineEditingField(Node orgNode, String propertyName,
      String defaultValue, String inputType, String idGenerator, String cssClass,
      boolean isGenericProperty, String... arguments) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName, defaultValue, inputType,
                                                                       idGenerator, cssClass, isGenericProperty, arguments);
  }

  public String getInlineEditingField(Node orgNode, String propertyName) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName);
  }
}