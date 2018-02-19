/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.exoplatform.ecm.webui.component.admin.taxonomy.action.UIActionForm;
import org.exoplatform.ecm.webui.component.admin.taxonomy.action.UIActionTaxonomyManager;
import org.exoplatform.ecm.webui.component.admin.taxonomy.action.UIActionTypeForm;
import org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info.UIPermissionTreeForm;
import org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info.UIPermissionTreeInfo;
import org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info.UIPermissionTreeManager;
import org.exoplatform.ecm.webui.core.bean.PermissionBean;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyTreeData;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyNodeAlreadyExistsException;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 3, 2009
 */

@ComponentConfig(
    template =  "app:/groovy/webui/component/admin/taxonomy/UITaxonomyTreeWizard.gtmpl",
    events = {
      @EventConfig(listeners = UITaxonomyTreeContainer.RefreshActionListener.class),
      @EventConfig(listeners = UITaxonomyTreeContainer.CancelActionListener.class)
    }
)

public class UITaxonomyTreeContainer extends UIContainer implements UISelectable {

  private int                selectedStep_          = 1;

  private int                currentStep_           = 0;

  private TaxonomyTreeData   taxonomyTreeData;

  public static final String POPUP_PERMISSION       = "PopupTaxonomyTreePermission";

  public static final String POPUP_TAXONOMYHOMEPATH = "PopupTaxonomyJCRBrowser";

  private String[]           actions_               = { "Cancel" };

  public UITaxonomyTreeContainer() throws Exception {
    addChild(UITaxonomyTreeMainForm.class, null, "TaxonomyTreeMainForm");
    addChild(UIPermissionTreeManager.class, null, "TaxonomyPermissionTree").setRendered(false);
    addChild(UIActionTaxonomyManager.class, null, null).setRendered(false);
  }

  public String[] getActions() {return actions_;}

  public void setCurrentSep(int step) {
    currentStep_ = step;
  }

  public int getCurrentStep() {
    return currentStep_;
  }

  public void setSelectedStep(int step) {
    selectedStep_ = step;
  }

  public int getSelectedStep() {
    return selectedStep_;
  }

  public TaxonomyTreeData getTaxonomyTreeData() {
    return taxonomyTreeData;
  }

  public void setTaxonomyTreeData(TaxonomyTreeData taxonomyTreeData) {
    this.taxonomyTreeData = taxonomyTreeData;
  }

  public void viewStep(int step) {
    selectedStep_ = step;
    currentStep_ = step - 1;
    List<UIComponent> children = getChildren();
    for(int i=0; i<children.size(); i++){
      if(i == getCurrentStep()) {
        children.get(i).setRendered(true);
      } else {
        children.get(i).setRendered(false);
      }
    }
  }

  public void refresh() throws Exception {
    if (taxonomyTreeData == null) {
      taxonomyTreeData = new TaxonomyTreeData();
    }
    taxonomyTreeData.setRepository(getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                                   .getConfiguration()
                                                                                   .getName());
    String taxoTreeName = taxonomyTreeData.getTaxoTreeName();
    UIActionTaxonomyManager uiActionTaxonomyManager = getChild(UIActionTaxonomyManager.class);
    UIActionTypeForm uiActionTypeForm = uiActionTaxonomyManager.getChild(UIActionTypeForm.class);
    if (taxonomyTreeData.isEdit()) {
      removeChild(UITaxonomyTreeCreateChild.class);
      UITaxonomyTreeCreateChild uiTaxonomyCreateChild = addChild(UITaxonomyTreeCreateChild.class, null, null);
      TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
      ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
      Node taxoTreeNode = taxonomyService.getTaxonomyTree(taxoTreeName,
          true);
      if (taxoTreeNode != null) {
        loadData(taxoTreeNode);
        Node actionNode = actionService.getAction(taxoTreeNode, taxonomyTreeData
            .getTaxoTreeActionName());
        uiActionTaxonomyManager.removeChild(UIActionForm.class);
        UIActionForm uiActionForm = uiActionTaxonomyManager.addChild(UIActionForm.class, null, null);
        uiActionTypeForm.setDefaultActionType(taxonomyTreeData.getTaxoTreeActionTypeName());
        uiActionForm.createNewAction(taxoTreeNode, taxonomyTreeData.getTaxoTreeActionTypeName(), true);
        uiActionForm.setWorkspace(taxonomyTreeData.getTaxoTreeWorkspace());
        uiActionForm.setNodePath(actionNode.getPath());
        if (uiTaxonomyCreateChild == null)
          uiTaxonomyCreateChild = addChild(UITaxonomyTreeCreateChild.class, null, null);
        uiTaxonomyCreateChild.setWorkspace(getTaxonomyTreeData().getTaxoTreeWorkspace());
        uiTaxonomyCreateChild.setTaxonomyTreeNode(taxoTreeNode);
        UIPermissionTreeInfo uiPermInfo = findFirstComponentOfType(UIPermissionTreeInfo.class);
        UIPermissionTreeForm uiPermForm = findFirstComponentOfType(UIPermissionTreeForm.class);
        uiPermInfo.setCurrentNode(taxoTreeNode);
        uiPermForm.setCurrentNode(taxoTreeNode);
        uiPermInfo.updateGrid();
      }
    }
    uiActionTypeForm.setDefaultActionType(null);
    findFirstComponentOfType(UITaxonomyTreeMainForm.class).update(taxonomyTreeData);
  }

