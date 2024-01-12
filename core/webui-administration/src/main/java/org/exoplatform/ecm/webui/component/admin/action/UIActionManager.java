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
package org.exoplatform.ecm.webui.component.admin.action;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 20, 2006
 * 16:37:15 AM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)

public class UIActionManager extends UIAbstractManager {

  public UIActionManager() throws Exception {
    addChild(UIActionTypeList.class, null, null) ;
  }

  public void refresh() throws Exception {
    UIActionTypeList list = getChild(UIActionTypeList.class) ;
    list.refresh(1);
  }
  
  public String getScriptLabel(NodeType nodeType) throws Exception {
    if(nodeType.isNodeType("exo:scriptAction")) {
      PropertyDefinition[] arrProperties = nodeType.getPropertyDefinitions();
      for(PropertyDefinition property : arrProperties) {
        if(property.getName().equals("exo:scriptLabel")) {
          return property.getDefaultValues()[0].getString();
        }
      }
    }
    return StringUtils.EMPTY;
  }  

  public void initPopup(UIComponent uiActionForm, int width) throws Exception {
    UIPopupWindow uiPopup = getChild(UIPopupWindow.class) ;
    if(uiPopup == null) {
      uiPopup = addChild(UIPopupWindow.class, null, "ActionPopup") ;
      uiPopup.setUIComponent(uiActionForm) ;
      uiPopup.setWindowSize(width, 0) ;
      uiPopup.setShow(true) ;
      return ;
    }
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
