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
package org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.permission.PermissionBean;
import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.component.admin.taxonomy.UITaxonomyManagerTrees;
import org.exoplatform.ecm.webui.component.admin.taxonomy.UITaxonomyTreeContainer;
import org.exoplatform.ecm.webui.component.admin.taxonomy.action.UIActionForm;
import org.exoplatform.ecm.webui.component.admin.taxonomy.action.UIActionTaxonomyManager;
import org.exoplatform.ecm.webui.component.admin.taxonomy.info.UIPermissionForm;
import org.exoplatform.ecm.webui.selector.UIAnyPermission;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyTreeData;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 17, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIPermissionTreeForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.NextAddActionActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.PreviousActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.ResetActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.SelectUserActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.SelectMemberActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIPermissionTreeForm.AddAnyActionListener.class)
    }
)
public class UIPermissionTreeForm extends UIForm implements UISelectable {

  public static final String PERMISSION      = "permission";

  public static final String POPUP_SELECT    = "TaxoTreeSelectUserOrGroupPopup";

  public static final String SELECT_GROUP_ID = "TaxoTreeSelectUserOrGroup";
  private static final Log LOG  = ExoLogger.getLogger("admin.UIPermissionTreeForm");
  private NodeLocation  currentNode;

  private PermissionBean     permBean;

  public UIPermissionTreeForm() throws Exception {
    addChild(new UIPermissionInputSet(PERMISSION));
    setActions(new String[] { "Previous", "Save", "Reset", "NextAddAction" });
  }

  private void refresh() {
    reset();
    checkAll(false);
  }

