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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.TypeNodeComparator;
import org.exoplatform.ecm.jcr.model.ClipboardCommand;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.comparator.NodeNameComparator;
import org.exoplatform.ecm.webui.comparator.PropertyValueComparator;
import org.exoplatform.ecm.webui.comparator.StringComparator;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
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
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupContainer;
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
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIJCRExplorer");
	
  private LinkedList<ClipboardCommand> clipboards_ = new LinkedList<ClipboardCommand>() ;
  private LinkedList<String> nodesHistory_ = new LinkedList<String>() ;
  private LinkedList<String> wsHistory_ = new LinkedList<String>();
  private PortletPreferences pref_ ;
  private Preference preferences_;
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
  
  private int tagScope;

  private List<String> checkedSupportType = new ArrayList<String>();
  private Set<String> allItemFilterMap = new HashSet<String>();
  private Set<String> allItemByTypeFilterMap = new HashSet<String>();
  
  public Set<String> getAllItemFilterMap() { return allItemFilterMap; }
  public Set<String> getAllItemByTypeFilterMap() { return allItemByTypeFilterMap; }
  
  public int getTagScope() { return tagScope; }
  public void setTagScope(int scope) { tagScope = scope; }
      
  public boolean isFilterSave() {
    return isFilterSave_;
  }
  
  public void setFilterSave(boolean isFilterSave) {
    isFilterSave_ = isFilterSave;
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
  public void setCurrentPath(String currentPath) { currentPath_ = currentPath ; }
  
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
  
  @Deprecated
  public void setBackNodePath(String previousPath) throws Exception {
    setBackNodePath(null, previousPath);
  }
  
  /**
   * Tells to go back to the given location 
   */
  public void setBackNodePath(String previousWorkspaceName, String previousPath) throws Exception {
    setBackSelectNode(previousWorkspaceName, previousPath);
    refreshExplorer();
  }
  
  public void setDriveData(DriveData driveData) { driveData_ = driveData ; }
  public DriveData getDriveData() { return driveData_ ; }

  public void setLanguage(String language) { language_ = language ; }
  public String getLanguage() { return language_ ; }

  public LinkedList<String> getNodesHistory() { return nodesHistory_ ; }
  @Deprecated
  public void setNodesHistory(LinkedList<String> h) {nodesHistory_ = h;}
  
  public LinkedList<String> getWorkspacesHistory() { return wsHistory_; }
  @Deprecated
  public void setWorkspaceHistory(LinkedList<String> wsHistory) { wsHistory_ =  wsHistory; }
  
  public Collection<HistoryEntry> getHistory() { return addressPath_.values() ; }
  @Deprecated
  public Set<String> getAddressPath() { return addressPath_.keySet() ; }
  @Deprecated
  public void setAddressPath(Set<String> s) {/*addressPath_ = s;*/} ;

  public SessionProvider getSessionProvider() { return SessionProviderFactory.createSessionProvider(); }  

  public SessionProvider getSystemProvider() { return SessionProviderFactory.createSystemProvider(); }  

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
        dmsConfiguration.getConfig(currentDriveRepositoryName_);
      String workspace =  dmsRepoConfig.getSystemWorkspace();
      jcrTemplateResourceResolver_ = new JCRResourceResolver(currentDriveRepositoryName_, workspace, "exo:templateFile") ;
    } catch(Exception e) {
      LOG.error("Cannot instantiate the JCRResourceResolver", e);
    }         
  }
  
  /**
   * Sets the repository of the current drive
   */
  public void setRepositoryName(String repositoryName) { currentDriveRepositoryName_ = repositoryName ; }
  /**
   * @return the repository of the current drive
   */
  public String getRepositoryName() { return currentDriveRepositoryName_ ; }
  
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
      LOG.warn("The workspace of the current node cannot be found, the workspace of the drive will be used", e);
    } 
    return getCurrentDriveWorkspace();
  }

  public ManageableRepository getRepository() throws Exception{         
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class) ;      
    return repositoryService.getRepository(currentDriveRepositoryName_);
  }

  public Session getSessionByWorkspace(String wsName) throws Exception{    
    if(wsName == null ) return getSession() ;                      
    return getSessionProvider().getSession(wsName,getRepository()) ;
  }
  
  public boolean isSystemWorkspace() throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    String systemWS = 
      repositoryService.getRepository(getRepositoryName()).getConfiguration().getSystemWorkspaceName() ;
    if(getCurrentWorkspace().equals(systemWS)) return true ;
    return false ;
  }

  public void refreshExplorer() throws Exception {
    refreshExplorer(null);
  }
  
  public void setPathToAddressBar(String path) throws Exception {
    findFirstComponentOfType(UIAddressBar.class).getUIStringInput(
                                          UIAddressBar.FIELD_ADDRESS).setValue(filterPath(path)) ;
  } 
  
  private void refreshExplorer(Node currentNode) throws Exception { 
    try {
      Node nodeGet = currentNode == null ? getCurrentNode() : currentNode;
      if(nodeGet.hasProperty(Utils.EXO_LANGUAGE)) {
        setLanguage(nodeGet.getProperty(Utils.EXO_LANGUAGE).getValue().getString());
      }
    } catch(PathNotFoundException path) {
      LOG.error("The node cannot be found ", path);
      setCurrentPath(currentRootPath_);
    }
    findFirstComponentOfType(UIAddressBar.class).getUIStringInput(UIAddressBar.FIELD_ADDRESS).
        setValue(filterPath(currentPath_)) ;
    UIWorkingArea uiWorkingArea = getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    if(uiDocumentWorkspace.isRendered()) {
      UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class) ;
      if(isShowViewFile()) {
        UIDocumentWithTree uiDocumentWithTree = uiDocumentContainer.getChildById("UIDocumentWithTree");
        uiDocumentWithTree.updatePageListData();
        uiDocumentContainer.setRenderedChild("UIDocumentWithTree");
      } else {
        UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
        uiDocumentInfo.updatePageListData();
        uiDocumentContainer.setRenderedChild("UIDocumentInfo") ;
      }
      uiDocumentWorkspace.setRenderedChild(UIDocumentContainer.class) ;
    }
    if(preferences_.isShowSideBar()) {
      UITreeExplorer treeExplorer = findFirstComponentOfType(UITreeExplorer.class);
      treeExplorer.buildTree();
    }
    UIPopupContainer popupAction = getChild(UIPopupContainer.class);
    popupAction.deActivate();
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
    if (node.isLocked()) {
      String lockToken = LockUtil.getLockToken(node);
      if(lockToken != null) {
        node.getSession().addLockToken(lockToken);
      }
    }
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
      LOG.error("Primary item not found for " + getCurrentNode().getPath());      
      return getCurrentNode() ;
    } catch(Exception e) { 
      LOG.error("The node cannot be seen", e);      
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
        LOG.error("The property '" + name + "' cannot be found ", e);        
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
    UIAddressBar uiAddressBar = findFirstComponentOfType(UIAddressBar.class) ;
    uiAddressBar.getUIStringInput(UIAddressBar.FIELD_ADDRESS).setValue(
        Text.unescapeIllegalJcrChars(filterPath(currentPath_))) ;
    event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBar) ;
    UIWorkingArea uiWorkingArea = getChild(UIWorkingArea.class) ;
    UIActionBar uiActionBar = findFirstComponentOfType(UIActionBar.class) ;
    if(preferences_.isShowSideBar()) {
      findFirstComponentOfType(UITreeExplorer.class).buildTree();
    }
    UIDocumentWorkspace uiDocWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    if(uiDocWorkspace.isRendered()) {
      UIDocumentContainer uiDocumentContainer = uiDocWorkspace.getChild(UIDocumentContainer.class) ;
      if(isShowViewFile()) {
        UIDocumentWithTree uiDocumentWithTree = uiDocumentContainer.getChildById("UIDocumentWithTree");
        uiDocumentWithTree.updatePageListData();
        uiDocumentContainer.setRenderedChild("UIDocumentWithTree");
      } else {
        UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
        uiDocumentInfo.updatePageListData();
        uiDocumentContainer.setRenderedChild("UIDocumentInfo") ;
      }
      uiDocWorkspace.setRenderedChild(UIDocumentContainer.class) ;
    }
    event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingArea);
    event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar);
    if(!isHidePopup_) {
      UIPopupContainer popupAction = getChild(UIPopupContainer.class) ;
      if(popupAction.isRendered()) {
        popupAction.deActivate();
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
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
    if(isFolder && templateService.getDocumentTemplates(getRepositoryName()).contains(nodeType.getName())) {
      return true ;
    }
    return false;
  }

  public void cancelAction() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
    UIPopupContainer popupAction = getChild(UIPopupContainer.class) ;
    popupAction.deActivate() ;
    context.addUIComponentToUpdateByAjax(popupAction) ;
  }

  public void record(String str, String ws) {
    //Uncomment this line if you have problem with the history 
    //LOG.info("record(" + str + ", " + ws + ")", new Exception());
    nodesHistory_.add(str);
    wsHistory_.add(ws);
    addressPath_.put(str, new HistoryEntry(ws, str));
    
    UIWorkingArea uiWorkingArea = getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    UIDocumentContainer uiDocumentContainer = uiDocWorkspace.getChild(UIDocumentContainer.class) ;
  }

  public void clearNodeHistory(String currentPath) {
    nodesHistory_.clear();
    wsHistory_.clear();
    addressPath_.clear();
    currentPath_ = currentPath;
  }
  
  public String rewind() { return nodesHistory_.removeLast() ; }

  public String previousWsName() { return wsHistory_.removeLast(); }
  
  @Deprecated
  public void setSelectNode(String uri, Session session) throws Exception {
    setSelectNode(session.getWorkspace().getName(), uri);
  }

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
  
  public void setSelectNode(String uri, boolean back) throws Exception {  
    Node currentNode;
    if(uri == null || uri.length() == 0) uri = "/";
    String previousPath = currentPath_;
    try {
      setCurrentPath(uri);
      currentNode = getCurrentNode();
    } catch (Exception e) {
      LOG.error("Cannot find the node at " + uri, e);
      setCurrentPath(LinkUtils.getParentPath(currentPath_));
      currentNode = getCurrentNode();
    }    
    if(currentNode.hasProperty(Utils.EXO_LANGUAGE)) {
      setLanguage(currentNode.getProperty(Utils.EXO_LANGUAGE).getValue().getString());
    }
    if(previousPath != null && !currentPath_.equals(previousPath) && !back) {
      record(previousPath, lastWorkspaceName_);
    }
  }
  
  public List<Node> getChildrenList(String path, boolean isReferences) throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    Node node = (Node) ItemLinkAware.newInstance(getSession(), path, getNodeByPath(path, getSession()));
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
        templateService.isManagedNodeType(nodeType.getName(), currentDriveRepositoryName_) && !isFolder) {
      return childrenList ;
    } 
    if(isReferenceableNode(getCurrentNode()) && isReferences) {
      ManageableRepository manageableRepository = repositoryService.getRepository(currentDriveRepositoryName_) ;
      SessionProvider sessionProvider = SessionProviderFactory.createSystemProvider();
      for(String workspace:manageableRepository.getWorkspaceNames()) {
        Session session = sessionProvider.getSession(workspace,manageableRepository) ;
        try {
          Node taxonomyNode = session.getNodeByUUID(getCurrentNode().getUUID()) ;
          PropertyIterator categoriesIter = taxonomyNode.getReferences() ;
          while(categoriesIter.hasNext()) {
            Property exoCategoryProp = categoriesIter.nextProperty();
            Node refNode = exoCategoryProp.getParent() ;
            childrenList.add(refNode) ;            
          }
        } catch(Exception e) {
          // do nothing
        }
      }
    }
    if(!preferences_.isShowNonDocumentType()) {
      List<String> documentTypes = templateService.getDocumentTemplates(currentDriveRepositoryName_) ;      
      while(childrenIterator.hasNext()){
        Node child = (Node)childrenIterator.next() ;
        if(PermissionUtil.canRead(child)) {
          NodeType type = child.getPrimaryNodeType() ;
          String typeName = type.getName();
          String primaryTypeName = typeName;
          
          if(typeName.equals(Utils.EXO_SYMLINK)) { 
            primaryTypeName = child.getProperty(Utils.EXO_PRIMARYTYPE).getString();
          }
          if(Utils.NT_UNSTRUCTURED.equals(primaryTypeName) || Utils.NT_FOLDER.equals(primaryTypeName)) {
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
    if (Preference.SORT_BY_NODENAME.equals(preferences_.getSortType())) {      
      Collections.sort(childrenList, new NodeNameComparator(preferences_.getOrder())) ;
    } else if (Preference.SORT_BY_NODETYPE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new TypeNodeComparator(preferences_.getOrder())) ;
    } else if (Preference.SORT_BY_VERSIONABLE.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new StringComparator(preferences_.getOrder(), Preference.SORT_BY_VERSIONABLE));
    } else if (Preference.SORT_BY_AUDITING.equals(preferences_.getSortType())) {
      Collections.sort(childrenList, new StringComparator(preferences_.getOrder(), Preference.SORT_BY_AUDITING));
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
      }
      if (firstTime) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
        JCRExceptionManager.process(uiApp, e);
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
        context.addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        String workspace = null;
        try {
          workspace = session.getWorkspace().getName();
        } catch (Exception e2) {
          // do nothing
        }
        LOG.warn("The node cannot be found at " + nodePath + (workspace == null ? "" : " into the workspace " + workspace));        
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
    List<String> documentsType = templateService.getDocumentTemplates(getRepositoryName()) ;
    List<Node> documentsOnTag = new ArrayList<Node>() ;
    WebuiRequestContext ctx = WebuiRequestContext.getCurrentInstance();
    SessionProvider sessionProvider = (ctx.getRemoteUser() == null) ?
    																	SessionProviderFactory.createAnonimProvider() :
    																	SessionProviderFactory.createSessionProvider();
    for(Node node : newFolksonomyService.getAllDocumentsByTag(tagPath_, getRepositoryName(), 
    																													getRepository().getConfiguration().getDefaultWorkspaceName(), 
    																													sessionProvider)) {
      if(documentsType.contains(node.getPrimaryNodeType().getName()) &&
      	 PermissionUtil.canRead(node)) {
        documentsOnTag.add(node) ;
      }
    }
    return documentsOnTag ;
  }
      
  public void setIsViewTag(boolean isViewTag) { isViewTag_ = isViewTag ; }
  
  public boolean isViewTag() { return isViewTag_ ; }

  public LinkedList<ClipboardCommand> getAllClipBoard() { return clipboards_ ;}

  public PortletPreferences getPortletPreferences() { return pref_ ; }

  public boolean isReadAuthorized(ExtendedNode node) throws RepositoryException {
    try {
      node.checkPermission(PermissionType.READ);
      return true;
    } catch(AccessControlException e) {
      return false;
    }    
  }  
    
  public Preference getPreference() { return preferences_; }  
  public void setPreferences(Preference preference) {this.preferences_ = preference; } 
  
  @Deprecated
  public String getPreferencesPath() {
    String prefPath = driveData_.getHomePath() ;
    if (prefPath == null || prefPath.length() == 0 || prefPath.equals("/"))return "" ;
    return prefPath ;
  }

  @Deprecated
  public String getPreferencesWorkspace() {       
    String workspaceName = driveData_.getWorkspace() ;
    if(workspaceName == null || workspaceName.length() == 0) return "" ;
    return workspaceName ;
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