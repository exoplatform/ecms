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
package org.exoplatform.wcm.webui.wiki;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;

import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.scv.UISingleContentViewerPortlet;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Mar 4, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIWikiContentForm.SaveActionListener.class),
      @EventConfig(listeners = UIWikiContentForm.CancelActionListener.class)
    }
)
@SuppressWarnings("deprecation")
public class UIWikiContentForm extends UIForm {

  /** The Constant NAME_CONTENT. */
  public static final String NAME_CONTENT = "Name";
  
  /** The Constant CONTENT. */
  public static final String CONTENT = "Content";
  
  /** The Constant TITLE. */
  public static final String TITLE = "Title";

  /** The parent uri. */
  private String parentUri;
  
  /** The page uri. */
  private String pageUri;
  
  /** The page navigation. */
  private PageNavigation pageNavigation;

  /**
   * Instantiates a new uI wiki content form.
   * 
   * @throws Exception the exception
   */
  public UIWikiContentForm() throws Exception {
    UIFormStringInput nameInput = new UIFormStringInput(NAME_CONTENT, "");
    addChild(nameInput);
    addUIFormInput(new UIFormStringInput(TITLE, TITLE, null).addValidator(
        MandatoryValidator.class).addValidator(ECMNameValidator.class));
    UIFormWYSIWYGInput uiWYSWYGInput = new UIFormWYSIWYGInput(CONTENT, CONTENT, "");
    uiWYSWYGInput.setWidth(String.valueOf(750));
    addChild(uiWYSWYGInput);
  }

  /**
   * Creates the new page.
   * 
   * @param pageName the page name
   * @param newNodeUUID the new node uuid
   * 
   * @return the string
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public String createNewPage(String pageName, String newNodeUUID) throws Exception {
    //Step 1 : create a UIPortlet
    UIPortal uiPortal = Util.getUIPortal(); 
    UserPortalConfigService portalCfgService = 
      uiPortal.getApplicationComponent(UserPortalConfigService.class);
    UIPortlet scvPortlet = createSCVPortlet(uiPortal, newNodeUUID);
    // Step 2: Create a new Page and add the new UIPortlet into new Page
    WebuiRequestContext webuiReqContext = Util.getPortalRequestContext();
    UIPage uiPage = uiPortal.createUIComponent(webuiReqContext, UIPage.class, null, null);
    uiPage.setEditPermission(uiPortal.getEditPermission());
    uiPage.setAccessPermissions(new String[] {UserACL.EVERYONE});
    uiPage.setOwnerId(uiPortal.getName());
    uiPage.setOwnerType(PortalConfig.PORTAL_TYPE);
    uiPage.setName(pageName);
    Page page = PortalDataMapper.toPageModel(uiPage);
    portalCfgService.create(page);
    // Step 3: add new UIPortlet into new Page
    addSCVPortletIntoPage(scvPortlet, uiPage, page, portalCfgService);
    //Step 4 : add the new page into parentPageNode
    PageNavigation pageNavigation = getPageNavigation();
    PageNode parentPageNode = null;
    for (PageNode pageNode: pageNavigation.getNodes()) {
      if (pageNode.getUri().equals(getParentUri())) {
        parentPageNode = pageNode;
        break;
      }
    }
    PageNode newPageNode = createNewPageNode(parentPageNode, page.getPageId());
    portalCfgService.update(pageNavigation);
    return newPageNode.getUri();
  }

  /**
   * Creates the new page node.
   * 
   * @param parentPageNode the parent page node
   * @param pageId the page id
   * 
   * @return the page node
   * 
   * @throws Exception the exception
   */
  private PageNode createNewPageNode(PageNode parentPageNode, String pageId) throws Exception {
    PageNode newPageNode = null;
    if (getPageUri().indexOf("/") > 0) {
      String[] pageNodeNames = getPageUri().split("/");
      for (String newPageNodeName: pageNodeNames) {
        newPageNode = createNewPageNode(parentPageNode, newPageNodeName, pageId);
        parentPageNode = newPageNode;
      }
    } else {
      newPageNode = createNewPageNode(parentPageNode, getPageUri(), pageId); 
    }
    return newPageNode;
  }

