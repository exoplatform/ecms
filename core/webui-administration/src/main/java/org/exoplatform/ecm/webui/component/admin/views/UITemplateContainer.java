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
package org.exoplatform.ecm.webui.component.admin.views;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 23, 2006
 * 3:47:44 PM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UITemplateContainer extends UIContainer {

  public UITemplateContainer() throws Exception {
  }

  public void initPopup(String compId, String type) throws Exception {
    String popupId = compId + type ;
    removeChildById(popupId) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600,400) ;
    UITemplateForm uiTempForm = createUIComponent(UITemplateForm.class, null, compId) ;
    if(type.equals("Add")) {
      uiTempForm.isAddNew_ = true ;
      uiTempForm.setActions(new String[]{"Save", "Reset", "Cancel"}) ;
    } else if(type.equals("Edit")) {
      uiTempForm.isAddNew_ = false ;
      uiTempForm.setActions(new String[]{"Save", "Cancel"}) ;
    }
    uiTempForm.updateOptionList() ;
    uiPopup.setUIComponent(uiTempForm) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
