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
package org.exoplatform.ecm.webui.component.admin.namespace;


import org.exoplatform.webui.config.annotation.ComponentConfig;
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
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UINamespaceManager extends UIAbstractManager {

  public UINamespaceManager() throws Exception {addChild(UINamespaceList.class, null, null) ;}

  public void refresh ()throws Exception {
    UINamespaceList list = getChild(UINamespaceList.class) ;
    list.refresh(1);
  }

  public void initPopup() throws Exception {
    UIPopupWindow uiPopup = getChild(UIPopupWindow.class) ;
    if(uiPopup == null) {      
      uiPopup = addChild(UIPopupWindow.class, null, "NamespacePopup") ;
      uiPopup.setWindowSize(600,0) ;
      UINamespaceForm uiNamespaceForm = createUIComponent(UINamespaceForm.class, null, null) ;
      uiPopup.setUIComponent(uiNamespaceForm) ;
      uiPopup.setShow(true) ;
      uiPopup.setResizable(true) ;
      uiPopup.setShowMask(true);
      return ;
    }
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
  }
}