  /**
   * Creates the scv portlet.
   * 
   * @param uiPortal the ui portal
   * @param newNodeUUID the new node uuid
   * 
   * @return the uI portlet
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private UIPortlet createSCVPortlet(UIPortal uiPortal, String newNodeUUID) throws Exception {
    UIPortlet scvPortlet = new UIPortlet();
    scvPortlet.setShowInfoBar(false);

    WCMConfigurationService wcmCfgService = 
      uiPortal.getApplicationComponent(WCMConfigurationService.class);
    StringBuilder windowId = new StringBuilder();
    String random = IdGenerator.generate();
    windowId.append(PortalConfig.PORTAL_TYPE)
    .append("#")
    .append(uiPortal.getOwner())
    .append(":")
    .append(wcmCfgService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET))
    .append("/")
    .append(random);
//    scvPortlet.setWindowId(windowId.toString());
    // Add portlet Preference into portlet
    PortletPreferences prefs = new PortletPreferences();
    prefs.setWindowId(windowId.toString());
//    prefs.setOwnerType(PortalConfig.PORTAL_TYPE);
//    prefs.setOwnerId(uiPortal.getOwner());
    ArrayList<Preference> preferenceList = new ArrayList<Preference>();

    Preference prefR = new Preference();
    RepositoryService repoService = 
      uiPortal.getApplicationComponent(RepositoryService.class);
    String repositoryName = repoService.getCurrentRepository().getConfiguration().getName();
    ArrayList<String> listValue = new ArrayList<String>();
    listValue.add(repositoryName);
    prefR.setName(UISingleContentViewerPortlet.REPOSITORY);
    prefR.setValues(listValue);
    preferenceList.add(prefR);

    Preference prefW = new Preference();
    NodeLocation nodeLocation = wcmCfgService.getLivePortalsLocation(repositoryName);
    String workspaceName = nodeLocation.getWorkspace();
    listValue = new ArrayList<String>();
    listValue.add(workspaceName);
    prefW.setName(UISingleContentViewerPortlet.WORKSPACE);
    prefW.setValues(listValue);
    preferenceList.add(prefW);

    Preference prefI = new Preference();
    listValue = new ArrayList<String>();
    listValue.add(newNodeUUID);
    prefI.setName(UISingleContentViewerPortlet.IDENTIFIER);
    prefI.setValues(listValue);
    preferenceList.add(prefI);

    prefs.setPreferences(preferenceList);
    DataStorage ds = getApplicationComponent(DataStorage.class);
    ds.save(prefs);

    return scvPortlet;
  }

  /**
   * Adds the scv portlet into page.
   * 
   * @param scvPortlet the scv portlet
   * @param uiPage the ui page
   * @param page the page
   * @param userPortalCfgService the user portal cfg service
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private void addSCVPortletIntoPage(UIPortlet scvPortlet, UIPage uiPage, 
      Page page,
      UserPortalConfigService userPortalCfgService) throws Exception {
//    ArrayList<Object> listPortlet = page.getChildren();
//    listPortlet.add(PortalDataMapper.toPortletModel(scvPortlet));
//    page.setChildren(listPortlet);
    userPortalCfgService.update(page);
    uiPage.setChildren(null);
    PortalDataMapper.toUIPage(uiPage, page);
  }

  /**
   * Creates the new web content node.
   * 
   * @param webContentName the web content name
   * @param title the title
   * @param content the content
   * 
   * @return the node
   * 
   * @throws Exception the exception
   */
  private Node createNewWebContentNode(String webContentName, String title, String content) throws Exception {
    String portalName = Util.getUIPortalApplication().getOwner();
    LivePortalManagerService livePortalManagerService = getApplicationComponent(LivePortalManagerService.class);
    Node portalNode = livePortalManagerService.getLivePortal(Utils.getSessionProvider(), portalName);
    WebSchemaConfigService webSchemaConfigService = getApplicationComponent(WebSchemaConfigService.class);
    PortalFolderSchemaHandler handler = webSchemaConfigService
    .getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
    Node webContentStorage = handler.getWebContentStorage(portalNode);
    Node webContentNode = webContentStorage.addNode(webContentName, "exo:webContent");
    WebContentSchemaHandler webContentSchemaHandler = webSchemaConfigService
    .getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    webContentSchemaHandler.createDefaultSchema(webContentNode);
    webContentNode.getNode("default.html/jcr:content").setProperty("jcr:data", content);
    webContentNode.setProperty("exo:title", title);
    Session session = webContentStorage.getSession();
    session.save();

    return webContentNode;
  }

