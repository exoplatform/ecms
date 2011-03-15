/*
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
 */
package org.exoplatform.wcm.webui.dialog;

import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.permission.UIPermissionManager;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 29, 2009
 */
@ComponentConfig (
  template = "system:/groovy/webui/form/ext/UITabPaneWithAction.gtmpl",
  events = {
    @EventConfig(listeners = UIContentDialogPreference.BackActionListener.class)
  }
)
public class UIContentDialogPreference extends UITabPane {

  public String[] getActions() {
    return new String[] {"Back"};
  }

  public void init() throws Exception {
    UIPermissionManager permissionManager = addChild(UIPermissionManager.class, null, null);
    permissionManager.updateGrid();
    setSelectedTab(permissionManager.getId());
  }

  public static class BackActionListener extends EventListener<UIContentDialogPreference> {

    public void execute(Event<UIContentDialogPreference> event) throws Exception {
      UIContentDialogPreference contentDialogPreference = event.getSource();
      UIPopupContainer popupContainer = contentDialogPreference.getAncestorOfType(UIPopupContainer.class);
      UIContentDialogForm contentDialogForm = popupContainer.getChild(UIContentDialogForm.class);
      popupContainer.removeChildById(contentDialogForm.getId());
      Utils.updatePopupWindow(contentDialogPreference, contentDialogForm, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }
}