  private void loadData(Node taxoTreeTargetNode) throws RepositoryException, Exception{
    String taxoTreeName = taxonomyTreeData.getTaxoTreeName();
    if (taxoTreeName == null || taxoTreeName.length() == 0) return;
    if (taxoTreeTargetNode != null) {
      Session session = taxoTreeTargetNode.getSession();
      taxonomyTreeData.setTaxoTreeWorkspace(session.getWorkspace().getName());
      taxonomyTreeData.setTaxoTreeHomePath(taxoTreeTargetNode.getParent().getPath());
      taxonomyTreeData.setTaxoTreePermissions("");
      ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
      List<Node> lstActionNodes = actionService.getActions(taxoTreeTargetNode);
      if (lstActionNodes != null && lstActionNodes.size() > 0) {
        Node node = lstActionNodes.get(0);
        if (node != null) {
          taxonomyTreeData.setTaxoTreeActionName(node.getName());
          taxonomyTreeData.setTaxoTreeActionTypeName(node.getPrimaryNodeType().getName());
        }
      }
    }
  }

  private UIFormStringInput getFormInputById(String id) {
    return (UIFormStringInput)findComponentById(id);
  }

  public Session getSession(String workspace) throws RepositoryException {
    return WCMCoreUtils.getSystemSessionProvider().getSession(workspace, 
                                                              WCMCoreUtils.getRepository());
  }

  public void doSelect(String selectField, Object value) throws Exception {
    getFormInputById(selectField).setValue(value.toString());
    UITaxonomyManagerTrees uiContainer = getAncestorOfType(UITaxonomyManagerTrees.class);
    for (UIComponent uiChild : uiContainer.getChildren()) {
      if (uiChild.getId().equals(UITaxonomyTreeContainer.POPUP_PERMISSION)
          || uiChild.getId().equals(UITaxonomyTreeContainer.POPUP_TAXONOMYHOMEPATH)) {
        UIPopupWindow uiPopup = uiContainer.getChildById(uiChild.getId());
        uiPopup.setRendered(false);
        uiPopup.setShow(false);
      }
    }
  }

