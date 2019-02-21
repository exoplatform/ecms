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
package org.exoplatform.ecm.webui.tree.selectone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector;
import org.exoplatform.ecm.webui.tree.UINodeTreeBuilder;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 18, 2006
 * 2:12:26 PM
 */
@ComponentConfigs(
    {
      @ComponentConfig(
          template = "classpath:groovy/ecm/webui/UIOneNodePathSelector.gtmpl"
      ),
      @ComponentConfig(
          type = UIBreadcumbs.class, id = "BreadcumbCategoriesOne",
          template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
          events = @EventConfig(listeners = UIOneNodePathSelector.SelectPathActionListener.class)
      )
    }
)

public class UIOneNodePathSelector extends UIBaseNodeTreeSelector {

  private String[] acceptedNodeTypesInTree = {};
  private String[] acceptedNodeTypesInPathPanel = {};
  private String[] acceptedMimeTypes = {};

  private String[] exceptedNodeTypesInPathPanel = {};
  private String[] exceptedNodeTypesInTree = {};

  private String[] defaultExceptedNodeTypes = {"exo:symlink"};

  private String repositoryName = null;
  private String workspaceName = null;
  private String rootTreePath = null;
  private boolean isDisable = false;
  private boolean allowPublish = false;
  private boolean alreadyChangePath = false;
  private boolean showOnlyFolderNodeInTree = true;

  private String rootTaxonomyName = null;

  public UIOneNodePathSelector() throws Exception {
    addChild(UIBreadcumbs.class, "BreadcumbCategoriesOne", "BreadcumbCategoriesOne");
    addChild(UIWorkspaceList.class, null, null);
    addChild(UINodeTreeBuilder.class, null, UINodeTreeBuilder.class.getSimpleName()+hashCode());
    addChild(UISelectPathPanel.class, null, null).setShowTrashHomeNode(false);
  }

  public String getRootTaxonomyName() { return rootTaxonomyName; }

  public void setRootTaxonomyName(String rootTaxonomyName) {
    this.rootTaxonomyName = rootTaxonomyName;
  }

  public void init(SessionProvider sessionProvider) throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    TemplateService templateService  = getApplicationComponent(TemplateService.class);
    List<String> templates = templateService.getDocumentTemplates();
    Node rootNode;
    if (rootTreePath.trim().equals("/")) {
      rootNode = sessionProvider.getSession(workspaceName, manageableRepository).getRootNode();
    } else {
      NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
      if (rootTreePath.indexOf("${userId}") > -1) {
        String userId = Util.getPortalRequestContext().getRemoteUser();
        String rootTreeOfSpecialDriver = 
          org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(rootTreePath , userId);
        rootTreePath = rootTreeOfSpecialDriver;
      }
      rootNode = (Node) nodeFinder.getItem(workspaceName, rootTreePath);
    }

    UIWorkspaceList uiWorkspaceList = getChild(UIWorkspaceList.class);
    uiWorkspaceList.setWorkspaceList();
    uiWorkspaceList.setIsDisable(workspaceName, isDisable);
    UINodeTreeBuilder builder = getChild(UINodeTreeBuilder.class);
    builder.setAllowPublish(allowPublish, publicationService, templates);
    if (this.showOnlyFolderNodeInTree) {
      List<String> nodeTypesInTree = new ArrayList<String>(Arrays.asList(acceptedNodeTypesInTree));
      if (!nodeTypesInTree.contains(Utils.NT_UNSTRUCTURED))
        nodeTypesInTree.add(Utils.NT_UNSTRUCTURED);
      if (!nodeTypesInTree.contains(Utils.NT_FOLDER))
        nodeTypesInTree.add(Utils.NT_FOLDER);
      if (!nodeTypesInTree.contains(Utils.EXO_TAXONOMY))
        nodeTypesInTree.add(Utils.EXO_TAXONOMY);
      this.acceptedNodeTypesInTree = nodeTypesInTree.toArray(new String[]{});
    }
    builder.setAcceptedNodeTypes(acceptedNodeTypesInTree);
    builder.setDefaultExceptedNodeTypes(defaultExceptedNodeTypes);
    builder.setRootTreeNode(rootNode);

