/*
* Copyright (C) 2003-2014 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.exoplatform.ecm.webui.component.explorer.popup.actions;


import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;



/**
* Created by The eXo Platform SAS
* Author : Walid Khessairi
*          wkhessairi@exoplatform.com
* Aug 11, 2016
*/
@ComponentConfig(
        template =  "war:/groovy/ecm/social-integration/share-document/UIWhoHasAccessEntry.gtmpl",
        events = {
          @EventConfig(listeners = UIWhoHasAccessEntry.RemoveEntryActionListener.class),
          @EventConfig(listeners = UIWhoHasAccessEntry.ChangeEntryActionListener.class)
        }
)
public class UIWhoHasAccessEntry extends UIContainer {

  private static final Log    LOG                 = ExoLogger.getLogger(UIWhoHasAccessEntry.class);

  private static final String SHARE_PERMISSION_VIEW        = PermissionType.READ;
  private static final String SHARE_PERMISSION_MODIFY      = "modify";
  private static final String SPACE_PREFIX1 = "space::";
  private static final String SPACE_PREFIX2 = "*:/spaces/";

  private boolean permissionDropDown = false;

  public boolean hasPermissionDropDown() {
    return permissionDropDown;
  }

  public void setPermissionDropDown(boolean permissionDropDown) {
    this.permissionDropDown = permissionDropDown;
  }

  private String name;

  private String permission;


  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
     return name;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }
  public String getPermission() {
    return permission;
  }


  public static class RemoveEntryActionListener extends EventListener<UIWhoHasAccessEntry> {

    @Override
    public void execute(Event<UIWhoHasAccessEntry> event) throws Exception {
      UIWhoHasAccessEntry uiform = event.getSource();
      UIWhoHasAccess uiWhoHasAccess = uiform.getParent();
      UIShareDocuments uiShareDocuments = uiWhoHasAccess.getParent();
      String user = ConversationState.getCurrent().getIdentity().getUserId();
      if (uiShareDocuments.isOwner(user) || uiShareDocuments.getNode().getACL().getPermissions(user).contains("remove")) {
        if  (!user.equals(uiform.getId())) {
          uiWhoHasAccess.removeEntry(uiform.getId());
        } else {
          UIApplication uiApp = uiShareDocuments.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.InvalidDeletion", null,
              ApplicationMessage.WARNING));
        }
      } else {
        UIApplication uiApp = uiShareDocuments.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoPermissionDelete", null,
            ApplicationMessage.WARNING));
      }
      event.getRequestContext().getJavascriptManager()
          .require("SHARED/share-content", "shareContent")
          .addScripts("eXo.ecm.ShareContent.checkUpdatedEntry();");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiShareDocuments);
    }
  }


  public static class ChangeEntryActionListener extends EventListener<UIWhoHasAccessEntry> {

    @Override
    public void execute(Event<UIWhoHasAccessEntry> event) throws Exception {
      UIWhoHasAccessEntry uiform = event.getSource();
      if (!uiform.getPermission().equals(SHARE_PERMISSION_MODIFY)) {
        uiform.setPermission(SHARE_PERMISSION_MODIFY);
      } else {
        uiform.setPermission(SHARE_PERMISSION_VIEW);
      }
      UIWhoHasAccess uiWhoHasAccess = uiform.getParent();
      uiWhoHasAccess.updateEntry(uiform.getId(), uiform.getPermission());
      event.getRequestContext().getJavascriptManager()
          .require("SHARED/share-content", "shareContent")
          .addScripts("eXo.ecm.ShareContent.checkUpdatedEntry();");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiform.getParent().getParent());
    }
  }

  public void init(String id, String permission) {
    try {
      setName(id);
      setPermission(permission);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }
}