/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;
import org.exoplatform.webui.organization.account.UIGroupSelector;
import org.exoplatform.webui.organization.account.UIUserSelector;


/**
 * Created by The eXo Platform SAS
 * Author : viet.nguyen
 *          viet.nguyen@exoplatform.com
 * Jan 5, 2011  
 */
@ComponentConfigs({
@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/wiki/webui/UIPermissionForm.gtmpl",
  events = {
    @EventConfig(listeners = UIPermissionForm.AddEntryActionListener.class),
    @EventConfig(listeners = UIPermissionForm.DeleteEntryActionListener.class),
    @EventConfig(listeners = UIPermissionForm.OpenSelectUserFormActionListener.class),
    @EventConfig(listeners = UIPermissionForm.SelectUserActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPermissionForm.OpenSelectGroupFormActionListener.class),
    @EventConfig(listeners = UIPermissionForm.SelectGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPermissionForm.OpenSelectMembershipFormActionListener.class),
    @EventConfig(listeners = UIPermissionForm.SelectMembershipActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPermissionForm.SaveActionListener.class),
    @EventConfig(listeners = UIPermissionForm.CloseActionListener.class)
  }
),
@ComponentConfig(type = UIPopupWindow.class, id = UIPermissionForm.USER_PERMISSION_POPUP_SELECTOR, template = "system:/groovy/webui/core/UIPopupWindow.gtmpl", events = {
    @EventConfig(listeners = UIPermissionForm.ClosePopupActionListener.class, name = "ClosePopup"),
    @EventConfig(listeners = UIPermissionForm.SelectUserActionListener.class, name = "Add", phase = Phase.DECODE),
    @EventConfig(listeners = UIPermissionForm.CloseUserPopupActionListener.class, name = "Close", phase = Phase.DECODE) })
})
public class UIPermissionForm extends UIForm implements UIPopupComponent {

  private List<String> permissionEntries = new ArrayList<String>();

  private Scope scope;

  public final static String ANY = "any";
  
  public final static String ADD_ENTRY = "AddEntry";
  
  public final static String DELETE_ENTRY = "DeleteEntry";
  
  public final static String WIKI_PERMISSION_OWNER = "uiWikiPermissionOwner";
  
  public final static String PERMISSION_OWNER = "PermissionOwner";
  
  public final static String PERMISSION_POPUP_SELECTOR = "UIWikiPermissionPopupSelector";
  
  public final static String USER_PERMISSION_POPUP_SELECTOR = "UIWikiUserPermissionPopupSelector";
  
  public final static String OPEN_SELECT_USER_FORM = "OpenSelectUserForm";
  
  public final static String OPEN_SELECT_GROUP_FORM= "OpenSelectGroupForm";
  
  public final static String OPEN_SELECT_MEMBERSHIP_FORM= "OpenSelectMembershipForm";
  
  public final static String GROUP_ICON = "uiIconGroup";
  
  public final static String USER_ICON = "uiIconUser";
  
  public final static String MEMBERSHIP_ICON = "uiIconMembership";
  
  public final static String ADD_ICON = "ActionIcon Add";
  
  public final static String SAVE = "Save";
  
  public final static String CLOSE = "Close";  

  public static enum Scope {
    WIKI, PAGE
  }
  
  public UIPopupWindow getUserPermissionPopupSelector() {
    return (UIPopupWindow) getChildById(createIdByScope(USER_PERMISSION_POPUP_SELECTOR));
  }
  
  public UIPopupWindow getPermissionPopupSelector() {
    return (UIPopupWindow) getChildById(createIdByScope(PERMISSION_POPUP_SELECTOR));
  }
  
  private String createIdByScope(String defaultId) {
    if (scope == null) {
      return defaultId;
    }
    return defaultId + "_" + scope.name();
  }

