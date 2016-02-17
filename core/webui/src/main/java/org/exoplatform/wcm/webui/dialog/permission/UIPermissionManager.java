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
package org.exoplatform.wcm.webui.dialog.permission;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.selector.account.UIUserContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 29, 2009
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/dialog/permission/UIPermissionManager.gtmpl",
    events = {
      @EventConfig(listeners = UIPermissionManager.DeleteActionListener.class,
                   confirm = "UIPermissionManagerGrid.msg.confirm-delete-permission"),
      @EventConfig(listeners = UIPermissionManager.EditActionListener.class),
      @EventConfig(listeners = UIPermissionManager.SaveActionListener.class),
      @EventConfig(listeners = UIPermissionManager.ClearActionListener.class),
      @EventConfig(listeners = UIPermissionManager.SelectUserActionListener.class),
      @EventConfig(listeners = UIPermissionManager.SelectMemberActionListener.class),
      @EventConfig(listeners = UIPermissionManager.AddAnyActionListener.class)
    }
)
public class UIPermissionManager extends UIForm implements UISelectable {

  /** The Constant PERMISSION_MANAGER_GRID. */
  public static final String PERMISSION_MANAGER_GRID     = "UIPermissionManagerGrid";

  /** The Constant PERMISSION_INPUT_SET. */
  public static final String PERMISSION_INPUT_SET        = "UIPermissionInputSetWithAction";

  /** The Constant PERMISSION_STRING_INPUT. */
  public static final String PERMISSION_STRING_INPUT     = "UIPermissionStringInput";

  /** The Constant ACCESSIBLE_CHECKBOX_INPUT. */
  public static final String ACCESSIBLE_CHECKBOX_INPUT   = "UIAccessibleCheckboxInput";

  /** The Constant EDITABLE_CHECKBOX_INPUT. */
  public static final String EDITABLE_CHECKBOX_INPUT     = "UIEditableCheckboxInput";

  public static final String USER_SELECTOR_POPUP_WINDOW  = "UIUserSelectorPopupWindow";

  public static final String GROUP_SELECTOR_POPUP_WINDOW = "UIGroupSelectorPopupWindow";

  private String popupId;

  public String getPopupId() {
    return popupId;
  }

  public void setPopupId(String popupId) {
    this.popupId = popupId;
  }

