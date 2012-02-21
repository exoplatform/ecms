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

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.selector.UIAnyPermission;
import org.exoplatform.ecm.webui.selector.UIGroupMemberSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
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
    template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UITagPermissionForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UITagPermissionForm.SelectMemberActionListener.class)
    }
)
public class UITagPermissionForm extends UIForm implements UISelectable {
  final static public String PERMISSION   = "permission";

  final static public String POPUP_SELECT = "SelectUserOrGroup";
  private static final Log LOG  = ExoLogger.getLogger(UITagPermissionForm.class);

  public UITagPermissionForm() throws Exception {
    addChild(new UITagPermissionInputSet(PERMISSION));
    setActions(new String[] {"Save"});
  }

  static public class SaveActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
       UITagPermissionForm uiForm = event.getSource();
       UITagPermissionManager uiParent = uiForm.getParent();
       UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
       String userOrGroup = uiForm.getChild(UITagPermissionInputSet.class).getUIStringInput(
           UITagPermissionInputSet.FIELD_USERORGROUP).getValue();
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
      uiForm.getChild(UITagPermissionInputSet.class).getChild(UIFormStringInput.class).setValue("");
    }
  }

  static public class SelectMemberActionListener extends EventListener<UITagPermissionForm> {
    public void execute(Event<UITagPermissionForm> event) throws Exception {
      UITagPermissionForm uiForm = event.getSource();
      UIGroupMemberSelector uiGroupMemberSelector = uiForm.createUIComponent(UIGroupMemberSelector.class, null, null);
      uiGroupMemberSelector.setSourceComponent(uiForm, new String[] { UITagPermissionInputSet.FIELD_USERORGROUP });
      uiGroupMemberSelector.setShowAnyPermission(false);
      uiGroupMemberSelector.removeChild(UIAnyPermission.class);
      uiForm.getAncestorOfType(UITagPermissionManager.class).initPopupPermission(uiGroupMemberSelector);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
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

}
