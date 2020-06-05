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
package org.exoplatform.ecm.webui.component.explorer.sidebar ;

import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Row;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.util.Base64;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.DocumentProviderUtils;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.services.wcm.search.base.LazyPageList;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Aug 2, 2006
 */
@ComponentConfig(
                 template =  "app:/groovy/webui/component/explorer/sidebar/UITreeExplorer.gtmpl",
                 events = {
                     @EventConfig(listeners = UITreeExplorer.ExpandActionListener.class),
                     @EventConfig(listeners = UITreeExplorer.CollapseActionListener.class),
                     @EventConfig(listeners = UITreeExplorer.ExpandTreeActionListener.class)
                 }
    )

public class UITreeExplorer extends UIContainer {

  private class TreeNodeDataCreater implements SearchDataCreator<TreeNode>{

    @Override
    public TreeNode createData(Node node, Row row, SearchResult searchResult){
      try {
        return new TreeNode(node);
      } catch (RepositoryException e) {
        return null;
      }
    }
  }
  
  /**
   * Logger.
   */
  private static final Log  LOG        = ExoLogger.getLogger(UITreeExplorer.class.getName());

  private TreeNode          treeRoot_;

  private String            expandPath = null;

  private boolean           isExpand   = false;

  /** The cloud drive service. */
  private CloudDriveService cloudDriveService;

  public UITreeExplorer() throws Exception {
    cloudDriveService = CommonsUtils.getService(CloudDriveService.class);
  }


  public UIRightClickPopupMenu getContextMenu() {
    return getAncestorOfType(UIWorkingArea.class).getChild(UIRightClickPopupMenu.class) ;
  }

  UIWorkingArea getWorkingArea() {
    return getAncestorOfType(UIWorkingArea.class);
  }

  private UISideBar getSideBar() {
    return getWorkingArea().findFirstComponentOfType(UISideBar.class);
  }

  UIComponent getCustomAction() throws Exception {
    return getAncestorOfType(UIWorkingArea.class).getCustomAction();
  }

  public TreeNode getRootTreeNode() { return treeRoot_ ; }

  public String getRootActionList() throws Exception {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (!clipboardService.getClipboardList(userId, false).isEmpty()) {
      return getContextMenu().getJSOnclickShowPopup(uiExplorer.getCurrentDriveWorkspace() + ":"
          + uiExplorer.getRootPath(),
          "Paste").toString();
    }
    return "" ;
  }

  public boolean isDirectlyDrive() {
    PortletPreferences portletPref =
        getAncestorOfType(UIJCRExplorerPortlet.class).getPortletPreferences();
    String usecase =  portletPref.getValue("usecase", "").trim();
    if ("selection".equals(usecase)) {
      return false;
    }
    return true;
  }

  public String getDriveName() {
    return getAncestorOfType(UIJCRExplorer.class).getDriveData().getName() ;
  }