  public UIPermissionForm() throws Exception {
    UIPermissionGrid permissionGrid = addChild(UIPermissionGrid.class, null, null);
    permissionGrid.setPermissionEntries(this.permissionEntries);
    String [] actionNames = new String[]{OPEN_SELECT_USER_FORM, OPEN_SELECT_MEMBERSHIP_FORM,
                                         OPEN_SELECT_GROUP_FORM, ADD_ENTRY};
    String [] actionIcons = new String[]{USER_ICON, MEMBERSHIP_ICON, GROUP_ICON, ADD_ICON};
    List<ActionData> actions = new ArrayList<ActionData>();
    ActionData action;
    for (int i = 0; i < actionNames.length; ++i) {
      action = new ActionData();
      action.setActionListener(actionNames[i]);
      if (i < actionNames.length - 1) {
        action.setActionType(ActionData.TYPE_ICON);
      } else {
        action.setActionType(ActionData.TYPE_LINK);
      }
      action.setActionName(actionNames[i]);
      action.setCssIconClass(actionIcons[i]);
      actions.add(action);
    }
    UIFormInputWithActions owner = new UIFormInputWithActions(WIKI_PERMISSION_OWNER);
    owner.addUIFormInput(new UIFormStringInput(PERMISSION_OWNER, PERMISSION_OWNER, null));
    owner.setActionField(PERMISSION_OWNER, actions);

    addChild(owner);
    addPopupWindow();

    setActions(new String[] { SAVE, CLOSE });
  }
  
  public Scope getScope() {
    return scope;
  }
  
 
  
  public void cancelPopupAction() throws Exception {
    
  }

  private void addPopupWindow() throws Exception {
    addChild(UIPopupWindow.class, USER_PERMISSION_POPUP_SELECTOR, createIdByScope(USER_PERMISSION_POPUP_SELECTOR));
    addChild(UIPopupWindow.class, null, createIdByScope(PERMISSION_POPUP_SELECTOR));
  }
  
  private void removeAllPopupWindow() {
    List<UIComponent> children = new ArrayList<UIComponent>(getChildren());
    for (UIComponent uichild : children) {
      if(uichild instanceof UIPopupWindow) {
        removeChild(uichild.getClass());
      }
    }
  }
  
  private void closeAllPopupAction() {
    List<UIComponent> children = new ArrayList<UIComponent>(getChildren());
    for (UIComponent uichild : children) {
      if (uichild instanceof UIPopupWindow) {
        closePopupAction((UIPopupWindow) uichild);
      }
    }
  }

  private static void closePopupAction(UIPopupWindow uiPopupWindow) {
    uiPopupWindow.setUIComponent(null);
    uiPopupWindow.setShow(false);
    WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
    rcontext.addUIComponentToUpdateByAjax(uiPopupWindow);
  }

  private static void openPopupAction(UIPopupWindow uiPopup, UIComponent component) {
    uiPopup.setUIComponent(component);
    uiPopup.setShow(true);
    uiPopup.setWindowSize(550, 0);
    WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
    rcontext.addUIComponentToUpdateByAjax(uiPopup);
  }

  