  /**
   * Instantiates a new uI permission info.
   *
   * @throws Exception the exception
   */
  public UIPermissionManager() throws Exception {
    UIGrid uiGrid = createUIComponent(UIGrid.class, null, PERMISSION_MANAGER_GRID);
    uiGrid.setLabel(PERMISSION_MANAGER_GRID);
    uiGrid.configure("owner", new String[] {"owner", "accessible", "editable"}, new String[] {"Edit", "Delete"});
    addChild(uiGrid);

    UIFormInputSetWithAction permissionInputSet = new UIFormInputSetWithAction(PERMISSION_INPUT_SET);
    UIFormStringInput formStringInput = new UIFormStringInput(PERMISSION_STRING_INPUT, PERMISSION_STRING_INPUT, null);
    formStringInput.setReadOnly(true);
    permissionInputSet.addChild(formStringInput);
    permissionInputSet.setActionInfo(PERMISSION_STRING_INPUT, new String[] {"SelectUser", "SelectMember", "AddAny"});
    permissionInputSet.showActionInfo(true);
    addChild(permissionInputSet);
    addChild(new UICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT, ACCESSIBLE_CHECKBOX_INPUT, null));
    addChild(new UICheckBoxInput(EDITABLE_CHECKBOX_INPUT, EDITABLE_CHECKBOX_INPUT, null));
    setActions(new String[] {"Save", "Clear"});
  }

  /**
   * Update grid.
   *
   * @throws Exception the exception
   */
  public void updateGrid() throws Exception {
    // Get node
    UIContentDialogForm contentDialogForm = getAncestorOfType(UIPopupContainer.class).getChild(UIContentDialogForm.class);
    NodeLocation webcontentNodeLocation = contentDialogForm.getWebcontentNodeLocation();
    Node node = NodeLocation.getNodeByLocation(webcontentNodeLocation);
    ExtendedNode webcontent = (ExtendedNode) node;

    // Convert permission entries to map
    List<UIPermissionConfig> permissionConfigs = new ArrayList<UIPermissionConfig>();
    Map<String, List<String>> permissionMap = new HashMap<String, List<String>>();
    List<AccessControlEntry> accessControlEntries = webcontent.getACL().getPermissionEntries();
    for (AccessControlEntry accessControlEntry : accessControlEntries) {
      String identity = accessControlEntry.getIdentity();
      String permission = accessControlEntry.getPermission();
      List<String> currentPermissions = permissionMap.get(identity);
      if (!permissionMap.containsKey(identity)) {
        permissionMap.put(identity, null);
      }
      if (currentPermissions == null)
        currentPermissions = new ArrayList<String>();
      if (!currentPermissions.contains(permission)) {
        currentPermissions.add(permission);
      }
      permissionMap.put(identity, currentPermissions);
    }

    // Add owner's permission
    String owner = IdentityConstants.SYSTEM;
    if (webcontent.hasProperty("exo:owner"))
      owner = webcontent.getProperty("exo:owner").getString();
    UIPermissionConfig permissionConfig = new UIPermissionConfig();
    if (!permissionMap.containsKey(owner)) {
      permissionConfig.setOwner(owner);
      permissionConfig.setAccessible(true);
      permissionConfig.setEditable(true);
      permissionConfigs.add(permissionConfig);
    }

    // Add node's permission
    Iterator<String> permissionIterator = permissionMap.keySet().iterator();
    while (permissionIterator.hasNext()) {
      String identity = (String) permissionIterator.next();
      List<String> userPermissions = permissionMap.get(identity);
      UIPermissionConfig permBean = new UIPermissionConfig();
      permBean.setOwner(identity);
      int numberPermission = 0;
      for (String p : PermissionType.ALL) {
        if (!userPermissions.contains(p)) break;
        numberPermission++;
      }
      if (numberPermission == PermissionType.ALL.length) {
        permBean.setEditable(true);
        permBean.setAccessible(true);
      } else {
        permBean.setAccessible(true);
      }
      permissionConfigs.add(permBean);
    }
    ListAccess<UIPermissionConfig> permConfigList = new ListAccessImpl<UIPermissionConfig>(UIPermissionConfig.class,
                                                                                           permissionConfigs);
    LazyPageList<UIPermissionConfig> dataPageList = new LazyPageList<UIPermissionConfig>(permConfigList,
                                                                                         10);
    UIGrid uiGrid = getChildById(PERMISSION_MANAGER_GRID);
    uiGrid.getUIPageIterator().setPageList(dataPageList);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    UIFormInputSetWithAction permissionInputSet = getChildById(PERMISSION_INPUT_SET);
    permissionInputSet.getUIStringInput(PERMISSION_STRING_INPUT).setValue(value.toString());
    Utils.closePopupWindow(this, popupId);
  }

  /**
  * Checks for change permission right.
  *
  * @param node the node
  *
  * @return true, if successful
  *
  * @throws Exception the exception
  */
  private boolean hasChangePermissionRight(ExtendedNode node) throws Exception {
   try {
     node.checkPermission(PermissionType.ADD_NODE);
     node.checkPermission(PermissionType.REMOVE);
     node.checkPermission(PermissionType.SET_PROPERTY);
     return true;
   } catch (AccessControlException e) {
     return false;
   }
  }

  /**
   * The listener interface for receiving deleteAction events.
   * The class that is interested in processing a deleteAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDeleteActionListener</code> method. When
   * the deleteAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class DeleteActionListener extends EventListener<UIPermissionManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPermissionManager> event) throws Exception {
      UIPermissionManager permissionManager = event.getSource();
      UIContentDialogForm contentDialogForm = permissionManager.getAncestorOfType(UIPopupContainer.class)
                                                               .getChild(UIContentDialogForm.class);
      NodeLocation webcontentNodeLocation = contentDialogForm.getWebcontentNodeLocation();
      Node node = NodeLocation.getNodeByLocation(webcontentNodeLocation);
      ExtendedNode webcontent = (ExtendedNode) node;
      Session session = webcontent.getSession();

      String name = event.getRequestContext().getRequestParameter(OBJECTID);
      String nodeOwner = webcontent.getProperty("exo:owner").getString();
      if (name.equals(nodeOwner)) {
        Utils.createPopupMessage(permissionManager,
                                 "UIPermissionManagerGrid.msg.no-permission-remove",
                                 null,
                                 ApplicationMessage.WARNING);
        return;
      }
      if (permissionManager.hasChangePermissionRight(webcontent)) {
        if (webcontent.canAddMixin("exo:privilegeable")) {
          webcontent.addMixin("exo:privilegeable");
          webcontent.setPermission(nodeOwner, PermissionType.ALL);
        }
        try {
          webcontent.removePermission(name);
          session.save();
          permissionManager.updateGrid();
        } catch (AccessControlException e) {
          Object[] args = {webcontent.getPath()};
          Utils.createPopupMessage(permissionManager,
                                   "UIPermissionManagerGrid.msg.node-locked",
                                   args,
                                   ApplicationMessage.WARNING);
          return;
        } catch (AccessDeniedException ace) {
          Utils.createPopupMessage(permissionManager,
                                   "UIPermissionManagerGrid.msg.access-denied",
                                   null,
                                   ApplicationMessage.WARNING);
          return;
        }
      }
    }
  }

  /**
   * The listener interface for receiving editAction events.
   * The class that is interested in processing a editAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addEditActionListener</code> method. When
   * the editAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class EditActionListener extends EventListener<UIPermissionManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPermissionManager> event) throws Exception {
      UIPermissionManager permissionManager = event.getSource();
      UIContentDialogForm contentDialogForm = permissionManager.getAncestorOfType(UIPopupContainer.class)
                                                               .getChild(UIContentDialogForm.class);
      NodeLocation webcontentNodeLocation = contentDialogForm.getWebcontentNodeLocation();
      Node node = NodeLocation.getNodeByLocation(webcontentNodeLocation);
      ExtendedNode webcontent = (ExtendedNode) node;
      String name = event.getRequestContext().getRequestParameter(OBJECTID);

      UIFormInputSetWithAction permissionInputSet = permissionManager.getChildById(PERMISSION_INPUT_SET);
      permissionInputSet.getUIStringInput(PERMISSION_STRING_INPUT).setValue(name);
      String owner = node.getProperty("exo:owner").getString();
      if (name.equals(owner)) {
        permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(true);
        permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(true);
        permissionManager.setActions(new String[] {"Clear"});
        permissionInputSet.setActionInfo(PERMISSION_STRING_INPUT, null);
      } else {
        List<AccessControlEntry> permsList = webcontent.getACL().getPermissionEntries();
        StringBuilder userPermission = new StringBuilder();
        for (AccessControlEntry accessControlEntry : permsList) {
          if (name.equals(accessControlEntry.getIdentity())) {
            userPermission.append(accessControlEntry.getPermission()).append(" ");
          }
        }
        int numPermission = 0;
        for (String perm : PermissionType.ALL) {
          if (userPermission.toString().contains(perm))
            numPermission++;
        }
        if (numPermission == PermissionType.ALL.length) {
          permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(true);
          permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(true);
        } else {
          permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(true);
          permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(false);
        }
        permissionManager.setActions(new String[] {"Save", "Clear"});
        permissionInputSet.setActionInfo(PERMISSION_STRING_INPUT, new String[] { "SelectUser",
            "SelectMember", "AddAny" });
      }
    }
  }

  /**
  * The listener interface for receiving saveAction events.
  * The class that is interested in processing a saveAction
  * event implements this interface, and the object created
  * with that class is registered with a component using the
  * component's <code>addSaveActionListener</code> method. When
  * the saveAction event occurs, that object's appropriate
  * method is invoked.
  */
  public static class SaveActionListener extends EventListener<UIPermissionManager> {

   /* (non-Javadoc)
    * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
    */
   public void execute(Event<UIPermissionManager> event) throws Exception {
     UIPermissionManager permissionManager = event.getSource();
      UIContentDialogForm contentDialogForm = permissionManager.getAncestorOfType(UIPopupContainer.class)
                                                               .getChild(UIContentDialogForm.class);
     NodeLocation webcontentNodeLocation = contentDialogForm.getWebcontentNodeLocation();
     Node node = NodeLocation.getNodeByLocation(webcontentNodeLocation);
     ExtendedNode webcontent = (ExtendedNode) node;
     Session session = webcontent.getSession();

     UIFormInputSetWithAction formInputSet = permissionManager.getChildById(PERMISSION_INPUT_SET);
     String identity = ((UIFormStringInput) formInputSet.getChildById(PERMISSION_STRING_INPUT)).getValue();
     List<String> permsList = new ArrayList<String>();
     if (!webcontent.isCheckedOut()) {
        Utils.createPopupMessage(permissionManager,
                                 "UIPermissionManagerGrid.msg.node-checkedin",
                                 null,
                                 ApplicationMessage.WARNING);
       return;
     }
     if (permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).isChecked()) {
       permsList.clear();
       permsList.add(PermissionType.READ);
     }
     if (permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).isChecked()) {
       permsList.clear();
       for (String perm : PermissionType.ALL)
         permsList.add(perm);
     }
     if (identity == null || identity.trim().length() == 0) {
        Utils.createPopupMessage(permissionManager,
                                 "UIPermissionManagerGrid.msg.userOrGroup-required",
                                 null,
                                 ApplicationMessage.WARNING);
       return;
     }
     if (permsList.size() == 0) {
        Utils.createPopupMessage(permissionManager,
                                 "UIPermissionManagerGrid.msg.checkbox-require",
                                 null,
                                 ApplicationMessage.WARNING);
       return;
     }
     String[] permsArray = permsList.toArray(new String[permsList.size()]);
     if (webcontent.canAddMixin("exo:privilegeable")) {
       webcontent.addMixin("exo:privilegeable");
       webcontent.setPermission(webcontent.getProperty("exo:owner").getString(), PermissionType.ALL);
     }
     try {
      webcontent.setPermission(identity, permsArray);
    } catch (AccessControlException e) {
      Object[] args = {webcontent.getPath()};
        Utils.createPopupMessage(permissionManager,
                                 "UIPermissionManagerGrid.msg.node-locked",
                                 args,
                                 ApplicationMessage.WARNING);
      return;
    }
     session.save();

     permissionManager.updateGrid();
     UIFormInputSetWithAction permissionInputSet = permissionManager.getChildById(PERMISSION_INPUT_SET);
     ((UIFormStringInput) permissionInputSet.getChildById(PERMISSION_STRING_INPUT)).setValue("");
     permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(false);
     permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(false);
   }
  }


  /**
  * The listener interface for receiving resetAction events.
  * The class that is interested in processing a resetAction
  * event implements this interface, and the object created
  * with that class is registered with a component using the
  * component's <code>addResetActionListener</code> method. When
  * the resetAction event occurs, that object's appropriate
  * method is invoked.
  */
  public static class ClearActionListener extends EventListener<UIPermissionManager> {

   /* (non-Javadoc)
    * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
    */
   public void execute(Event<UIPermissionManager> event) throws Exception {
     UIPermissionManager permissionManager = event.getSource();
     UIFormInputSetWithAction permissionInputSet = permissionManager.getChildById(PERMISSION_INPUT_SET);
     ((UIFormStringInput) permissionInputSet.getChildById(PERMISSION_STRING_INPUT)).setValue("");
     permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(false);
     permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(false);
     permissionManager.setActions(new String[] {"Save", "Clear"});
      permissionInputSet.setActionInfo(PERMISSION_STRING_INPUT, new String[] { "SelectUser",
          "SelectMember", "AddAny" });
   }
  }

  /**
  * The listener interface for receiving selectUserAction events.
  * The class that is interested in processing a selectUserAction
  * event implements this interface, and the object created
  * with that class is registered with a component using the
  * component's <code>addSelectUserActionListener</code> method. When
  * the selectUserAction event occurs, that object's appropriate
  * method is invoked.
  */
  public static class SelectUserActionListener extends EventListener<UIPermissionManager> {

   /* (non-Javadoc)
    * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
    */
   public void execute(Event<UIPermissionManager> event) throws Exception {
     UIPermissionManager permissionManager = event.getSource();
     UIUserContainer userContainer = permissionManager.createUIComponent(UIUserContainer.class, null, null);
     userContainer.setSelectable(permissionManager);
     userContainer.setSourceComponent(PERMISSION_STRING_INPUT);
     Utils.createPopupWindow(permissionManager, userContainer, USER_SELECTOR_POPUP_WINDOW, 740);
     permissionManager.setPopupId(USER_SELECTOR_POPUP_WINDOW);
   }
  }

  /**
  * The listener interface for receiving selectMemberAction events.
  * The class that is interested in processing a selectMemberAction
  * event implements this interface, and the object created
  * with that class is registered with a component using the
  * component's <code>addSelectMemberActionListener</code> method. When
  * the selectMemberAction event occurs, that object's appropriate
  * method is invoked.
  */
  public static class SelectMemberActionListener extends EventListener<UIPermissionManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPermissionManager> event) throws Exception {
      UIPermissionManager permissionManager = event.getSource();
      UIGroupMemberSelector groupContainer = permissionManager.createUIComponent(UIGroupMemberSelector.class, null, null);
      groupContainer.setShowAnyPermission(false);
      groupContainer.setSourceComponent(permissionManager, new String[] {PERMISSION_STRING_INPUT});
      Utils.createPopupWindow(permissionManager, groupContainer, GROUP_SELECTOR_POPUP_WINDOW, 600);
      permissionManager.setPopupId(GROUP_SELECTOR_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving addAnyAction events.
   * The class that is interested in processing a addAnyAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddAnyActionListener</code> method. When
   * the addAnyAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class AddAnyActionListener extends EventListener<UIPermissionManager> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPermissionManager> event) throws Exception {
      UIPermissionManager permissionManager = event.getSource();
      UIFormInputSetWithAction permisionInputSet = permissionManager.getChildById(PERMISSION_INPUT_SET);
      ((UIFormStringInput) permisionInputSet.getChildById(PERMISSION_STRING_INPUT)).setValue(IdentityConstants.ANY);
      permissionManager.getUICheckBoxInput(ACCESSIBLE_CHECKBOX_INPUT).setChecked(true);
      permissionManager.getUICheckBoxInput(EDITABLE_CHECKBOX_INPUT).setChecked(false);
    }
  }

}
