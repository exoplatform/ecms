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
package org.exoplatform.ecm.webui.component.explorer ;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.ecm.jcr.TypeNodeComparator;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.utils.comparator.PropertyValueComparator;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.comparator.DateComparator;
import org.exoplatform.ecm.webui.comparator.NodeSizeComparator;
import org.exoplatform.ecm.webui.comparator.NodeTitleComparator;
import org.exoplatform.ecm.webui.comparator.StringComparator;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UISelectDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeNodePageIterator;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.link.ItemLinkAware;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.link.NodeLinkAware;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIJCRExplorer extends UIContainer {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIJCRExplorer.class.getName());

  private LinkedList<String> nodesHistory_ = new LinkedList<String>() ;
  private LinkedList<String> wsHistory_ = new LinkedList<String>();
  private PortletPreferences pref_ ;
  private Preference preferences_;
  private Map<String, Integer> pageIndexHistory_ = new HashMap<String, Integer>();
  private Map<String, HistoryEntry> addressPath_ = new HashMap<String, HistoryEntry>() ;
  private JCRResourceResolver jcrTemplateResourceResolver_ ;

  private String currentRootPath_ ;
  private String currentPath_ ;
  private String currentStatePath_ ;
  private String currentStateWorkspaceName_ ;
  private String lastWorkspaceName_ ;
  private String currentDriveRootPath_ ;
  private String currentDriveWorkspaceName_ ;
  private String currentDriveRepositoryName_ ;
  private String documentInfoTemplate_ ;
  private String language_ ;
  private String tagPath_ ;
  private String referenceWorkspace_ ;

  private boolean isViewTag_;
  private boolean isHidePopup_;
  private boolean isReferenceNode_;
  private DriveData driveData_ ;

  private boolean isFilterSave_ ;
  private boolean  isShowDocumentViewForFile_ = true;
  private boolean preferencesSaved_ = false;

  private int tagScope;

  private List<String> checkedSupportType = new ArrayList<String>();
  private Set<String> allItemFilterMap = new HashSet<String>();
  private Set<String> allItemByTypeFilterMap = new HashSet<String>();

  public Set<String> getAllItemFilterMap() { return allItemFilterMap; }
  public Set<String> getAllItemByTypeFilterMap() { return allItemByTypeFilterMap; }

  public int getTagScope() { return tagScope; }
  public void setTagScope(int scope) { tagScope = scope; }

  public boolean isFilterSave() { return isFilterSave_; }
  public void setFilterSave(boolean isFilterSave) { isFilterSave_ = isFilterSave; }

  public boolean  isShowDocumentViewForFile() { return isShowDocumentViewForFile_; }
  public void setShowDocumentViewForFile(boolean value) { isShowDocumentViewForFile_ = value; }

  public boolean isPreferencesSaved() { return preferencesSaved_; }
  public void setPreferencesSaved(boolean value) { preferencesSaved_ = value; }

  public boolean isAddingDocument() {
    UIPopupContainer uiPopupContainer = this.getChild(UIPopupContainer.class);
    UIPopupWindow uiPopup = uiPopupContainer.getChild(UIPopupWindow.class);

    UIWorkingArea uiWorkingArea = this.getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    //check if edit with popup
    UIComponent uiComp = uiPopup.getUIComponent();
    if (uiComp instanceof UIDocumentFormController && ((UIDocumentFormController)uiComp).isRendered()) {
      return ((UIDocumentFormController)uiComp).getChild(UIDocumentForm.class).isAddNew();
    }
    //check if edit without popup
    if (uiDocumentWorkspace.isRendered()) {
      UIDocumentFormController controller = uiDocumentWorkspace.getChild(UIDocumentFormController.class);
      if (controller != null && controller.isRendered()) {
        return controller.getChild(UIDocumentForm.class).isAddNew();
      }
    }
    return false;
  }

  public boolean isEditingDocument() {
    UIPopupContainer uiPopupContainer = this.getChild(UIPopupContainer.class);
    UIPopupWindow uiPopup = uiPopupContainer.getChild(UIPopupWindow.class);

    UIWorkingArea uiWorkingArea = this.getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    //check if edit with popup
    UIComponent uiComp = uiPopup.getUIComponent();

    if (uiPopup.isShow() && uiPopup.isRendered() &&
        uiComp instanceof UIDocumentFormController && ((UIDocumentFormController)uiComp).isRendered()) {
      return true;
    }
    //check if edit without popup
    if (uiDocumentWorkspace.isRendered()) {
      UIDocumentFormController controller = uiDocumentWorkspace.getChild(UIDocumentFormController.class);
      if (controller != null && controller.isRendered()) {
        return true;
      }
    }
    return false;
  }

  public List<String> getCheckedSupportType() {
    return checkedSupportType;
  }

  public void setCheckedSupportType(List<String> checkedSupportType) {
    this.checkedSupportType = checkedSupportType;
  }

  public UIJCRExplorer() throws Exception {
    addChild(UIControl.class, null, null);
    addChild(UIWorkingArea.class, null, null);
    addChild(UIPopupContainer.class, null, null);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, null);
    uiPopup.setId(uiPopup.getId() + "-" + UUID.randomUUID().toString().replaceAll("-", ""));
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
    pref_ = pcontext.getRequest().getPreferences();
    getChild(UIWorkingArea.class).initialize();
  }

  public String filterPath(String currentPath) throws Exception {
    if(LinkUtils.getDepth(currentRootPath_) == 0) return currentPath ;
    if(currentRootPath_.equals(currentPath_)) return "/" ;
    return currentPath.replaceFirst(currentRootPath_, "") ;
  }

  /**
   * Sets the root path
   */
  public void setRootPath(String rootPath) {
    currentDriveRootPath_ = rootPath;
    setCurrentRootPath(rootPath);
  }

  private void setCurrentRootPath(String rootPath) {
    currentRootPath_ = rootPath ;
  }

  /**
   * @return the root node itself if it is not a link otherwise the target node (= resolve the link)
   */
  public Node getRootNode() throws Exception {
    return getNodeByPath(currentRootPath_, getSession()) ;
  }

  /**
   * @return the root path
   */
  public String getRootPath() { return currentRootPath_; }


  private String getDefaultRootPath() { return "/"; }

  /**
   * @return the current node itself if it is not a link otherwise the target node (= resolve the link)
   */
  public Node getCurrentNode() throws Exception { return getNodeByPath(currentPath_, getSession()) ; }

  /**
   * @return the current node even if it is a link (= don't resolve the link)
   */
  public Node getRealCurrentNode() throws Exception {
    return getNodeByPath(currentPath_, getSession(), false);
  }

  /**
   * @return the virtual current path
   */
  public String getCurrentPath() { return currentPath_ ; }

  /**
   * Sets the virtual current path
   */
  public void setCurrentPath(String  currentPath) {
    if (currentPath_ == null || !currentPath_.equals(currentPath)) {
      isShowDocumentViewForFile_ = true;
    }
    currentPath_ = currentPath;
  }

  /**
   * Indicates if the current node is a referenced node
   */
  public boolean isReferenceNode() { return isReferenceNode_ ; }

  /**
   * Tells that the current node is a referenced node
   */
  public void setIsReferenceNode(boolean isReferenceNode) { isReferenceNode_ = isReferenceNode ; }

  /**
   * Sets the workspace name the referenced node
   */
  public void setReferenceWorkspace(String referenceWorkspace) { referenceWorkspace_ = referenceWorkspace ; }
  public String getReferenceWorkspace() { return referenceWorkspace_ ; }

  private String setTargetWorkspaceProperties(String workspaceName) {
    if (workspaceName != null && workspaceName.length() > 0) {
      if (!workspaceName.equals(getCurrentDriveWorkspace())) {
        setIsReferenceNode(true);
        setReferenceWorkspace(workspaceName);
        setCurrentRootPath(getDefaultRootPath());
        return workspaceName;
      } else if(isReferenceNode()) {
        setIsReferenceNode(false);
        setCurrentRootPath(currentDriveRootPath_);
      }
    }
    return getCurrentDriveWorkspace();
  }

  /**
   * Tells to go back to the given location
   */
  public void setBackNodePath(String previousWorkspaceName, String previousPath) throws Exception {
    setBackSelectNode(previousWorkspaceName, previousPath);
    refreshExplorer();

    // Back to last pageIndex if previous path has paginator
    if (pageIndexHistory_.containsKey(previousPath) && hasPaginator(previousPath, previousWorkspaceName)) {
      UIPageIterator contentPageIterator = this.findComponentById(UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID);
      if (contentPageIterator != null ) {
        // Get last pageIndex
        int previousPageIndex = pageIndexHistory_.get(previousPath);
        int avaiablePage = contentPageIterator.getAvailablePage();
        previousPageIndex = (avaiablePage >= previousPageIndex)? previousPageIndex : avaiablePage;

        // Set last pageIndex for paginator of UIDocumentInfo
        contentPageIterator.setCurrentPage(previousPageIndex);

        // Set last pageIndex for UITreeNodePageIterator
        UITreeExplorer uiTreeExplorer = this.findFirstComponentOfType(UITreeExplorer.class);
        if (uiTreeExplorer != null) {
          UITreeNodePageIterator extendedPageIterator =
              uiTreeExplorer.getUIPageIterator(previousPath);
          if (extendedPageIterator != null) {
            extendedPageIterator.setCurrentPage(previousPageIndex);
          }
        }
      }
    }
  }

  /**
   * Check if node has paginator when viewing it's children.
   *
   * @param nodePath
   * @param workspaceName
   * @return
   * @throws Exception
   */
  public boolean hasPaginator(String nodePath, String workspaceName) throws Exception {
    int nodePerPages = this.getPreference().getNodesPerPage();
    return getNodeByPath(nodePath, this.getSessionByWorkspace(workspaceName)).getNodes().getSize() > nodePerPages;
  }

  public void setDriveData(DriveData driveData) { driveData_ = driveData ; }
  public DriveData getDriveData() { return driveData_ ; }

  public void setLanguage(String language) { language_ = language ; }
  public String getLanguage() { return language_ ; }

  public LinkedList<String> getNodesHistory() { return nodesHistory_ ; }

  public LinkedList<String> getWorkspacesHistory() { return wsHistory_; }

  public Collection<HistoryEntry> getHistory() { return addressPath_.values() ; }

  public SessionProvider getSessionProvider() {
    if(WCMCoreUtils.getRemoteUser().equals(WCMCoreUtils.getSuperUser())) {
      return getSystemProvider();
    }
    return WCMCoreUtils.getUserSessionProvider();
  }

  public SessionProvider getSystemProvider() { return WCMCoreUtils.getSystemSessionProvider(); }

  /**
   * @return the session of the current node (= UIJCRExplorer.getCurrentNode())
   */
  public Session getTargetSession() throws Exception {
    return getCurrentNode().getSession();
  }

  public Session getSession() throws Exception {
    if(isReferenceNode_) return getSessionProvider().getSession(referenceWorkspace_, getRepository()) ;
    return getSessionProvider().getSession(currentDriveWorkspaceName_, getRepository()) ;
  }

  public String getWorkspaceName() {
    return (isReferenceNode_ ? referenceWorkspace_ : currentDriveWorkspaceName_);
  }

  public Session getSystemSession() throws Exception {
    if(isReferenceNode_) return getSystemProvider().getSession(referenceWorkspace_, getRepository()) ;
    return getSystemProvider().getSession(currentDriveWorkspaceName_, getRepository()) ;
  }

  public String getDocumentInfoTemplate() { return documentInfoTemplate_ ; }
  public void setRenderTemplate(String template) {
    newJCRTemplateResourceResolver() ;
    documentInfoTemplate_  = template ;
  }

  public void setCurrentState() {
    setCurrentState(currentDriveWorkspaceName_, currentPath_);
  }

  public void setCurrentState(String currentStateWorkspaceName, String currentStatePath) {
    currentStateWorkspaceName_ = currentStateWorkspaceName;
    currentStatePath_ =  currentStatePath ;
  }

  public String getCurrentStatePath() { return currentStatePath_;};
  public void setCurrentStatePath(String currentStatePath) {
    setCurrentState(currentDriveWorkspaceName_, currentStatePath);
  }

  public Node getCurrentStateNode() throws Exception {
    return getNodeByPath(currentStatePath_, getSessionProvider().getSession(currentStateWorkspaceName_, getRepository())) ;
  }

  public JCRResourceResolver getJCRTemplateResourceResolver() { return jcrTemplateResourceResolver_; }
  public void newJCRTemplateResourceResolver() {
    try{
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      DMSRepositoryConfiguration dmsRepoConfig =
          dmsConfiguration.getConfig();
      String workspace =  dmsRepoConfig.getSystemWorkspace();
      jcrTemplateResourceResolver_ = new JCRResourceResolver(workspace) ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot instantiate the JCRResourceResolver", e);
      }
    }
  }

  /**
   * Sets the repository of the current drive
   */
  public void setRepositoryName(String repositoryName) { currentDriveRepositoryName_ = repositoryName ; }
  /**
   * @return the repository of the current drive
   */
  public String getRepositoryName() {
    try {
      return getApplicationComponent(RepositoryService.class).getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      String repoName = System.getProperty("gatein.tenant.repository.name");
      if (repoName!=null)
        return repoName;
      return currentDriveRepositoryName_;
    }
  }

  /**
   * Sets the workspace of the current drive
   */
  public void setWorkspaceName(String workspaceName) {
    currentDriveWorkspaceName_ = workspaceName ;
    if (lastWorkspaceName_ == null) {
      setLastWorkspace(workspaceName);
    }
  }

  private void setLastWorkspace(String lastWorkspaceName) {
    lastWorkspaceName_ = lastWorkspaceName;
  }

  /**
   * @return the workspace of the current drive
   */
  public String getCurrentDriveWorkspace() { return currentDriveWorkspaceName_ ; }

  /**
   * @return the workspace of the session of the current node (= UIJCRExplorer.getCurrentNode())
   */
  public String getCurrentWorkspace() {
    try {
      return getCurrentNode().getSession().getWorkspace().getName();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("The workspace of the current node cannot be found, the workspace of the drive will be used", e);
      }
    }
    return getCurrentDriveWorkspace();
  }

  public ManageableRepository getRepository() throws Exception{
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class) ;
    return repositoryService.getCurrentRepository();
  }

  public Session getSessionByWorkspace(String wsName) throws Exception{
    if(wsName == null ) return getSession() ;
    return getSessionProvider().getSession(wsName,getRepository()) ;
  }

  public boolean isSystemWorkspace() throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    String systemWS = repositoryService.getCurrentRepository()
        .getConfiguration()
        .getSystemWorkspaceName();
    if(getCurrentWorkspace().equals(systemWS)) return true ;
    return false ;
  }

  public void refreshExplorer() throws Exception {
    refreshExplorer(null, true);
  }

  public void refreshExplorerWithoutClosingPopup() throws Exception {
    refreshExplorer(null, false);
  }

  public void setPathToAddressBar(String path) throws Exception {
    findFirstComponentOfType(UIAddressBar.class).getUIStringInput(
                                                                  UIAddressBar.FIELD_ADDRESS).setValue(Text.unescapeIllegalJcrChars(filterPath(path))) ;
    findFirstComponentOfType(UIAddressBar.class).getUIInput(
                                                            UIAddressBar.FIELD_ADDRESS_HIDDEN).setValue(filterPath(path)) ;
  }

  private void refreshExplorer(Node currentNode) throws Exception {
    refreshExplorer(currentNode, true);
  }

  public void refreshExplorer(Node currentNode, boolean closePopup) throws Exception {
    try {
      Node nodeGet = currentNode == null ? getCurrentNode() : currentNode;
      if(nodeGet.hasProperty(Utils.EXO_LANGUAGE)) {
        setLanguage(nodeGet.getProperty(Utils.EXO_LANGUAGE).getValue().getString());
      }
    } catch(PathNotFoundException path) {
      if (LOG.isErrorEnabled()) {
        LOG.error("The node cannot be found ", path);
      }
      setCurrentPath(currentRootPath_);
    }
    findFirstComponentOfType(UIAddressBar.class).getUIStringInput(UIAddressBar.FIELD_ADDRESS).
    setValue(Text.unescapeIllegalJcrChars(filterPath(currentPath_))) ;
    findFirstComponentOfType(UIAddressBar.class).getUIInput(UIAddressBar.FIELD_ADDRESS_HIDDEN).
    setValue(filterPath(currentPath_)) ;
    UIWorkingArea uiWorkingArea = getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);

    UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class);
    UIDocumentWithTree uiDocumentWithTree = uiDocumentContainer.getChildById("UIDocumentWithTree");
    UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
    uiDocumentWithTree.updatePageListData();
    uiDocumentInfo.updatePageListData();

    if(uiDocumentWorkspace.isRendered()) {
      if (uiDocumentWorkspace.getChild(UIDocumentFormController.class) == null ||
          !uiDocumentWorkspace.getChild(UIDocumentFormController.class).isRendered()) {
        if(isShowViewFile() &&  !(isShowDocumentViewForFile())) {
          uiDocumentContainer.setRenderedChild("UIDocumentWithTree");
        } else {
          uiDocumentContainer.setRenderedChild("UIDocumentInfo") ;
        }
        if(getCurrentNode().isNodeType(Utils.NT_FOLDER) || getCurrentNode().isNodeType(Utils.NT_UNSTRUCTURED))
          uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class) ;
      } else {
        UIDocumentFormController uiDocController = uiDocumentWorkspace.getChild(UIDocumentFormController.class);
        UISelectDocumentForm uiSelectDoc = uiDocController.getChild(UISelectDocumentForm.class);
        if (uiSelectDoc != null && uiSelectDoc.isRendered()) {
          uiSelectDoc.updatePageListData();
        }
      }
    }
    UISideBar uiSideBar = uiWorkingArea.findFirstComponentOfType(UISideBar.class);
    uiSideBar.setRendered(preferences_.isShowSideBar());
    if(preferences_.isShowSideBar()) {
      UITreeExplorer treeExplorer = findFirstComponentOfType(UITreeExplorer.class);
      if (treeExplorer.equals(uiSideBar.getChildById(uiSideBar.getCurrentComp()))) {
        treeExplorer.buildTree();
      }
      uiSideBar.updateSideBarView();
    }
    if (closePopup) {
      UIPopupContainer popupAction = getChild(UIPopupContainer.class);
      popupAction.deActivate();
    }
  }

  public boolean nodeIsLocked(String path, Session session) throws Exception {
    Node node = getNodeByPath(path, session) ;
    return nodeIsLocked(node);
  }

  public boolean nodeIsLocked(Node node) throws Exception {
    if(!node.isLocked()) return false;
    String lockToken = LockUtil.getLockTokenOfUser(node);
    if(lockToken != null) {
      node.getSession().addLockToken(LockUtil.getLockToken(node));
      return false;
    }
    return true;
  }

  /**
   * Allows you to add a lock token to the given node
   */
  public void addLockToken(Node node) throws Exception {
    org.exoplatform.wcm.webui.Utils.addLockToken(node);
  }

  public boolean hasAddPermission() {
    try {
      ((ExtendedNode)getCurrentNode()).checkPermission(PermissionType.ADD_NODE) ;
    } catch(Exception e) {
      return false ;
    }
    return true ;
  }

  public boolean hasEditPermission() {
    try {
      ((ExtendedNode)getCurrentNode()).checkPermission(PermissionType.SET_PROPERTY) ;
    } catch(Exception e) {
      return false ;
    }
    return true ;
  }

  public boolean hasRemovePermission() {
    try {
      ((ExtendedNode)getCurrentNode()).checkPermission(PermissionType.REMOVE) ;
    } catch(Exception e) {
      return false ;
    }
    return true ;
  }

  public boolean hasReadPermission() {
    try {
      ((ExtendedNode)getCurrentNode()).checkPermission(PermissionType.READ) ;
    } catch(Exception e) {
      return false ;
    }
    return true ;
  }

  public Node getViewNode(String nodeType) throws Exception {
    try {
      Item primaryItem = getCurrentNode().getPrimaryItem() ;
      if(primaryItem == null || !primaryItem.isNode()) return getCurrentNode() ;
      if(primaryItem != null && primaryItem.isNode()) {
        Node primaryNode = (Node) primaryItem ;
        if(primaryNode.isNodeType(nodeType)) return primaryNode ;
      }
    } catch(ItemNotFoundException item) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Primary item not found for " + getCurrentNode().getPath());
      }
      return getCurrentNode() ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("The node cannot be seen", e);
      }
      return getCurrentNode() ;
    }
    return getCurrentNode() ;
  }

  public List<String> getMultiValues(Node node, String name) throws Exception {
    List<String> list = new ArrayList<String>();
    if(!node.hasProperty(name)) return list;
    if (!node.getProperty(name).getDefinition().isMultiple()) {
      try {
        if (node.hasProperty(name)) {
          list.add(node.getProperty(name).getString());
        }
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("The property '" + name + "' cannot be found ", e);
        }
        list.add("") ;
      }
      return list;
    }
    Value[] values = node.getProperty(name).getValues();
    for (Value value : values) {
      list.add(value.getString());
    }
    return list;
  }

  public void setIsHidePopup(boolean isHidePopup) { isHidePopup_ = isHidePopup ; }

  public void updateAjax(Event<?> event) throws Exception {
    UIJCRExplorerPortlet uiPortlet = getAncestorOfType(UIJCRExplorerPortlet.class);
    UIAddressBar uiAddressBar = findFirstComponentOfType(UIAddressBar.class) ;
    UIWorkingArea uiWorkingArea = getChild(UIWorkingArea.class) ;
    UIActionBar uiActionBar = findFirstComponentOfType(UIActionBar.class) ;
    UISideBar uiSideBar = findFirstComponentOfType(UISideBar.class);
    UITreeExplorer uiTreeExplorer = findFirstComponentOfType(UITreeExplorer.class);

    uiAddressBar.getUIStringInput(UIAddressBar.FIELD_ADDRESS).setValue(
                                                                       Text.unescapeIllegalJcrChars(filterPath(currentPath_))) ;
    uiAddressBar.getUIInput(UIAddressBar.FIELD_ADDRESS_HIDDEN).setValue(
	    filterPath(currentPath_)) ;
    event.getRequestContext().addUIComponentToUpdateByAjax(getChild(UIControl.class)) ;
    UIPageIterator contentPageIterator = this.findComponentById(UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID);
    int currentPage = contentPageIterator.getCurrentPage();
    int currentPageInTree = 1;
    
    UITreeNodePageIterator extendedPageIterator =
        uiTreeExplorer.findFirstComponentOfType(UITreeNodePageIterator.class);
    if(extendedPageIterator != null) currentPageInTree = extendedPageIterator.getCurrentPage();
    
    if(preferences_.isShowSideBar()) {
      UITreeExplorer treeExplorer = findFirstComponentOfType(UITreeExplorer.class);
      if (treeExplorer.equals(uiSideBar.getChildById(uiSideBar.getCurrentComp()))) {
        treeExplorer.buildTree();
      }
    }
    UIDocumentWorkspace uiDocWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    if(uiDocWorkspace.isRendered()) {
      if (uiDocWorkspace.getChild(UIDocumentFormController.class) == null ||
          !uiDocWorkspace.getChild(UIDocumentFormController.class).isRendered()) {
        UIDocumentContainer uiDocumentContainer = uiDocWorkspace.getChild(UIDocumentContainer.class) ;
        UIDocumentWithTree uiDocumentWithTree = uiDocumentContainer.getChildById("UIDocumentWithTree");
        if(isShowViewFile() &&  !(isShowDocumentViewForFile())) {
          uiDocumentWithTree.updatePageListData();
          uiDocumentContainer.setRenderedChild("UIDocumentWithTree");
        } else {
          UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
          uiDocumentInfo.updatePageListData();
          contentPageIterator.setCurrentPage(currentPage);
          uiDocumentContainer.setRenderedChild("UIDocumentInfo") ;
        }
        if(getCurrentNode().isNodeType(Utils.NT_FOLDER) || getCurrentNode().isNodeType(Utils.NT_UNSTRUCTURED))
          uiDocumentWithTree.updatePageListData();
        uiDocWorkspace.setRenderedChild(UIDocumentContainer.class) ;
      } else {
        UIDocumentFormController uiDocController = uiDocWorkspace.getChild(UIDocumentFormController.class);
        UISelectDocumentForm uiSelectDoc = uiDocController.getChild(UISelectDocumentForm.class);
        if (uiSelectDoc != null && uiSelectDoc.isRendered()) {
          uiSelectDoc.updatePageListData();
        }
      }
    }
    uiActionBar.setRendered(uiPortlet.isShowActionBar());
    uiAddressBar.setRendered(uiPortlet.isShowTopBar());
    uiSideBar.setRendered(preferences_.isShowSideBar());
    if(extendedPageIterator != null) extendedPageIterator.setCurrentPage(currentPageInTree);
    event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    if (uiSideBar.isRendered()) event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
    event.getRequestContext().addUIComponentToUpdateByAjax(getChild(UIControl.class)) ;

    if(!isHidePopup_) {
      UIPopupContainer popupAction = getChild(UIPopupContainer.class) ;
      if(popupAction.isRendered()) {
        popupAction.deActivate();
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      }
      UIPopupWindow popupWindow = getChild(UIPopupWindow.class) ;
      if(popupWindow != null && popupWindow.isShow()) {
        popupWindow.setShow(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow);
      }
    }
    isHidePopup_ = false ;
  }

  public boolean isShowViewFile() throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    NodeType nodeType = getCurrentNode().getPrimaryNodeType() ;
    NodeType[] superTypes = nodeType.getSupertypes() ;
    boolean isFolder = false ;
    for(NodeType superType : superTypes) {
      if(superType.getName().equals(Utils.NT_FOLDER) || superType.getName().equals(Utils.NT_UNSTRUCTURED)) {
        isFolder = true ;
      }
    }
    if(isFolder && templateService.getDocumentTemplates().contains(nodeType.getName())) {
      return true ;
    }
    return false;
  }

  public void cancelAction() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    UIPopupContainer popupAction = getChild(UIPopupContainer.class) ;
    popupAction.deActivate() ;
    context.addUIComponentToUpdateByAjax(popupAction);
    context.getJavascriptManager().require("SHARED/uiFileView", "uiFileView").
    addScripts("uiFileView.UIFileView.clearCheckboxes();");
  }

  public void record(String str, String ws) {
    /**
     * Uncomment this line if you have problem with the history
     *
     */
    //LOG.info("record(" + str + ", " + ws + ")", new Exception());
    nodesHistory_.add(str);
    wsHistory_.add(ws);
    addressPath_.put(str, new HistoryEntry(ws, str));
  }

  public void record(String str, String ws, int pageIndex) {
    record(str, ws);
    pageIndexHistory_.put(str, pageIndex);
  }

  public void clearNodeHistory(String currentPath) {
    nodesHistory_.clear();
    wsHistory_.clear();
    pageIndexHistory_.clear();
    addressPath_.clear();
    currentPath_ = currentPath;
  }

  public String rewind() { return nodesHistory_.removeLast() ; }

  public String previousWsName() { return wsHistory_.removeLast(); }

  public void setSelectNode(String workspaceName, String uri) throws Exception {
    String lastWorkspaceName = setTargetWorkspaceProperties(workspaceName);
    setSelectNode(uri);
    setLastWorkspace(lastWorkspaceName);
  }

  public void setBackSelectNode(String workspaceName, String uri) throws Exception {
    String lastWorkspaceName = setTargetWorkspaceProperties(workspaceName);
    setSelectNode(uri, true);
    setLastWorkspace(lastWorkspaceName);
  }

  public void setSelectRootNode() throws Exception {
    setSelectNode(getCurrentDriveWorkspace(), getRootPath());
  }

  public void setSelectNode(String uri) throws Exception {
    setSelectNode(uri, false);
  }

  private boolean checkTargetForSymlink(String uri) throws Exception {
    Node testedNode;
    NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
    try {
      testedNode = (Node) nodeFinder.getItem(this.getSession(), uri, true);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the node at " + uri);
      }
      UIApplication uiApp = this.getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIJCRExplorer.msg.target-path-not-found",
                                              null,
                                              ApplicationMessage.WARNING));
      return false;
    }
    if (testedNode.isNodeType(Utils.EXO_RESTORELOCATION)) {
      UIApplication uiApp = this.getAncestorOfType(UIApplication.class);
      uiApp.addMessage(new ApplicationMessage("UIJCRExplorer.msg.target-path-not-found",
                                              null,
                                              ApplicationMessage.WARNING));
      return false;
    }
    return true;
  }

  public void setSelectNode(String uri, boolean back) throws Exception {
    Node currentNode = null;
    if(uri == null || uri.length() == 0) uri = "/";
    String previousPath = currentPath_;
    if (checkTargetForSymlink(uri)) {
      try {
        setCurrentPath(uri);
        currentNode = getCurrentNode();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Cannot find the node at " + uri, e);
        }
        setCurrentPath(LinkUtils.getParentPath(currentPath_));
        currentNode = getCurrentNode();
      }
    } else {
      currentNode = getCurrentNode();
    }
    if(currentNode.hasProperty(Utils.EXO_LANGUAGE)) {
      setLanguage(currentNode.getProperty(Utils.EXO_LANGUAGE).getValue().getString());
    }

    // Store previous node path to history for backing
    if(previousPath != null && !currentPath_.equals(previousPath) && !back) {
      // If previous node path has paginator, store last page index to history
      try{
        if(this.hasPaginator(previousPath, lastWorkspaceName_)){
          UIPageIterator pageIterator = this.findComponentById(UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID);
          if (pageIterator != null) {
            record(previousPath, lastWorkspaceName_, pageIterator.getCurrentPage());
          }
        }else{
          record(previousPath, lastWorkspaceName_);
        }
      }catch(PathNotFoundException e){
        LOG.info("This node " + previousPath +" is no longer accessible ");
      }
    }
  }

  public List<Node> getChildrenList(String path, boolean isReferences) throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    Node node = (Node) ItemLinkAware.newInstance(getWorkspaceName(), path, getNodeByPath(path, getSession()));
    NodeIterator childrenIterator = node.getNodes();
    List<Node> childrenList  = new ArrayList<Node>() ;
    NodeType nodeType = node.getPrimaryNodeType();
    NodeType[] superTypes = nodeType.getSupertypes();
    boolean isFolder = false ;
    for(NodeType superType : superTypes) {
      if(superType.getName().equals(Utils.NT_FOLDER) || superType.getName().equals(Utils.NT_UNSTRUCTURED)) {
        isFolder = true ;
      }
    }
    if(!preferences_.isJcrEnable() &&
        templateService.isManagedNodeType(nodeType.getName()) && !isFolder) {
      return childrenList ;
    }
    if(!preferences_.isShowNonDocumentType()) {
      List<String> documentTypes = templateService.getDocumentTemplates() ;
      while(childrenIterator.hasNext()){
        Node child = (Node)childrenIterator.next() ;
        if(PermissionUtil.canRead(child)) {
          NodeType type = child.getPrimaryNodeType() ;
          String typeName = type.getName();
          String primaryTypeName = typeName;

          if(typeName.equals(Utils.EXO_SYMLINK)) {
            primaryTypeName = child.getProperty(Utils.EXO_PRIMARYTYPE).getString();
          }
          if(child.isNodeType(Utils.NT_UNSTRUCTURED) || child.isNodeType(Utils.NT_FOLDER)) {
            childrenList.add(child) ;
          } else if(typeName.equals(Utils.EXO_SYMLINK) &&
              documentTypes.contains(primaryTypeName)) {
            childrenList.add(child);
          } else if(documentTypes.contains(typeName)) {
            childrenList.add(child) ;
          }
        }
      }
    } else {
      while(childrenIterator.hasNext()) {
        Node child = (Node)childrenIterator.next() ;
        if(PermissionUtil.canRead(child))  childrenList.add(child) ;
      }
    }
    List<Node> childList = new ArrayList<Node>() ;
    if(!preferences_.isShowHiddenNode()) {
      for(Node child : childrenList) {
        Node realChild = child instanceof NodeLinkAware ? ((NodeLinkAware) child).getRealNode() : child;
        if(PermissionUtil.canRead(child) && !realChild.isNodeType(Utils.EXO_HIDDENABLE)) {
          childList.add(child) ;
        }
      }
    } else {
      childList = childrenList ;
    }
    sort(childList);
    return childList ;
  }

  private void sort(List<Node> childrenList) {
    if (NodetypeConstant.SORT_BY_NODENAME.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new NodeTitleComparator(preferences_.getOrder())) ;
    } else if (NodetypeConstant.SORT_BY_NODETYPE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new TypeNodeComparator(preferences_.getOrder())) ;
    } else if (NodetypeConstant.SORT_BY_NODESIZE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new NodeSizeComparator(preferences_.getOrder())) ;
    } else if (NodetypeConstant.SORT_BY_VERSIONABLE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new StringComparator(preferences_.getOrder(), NodetypeConstant.SORT_BY_VERSIONABLE));
    } else if (NodetypeConstant.SORT_BY_AUDITING.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new StringComparator(preferences_.getOrder(), NodetypeConstant.SORT_BY_AUDITING));
    } else if (NodetypeConstant.SORT_BY_CREATED_DATE.equals(preferences_.getSortType())) {
        Collections.sort(childrenList, new PropertyValueComparator(Utils.EXO_CREATED_DATE, preferences_.getOrder()));
    } else if (NodetypeConstant.SORT_BY_MODIFIED_DATE.equals(preferences_.getSortType())) {
        Collections.sort(childrenList, 
                         new PropertyValueComparator(NodetypeConstant.EXO_LAST_MODIFIED_DATE, preferences_.getOrder()));
    } else if (NodetypeConstant.SORT_BY_DATE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new DateComparator(preferences_.getOrder()));
    } else {
      Collections.sort(childrenList, new PropertyValueComparator(preferences_.getSortType(), preferences_.getOrder()));
    }
  }

  public boolean isReferenceableNode(Node node) throws Exception {
    return node.isNodeType(Utils.MIX_REFERENCEABLE) ;
  }

  public boolean isPreferenceNode(Node node) {
    try {
      return (getCurrentNode().hasNode(node.getName())) ? false : true ;
    } catch(Exception e) {
      return false ;
    }
  }

  public Node getNodeByPath(String nodePath, Session session) throws Exception {
    return getNodeByPath(nodePath, session, true);
  }

  public Node getNodeByPath(String nodePath, Session session, boolean giveTarget) throws Exception {
    return getNodeByPath(nodePath.trim(), session, giveTarget, true);
  }

  private Node getNodeByPath(String nodePath, Session session, boolean giveTarget, boolean firstTime) throws Exception {
    NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
    Node node;
    try {
      node = (Node) nodeFinder.getItem(session, nodePath, giveTarget);
    } catch (Exception e) {
      if (nodePath.equals(currentPath_) && !nodePath.equals(currentRootPath_)) {
        setCurrentPath(LinkUtils.getParentPath(currentPath_));
        return getNodeByPath(currentPath_, session, giveTarget, false);
      }
      try {
        node = (Node) nodeFinder.getItem(session, nodePath, !giveTarget);
        return node;
      } catch (Exception e3) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e3.getMessage());
        }
      }
      if (firstTime) {
        String workspace = session.getWorkspace().getName();
        if (LOG.isWarnEnabled()) {
          LOG.warn("The node cannot be found at " + nodePath
            + " into the workspace " + workspace);
        }
      }
      throw e;
    }
    if (!firstTime) {
      refreshExplorer(node);
    }
    return node;
  }

  public void setTagPath(String tagPath) { tagPath_ = tagPath ; }

  public String getTagPath() { return tagPath_ ; }

  public List<Node> getDocumentByTag()throws Exception {
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    List<String> documentsType = templateService.getDocumentTemplates() ;
    List<Node> documentsOnTag = new ArrayList<Node>() ;
    WebuiRequestContext ctx = WebuiRequestContext.getCurrentInstance();
    SessionProvider sessionProvider = (ctx.getRemoteUser() == null) ?
                                                                     WCMCoreUtils.createAnonimProvider() :
                                                                       WCMCoreUtils.getUserSessionProvider();
                                                                     for (Node node : newFolksonomyService.getAllDocumentsByTag(tagPath_,
                                                                                                                                getRepository().getConfiguration().getDefaultWorkspaceName(),
                                                                                                                                sessionProvider)) {
                                                                       if (documentsType.contains(node.getPrimaryNodeType().getName())
                                                                           && PermissionUtil.canRead(node)) {
                                                                         documentsOnTag.add(node);
                                                                       }
                                                                     }
                                                                     return documentsOnTag ;
  }

  public void setIsViewTag(boolean isViewTag) { isViewTag_ = isViewTag ; }

  public boolean isViewTag() { return isViewTag_ ; }

  public PortletPreferences getPortletPreferences() { return pref_ ; }

  public boolean isReadAuthorized(ExtendedNode node) throws RepositoryException {
    try {
      node.checkPermission(PermissionType.READ);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }

  public static Cookie getCookieByCookieName(String cookieName, Cookie[] cookies) {
    String userId = Util.getPortalRequestContext().getRemoteUser();
    cookieName += userId;
    for(int loopIndex = 0; loopIndex < cookies.length; loopIndex++) {
      Cookie cookie1 = cookies[loopIndex];
      if (cookie1.getName().equals(cookieName)) return cookie1;
    }
    return null;
  }

  public Preference getPreference() {
    if (preferencesSaved_) {
      if (preferences_ != null && !this.getAncestorOfType(UIJCRExplorerPortlet.class).isShowSideBar())
        preferences_.setShowSideBar(false);
      return preferences_;
    }
    HttpServletRequest request = Util.getPortalRequestContext().getRequest();
    Cookie[] cookies = request.getCookies();
    Cookie getCookieForUser;
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_ENABLESTRUCTURE, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setJcrEnable(true);
      else
        preferences_.setJcrEnable(false);
    }
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_SHOWSIDEBAR, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setShowSideBar(true);
      else
        preferences_.setShowSideBar(false);
    }
    if (preferences_ != null && !this.getAncestorOfType(UIJCRExplorerPortlet.class).isShowSideBar())
      preferences_.setShowSideBar(false);
    getCookieForUser = getCookieByCookieName(Preference.SHOW_NON_DOCUMENTTYPE, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setShowNonDocumentType(true);
      else
        preferences_.setShowNonDocumentType(false);
    }
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_SHOWREFDOCUMENTS, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setShowPreferenceDocuments(true);
      else
        preferences_.setShowPreferenceDocuments(false);
    }
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_SHOW_HIDDEN_NODE, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setShowHiddenNode(true);
      else
        preferences_.setShowHiddenNode(false);
    }
    getCookieForUser = getCookieByCookieName(Preference.ENABLE_DRAG_AND_DROP, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) {
      if (getCookieForUser.getValue().equals("true"))
        preferences_.setEnableDragAndDrop(true);
      else
        preferences_.setEnableDragAndDrop(false);
    }
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_QUERY_TYPE, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) preferences_.setQueryType(getCookieForUser.getValue());
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_SORT_BY, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) preferences_.setSortType(getCookieForUser.getValue());
    getCookieForUser = getCookieByCookieName(Preference.PREFERENCE_ORDER_BY, cookies);
    if ((getCookieForUser != null) && (preferences_ != null)) preferences_.setOrder(getCookieForUser.getValue());
    getCookieForUser = getCookieByCookieName(Preference.NODES_PER_PAGE, cookies);
    if ((getCookieForUser != null) && (preferences_ != null))
      preferences_.setNodesPerPage(Integer.parseInt(getCookieForUser.getValue()));

    return preferences_;
  }
  public void setPreferences(Preference preference) {this.preferences_ = preference; }

  public void closeEditingFile() throws Exception {

    UIPopupContainer uiPopupContainer = this.getChild(UIPopupContainer.class);
    UIPopupWindow uiPopup = uiPopupContainer.getChild(UIPopupWindow.class);

    UIWorkingArea uiWorkingArea = this.getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);

    //check if edit with popup
    UIComponent uiComp = uiPopup.getUIComponent();
    if (uiComp instanceof UIDocumentFormController && ((UIDocumentFormController)uiComp).isRendered()) {
      uiPopupContainer.deActivate();
      this.refreshExplorer();
      return;
    }

    //check if edit without popup
    if (uiDocumentWorkspace.isRendered()) {
      UIDocumentFormController controller = uiDocumentWorkspace.getChild(UIDocumentFormController.class);
      if (controller != null) {
        uiDocumentWorkspace.removeChild(UIDocumentFormController.class).deActivate();
        uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class);
        this.refreshExplorer();
      }
    }
  }

  public static class HistoryEntry {
    private final String workspace;
    private final String path;

    private HistoryEntry(String workspace, String path) {
      this.workspace = workspace;
      this.path = path;
    }

    public String getWorkspace() {
      return workspace;
    }

    public String getPath() {
      return path;
    }
  }

}
