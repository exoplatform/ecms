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
import java.util.List;
import java.util.MissingResourceException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector;
import org.exoplatform.ecm.webui.tree.UITreeTaxonomyBuilder;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;
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
          template = "classpath:groovy/ecm/webui/UIOneTaxonomySelector.gtmpl"
      ),
      @ComponentConfig(
          type = UIBreadcumbs.class, id = "BreadcumbOneTaxonomy",
          template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
          events = @EventConfig(listeners = UIOneTaxonomySelector.SelectPathActionListener.class)
      )
    }
)

public class UIOneTaxonomySelector extends UIBaseNodeTreeSelector {

  private String[] acceptedNodeTypesInTree = {};
  private String[] acceptedNodeTypesInPathPanel = {};
  private String[] acceptedMimeTypes = {};

  private String[] exceptedNodeTypesInPathPanel = {};

  private String repositoryName = null;
  private String workspaceName = null;
  private String rootTreePath = null;
  private boolean isDisable = false;
  private boolean allowPublish = false;

  private boolean alreadyChangePath = false;

  private String rootTaxonomyName = null;

  private String[] defaultExceptedNodeTypes = {"exo:symlink"};

  public UIOneTaxonomySelector() throws Exception {
    addChild(UIBreadcumbs.class, "BreadcumbOneTaxonomy", "BreadcumbOneTaxonomy");
    addChild(UITreeTaxonomyList.class, null, null);
    addChild(UITreeTaxonomyBuilder.class, null, UITreeTaxonomyBuilder.class.getSimpleName()+hashCode());
    addChild(UISelectTaxonomyPanel.class, null, null);
  }

  public String getRootTaxonomyName() { return rootTaxonomyName; }

  public void setRootTaxonomyName(String rootTaxonomyName) {
    this.rootTaxonomyName = rootTaxonomyName;
  }

  Node getTaxoTreeNode(String taxoTreeName) throws RepositoryException {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    return taxonomyService.getTaxonomyTree(taxoTreeName);
  }

  public String[] getDefaultExceptedNodeTypes() { return defaultExceptedNodeTypes; }

  public void init(SessionProvider sessionProvider) throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    List<String> templates = templateService.getDocumentTemplates();
    Node rootNode;
    if (rootTreePath.trim().equals("/")) {
      rootNode = sessionProvider.getSession(workspaceName, manageableRepository)
          .getRootNode();
    } else {
      NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
      try {
        rootNode = (Node) nodeFinder.getItem(workspaceName, rootTreePath);
      } catch (PathNotFoundException pathNotFoundException) {
        rootNode = null;
      }
    }

