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
package org.exoplatform.ecm.webui.component.admin.taxonomy.tree.info;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 17, 2009
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UIPermissionTreeSelectUser.AddUserActionListener.class)}
)

public class UIPermissionTreeSelectUser extends UIContainer implements UIPopupComponent  {

  public static final String USER_SELECTOR_ID = "PermissionTreeSelectUser";

  public UIPermissionTreeSelectUser() throws Exception {
    UIUserSelector uiUserSelector = getChildById(USER_SELECTOR_ID);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, USER_SELECTOR_ID);
    }
    uiUserSelector.setMulti(false);
    uiUserSelector.setShowSearchUser(true);
    uiUserSelector.setShowSearch(true);
  }

  public void activate() {

  }

  public void deActivate() {
  }

  public static class AddUserActionListener extends EventListener<UIPermissionTreeSelectUser> {
    public void execute(Event<UIPermissionTreeSelectUser> event) throws Exception {
      UIPermissionTreeSelectUser uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChildById(USER_SELECTOR_ID);
      UIPermissionTreeManager uiParent = uiUserContainer.getAncestorOfType(UIPermissionTreeManager.class);
      UIPermissionTreeForm uiPermissionForm = uiParent.getChild(UIPermissionTreeForm.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiUserSelector.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.findComponentById(UIPermissionTreeManager.POPUP_TAXONOMY_SELECT_USER);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