  public String getLabel()  {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    DriveData driveData = getAncestorOfType(UIJCRExplorer.class).getDriveData();
    String id = driveData.getName();
    String path = driveData.getHomePath();

    String driveLabelKey = "Drives.label." + id.replace(".", "").replace(" ", "");
    try {
      if (ManageDriveServiceImpl.USER_DRIVE_NAME.equals(id)) {
        // User Documents drive
        driveLabelKey = "Drives.label.UserDocuments";
        String userDisplayName = "";

        String userIdPath = driveData.getParameters().get(ManageDriveServiceImpl.DRIVE_PARAMATER_USER_ID);
        String userId = userIdPath != null ? userIdPath.substring(userIdPath.lastIndexOf("/") + 1) : null;
        if (StringUtils.isNotEmpty(userId)) {
          userDisplayName = userId;
          User user = this.getApplicationComponent(OrganizationService.class).getUserHandler().findUserByName(userId);
          if (user != null) {
            userDisplayName = user.getDisplayName();
          }
        }
        try {
          return res.getString(driveLabelKey).replace("{0}", userDisplayName);
        } catch (MissingResourceException mre) {
          LOG.error("Cannot get resource string for " + driveLabelKey);
        }

      } else {
        try {
          return res.getString(driveLabelKey);
        } catch (MissingResourceException ex) {
          try {
            CloudDrive cloudDrives = cloudDriveService.findDrive(driveData.getWorkspace(), driveData.getHomePath());
            if (cloudDrives != null) {
              // Cloud drives
              return cloudDrives.getTitle();
            } else {
              try {
                RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
                Node groupNode = (Node) WCMCoreUtils.getSystemSessionProvider()
                                                    .getSession(repoService.getCurrentRepository()
                                                                           .getConfiguration()
                                                                           .getDefaultWorkspaceName(),
                                                                repoService.getCurrentRepository())
                                                    .getItem(path);
                return groupNode.getProperty(NodetypeConstant.EXO_LABEL).getString();
              } catch (Exception e) {
                return id.replace(".", " / ");
              }
            }
          } catch (RepositoryException e) {
            LOG.warn("Cannot find clouddrive " + driveData.getHomePath() + " in " + driveData.getWorkspace(), e);
            return id.replace(".", " / ");
          }
        }
      }
    } catch (Exception ex) {
      LOG.warn("Can not find resource string for " + driveLabelKey, ex);
    }
    return id.replace(".", " / ");
  }

  public boolean isAllowNodeTypesOnTree(Node node) throws RepositoryException {
    DriveData currentDrive = getAncestorOfType(UIJCRExplorer.class).getDriveData();
    String allowNodeTypesOnTree = currentDrive.getAllowNodeTypesOnTree();
    if ((allowNodeTypesOnTree == null) || (allowNodeTypesOnTree.equals("*"))) return true;
    String[] arrayAllowNodeTypesOnTree = allowNodeTypesOnTree.split(",");
    for (String itemAllowNodeTypes : arrayAllowNodeTypesOnTree) {
      if ((itemAllowNodeTypes.trim().length() > 0) && node.isNodeType(itemAllowNodeTypes.trim())) return true;
    }
    return false;
  }

  public String getActionsList(Node node) throws Exception {
    if(node == null) return "" ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    try {
      NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
      nodeFinder.getItem(uiExplorer.getSession(), node.getPath());
      //uiExplorer.getSession().getItem(node.getPath());
      return getAncestorOfType(UIWorkingArea.class).getActionsExtensionList(node) ;
    } catch(PathNotFoundException pne) {
      uiExplorer.refreshExplorerWithoutClosingPopup();
      return "";
    }
  }

  public List<Node> getCustomActions(Node node) throws Exception {
    return getAncestorOfType(UIWorkingArea.class).getCustomActions(node) ;
  }

  public boolean isPreferenceNode(Node node) {
    return getAncestorOfType(UIWorkingArea.class).isPreferenceNode(node) ;
  }

  @SuppressWarnings("unchecked")
  public List<TreeNode> getRenderedChildren(TreeNode treeNode) throws Exception {
    if(isPaginated(treeNode)) {
      UITreeNodePageIterator pageIterator = findComponentById(treeNode.getPath());
      return pageIterator.getCurrentPageData();
    }
    if(isShowChildren(treeNode) && treeNode.getChildrenSize() > 0 && treeNode.getChildren().size() == 0) {
      UIJCRExplorer jcrExplorer = getAncestorOfType(UIJCRExplorer.class);
      treeNode.setChildren(jcrExplorer.getChildrenList(treeNode.getPath(), false));
      return treeNode.getChildren();
    }
    return treeNode.getChildren();
  }

