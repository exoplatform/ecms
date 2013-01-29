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
package org.exoplatform.ecm.webui.component.admin.templates.clv;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 22, 2013
 * 9:55:37 AM  
 */
@ComponentConfigs( {
  @ComponentConfig(lifecycle = UIContainerLifecycle.class),           
  @ComponentConfig(
          type = UIPopupWindow.class, id="UICLVTemplateContainer", template = "system:/groovy/webui/core/UIPopupWindow.gtmpl", 
          events = @EventConfig(listeners = UICLVTemplateContainer.CloseActionListener.class))})

public class UICLVTemplateContainer  extends UIContainer  {
  
  public UICLVTemplateContainer() throws Exception {
    addChild(UICLVTemplateList.class, null, null);
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
    UICLVTemplateList uiTemplateList = getChild(UICLVTemplateList.class);
    uiTemplateList.refresh(uiTemplateList.getUIPageIterator().getCurrentPage());
  }  
  
  public static class CloseActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UICLVTemplatesManager uiTemplateManager = event.getSource().getAncestorOfType(UICLVTemplatesManager.class) ;
      UICLVTemplateContainer uiTemplateContainer = uiTemplateManager.getChildById(uiTemplateManager.getSelectedTabId());
      UIPopupWindow uiPopupWindow = uiTemplateContainer.getChild(UIPopupWindow.class) ;
      uiPopupWindow.setRendered(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplateManager) ;
    }
  }
}
