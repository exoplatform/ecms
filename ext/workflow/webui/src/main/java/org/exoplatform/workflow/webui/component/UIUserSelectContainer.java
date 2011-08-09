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
package org.exoplatform.workflow.webui.component;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;
import org.exoplatform.workflow.webui.component.controller.UITask;
import org.exoplatform.workflow.webui.component.controller.UITaskManager;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 12, 2009
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UIUserSelectContainer.AddUserActionListener.class)}
)

public class UIUserSelectContainer extends UIContainer{

  String fieldname;

  public UIUserSelectContainer() throws Exception {
    UIUserSelector uiUserSelector = getChild(UIUserSelector.class);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, null);
    }
    uiUserSelector.setMulti(false);
    uiUserSelector.setShowSearchGroup(true);
    uiUserSelector.setShowSearchUser(true);
    uiUserSelector.setShowSearch(true);
  }

  static  public class AddUserActionListener extends EventListener<UIUserSelectContainer> {
    public void execute(Event<UIUserSelectContainer> event) throws Exception {
      UIUserSelectContainer uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChild(UIUserSelector.class);
      UITaskManager uiTaskManager = uiUserContainer.getAncestorOfType(UITaskManager.class);
      UITask uiTask = uiTaskManager.getChild(UITask.class);
      uiTask.doSelect(uiUserContainer.getFieldname(), uiUserSelector.getSelectedUsers());
      UIPopupWindow uiPopup = uiUserContainer.getParent();
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTask);
    }
  }

  public String getFieldname() {
    return fieldname;
  }

  public void setFieldname(String fieldname) {
    this.fieldname = fieldname;
  }
}