  /**
   * Add taxonomy tree with given name, workspace, home path. Add permission for tree node
   * @param name
   * @param workspace
   * @param homePath
   * @param permBeans
   * @throws TaxonomyAlreadyExistsException
   * @throws TaxonomyNodeAlreadyExistsException
   * @throws AccessControlException
   * @throws Exception
   */
  public void addTaxonomyTree(String name, String workspace, String homePath, List<PermissionBean> permBeans)
      throws TaxonomyAlreadyExistsException, TaxonomyNodeAlreadyExistsException, AccessControlException, Exception {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    taxonomyService.addTaxonomyNode(workspace, homePath, name, Util.getPortalRequestContext().getRemoteUser());
    Session session = getSession(workspace);
    Node homeNode = (Node)session.getItem(homePath);
    Node taxonomyTreeNode = homeNode.getNode(name);
    ExtendedNode node = (ExtendedNode) taxonomyTreeNode;
    if (permBeans != null && permBeans.size() > 0) {
      if (PermissionUtil.canChangePermission(node)) {
        if (node.canAddMixin("exo:privilegeable")){
          node.addMixin("exo:privilegeable");
        }
        if (node.isNodeType("exo:privilegeable")) {
          AccessControlList acl = node.getACL();
          List<AccessControlEntry> permissionEntries = acl.getPermissionEntries();
          String nodeOwner = Utils.getNodeOwner(node);
          for (AccessControlEntry accessControlEntry : permissionEntries) {
            String identity = accessControlEntry.getIdentity();
            if (IdentityConstants.SYSTEM.equals(identity) || identity.equals(nodeOwner)) {
              continue;
            }
            node.removePermission(identity);
          }
          node.setPermission(nodeOwner,PermissionType.ALL);
          if(PermissionUtil.canChangePermission(node)) {
            for(PermissionBean permBean : permBeans) {
              List<String> permsList = new ArrayList<String>();
              if (permBean.isRead()) permsList.add(PermissionType.READ);
              if (permBean.isAddNode()) permsList.add(PermissionType.ADD_NODE);
              if (permBean.isRemove()) permsList.add(PermissionType.REMOVE);
  //            if (permBean.isSetProperty()) permsList.add(PermissionType.SET_PROPERTY);
              if (permsList.size() > 0) {
                node.setPermission(permBean.getUsersOrGroups(), permsList.toArray(new String[permsList.size()]));
              }
            }
          }
          node.save();
        }
      }
    }
    homeNode.save();
    session.save();
    taxonomyService.addTaxonomyTree(taxonomyTreeNode);
  }

  /**
   * Update taxonomy tree: If home path or workspace is changed, move taxonomy tree to new target
   * @param name
   * @param workspace
   * @param homePath
   * @return true: if taxonomy tree already has moved successfully
   *         false: if taxonomy has not changed
   * @throws RepositoryException
   * @throws AccessControlException
   * @throws Exception
   */
  public boolean updateTaxonomyTree(String name, String workspace, String homePath, String actionName)
      throws RepositoryException, AccessControlException, Exception {
    String repository = getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                        .getConfiguration()
                                                                        .getName();
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    Node taxonomyTreeNode = taxonomyService.getTaxonomyTree(name, true);
    Node homeNode = taxonomyTreeNode.getParent();
    String srcWorkspace = taxonomyTreeNode.getSession().getWorkspace().getName();
    Session session = getSession(workspace);
    Workspace objWorkspace = session.getWorkspace();
    //No change
    if (homeNode.getPath().equals(homePath) && srcWorkspace.equals(workspace)) return false;
    ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
    if (actionService.hasActions(taxonomyTreeNode)) {
      actionService.removeAction(taxonomyTreeNode, actionName, repository);
    }

    String destPath = homePath + "/" + name;
    destPath = destPath.replaceAll("/+", "/");
    if (srcWorkspace.equals(workspace)) {
      objWorkspace.move(taxonomyTreeNode.getPath(), destPath);
    } else {
      objWorkspace.copy(srcWorkspace, taxonomyTreeNode.getPath(), destPath);
      taxonomyTreeNode.remove();
      homeNode.save();
    }
    session.save();
    //Update taxonomy tree
    taxonomyTreeNode = (Node)session.getItem(destPath);
    taxonomyService.updateTaxonomyTree(name, taxonomyTreeNode);
    return true;
  }

  public static class RefreshActionListener extends EventListener<UITaxonomyTreeContainer> {
    public void execute(Event<UITaxonomyTreeContainer> event) throws Exception {
      event.getSource().refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }

  public static class CancelActionListener extends EventListener<UITaxonomyTreeContainer> {
    public void execute(Event<UITaxonomyTreeContainer> event) throws Exception {
      UITaxonomyTreeContainer uiTaxonomyTreeContainer = event.getSource();
      UIPopupWindow uiPopup = uiTaxonomyTreeContainer.getParent();
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = uiTaxonomyTreeContainer.getAncestorOfType(UITaxonomyManagerTrees.class);
      uiTaxonomyManagerTrees.removeChildById(UITaxonomyTreeList.ST_ADD);
      uiTaxonomyManagerTrees.removeChildById(UITaxonomyTreeList.ST_EDIT);
      uiTaxonomyManagerTrees.update();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }
}