  public boolean isSystemWorkspace() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).isSystemWorkspace() ;
  }

  public UITreeNodePageIterator getUIPageIterator(String id) throws Exception {
    return findComponentById(id);
  }

  public boolean isSymLink(Node node) throws RepositoryException {
    LinkManager linkManager = getApplicationComponent(LinkManager.class);
    return linkManager.isLink(node);
  }

  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  public boolean isPaginated(TreeNode treeNode) {
    UIJCRExplorer jcrExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    int nodePerPages = jcrExplorer.getPreference().getNodesPerPage();
    return (treeNode.getChildrenSize()>nodePerPages) && (findComponentById(treeNode.getPath()) != null) ;
  }

  public String getPortalName() {
    PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  public String getServerPath() {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
        portletRequestContext.getRequest().getServerName() + ":" +
        String.format("%s",portletRequestContext.getRequest().getServerPort()) ;
    return prefixWebDAV ;
  }
  
  public boolean isShowChildren(TreeNode treeNode){
    UIJCRExplorer jcrExplorer = getAncestorOfType(UIJCRExplorer.class);
    String currentPath = jcrExplorer.getCurrentPath();
    return treeNode.isExpanded() || currentPath.startsWith(treeNode.getPath());
  }

  public String getRepository() {
    return getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
  }

  public String getEncodeCurrentPath() {
    return encodeBase64(getAncestorOfType(UIJCRExplorer.class).getCurrentPath());
  }

  public String getEncodeExpandPath() {
    if(expandPath != null)
      return encodeBase64(expandPath);
    else return null;
  }

  public boolean getIsExpand() {
    return isExpand;
  }

  public static String encodeBase64(String value) {
    value = value == null ? "" : value;
    return Base64.encode(value.getBytes()).replaceAll(Base64.LINE_SEPARATOR,"");
  }

  /**
   * 
   * @param id ID of TreeNodePageIterator, should be path of node
   * @param pageList Paged list children of node
   * @param selectedPath Path relate with pageList
   * @param currentPath Path that user are working on
   * 
   * */
  private void addTreeNodePageIteratorAsChild(String id,
                                              PageList<TreeNode> pageList,
                                              String selectedPath,
                                              String currentPath) throws Exception {
    if (findComponentById(id) == null) {
      UITreeNodePageIterator nodePageIterator = addChild(UITreeNodePageIterator.class, null, id);
      nodePageIterator.setPageList(pageList);
      nodePageIterator.setSelectedPath(selectedPath);
    } else {
      UITreeNodePageIterator existedComponent = findComponentById(id);
      int currentPage = existedComponent.getCurrentPage();
      existedComponent.setPageList(pageList);
      if (!selectedPath.equalsIgnoreCase(currentPath)) {
        if (currentPage <= existedComponent.getAvailablePage()) {
          existedComponent.setCurrentPage(currentPage);
        }
      }
    }
  }

  private Node getRootNode() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return uiExplorer.getRootNode();
  }

  private void buildTree(String path) throws Exception {
    if(getSideBar() == null || !getSideBar().isRenderComponent("Explorer")) {
      return;
    }
    UIJCRExplorer jcrExplorer = getAncestorOfType(UIJCRExplorer.class);
    int nodePerPages = jcrExplorer.getPreference().getNodesPerPage();
    TreeNode treeRoot = new TreeNode(getRootNode());
    if (path == null)
      path = jcrExplorer.getCurrentPath();
    String[] arr = path.replaceFirst(treeRoot.getPath(), "").split("/");
    TreeNode temp = treeRoot;
    StringBuffer subPath = null;
    String rootPath = treeRoot.getPath();
    StringBuffer prefix = new StringBuffer(rootPath);
    if (!rootPath.equals("/")) {
      prefix.append("/");
    }
    HashSet<String> emptySet = new HashSet<String>();//This control doesn't have any filter
    //Build root tree node
    if (temp.getChildrenSize() > nodePerPages) {
      LazyPageList<TreeNode> pageList = DocumentProviderUtils.getInstance().getPageList(jcrExplorer.getWorkspaceName(), 
                                                                                        rootPath, 
                                                                                        jcrExplorer.getPreference(), 
                                                                                        emptySet, 
                                                                                        emptySet, 
                                                                                        new TreeNodeDataCreater());
      addTreeNodePageIteratorAsChild(treeRoot.getPath(), pageList, rootPath, path);
    } else temp.setChildren(jcrExplorer.getChildrenList(rootPath, false));
    //Build children
    for (String nodeName : arr) {
      if (nodeName.length() == 0)
        continue;
      temp = temp.getChildByName(nodeName);
      if (temp == null) {
        treeRoot_ = treeRoot;
        return;
      }
      if (subPath == null) {
        subPath = new StringBuffer();
        subPath.append(prefix).append(nodeName);
      } else {
        subPath.append("/").append(nodeName);
      }
      if (temp.getChildrenSize() > nodePerPages) {
        LazyPageList<TreeNode> pageList = DocumentProviderUtils.getInstance().getPageList(jcrExplorer.getWorkspaceName(), 
                                                                                          subPath.toString(), 
                                                                                          jcrExplorer.getPreference(), 
                                                                                          emptySet, 
                                                                                          emptySet, 
                                                                                          new TreeNodeDataCreater());
        addTreeNodePageIteratorAsChild(temp.getPath(), pageList, subPath.toString(), path);
      } else temp.setChildren(jcrExplorer.getChildrenList(subPath.toString(), false));
    }
    treeRoot_ = treeRoot;
  }

  public void buildTree() throws Exception {
    buildTree(null);

  }

  public boolean isDocumentNodeType(Node node) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.isManagedNodeType(node.getPrimaryNodeType().getName());
  }

  public String getSelectedPath() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return encodeBase64(uiExplorer.getCurrentPath());
  }


  static public class ExpandActionListener extends EventListener<UITreeExplorer> {
    public void execute(Event<UITreeExplorer> event) throws Exception {
      UITreeExplorer uiTreeExplorer = event.getSource();
      String path = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiTreeExplorer.isExpand = false;
      UIJCRExplorer uiExplorer = uiTreeExplorer.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiTreeExplorer.getAncestorOfType(UIApplication.class) ;
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      Item item = null;
      try {
        Session session = uiExplorer.getSessionByWorkspace(workspaceName);
        // Check if the path exists
        NodeFinder nodeFinder = uiTreeExplorer.getApplicationComponent(NodeFinder.class);
        item = nodeFinder.getItem(session, path);
      } catch(PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch(ItemNotFoundException inf) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      if (isInTrash(item))
        return;

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiDocumentWorkspace.setRendered(true);
      } else {
        uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class);
      }
      uiExplorer.setSelectNode(workspaceName, path);
      //      UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class);
      //      UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;

      UIPageIterator contentPageIterator = uiExplorer.findComponentById(UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID);
      if(contentPageIterator != null) {
        contentPageIterator.setCurrentPage(1);
      }
      uiExplorer.updateAjax(event);
      event.getRequestContext().getJavascriptManager().
      require("SHARED/multiUpload", "multiUpload").
      addScripts("multiUpload.setLocation('" + 
          uiExplorer.getWorkspaceName()  + "','" + 
          uiExplorer.getDriveData().getName()  + "','" +
          uiTreeExplorer.getLabel()  + "','" +
          uiExplorer.getCurrentPath() + "','" +
        Utils.getPersonalDrivePath(uiExplorer.getDriveData().getHomePath(),
                               ConversationState.getCurrent().getIdentity().getUserId()) + "', '"+
              autoVersionService.isVersionSupport(uiExplorer.getCurrentPath(), uiExplorer.getCurrentWorkspace())+"');");
    }

  }

  private static boolean isInTrash(Item item) throws RepositoryException {
    return (item instanceof Node) && Utils.isInTrash((Node) item);
  }

  static public class ExpandTreeActionListener extends EventListener<UITreeExplorer> {
    public void execute(Event<UITreeExplorer> event) throws Exception {
      UITreeExplorer uiTreeExplorer = event.getSource();
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      uiTreeExplorer.expandPath = path;
      uiTreeExplorer.isExpand = true;
      UIJCRExplorer uiExplorer = uiTreeExplorer.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiTreeExplorer.getAncestorOfType(UIApplication.class);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      Item item = null;
      try {
        Session session = uiExplorer.getSessionByWorkspace(workspaceName);
        // Check if the path exists
        NodeFinder nodeFinder = uiTreeExplorer.getApplicationComponent(NodeFinder.class);
        item = nodeFinder.getItem(session, path);
      } catch (PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (ItemNotFoundException inf) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      if (isInTrash(item))
        return;
      if (uiExplorer.getPreference().isShowSideBar()
          && uiExplorer.getAncestorOfType(UIJCRExplorerPortlet.class).isShowSideBar()) {
        uiTreeExplorer.buildTree(path);
      }
    }
  }

  static public class CollapseActionListener extends EventListener<UITreeExplorer> {
    public void execute(Event<UITreeExplorer> event) throws Exception {
      UITreeExplorer treeExplorer = event.getSource();
      UIApplication uiApp = treeExplorer.getAncestorOfType(UIApplication.class);
      try {
        String path = event.getRequestContext().getRequestParameter(OBJECTID) ;
        UIJCRExplorer uiExplorer = treeExplorer.getAncestorOfType(UIJCRExplorer.class) ;
        path = LinkUtils.getParentPath(path) ;
        uiExplorer.setSelectNode(path) ;
        uiExplorer.updateAjax(event) ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null,
                                                ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  /**
   * Check the node is passed have child node or not.
   *
   * @param node
   * @return
   * @throws Exception
   */
  public boolean hasChildNode(Node node) throws Exception {
    if(!node.hasNodes()) return false;
    UIJCRExplorer uiExplorer =  getAncestorOfType(UIJCRExplorer.class);
    Preference preferences = uiExplorer.getPreference();
    if(!isFolderType(node) && !preferences.isJcrEnable() && !node.isNodeType(org.exoplatform.ecm.webui.utils.Utils.EXO_TAXONOMY))
      return false;
    NodeIterator iterator = node.getNodes();
    while(iterator.hasNext()) {
      Node tmpNode = iterator.nextNode();
      // Not allow to show hidden and non-document nodes
      if (!preferences.isShowHiddenNode() && !preferences.isShowNonDocumentType()) {
        if(!tmpNode.isNodeType(org.exoplatform.ecm.webui.utils.Utils.EXO_HIDDENABLE) && isDocumentOrFolderType(tmpNode)) 
          return true;                 
      }
      // Not allow to show non-document nodes
      else if (preferences.isShowHiddenNode() && !preferences.isShowNonDocumentType()) {
        if(isDocumentOrFolderType(tmpNode)) return true; 
      }
      // Not allow to show hidden nodes
      else if (!preferences.isShowHiddenNode() && preferences.isShowNonDocumentType()) {
        if(!tmpNode.isNodeType(org.exoplatform.ecm.webui.utils.Utils.EXO_HIDDENABLE))
          return true;
      }
      // Allow to show hidden and non-document nodes
      else return true;
    }
    return false;
  }
  /**
   * Check the node is passed is a document/folder or not.
   *
   * @param node
   * @return
   * @throws Exception
   */
  private boolean isDocumentOrFolderType(Node node) throws Exception {
    if(node.isNodeType(org.exoplatform.ecm.webui.utils.Utils.NT_FOLDER) ||
        node.isNodeType(org.exoplatform.ecm.webui.utils.Utils.NT_UNSTRUCTURED)) return true;
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    NodeType nodeType = node.getPrimaryNodeType();
    return templateService.getDocumentTemplates().contains(nodeType.getName());
  }
  private boolean isFolderType(Node node) throws Exception {
    if(node.isNodeType(org.exoplatform.ecm.webui.utils.Utils.NT_FOLDER) ||
        node.isNodeType(org.exoplatform.ecm.webui.utils.Utils.NT_UNSTRUCTURED)) return true;
    return false;
  }
}
