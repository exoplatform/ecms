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
package org.exoplatform.ecm.webui.component.browsecontent;


import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

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
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tuan Pham
 *          phamtuanchip@gmail.com
 * Jan 15, 2007
 */

@ComponentConfig(
    events ={
        @EventConfig(listeners =  UIDocumentDetail.ChangeLanguageActionListener.class),
        @EventConfig(listeners =  UIDocumentDetail.ChangeNodeActionListener.class),
        @EventConfig(listeners =  UIDocumentDetail.DownloadActionListener.class)
    }
)

public class UIDocumentDetail extends UIBaseNodePresentation implements UIPopupComponent {
  protected Node node_ ;
  private Node originalNode_;
  private String language_ ;
  private JCRResourceResolver jcrTemplateResourceResolver_ ;

  private static final Log             LOG                   = ExoLogger.getLogger("portlet.DocumentDetail");

  public UIDocumentDetail() {}

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if(isValidNode()) {
      setRendered(true) ;
      super.processRender(context);
    } else {
      setRendered(false) ;
    }
  }
  protected boolean isValidNode() throws Exception  {
    if(originalNode_ == null) return false ;
    if(getUIBrowseContainer().getNodeByPath(originalNode_.getPath(), getWorkspaceName()) == null) return false ;
    return true ;
  }

  private UIBrowseContainer getUIBrowseContainer() {
    return  getAncestorOfType(UIBrowseContentPortlet.class).findFirstComponentOfType(UIBrowseContainer.class) ;
  }

  public UIComponent getCommentComponent() {
    return getUIBrowseContainer().getChild(UIToolBar.class);
  }

  @SuppressWarnings("cast")
  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null,
        "DocumentDetailRemoveAttach".concat(UUID.randomUUID().toString()));
    ((AbstractActionComponent)uicomponent).setLstComponentupdate(
        Arrays.asList(new Class[] {((UIComponent)this.getParent()).getClass()}));
    return uicomponent;
  }

  @SuppressWarnings("cast")
  public UIComponent getRemoveComment() throws Exception {
    removeChild(RemoveCommentComponent.class);
    UIComponent uicomponent = addChild(RemoveCommentComponent.class, null,
        "DocumentDetailRemoveComment".concat(UUID.randomUUID().toString()));
    ((AbstractActionComponent)uicomponent).setLstComponentupdate(
        Arrays.asList(new Class[] {((UIComponent)this.getParent()).getClass()}));
    return uicomponent;
  }

  public String getTemplatePath(){
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String template = null;
    try{
      if(SessionProviderFactory.isAnonim()) {
        template = templateService.getTemplatePathByAnonymous(false, getNodeType());
      } else {
        template = templateService.getTemplatePathByUser(false, getNodeType(), userName) ;
      }
      if(jcrTemplateResourceResolver_ == null) newJCRTemplateResourceResolver();
      return template;
    } catch (AccessControlException e) {
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { template };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.do-not-permission", arg,
          ApplicationMessage.ERROR));
      return null;
    }catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when get template ", e);
      }
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { template };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-support", arg,
          ApplicationMessage.ERROR));
      return null;
    }
  }

  public String getIcons(Node node, String type) throws Exception {
    return Utils.getNodeTypeIcon(node, type) ;
  }

  public String getTemplate(){ return getTemplatePath() ;}

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    if(jcrTemplateResourceResolver_ == null) newJCRTemplateResourceResolver() ;
    return jcrTemplateResourceResolver_ ;
  }

  public void newJCRTemplateResourceResolver() {
    try {
      String systemWorkspace = getAncestorOfType(UIBrowseContentPortlet.class).getChild(UIBrowseContainer.class)
                                                                              .getDmsSystemWorkspace();
      jcrTemplateResourceResolver_ = new JCRResourceResolver(systemWorkspace);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when get template resource", e);
      }
    }
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
        LOG.error("Not found class " + className, ex);
      }
    }
    return service;
  }

  public Node getNode() throws Exception {
    if(node_.hasProperty(Utils.EXO_LANGUAGE)) {
      String defaultLang = node_.getProperty(Utils.EXO_LANGUAGE).getString() ;
      if(language_ == null) language_ = defaultLang ;
      if(!language_.equals(defaultLang)) {
        Node curNode = node_.getNode(Utils.LANGUAGES + Utils.SLASH + language_) ;
        return curNode ;
      }
    }
    return node_;
  }

  public void setOriginalNode(Node node) { originalNode_ = node; }

  public Node getOriginalNode() throws Exception {return originalNode_ ;}

  public PortletPreferences getPortletPreferences() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
    PortletRequest prequest = pcontext.getRequest() ;
    PortletPreferences portletPref = prequest.getPreferences() ;
    return portletPref ;
  }

  public void setLanguage(String language) { language_ = language ; }
  public String getLanguage() { return language_ ; }

  public String getDownloadLink(Node node) throws Exception {
    return org.exoplatform.wcm.webui.Utils.getDownloadLink(node);
  }

  public String getImage(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    Node imageNode = node.getNode(Utils.EXO_IMAGE) ;
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(originalNode_.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
    portletRequestContext.getRequest().getServerName() + ":" +
    String.format("%s",portletRequestContext.getRequest().getServerPort()) ;
    return prefixWebDAV ;
  }

  public void setNode(Node docNode) {node_ = docNode ;}

  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    NodeIterator childrenIterator = node_.getNodes();;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    int attachData = 0 ;
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType =
        Utils.getListAllowedFileType(originalNode_, templateService) ;
      if (listCanCreateNodeType.contains(nodeType)) {

        // Case of childNode has jcr:data property
        if (childNode.hasProperty(Utils.JCR_DATA)) {
          attachData = childNode.getProperty(Utils.JCR_DATA).getStream().available();

          // Case of jcr:data has it's content available
          if (attachData > 0)
            attachments.add(childNode);
        } else {
          attachments.add(childNode);
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

  public boolean isNodeTypeSupported() {
    try {
      TemplateService templateService = getApplicationComponent(TemplateService.class);
      return templateService.isManagedNodeType(getNodeType());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isNodeTypeSupported(String nodeTypeName) {
    try {
      TemplateService templateService = getApplicationComponent(TemplateService.class);
      return templateService.isManagedNodeType(nodeTypeName);
    } catch (Exception e) {
      return false;
    }
  }

  public Node getNodeByUUID(String uuid) throws Exception{
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    SessionProvider provider = SessionProviderFactory.createSessionProvider();
    for(String ws : workspaces) {
      try{
        return provider.getSession(ws,manageRepo).getNodeByUUID(uuid) ;
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }

  public boolean hasPropertyContent(Node node, String property){
    try {
      return node.hasProperty(property) ;
    } catch (Exception e) {
      return false ;
    }

  }

  public String getNodeType() throws Exception {
    return originalNode_.getPrimaryNodeType().getName() ;
  }

  public String getRssLink() {return null ;}

  public boolean isRssLink() {return false ;}

  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(node_, getLanguage()) ;
  }

  public List<String> getSupportedLocalise() throws Exception {
    MultiLanguageService multiLanguageService = getApplicationComponent(MultiLanguageService.class) ;
    return multiLanguageService.getSupportedLanguages(originalNode_) ;
  }

  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public String getRepository() throws Exception {
    return ((ManageableRepository)originalNode_.getSession().getRepository()).getConfiguration().getName() ;
  }

  public String getWorkspaceName() throws Exception {
    return originalNode_.getSession().getWorkspace().getName();
  }

  public String encodeHTML(String text) throws Exception {
    return Utils.encodeHTML(text) ;
  }

  @SuppressWarnings("unused")
  public boolean isShowPlanView(Node node) throws Exception {
    return false;
  }

  public List<Node> getListNodes(Node node) throws Exception {
    Iterator childrenIterator = node.getNodes() ;
    List<Node> childrenList  = new ArrayList<Node>() ;
    while(childrenIterator.hasNext()) {
      Node child = (Node)childrenIterator.next() ;
      if(PermissionUtil.canRead(child)) {
        childrenList.add(child) ;
      }
    }
    return childrenList ;
  }

  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getSkinPath(nodeTypeName, skinName, getLanguage()) ;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return Utils.getUIComponent(mimeType, this);
  }

  static public class ChangeLanguageActionListener extends EventListener<UIDocumentDetail>{
    public void execute(Event<UIDocumentDetail> event) throws Exception {
      UIDocumentDetail uiDocument = event.getSource() ;
      String selectedLanguage = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiDocument.setLanguage(selectedLanguage) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocument.getParent()) ;
    }
  }

  static public class ChangeNodeActionListener extends EventListener<UIDocumentDetail>{
    public void execute(Event<UIDocumentDetail> event) throws Exception {
      UIDocumentDetail uiDocument = event.getSource() ;
      UIBrowseContentPortlet cbPortlet = uiDocument.getAncestorOfType(UIBrowseContentPortlet.class) ;
      UIPopupContainer uiPopupAction = cbPortlet.getChildById("UICBPopupAction") ;
      uiPopupAction.deActivate() ;
      String path = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String wsName = event.getRequestContext().getRequestParameter("workspaceName") ;
      Node node = null ;
      UIBrowseContainer uiContainer = cbPortlet.findFirstComponentOfType(UIBrowseContainer.class) ;
      if(wsName != null) {
        ManageableRepository manageableRepository =
          uiDocument.getApplicationComponent(RepositoryService.class).getCurrentRepository();
        Session session = null ;
        if(path.indexOf("/jcr:system")>0) {
          session = SessionProviderFactory.createSystemProvider().getSession(wsName,manageableRepository) ;
        }else {
          if(SessionProviderFactory.isAnonim()) {
            //TODO: Anonim session
            // session = SessionsUtils.getAnonimProvider().getSession(wsName,manageableRepository) ;
            session = SessionProviderFactory.createSystemProvider().getSession(wsName,manageableRepository) ;
          }else {
            session = SessionProviderFactory.createSessionProvider().getSession(wsName,manageableRepository) ;
          }
        }
        node = (Node)session.getItem(path) ;
      }else {
        node = uiContainer.getNodeByPath(path) ;
      }
      UIDocumentDetail uiDocumentView =  uiPopupAction.activate(UIDocumentDetail.class, null, 600, 450) ;
      uiDocumentView.setNode(node) ;
      uiDocumentView.setOriginalNode(node);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
    }
  }

  static  public class DownloadActionListener extends EventListener<UIDocumentDetail> {
    public void execute(Event<UIDocumentDetail> event) throws Exception {
      UIDocumentDetail uiComp = event.getSource() ;
      String downloadLink = uiComp.getDownloadLink(org.exoplatform.wcm.webui.Utils.getFileLangNode(uiComp.getNode()));
      event.getRequestContext().getJavascriptManager().addCustomizedOnLoadScript("ajaxRedirect('" + downloadLink + "');");
    }
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

  public String getInlineEditingField(Node orgNode, String propertyName,
      String defaultValue, String inputType, String idGenerator, String cssClass,
      boolean isGenericProperty, String... arguments) throws Exception{
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName, defaultValue, inputType,
                                                                       idGenerator, cssClass, isGenericProperty, arguments);
  }

  public String getInlineEditingField(Node orgNode, String propertyName) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName);
  }
}