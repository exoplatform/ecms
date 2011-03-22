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
package org.exoplatform.ecm.webui.selector;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;

/**
 * Created by The eXo Platform SAS
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@yahoo.com
 * Oct 9, 2008
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/ecm/webui/form/UIFormWithoutAction.gtmpl",
    events = {
      @EventConfig(listeners = UIAnyPermission.AddAnyPermissionActionListener.class)
    }
)
public class UIAnyPermission extends UIForm {
  static private String ANYPERMISSION = "anyPermission";

  public UIAnyPermission() throws Exception {
    UIFormInputSetWithAction rootNodeInfo = new UIFormInputSetWithAction(ANYPERMISSION);
    rootNodeInfo.addUIFormInput(new UIFormInputInfo("any", "any", null));
    String[] actionInfor = {"AddAnyPermission"};
    rootNodeInfo.setActionInfo("any", actionInfor);
    rootNodeInfo.showActionInfo(true);
    addUIComponentInput(rootNodeInfo);
  }

  static public class AddAnyPermissionActionListener extends EventListener<UIAnyPermission> {
    public void execute(Event<UIAnyPermission> event) throws Exception {
      UIAnyPermission uiAnyPermission = event.getSource();
      uiAnyPermission.<UIComponent>getParent().broadcast(event, event.getExecutionPhase()) ;
    }
  }
}