    UITreeTaxonomyList uiTreeTaxonomyList = getChild(UITreeTaxonomyList.class);
    uiTreeTaxonomyList.setTaxonomyTreeList();
    UITreeTaxonomyBuilder builder = getChild(UITreeTaxonomyBuilder.class);
    builder.setAllowPublish(allowPublish, publicationService, templates);
    builder.setAcceptedNodeTypes(acceptedNodeTypesInTree);
    builder.setDefaultExceptedNodeTypes(defaultExceptedNodeTypes);
    if (rootNode != null) builder.setRootTreeNode(rootNode);
    UISelectTaxonomyPanel selectPathPanel = getChild(UISelectTaxonomyPanel.class);
    selectPathPanel.setAllowPublish(allowPublish, publicationService, templates);
    selectPathPanel.setAcceptedNodeTypes(acceptedNodeTypesInPathPanel);
    selectPathPanel.setAcceptedMimeTypes(acceptedMimeTypes);
    selectPathPanel.setExceptedNodeTypes(exceptedNodeTypesInPathPanel);
    selectPathPanel.setDefaultExceptedNodeTypes(defaultExceptedNodeTypes);
    selectPathPanel.updateGrid();
    String taxoTreeName = ((UIFormSelectBox)this.findComponentById(UITreeTaxonomyList.TAXONOMY_TREE)).getValue();
    Node taxoTreeNode = this.getTaxoTreeNode(taxoTreeName);
    this.setWorkspaceName(taxoTreeNode.getSession().getWorkspace().getName());
    this.setRootTaxonomyName(taxoTreeNode.getName());
    this.setRootTreePath(taxoTreeNode.getPath());
    UITreeTaxonomyBuilder uiTreeJCRExplorer = this.findFirstComponentOfType(UITreeTaxonomyBuilder.class);
    uiTreeJCRExplorer.setRootTreeNode(taxoTreeNode);
    uiTreeJCRExplorer.buildTree();
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
    getChild(UITreeTaxonomyList.class).setIsShowSystem(isShowSystem);
  }

  public void setShowRootPathSelect(boolean isRendered) {
    UITreeTaxonomyList uiTreeTaxonomyList = getChild(UITreeTaxonomyList.class);
    uiTreeTaxonomyList.setShowRootPathSelect(isRendered);
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

  public String[] getExceptedNodeTypesInPathPanel() {
    return exceptedNodeTypesInPathPanel;
  }

  public void setExceptedNodeTypesInPathPanel(String[] exceptedNodeTypesInPathPanel) {
    this.exceptedNodeTypesInPathPanel = exceptedNodeTypesInPathPanel;
  }

  public String[] getAcceptedMimeTypes() { return acceptedMimeTypes; }

  public void setAcceptedMimeTypes(String[] acceptedMimeTypes) { this.acceptedMimeTypes = acceptedMimeTypes; }

  public String getRepositoryName() { return repositoryName; }
  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getWorkspaceName() { return workspaceName; }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  public String getRootTreePath() { return rootTreePath; }

  public void setRootTreePath(String rootTreePath) {
    this.rootTreePath = rootTreePath;
    getChild(UISelectTaxonomyPanel.class).setTaxonomyTreePath(rootTreePath);
  }

  public String getTaxonomyLabel(Node node) throws RepositoryException {
    try {
      String display = node.getName();
      if (rootTaxonomyName == null) rootTaxonomyName = rootTreePath.substring(rootTreePath.lastIndexOf("/") + 1);
      display = rootTaxonomyName.concat(node.getPath().replace(rootTreePath, "")).replaceAll("/", ".");
      return Utils.getResourceBundle(("eXoTaxonomies.").concat(display).concat(".label"));
    } catch (MissingResourceException me) {
      return node.getName();
    }
  }

  public void onChange(final Node currentNode, Object context) throws Exception {
    UISelectTaxonomyPanel selectPathPanel = getChild(UISelectTaxonomyPanel.class);
    UITreeTaxonomyList uiTreeTaxonomyList = getChild(UITreeTaxonomyList.class);
    String taxoTreeName = uiTreeTaxonomyList.getUIFormSelectBox(UITreeTaxonomyList.TAXONOMY_TREE).getValue();
    if (StringUtils.isEmpty(taxoTreeName)) return;
    Node parentRoot = getTaxoTreeNode(taxoTreeName);
    selectPathPanel.setParentNode(currentNode);
    selectPathPanel.updateGrid();
    UIBreadcumbs uiBreadcumbs = getChild(UIBreadcumbs.class);
    String pathName = currentNode.getName();
    if (currentNode.equals(parentRoot)) {
      pathName = "";
    }
    String label = pathName.length() > 0 ? getTaxonomyLabel(currentNode) : pathName;
    UIBreadcumbs.LocalPath localPath = new UIBreadcumbs.LocalPath(pathName, label);
    List<LocalPath> listLocalPath = uiBreadcumbs.getPath();
    StringBuilder buffer = new StringBuilder(1024);
    for(LocalPath iterLocalPath: listLocalPath) {
      buffer.append("/").append(iterLocalPath.getId());
    }
    if (!alreadyChangePath) {
      String path = buffer.toString();
      path = path.replaceAll("/+", "/");
      if (!path.startsWith(rootTreePath)) {
        StringBuffer buf = new StringBuffer();
        if (currentNode.getPath().contains(parentRoot.getPath())) {
          buf.append(currentNode.getPath());
        } else {
          buf.append(rootTreePath).append(path);
        }
        path = buf.toString();
      }
      if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
      if (path.length() == 0) path = "/";
      String breadcumbPath = rootTreePath + buffer.toString();
      if (!breadcumbPath.equals(path)) {
        if (breadcumbPath.startsWith(path)) {
          if (listLocalPath != null && listLocalPath.size() > 0) {
            listLocalPath.remove(listLocalPath.size() - 1);
          }
        } else {
          if (!currentNode.equals(parentRoot)) {
            listLocalPath.add(localPath);
          }
        }
      }
    }
    alreadyChangePath = false;
    uiBreadcumbs.setPath(listLocalPath);
  }

  private void changeNode(String stringPath, Object context) throws Exception {
    UITreeTaxonomyBuilder builder = getChild(UITreeTaxonomyBuilder.class);
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
      UIOneTaxonomySelector uiOneNodePathSelector = uiBreadcumbs.getParent();
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID);
      uiBreadcumbs.setSelectPath(objectId);
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId();
      uiOneNodePathSelector.changeGroup(selectGroupId, event.getRequestContext());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiOneNodePathSelector);
    }
  }
}
