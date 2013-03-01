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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import javax.jcr.Node;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.core.UIPermissionFormBase;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 14, 2009
 * 10:02:37 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/tags/UITagPermissionForm.gtmpl",
    events = {
      @EventConfig(listeners = UITagPermissionForm.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UITagPermissionForm.SelectMemberActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UITagPermissionForm.SelectUserActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UITagPermissionForm.AddAnyActionListener.class)
    }
)
public class UITagPermissionForm extends UIPermissionFormBase implements UISelectable{
  final static public String TAGS_PERMISSION   = "tags_permission";

  final static public String POPUP_SELECT = "SelectUserOrGroup";
  private static final Log LOG  = ExoLogger.getLogger(UITagPermissionForm.class.getName());

  public UITagPermissionForm() throws Exception {
    removeChildById(UIPermissionFormBase.PERMISSION);
    addChild(new UIPermissionInputSet(TAGS_PERMISSION, false));
    UIPermissionInputSet uiPerInputset = getChildById(TAGS_PERMISSION);
    uiPerInputset.setButtonActions(new String[] {"Add"});
    uiPerInputset.setPrimaryButtonAction("Add");
    uiPerInputset.setActionInfo(UIPermissionInputSet.FIELD_USERORGROUP, new String[] {"SelectUser", "SelectMember", "AddAny"});
  }

  static public class AddActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
       UITagPermissionForm uiForm = event.getSource();
       UITagPermissionManager uiParent = uiForm.getParent();
       UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
       String userOrGroup = uiForm.getChild(UIPermissionInputSet.class).getUIStringInput(
           UIPermissionInputSet.FIELD_USERORGROUP).getValue();
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);

      if (Utils.isNameEmpty(userOrGroup)) {
        uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.userOrGroup-required", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }
      if (newFolksonomyService.getTagPermissionList().contains(userOrGroup)) {
        uiApp.addMessage(new ApplicationMessage("UITagPermissionForm.msg.userOrGroup-alreadyExists", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
        return;
      }

      newFolksonomyService.addTagPermission(userOrGroup);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
      uiForm.getChild(UIPermissionInputSet.class).getChild(UIFormStringInput.class).setValue("");
    }
  }

  static public class SelectMemberActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
      UITagPermissionForm uiForm = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiForm.createUIComponent(UIGroupMemberSelector.class, null, null);
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
  static public class SelectUserActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
      UITagPermissionForm uiForm = event.getSource();
      ((UIPermissionManagerBase)uiForm.getParent()).initUserSelector();
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }
  static public class AddAnyActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
      UITagPermissionForm uiForm = event.getSource();
      UIPermissionInputSet uiInputSet = uiForm.getChildById(UITagPermissionForm.TAGS_PERMISSION);
      uiInputSet.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(IdentityConstants.ANY);
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      }
    }
  }

  public void doSelect(String selectField, Object value) {
    try {
      getUIStringInput(selectField).setValue(value.toString());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  @Override
  public Node getCurrentNode() throws Exception {
    //Nothing to do with get current node in tags permissions form
    return null;
  }
}
