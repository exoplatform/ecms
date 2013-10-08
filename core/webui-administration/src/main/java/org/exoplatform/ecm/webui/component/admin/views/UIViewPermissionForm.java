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
package org.exoplatform.ecm.webui.component.admin.views;

import javax.jcr.Node;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.core.UIPermissionFormBase;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Feb 18, 2013
 * 1:39:45 PM  
 */

@ComponentConfig(
        lifecycle = UIFormLifecycle.class,
        template = "app:/groovy/webui/component/admin/view/UIViewPermissionForm.gtmpl",
        events = {
          @EventConfig(listeners = UIViewPermissionForm.AddActionListener.class),
          @EventConfig(phase = Phase.DECODE, listeners = UIViewPermissionForm.SelectUserActionListener.class),
          @EventConfig(phase = Phase.DECODE, listeners = UIViewPermissionForm.SelectMemberActionListener.class),
          @EventConfig(phase = Phase.DECODE, listeners = UIViewPermissionForm.AddAnyActionListener.class)
        }
      )
public class UIViewPermissionForm extends UIPermissionFormBase implements UISelectable{
  
  public static final String TAB_PERMISSION   = "permission";

  public UIViewPermissionForm() throws Exception {
    removeChildById(UIPermissionFormBase.PERMISSION);
    addChild(new UIPermissionInputSet(TAB_PERMISSION));
    UIPermissionInputSet uiPerInputset = getChildById(TAB_PERMISSION);
    uiPerInputset.setButtonActions(new String[] {"Add"});
    uiPerInputset.setPrimaryButtonAction("Add");
    uiPerInputset.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, new String[] {"SelectUser", "SelectMember"});
  }
  
  static public class SelectUserActionListener extends EventListener<UIViewPermissionForm> {
    public void execute(Event<UIViewPermissionForm> event) throws Exception {
      UIViewPermissionForm uiForm = event.getSource();
      ((UIPermissionManagerBase)uiForm.getParent()).initUserSelector();
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }

  static public class AddAnyActionListener extends EventListener<UIViewPermissionForm> {
    public void execute(Event<UIViewPermissionForm> event) throws Exception {
      UIViewPermissionForm uiForm = event.getSource();
      UIPermissionInputSet uiInputSet = uiForm.getChildById(TAB_PERMISSION);
      uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(IdentityConstants.ANY);
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }

  static public class SelectMemberActionListener extends EventListener<UIViewPermissionForm> {
    public void execute(Event<UIViewPermissionForm> event) throws Exception {
      UIViewPermissionForm uiForm = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiForm.createUIComponent(UIGroupMemberSelector.class, null, null);
      uiGroupMemberSelector.setShowAnyPermission(false);
      uiGroupMemberSelector.setSourceComponent(uiForm, new String[] { UIPermissionInputSet.FIELD_USERORGROUP });
      uiForm.getAncestorOfType(UIPermissionManagerBase.class).initPopupPermission(uiGroupMemberSelector);
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }
  
  static public class AddActionListener extends EventListener<UIViewPermissionForm> {
    public void execute(Event<UIViewPermissionForm> event) throws Exception {
      UIViewPermissionForm uiForm = event.getSource();
      UIViewPermissionContainer uiContainer = uiForm.getParent();
      UIViewFormTabPane uiTabPane = uiContainer.getParent();
      UIViewForm uiViewForm = uiTabPane.getChild(UIViewForm.class);
      UIViewPermissionList uiList = uiContainer.getChild(UIViewPermissionList.class);
      String permission = uiForm.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).getValue();
      String strPermission = uiViewForm.getPermission();
      if (Utils.isNameEmpty(permission)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.userOrGroup-required", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
        uiTabPane.setSelectedTab(uiContainer.getId());
        return;
      }
      if(strPermission.contains(permission)) {
        UIApplication app = uiForm.getAncestorOfType(UIApplication.class);
        Object[] args = { permission };
        app.addMessage(new ApplicationMessage("UIViewPermissionForm.msg.permission-exist",
                                              args,
                                              ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
        uiTabPane.setSelectedTab(uiContainer.getId());
        return;
      }
      StringBuilder strBuilder = new StringBuilder(strPermission); 
      if(strPermission.length() > 0) {
        strBuilder = strBuilder.append(",");
      } 
      strBuilder.append(permission);
      uiViewForm.setPermission(strBuilder.toString());
      uiList.refresh(uiList.getUIPageIterator().getCurrentPage());
      uiForm.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  @Override
  public void doSelect(String selectField, Object value) {
    getUIStringInput(selectField).setValue(value.toString());    
  }

  @Override
  public Node getCurrentNode() throws Exception {
    return null;
  }

}