  public void setPermission(List<String> permissionEntries) throws Exception {
    this.permissionEntries = permissionEntries;
    UIPermissionGrid permissionGrid = getChild(UIPermissionGrid.class);
    permissionGrid.setPermissionEntries(this.permissionEntries);
  }
  
  
  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }

  private void processPostAction() throws Exception {
    
  }

  

  static public class AddEntryActionListener extends EventListener<UIPermissionForm> {
    @Override
    public void execute(Event<UIPermissionForm> event) throws Exception {
      
    }

    

    
  }

  static public class DeleteEntryActionListener extends EventListener<UIPermissionForm> {
    @Override
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      
      UIPermissionForm.setPermission(UIPermissionForm.permissionEntries);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPermissionForm.getChild(UIPermissionGrid.class));
    }
  }

  static public class OpenSelectUserFormActionListener extends EventListener<UIPermissionForm> {
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource();
      UIPermissionForm.closeAllPopupAction();
      UIPopupWindow uiPopup = UIPermissionForm.getUserPermissionPopupSelector();
      
    }
  }

  static public class SelectUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      UIPermissionForm UIPermissionForm = uiUserSelector.getAncestorOfType(UIPermissionForm.class);
      String values = uiUserSelector.getSelectedUsers();
      UIFormInputWithActions inputWithActions = UIPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue(values);      
      UIPopupWindow uiPopup = UIPermissionForm.getUserPermissionPopupSelector(); 
      closePopupAction(uiPopup);
      
      WebuiRequestContext rcontext = event.getRequestContext();
      rcontext.addUIComponentToUpdateByAjax(UIPermissionForm.getChildById(WIKI_PERMISSION_OWNER));
    }
  }

  static public class OpenSelectGroupFormActionListener extends EventListener<UIPermissionForm> {
    @Override
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource();
      UIPermissionForm.closeAllPopupAction();
      UIGroupSelector uiGroupSelector = UIPermissionForm.createUIComponent(UIGroupSelector.class, null, null);
      UIPopupWindow uiPopup = UIPermissionForm.getPermissionPopupSelector();
      openPopupAction(uiPopup, uiGroupSelector);
    }
  }

  static public class SelectGroupActionListener extends EventListener<UIGroupSelector> {
    @Override
    public void execute(Event<UIGroupSelector> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource().getParent().getParent();
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIFormInputWithActions inputWithActions = UIPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue("*:" + groupId);      
      closePopupAction(UIPermissionForm.getPermissionPopupSelector());
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPermissionForm);
    }
  }

  static public class OpenSelectMembershipFormActionListener extends EventListener<UIPermissionForm> {
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource();
      UIPermissionForm.closeAllPopupAction();
      UIGroupMembershipSelector uiGroupMembershipSelector = UIPermissionForm.createUIComponent(UIGroupMembershipSelector.class, null, null);
      UIPopupWindow uiPopup = UIPermissionForm.getPermissionPopupSelector();
      openPopupAction(uiPopup, uiGroupMembershipSelector);
    }
  }

  static public class SelectMembershipActionListener extends EventListener<UIGroupMembershipSelector> {
    public void execute(Event<UIGroupMembershipSelector> event) throws Exception {
      UIGroupMembershipSelector uiGroupMembershipSelector = event.getSource();
      UIPermissionForm UIPermissionForm = uiGroupMembershipSelector.getParent().getParent();
      String currentGroup = uiGroupMembershipSelector.getCurrentGroup().getId();
      String membershipId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIFormInputWithActions inputWithActions = UIPermissionForm.getChild(UIFormInputWithActions.class);
      UIFormStringInput uiFormStringInput = inputWithActions.getChild(UIFormStringInput.class);
      uiFormStringInput.setValue(membershipId + ":" + currentGroup);
      closePopupAction(UIPermissionForm.getPermissionPopupSelector());
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPermissionForm.getParent());
    }
  }

  static public class ClosePopupActionListener extends UIPopupWindow.CloseActionListener {
    public void execute(Event<UIPopupWindow> event) throws Exception {
       super.execute(event);
       closePopupAction(event.getSource());      
    }
  }

  static public class CloseUserPopupActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIPopupWindow uiPopup = (UIPopupWindow)event.getSource().getParent(); 
      closePopupAction(uiPopup);
    }
  }

  static public class SaveActionListener extends EventListener<UIPermissionForm> {
    @Override
    public void execute(Event<UIPermissionForm> event) throws Exception {
      UIPermissionForm UIPermissionForm = event.getSource();
      
    }
  }

  static public class CloseActionListener extends EventListener<UIPermissionForm> {
    @Override
    public void execute(Event<UIPermissionForm> event) throws Exception {
     
    }
  }
}
