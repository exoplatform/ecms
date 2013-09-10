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
package org.exoplatform.ecm.webui.component.admin.script;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * Jan 27, 2013
 * 2:09:18 PM
 */
@ComponentConfigs( {
  @ComponentConfig(lifecycle = UIContainerLifecycle.class) })

public class UIScriptContainer extends UIContainer {

  public UIScriptContainer() throws Exception {
    addChild(UIScriptList.class, null, null) ;
  }

  public void initPopup(UIComponent uiComponent, String popupId) throws Exception {
    removeChildById(popupId) ;    
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setShowMask(true);    
    uiPopup.setWindowSize(600,300) ;
    uiPopup.setUIComponent(uiComponent) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void update() throws Exception {
    UIScriptList uiScriptList = getChild(UIScriptList.class);
    uiScriptList.refresh(uiScriptList.getTemplateFilter(), uiScriptList.getUIPageIterator().getCurrentPage());
  }  
}
