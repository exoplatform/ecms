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
package org.exoplatform.ecms.personalfolder.component;

import org.exoplatform.ecms.personalfolder.services.ManagePersonalFolderServiceImpl;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 9, 2012
 * 3:48:12 PM  
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
  @EventConfig(listeners = UIPersonalFolderForm.SaveActionListener.class) })
public class UIPersonalFolderForm extends UIForm {

  final static private String USERNAME_FIELD = "userName";
  
  public UIPersonalFolderForm() throws Exception {
    addUIFormInput(new UIFormStringInput(USERNAME_FIELD, USERNAME_FIELD, null));
  }

  private void installlPersonalFolder(String userName) {
    ManagePersonalFolderServiceImpl manageFolderSer = WCMCoreUtils.getService(ManagePersonalFolderServiceImpl.class);
    manageFolderSer.initUserFolder(userName);
  }
  
  private boolean isExisting(String userName) throws Exception {
    OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
    if(organizationService.getUserHandler().findUserByName(userName) == null) return false;
    return true;
  }
  
  public static class SaveActionListener extends EventListener<UIPersonalFolderForm> {
    public void execute(Event<UIPersonalFolderForm> event) throws Exception {
      UIPersonalFolderForm uiForm = event.getSource();
      String userName = uiForm.getUIStringInput(USERNAME_FIELD).getValue();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if(!uiForm.isExisting(userName)) {
        uiApp.addMessage(new ApplicationMessage("UIPersonalFolderForm.msg.notExisting", new String[] {userName},
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp);
        return;
      }
      uiForm.installlPersonalFolder(userName);
      uiApp.addMessage(new ApplicationMessage("UIPersonalFolderForm.msg.initSuccessed", new String[] {userName},
          ApplicationMessage.INFO)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp);
    }
  }

}
