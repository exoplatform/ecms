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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 16, 2007
 * 11:27:32 AM
 */
@ComponentConfig (lifecycle = UIContainerLifecycle.class)
public class UIAddLanguageContainer extends UIContainer implements UIPopupComponent {

  public String nodeTypeName_ = null;

  public UIAddLanguageContainer() throws Exception {
    addChild(UILanguageTypeForm.class, null, null) ;
  }

  public void initPopup(UIComponent uiComp) throws Exception {
    removeChildById("PopupComponent") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "PopupComponent") ;
    uiPopup.setShowMask(true);
    uiPopup.setUIComponent(uiComp) ;
    uiPopup.setWindowSize(640, 300) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void setComponentDisplay(String nodeTypeName) throws Exception {
    nodeTypeName_ = nodeTypeName ;
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ;
    UILanguageDialogForm uiDialogForm = createUIComponent(UILanguageDialogForm.class, null, null) ;
    uiDialogForm.setTemplateNode(nodeTypeName) ;
    //  uiDialogForm.setNode(currentNode);
    uiDialogForm.setNodePath(currentNode.getPath()) ;
    uiDialogForm.setWorkspace(currentNode.getSession().getWorkspace().getName()) ;
    uiDialogForm.setStoredPath(currentNode.getPath()) ;
    addChild(uiDialogForm) ;
  }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}
}