  /**
   * Creates the new page node.
   * 
   * @param parentPageNode the parent page node
   * @param newPageNodeName the new page node name
   * @param pageID the page id
   * 
   * @return the page node
   * 
   * @throws Exception the exception
   */
  private PageNode createNewPageNode(PageNode parentPageNode, String newPageNodeName, String pageID) throws Exception {
    PageNode newPageNode = new PageNode();
    newPageNode.setName(newPageNodeName);
    newPageNode.setLabel(newPageNodeName);
    newPageNode.setUri(parentPageNode.getUri() + "/" + newPageNodeName);
    newPageNode.setPageReference(pageID);
    List<PageNode> children = parentPageNode.getChildren();
    if (children != null) {
      for (PageNode child: children) {
        if (child.getName().equals(newPageNodeName)) {
          return child;
        }
      }
    } else {
      children = new ArrayList<PageNode>();
    }
    children.add(newPageNode);
    parentPageNode.setChildren((ArrayList<PageNode>)children);
    return newPageNode;
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UIWikiContentForm> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWikiContentForm> event) throws Exception {
      UIWikiContentForm uiWikiForm = event.getSource();
      PortletRequestContext pContext = (PortletRequestContext) event.getRequestContext();
      String pageName = uiWikiForm.getUIStringInput(NAME_CONTENT).getValue();
      String title = uiWikiForm.getUIStringInput(TITLE).getValue();
      UIFormWYSIWYGInput uiFormWYSIWYG = uiWikiForm.getChild(UIFormWYSIWYGInput.class);
      String content = uiFormWYSIWYG.getValue();
      Node newWebContentNode = uiWikiForm.createNewWebContentNode(pageName, title, content);
      String newNodeUri = uiWikiForm.createNewPage(pageName, newWebContentNode.getUUID());
      HttpServletRequestWrapper reqWrapper = (HttpServletRequestWrapper) pContext.getRequest();
      HttpServletResponseWrapper resWrapper = (HttpServletResponseWrapper) pContext.getResponse();
      pContext.setResponseComplete(true);
      String newNodeURL = uiWikiForm.getNewNodeURL(reqWrapper, newNodeUri);
      resWrapper.sendRedirect(newNodeURL);
      Utils.updatePortal((PortletRequestContext) event.getRequestContext());
    }
  }

  /**
   * Gets the new node url.
   * 
   * @param reqWrapper the req wrapper
   * @param newNodeUri the new node uri
   * 
   * @return the new node url
   */
  private String getNewNodeURL(HttpServletRequestWrapper reqWrapper, 
      String newNodeUri) {
    String scheme = reqWrapper.getScheme();
    PortalRequestContext portalReqContext = Util.getPortalRequestContext();
    String portalUri = portalReqContext.getPortalURI();
    String serverName = reqWrapper.getServerName();
    String serverPort = String.valueOf(reqWrapper.getServerPort());
    String link = scheme + "://" + serverName + ":" + serverPort +
    portalUri + newNodeUri;
    return link;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.form.UIForm#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    PortletRequestContext porletRequestContext = (PortletRequestContext) context;
    HttpServletRequestWrapper requestWrapper = (HttpServletRequestWrapper) porletRequestContext.getRequest();
    String queryString = requestWrapper.getQueryString();
    String[] parameterArr = queryString.split("&");
    String parentUri = parameterArr[0].split("=")[1];
    String pageUri = parameterArr[1].split("=")[1];
    UIPortal uiPortal = Util.getUIPortal();
    List<PageNavigation> navigations = uiPortal.getNavigations();
    PageNavigation pageNavigation = null;
    for (PageNavigation paNavigation: navigations) {
      if (PortalConfig.PORTAL_TYPE.equals(paNavigation.getOwnerType())) {
        pageNavigation = paNavigation;
        break;
      }
    }
    setPageNavigation(pageNavigation);
    setParentUri(parentUri);
    setPageUri(pageUri);

    super.processRender(context);
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UIWikiContentForm> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWikiContentForm> event) throws Exception {
      event.getRequestContext().getJavascriptManager().addJavascript("history.go(-1);");
    }
  }

  /**
   * Gets the page navigation.
   * 
   * @return the page navigation
   */
  public PageNavigation getPageNavigation() {
    return pageNavigation;
  }

  /**
   * Sets the page navigation.
   * 
   * @param pageNavigation the new page navigation
   */
  public void setPageNavigation(PageNavigation pageNavigation) {
    this.pageNavigation = pageNavigation;
  }

  /**
   * Gets the parent uri.
   * 
   * @return the parent uri
   */
  public String getParentUri() {
    return parentUri;
  }

  /**
   * Sets the parent uri.
   * 
   * @param parentUri the new parent uri
   */
  public void setParentUri(String parentUri) {
    this.parentUri = parentUri;
  }

  /**
   * Gets the page uri.
   * 
   * @return the page uri
   */
  public String getPageUri() {
    return pageUri;
  }

  /**
   * Sets the page uri.
   * 
   * @param pageUri the new page uri
   */
  public void setPageUri(String pageUri) {
    this.pageUri = pageUri;
  }
}