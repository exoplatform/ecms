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

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.selector.UIPermissionSelector;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * 02-07-2007  
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIPermissionContainer  extends UIContainer implements UIPopupComponent {

  public UIPermissionContainer() throws Exception {
    UIPermissionSelector uiECMPermission = 
      addChild(UIPermissionSelector.class, null, "PermissionPopupSelect") ;
    UIWorkspacePermissionForm uiWsPermissionForm = addChild(UIWorkspacePermissionForm.class, null, null) ;
    uiECMPermission.setSelectedMembership(true);
    uiECMPermission.setIsUsePopup(false) ;
    uiECMPermission.setSourceComponent(uiWsPermissionForm, null) ;
  }

  protected void setValues(String user, String permission) {    
    UIWorkspacePermissionForm uiWsPremForm = getChild(UIWorkspacePermissionForm.class) ;
    uiWsPremForm.reset() ;
    UIFormStringInput permissionField =  uiWsPremForm.getUIStringInput(UIWorkspacePermissionForm.FIELD_PERMISSION) ;
    permissionField.setValue(user) ;
    if( permission != null) {
      for(String perm : PermissionType.ALL) {
        uiWsPremForm.getUIFormCheckBoxInput(perm).setChecked(permission.contains(perm)) ;
      }
    }
  }
  protected void lockForm(boolean lock) {
    getChild(UIWorkspacePermissionForm.class).lockForm(lock) ;
  }
  protected void disposePopup() {}

  public void activate() throws Exception {

  }

  public void deActivate() throws Exception {
    getChild(UIWorkspacePermissionForm.class).reset() ;
  }
}