    UISelectPathPanel selectPathPanel = getChild(UISelectPathPanel.class);
    selectPathPanel.setAllowPublish(allowPublish, publicationService, templates);
    selectPathPanel.setAcceptedNodeTypes(acceptedNodeTypesInPathPanel);
    selectPathPanel.setAcceptedMimeTypes(acceptedMimeTypes);
    selectPathPanel.setExceptedNodeTypes(exceptedNodeTypesInPathPanel);
    selectPathPanel.setDefaultExceptedNodeTypes(defaultExceptedNodeTypes);
    selectPathPanel.updateGrid();
  }

  public boolean isAllowPublish() {
    return allowPublish;
  }

  public void setAllowPublish(boolean allowPublish) {
    this.allowPublish = allowPublish;
  }

  public void setRootNodeLocation(String repository, String workspace, String rootPath) throws Exception {
    this.repositoryName = repository;
    this.workspaceName = workspace;
    this.rootTreePath = rootPath;
  }

  public void setIsDisable(String wsName, boolean isDisable) {
    setWorkspaceName(wsName);
    this.isDisable = isDisable;
  }

  public boolean isDisable() { return isDisable; }

  public void setIsShowSystem(boolean isShowSystem) {
    getChild(UIWorkspaceList.class).setIsShowSystem(isShowSystem);
  }

  public void setShowRootPathSelect(boolean isRendered) {
    UIWorkspaceList uiWorkspaceList = getChild(UIWorkspaceList.class);
    uiWorkspaceList.setShowRootPathSelect(isRendered);
  }

  public String[] getAcceptedNodeTypesInTree() {
    return acceptedNodeTypesInTree;
  }

  public void setAcceptedNodeTypesInTree(String[] acceptedNodeTypesInTree) {
    this.acceptedNodeTypesInTree = acceptedNodeTypesInTree;
  }

  public String[] getAcceptedNodeTypesInPathPanel() {
    return acceptedNodeTypesInPathPanel;
  }

  public void setAcceptedNodeTypesInPathPanel(String[] acceptedNodeTypesInPathPanel) {
    this.acceptedNodeTypesInPathPanel = acceptedNodeTypesInPathPanel;
  }

  public String[] getExceptedNodeTypesInTree() {
    return exceptedNodeTypesInTree;
  }

  public void setExceptedNodeTypesInTree(String[] exceptedNodeTypesInTree) {
    this.exceptedNodeTypesInTree = exceptedNodeTypesInTree;
  }

  public String[] getExceptedNodeTypesInPathPanel() {
    return exceptedNodeTypesInPathPanel;
  }

  public void setExceptedNodeTypesInPathPanel(String[] exceptedNodeTypesInPathPanel) {
    this.exceptedNodeTypesInPathPanel = exceptedNodeTypesInPathPanel;
  }

  public String[] getDefaultExceptedNodeTypes() { return defaultExceptedNodeTypes; }

  public String[] getAcceptedMimeTypes() { return acceptedMimeTypes; }

  public void setAcceptedMimeTypes(String[] acceptedMimeTypes) { this.acceptedMimeTypes = acceptedMimeTypes; }

  public boolean isShowOnlyFolderNodeInTree() { return showOnlyFolderNodeInTree; }

  public void setShowOnlyFolderNodeInTree(boolean value) {
    showOnlyFolderNodeInTree = value;
  }

  public String getRepositoryName() { return repositoryName; }
  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getWorkspaceName() { return workspaceName; }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  public String getRootTreePath() { return rootTreePath; }

  public void setRootTreePath(String rootTreePath) { this.rootTreePath = rootTreePath;
  }

  public void onChange(final Node currentNode, Object context) throws Exception {
    UISelectPathPanel selectPathPanel = getChild(UISelectPathPanel.class);
    selectPathPanel.setParentNode(currentNode);
    selectPathPanel.updateGrid();
    UIBreadcumbs uiBreadcumbs = getChild(UIBreadcumbs.class);
    String pathName = currentNode.getName();
    String pathTitle = pathName;
    if (currentNode.hasProperty("exo:title")){
      pathTitle = currentNode.getProperty("exo:title").getString();
    } 
    NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
    Session session;
    if(currentNode.getSession().getUserID().equals(IdentityConstants.SYSTEM)) {
      // use system session to fetch node if current session is a system session
      session = WCMCoreUtils.getSystemSessionProvider().getSession(workspaceName, WCMCoreUtils.getRepository());
    } else {
      session = WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, WCMCoreUtils.getRepository());
    }
    Node rootNode = (Node) nodeFinder.getItem(session, rootTreePath);


    if (currentNode.equals(rootNode)) {
      pathName = "";
    }
    UIBreadcumbs.LocalPath localPath = new UIBreadcumbs.LocalPath(pathName, pathTitle);
    List<LocalPath> listLocalPath = uiBreadcumbs.getPath();
    StringBuilder buffer = new StringBuilder(1024);
    for(LocalPath iterLocalPath: listLocalPath) {
      buffer.append("/").append(iterLocalPath.getId());
    }
    if (!alreadyChangePath) {
      String path = buffer.toString();
      if (path.startsWith("//"))
        path = path.substring(1);
      if (!path.startsWith(rootTreePath)) {
        StringBuffer buf = new StringBuffer();
        buf.append(rootTreePath).append(path);
        path = buf.toString();
      }
      if (path.endsWith("/"))
        path = path.substring(0, path.length() - 1);
      if (path.length() == 0)
        path = "/";
      Node currentBreadcumbsNode = getNodeByVirtualPath(path, session);
      if (currentNode.equals(rootNode)
          || ((!currentBreadcumbsNode.equals(rootNode) && currentBreadcumbsNode.getParent()
                                                                               .equals(currentNode)))) {
        if (listLocalPath != null && listLocalPath.size() > 0) {
          listLocalPath.remove(listLocalPath.size() - 1);
        }
      } else {
        listLocalPath.add(localPath);
      }
    }
    alreadyChangePath = false;
    uiBreadcumbs.setPath(listLocalPath);
  }

  private Node getNodeByVirtualPath(String pathLinkNode, Session session) throws Exception{
    NodeFinder nodeFinder_ = getApplicationComponent(NodeFinder.class);
    Item item = nodeFinder_.getItem(session, pathLinkNode);
    return (Node)item;
  }

  private void changeNode(String stringPath, Object context) throws Exception {
    UINodeTreeBuilder builder = getChild(UINodeTreeBuilder.class);
    builder.changeNode(stringPath, context);
  }

  public void changeGroup(String groupId, Object context) throws Exception {
    StringBuffer stringPath = new StringBuffer(rootTreePath);
    if (!rootTreePath.equals("/")) {
      stringPath.append("/");
    }
    UIBreadcumbs uiBreadcumb = getChild(UIBreadcumbs.class);
    if (groupId == null) groupId = "";
    List<LocalPath> listLocalPath = uiBreadcumb.getPath();
    if (listLocalPath == null || listLocalPath.size() == 0) return;
    List<String> listLocalPathString = new ArrayList<String>();
    for (LocalPath localPath : listLocalPath) {
      listLocalPathString.add(localPath.getId().trim());
    }
    if (listLocalPathString.contains(groupId)) {
      int index = listLocalPathString.indexOf(groupId);
      alreadyChangePath = false;
      if (index == listLocalPathString.size() - 1) return;
      for (int i = listLocalPathString.size() - 1; i > index; i--) {
        listLocalPathString.remove(i);
        listLocalPath.remove(i);
      }
      alreadyChangePath = true;
      uiBreadcumb.setPath(listLocalPath);
      for (int i = 0; i < listLocalPathString.size(); i++) {
        String pathName = listLocalPathString.get(i);
        if (pathName != null && pathName.trim().length() != 0) {
          stringPath.append(pathName.trim());
          if (i < listLocalPathString.size() - 1) stringPath.append("/");
        }
      }
      changeNode(stringPath.toString(), context);
    }
  }

  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource();
      UIOneNodePathSelector uiOneNodePathSelector = uiBreadcumbs.getParent();
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID);
      uiBreadcumbs.setSelectPath(objectId);
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId();
      uiOneNodePathSelector.changeGroup(selectGroupId, event.getRequestContext());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiOneNodePathSelector);
    }
  }
}
