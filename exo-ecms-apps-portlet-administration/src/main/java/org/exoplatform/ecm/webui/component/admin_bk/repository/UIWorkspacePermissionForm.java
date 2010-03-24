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
package org.exoplatform.ecm.webui.component.admin.repository;

import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * 02-07-2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIWorkspacePermissionForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIWorkspacePermissionForm.CancelActionListener.class)
    }
)
public class UIWorkspacePermissionForm extends UIForm implements UISelectable {
  final static public String FIELD_PERMISSION = "permission" ;

  public UIWorkspacePermissionForm() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_PERMISSION, FIELD_PERMISSION, null).addValidator(MandatoryValidator.class).setEditable(false)) ;
    for (String perm : PermissionType.ALL) {
      addUIFormInput(new UIFormCheckBoxInput<String>(perm, perm, null)) ;
    }

  }
  protected boolean isCheckedAny() {
    for(String perm : PermissionType.ALL) {
      if(getUIFormCheckBoxInput(perm).isChecked()) return true ;
    }
    return false ;
  }

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    getUIStringInput(FIELD_PERMISSION).setValue(value.toString()) ;
    checkAll(false) ;
  }

  public void reset() {
    getUIStringInput(FIELD_PERMISSION).setValue(null) ;
    checkAll(false) ;
  }
  private void checkAll(boolean check) {
    for(String perm : PermissionType.ALL) {
      getUIFormCheckBoxInput(perm).setChecked(check) ;
    }
  }
  
  protected void lockForm(boolean lock) {
    boolean editable = !lock ;
    UIPermissionContainer uiContainer = getAncestorOfType(UIPermissionContainer.class) ;
    uiContainer.getChild(UIPermissionSelector.class).setRendered(editable) ;
    getUIStringInput(FIELD_PERMISSION).setEditable(false) ;
    for(String perm : PermissionType.ALL) {
      getUIFormCheckBoxInput(perm).setEnable(editable) ;
    }
    if(!editable) setActions(new String[]{"Cancel"}) ; 
    else {setActions(new String[]{"Save", "Cancel"}); } 
  }
  
  public static class SaveActionListener extends EventListener<UIWorkspacePermissionForm> {
    public void execute (Event<UIWorkspacePermissionForm> event) throws Exception {
      UIWorkspacePermissionForm uiForm =  event.getSource();
      UIWorkspaceWizardContainer uiWizardContainer = uiForm.getAncestorOfType(UIWorkspaceWizardContainer.class) ;
      UIWorkspaceWizard uiWizardForm = uiWizardContainer.getChild(UIWorkspaceWizard.class) ;
      String user = uiForm.getUIStringInput(UIWorkspacePermissionForm.FIELD_PERMISSION).getValue() ;
      if(!uiForm.isCheckedAny()) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIWorkspacePermissionForm.msg.check-one", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      StringBuilder sb = new StringBuilder() ;
      for(String perm : PermissionType.ALL) {
        if(uiForm.getUIFormCheckBoxInput(perm).isChecked()) sb.append(user +" "+ perm + ";") ;
      }
      if(uiForm.getUIFormCheckBoxInput(PermissionType.ADD_NODE).isChecked() ||
          uiForm.getUIFormCheckBoxInput(PermissionType.REMOVE).isChecked() || 
          uiForm.getUIFormCheckBoxInput(PermissionType.SET_PROPERTY).isChecked())
      {
        String readperm = user +" "+ PermissionType.READ + ";" ;
        if(!sb.toString().contains(readperm))
          sb.append(readperm) ;
      }
      UIWizardStep1 ws1 = uiWizardForm.getChildById(UIWorkspaceWizard.FIELD_STEP1) ;
      ws1.addPermissions(user, sb.toString()) ;
      ws1.refreshPermissionList() ;
      UIPopupContainer uiPopup = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardForm) ;
    }

  }

  public static class CancelActionListener extends EventListener<UIWorkspacePermissionForm> {
    public void execute(Event<UIWorkspacePermissionForm> event) throws Exception {
      UIPopupContainer uiPopup = event.getSource().getAncestorOfType(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }

}
