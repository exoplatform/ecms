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

package org.exoplatform.ecm.webui.component.admin.taxonomy.info;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 3, 2008
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UIUserContainer.AddUserActionListener.class)}
)

public class UIUserContainer extends UIContainer implements UIPopupComponent  {

  public UIUserContainer() throws Exception {
    UIUserSelector uiUserSelector = getChild(UIUserSelector.class);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, null);
    }
    uiUserSelector.setMulti(false);
    uiUserSelector.setShowSearchUser(true);
    uiUserSelector.setShowSearch(true);
  }

  public void activate() {

  }

  public void deActivate() {

  }

  static  public class AddUserActionListener extends EventListener<UIUserContainer> {
    public void execute(Event<UIUserContainer> event) throws Exception {
      UIUserContainer uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChild(UIUserSelector.class);
      UIPermissionManager uiParent = uiUserContainer.getAncestorOfType(UIPermissionManager.class);
      UIPermissionForm uiPermissionForm = uiParent.getChild(UIPermissionForm.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiUserSelector.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.findComponentById("PopupUserSelector");
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
