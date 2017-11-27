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
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 14, 2009
 * 5:00:55 PM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UITagUserContainer.AddUserActionListener.class)}
)
public class UITagUserContainer extends UIContainer implements UIPopupComponent {

  public UITagUserContainer() throws Exception {
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

  static  public class AddUserActionListener extends EventListener<UITagUserContainer> {
    public void execute(Event<UITagUserContainer> event) throws Exception {
      UITagUserContainer uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChild(UIUserSelector.class);
      UITagPermissionManager uiParent = uiUserContainer.getAncestorOfType(UITagPermissionManager.class);
      UITagPermissionForm uiPermissionForm = uiParent.getChild(UITagPermissionForm.class);
      uiPermissionForm.doSelect(UITagPermissionInputSet.FIELD_USERORGROUP, uiUserSelector.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.findComponentById("PopupUserSelector");
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }

}