  private void checkAll(boolean check) {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION) ;
    for (String perm : PermissionType.ALL) {
      uiInputSet.getUIFormCheckBoxInput(perm).setChecked(check);
    }
  }

  protected boolean isEditable(Node node) throws Exception {
    return PermissionUtil.canChangePermission(node);
  }

  @SuppressWarnings("unchecked")
  public void fillForm(String user, ExtendedNode node) throws Exception {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION);
    refresh();
    uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(user);
    if (node != null) {
      if (user.equals(Utils.getNodeOwner(node))) {
        for (String perm : PermissionType.ALL) {
          uiInputSet.getUIFormCheckBoxInput(perm).setChecked(true);
        }
      } else {
        List<AccessControlEntry> permsList = node.getACL().getPermissionEntries();
        Iterator perIter = permsList.iterator();
        StringBuilder userPermission = new StringBuilder();
        while (perIter.hasNext()) {
          AccessControlEntry accessControlEntry = (AccessControlEntry) perIter.next();
          if (user.equals(accessControlEntry.getIdentity())) {
            userPermission.append(accessControlEntry.getPermission()).append(" ");
          }
        }
        for (String perm : PermissionType.ALL) {
          boolean isCheck = userPermission.toString().contains(perm);
          uiInputSet.getUIFormCheckBoxInput(perm).setChecked(isCheck);
        }
      }
    } else {
      UIPermissionTreeInfo uiInfo = ((UIContainer)getParent()).getChild(UIPermissionTreeInfo.class);
      for (PermissionBean permBeanTemp : uiInfo.getPermBeans()) {
        if(permBeanTemp.getUsersOrGroups().equals(user)) {
          getUIFormCheckBoxInput(PermissionType.READ).setValue(permBeanTemp.isRead());
          getUIFormCheckBoxInput(PermissionType.ADD_NODE).setValue(permBeanTemp.isAddNode());
          getUIFormCheckBoxInput(PermissionType.REMOVE).setValue(permBeanTemp.isRemove());
          getUIFormCheckBoxInput(PermissionType.SET_PROPERTY).setValue(permBeanTemp.isSetProperty());
          break;
        }
      }
    }
  }

  protected void lockForm(boolean isLock) {
    UIPermissionInputSet uiInputSet = getChildById(PERMISSION);
    if (isLock) {
      setActions(new String[] { "Previous", "Reset", "NextAddAction" });
      uiInputSet.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, null);
    } else {
      setActions(new String[] { "Previous", "Save", "Reset", "NextAddAction" });
      uiInputSet.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, new String[] { "SelectUser",
          "SelectMember", "AddAny" });
    }
    for (String perm : PermissionType.ALL) {
      uiInputSet.getUIFormCheckBoxInput(perm).setEnable(!isLock);
    }
  }

  private String getExoOwner(Node node) throws Exception {
    return Utils.getNodeOwner(node);
  }

  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode);
  }

  public void setCurrentNode(Node currentNode) {
    this.currentNode = NodeLocation.getNodeLocationByNode(currentNode);
  }

  public PermissionBean getPermBean() {
    return permBean;
  }

  public void setPermBean(PermissionBean permBean) {
    this.permBean = permBean;
  }

  public void doSelect(String selectField, Object value) {
    try {
      ExtendedNode node = (ExtendedNode) this.getCurrentNode();
      checkAll(false);
      fillForm(value.toString(), node);
      lockForm(value.toString().equals(getExoOwner(node)));
      getUIStringInput(selectField).setValue(value.toString());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  public static class ResetActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      uiForm.lockForm(false);
      uiForm.refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class SaveActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      Node currentNode = uiForm.getCurrentNode();
      UIPermissionTreeManager uiParent = uiForm.getParent();
      UITaxonomyTreeContainer uiContainer = uiForm.getAncestorOfType(UITaxonomyTreeContainer.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String userOrGroup = uiForm.getChild(UIPermissionInputSet.class).getUIStringInput(
          UIPermissionInputSet.FIELD_USERORGROUP).getValue();
      List<String> permsList = new ArrayList<String>();
      List<String> permsRemoveList = new ArrayList<String>();
      PermissionBean permBean = new PermissionBean();
      permBean.setUsersOrGroups(userOrGroup);
      permBean.setRead(uiForm.getUIFormCheckBoxInput(PermissionType.READ).isChecked());
      permBean.setAddNode(uiForm.getUIFormCheckBoxInput(PermissionType.ADD_NODE).isChecked());
      permBean.setRemove(uiForm.getUIFormCheckBoxInput(PermissionType.REMOVE).isChecked());
      permBean.setSetProperty(uiForm.getUIFormCheckBoxInput(PermissionType.SET_PROPERTY).isChecked());

      for (String perm : PermissionType.ALL) {
        if (uiForm.getUIFormCheckBoxInput(perm).isChecked()) {
          permsList.add(perm);
        } else {
          permsRemoveList.add(perm);
        }
      }
      if (uiForm.getUIFormCheckBoxInput(PermissionType.ADD_NODE).isChecked()
          || uiForm.getUIFormCheckBoxInput(PermissionType.REMOVE).isChecked()
          || uiForm.getUIFormCheckBoxInput(PermissionType.SET_PROPERTY).isChecked()) {

        if (!permsList.contains(PermissionType.READ))
          permsList.add(PermissionType.READ);
      }

      if (Utils.isNameEmpty(userOrGroup)) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.userOrGroup-required", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      if (permsList.size() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.checkbox-require", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      String[] permsArray = permsList.toArray(new String[permsList.size()]);
      uiForm.setPermBean(permBean);
      if (currentNode != null) {
        if(currentNode.isLocked()) {
          String lockToken = LockUtil.getLockToken(currentNode);
          if(lockToken != null) currentNode.getSession().addLockToken(lockToken);
        }
        if(!currentNode.isCheckedOut()) {
          uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.node-checkedin", null,
              ApplicationMessage.WARNING));
          
          return;
        }

        ExtendedNode node = (ExtendedNode) currentNode;
        if (PermissionUtil.canChangePermission(node)) {
          if (node.canAddMixin("exo:privilegeable")){
            node.addMixin("exo:privilegeable");
            node.setPermission(Utils.getNodeOwner(node),PermissionType.ALL);
          }
          for (String perm : permsRemoveList) {
            try {
              node.removePermission(userOrGroup, perm);
            } catch (AccessDeniedException ade) {
              uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.access-denied", null,
                                                      ApplicationMessage.WARNING));
              
              return;
            }
          }
          if(PermissionUtil.canChangePermission(node)) node.setPermission(userOrGroup, permsArray);
          uiParent.getChild(UIPermissionTreeInfo.class).updateGrid();
          node.save();
        } else {
          uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.not-change-permission", null,
              ApplicationMessage.WARNING));
          
          return;
        }
        currentNode.getSession().save();
        TaxonomyService taxonomyService = uiForm.getApplicationComponent(TaxonomyService.class);
        taxonomyService.updateTaxonomyTree(uiContainer.getTaxonomyTreeData().getTaxoTreeName(), currentNode);
      }
      uiForm.refresh();
      UIPermissionTreeInfo uiInfo = uiParent.getChild(UIPermissionTreeInfo.class);
      uiInfo.updateGrid();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }

  public static class NextAddActionActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      UIPermissionTreeInfo uiPermInfo = ((UIContainer)uiForm.getParent()).getChild(UIPermissionTreeInfo.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if (uiPermInfo.getPermBeans().size() < 1) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionTreeForm.msg.have-not-any-permission", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      UITaxonomyTreeContainer uiTaxonomyTreeContainer = uiForm.getAncestorOfType(UITaxonomyTreeContainer.class);
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = uiTaxonomyTreeContainer.getAncestorOfType(UITaxonomyManagerTrees.class);
      TaxonomyTreeData taxonomyTreeData = uiTaxonomyTreeContainer.getTaxonomyTreeData();
      if (!taxonomyTreeData.isEdit()) {
        UIActionTaxonomyManager uiActionTaxonomyManager = uiTaxonomyTreeContainer.getChild(UIActionTaxonomyManager.class);
        UIActionForm uiActionForm = uiActionTaxonomyManager.getChild(UIActionForm.class);
        uiActionForm.createNewAction(null, TaxonomyTreeData.ACTION_TAXONOMY_TREE, true);
        uiActionForm.setWorkspace(taxonomyTreeData.getTaxoTreeWorkspace());
      }
      uiTaxonomyTreeContainer.viewStep(3);
      uiTaxonomyTreeContainer.setRenderedChild(UIActionTaxonomyManager.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }

  public static class SelectUserActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      ((UIPermissionTreeManager)uiForm.getParent()).initUserSelector();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class AddAnyActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      UIPermissionInputSet uiInputSet = uiForm.getChildById(UIPermissionForm.PERMISSION);
      uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(
          IdentityConstants.ANY);
      uiForm.checkAll(false);
      uiInputSet.getUIFormCheckBoxInput(PermissionType.READ).setChecked(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class SelectMemberActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UIPermissionTreeForm uiForm = event.getSource();
      UIGroupMemberSelector uiGroupMember;
      uiGroupMember = uiForm.createUIComponent(UIGroupMemberSelector.class, null, UIPermissionTreeForm.SELECT_GROUP_ID);
      uiGroupMember.getChild(UIAnyPermission.class).setId("TaxoTreeAnyPermission");
      uiGroupMember.getChild(UIBreadcumbs.class).setId("TaxoTreeBreadcumbMembershipSelector");
      uiGroupMember.getChild(UITree.class).setId("TaxoTreeMembershipSelector");
      uiGroupMember.setSourceComponent(uiForm, new String[] { UIPermissionInputSet.FIELD_USERORGROUP });
      uiForm.getAncestorOfType(UIPermissionTreeManager.class).initPopupPermission(uiGroupMember);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  public static class PreviousActionListener extends EventListener<UIPermissionTreeForm> {
    public void execute(Event<UIPermissionTreeForm> event) throws Exception {
      UITaxonomyTreeContainer uiTaxonomyTreeContainer = event.getSource().getAncestorOfType(UITaxonomyTreeContainer.class);
      uiTaxonomyTreeContainer.viewStep(1);
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = uiTaxonomyTreeContainer.getAncestorOfType(UITaxonomyManagerTrees.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }
}
