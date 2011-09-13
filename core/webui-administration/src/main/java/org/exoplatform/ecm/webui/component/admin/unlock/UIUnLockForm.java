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
package org.exoplatform.ecm.webui.component.admin.unlock;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 29, 2006
 * 11:30:29 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIUnLockForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIUnLockForm.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIUnLockForm.AddPermissionActionListener.class)
    }
)
public class UIUnLockForm extends UIForm implements UISelectable {

  final static public String GROUPS_OR_USERS = "groupsOrUsers";
  final static public String[] ACTIONS = {"Save", "Cancel"};

  public UIUnLockForm() throws Exception {
    UIFormInputSetWithAction uiInputAct = new UIFormInputSetWithAction("PermissionButton");
    uiInputAct.addUIFormInput( new UIFormStringInput(GROUPS_OR_USERS, GROUPS_OR_USERS, null).
                               setEditable(false).addValidator(MandatoryValidator.class));
    uiInputAct.setActionInfo(GROUPS_OR_USERS, new String[] {"AddPermission"});
    addUIComponentInput(uiInputAct);
  }

  public String[] getActions() { return ACTIONS ; }

  public void doSelect(String selectField, Object value) {
    getUIStringInput(selectField).setValue(value.toString());
    UIUnLockManager uiManager = getAncestorOfType(UIUnLockManager.class);
    UIPopupWindow uiPopup = uiManager.getChildById("PermissionPopup");
    uiPopup.setRendered(false);
    uiPopup.setShow(false);
    uiManager.getChild(UILockNodeList.class).setRendered(false);
    uiManager.getChild(UIUnLockForm.class).setRendered(true);
  }

  public void update()throws Exception {
    getUIStringInput(GROUPS_OR_USERS).setValue("");
  }

  static public class CancelActionListener extends EventListener<UIUnLockForm> {
    public void execute(Event<UIUnLockForm> event) throws Exception {
      UIUnLockForm uiForm = event.getSource();
      UIUnLockManager uiManager = uiForm.getAncestorOfType(UIUnLockManager.class);
      uiManager.removeChildById("PermissionPopup");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class SaveActionListener extends EventListener<UIUnLockForm> {
    public void execute(Event<UIUnLockForm> event) throws Exception {
      UIUnLockForm uiUnLockForm = event.getSource();
      UIApplication uiApp = uiUnLockForm.getAncestorOfType(UIApplication.class);
      UIFormInputSetWithAction permField = uiUnLockForm.getChildById("PermissionButton");
      String groupsOrUsers = permField.getUIStringInput(GROUPS_OR_USERS).getValue();
      if((groupsOrUsers == null)||(groupsOrUsers.trim().length() == 0)) {
        uiApp.addMessage(new ApplicationMessage("UIUnLockForm.msg.permission-require", null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      UIUnLockManager uiManager = uiUnLockForm.getAncestorOfType(UIUnLockManager.class);
      LockService lockService = uiUnLockForm.getApplicationComponent(LockService.class);
      lockService.addGroupsOrUsersForLock(groupsOrUsers);
      UILockNodeList uiLockList = uiManager.getChild(UILockNodeList.class);
      uiUnLockForm.update();
      uiLockList.refresh(1);
      uiLockList.setRendered(true);
      uiManager.removeChildById("PermissionPopup");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }

  static public class AddPermissionActionListener extends EventListener<UIUnLockForm> {
    public void execute(Event<UIUnLockForm> event) throws Exception {
      UIUnLockManager uiManager = event.getSource().getAncestorOfType(UIUnLockManager.class);
      String membership = event.getSource().getUIStringInput(GROUPS_OR_USERS).getValue();
      uiManager.initPermissionPopup(membership);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
}
