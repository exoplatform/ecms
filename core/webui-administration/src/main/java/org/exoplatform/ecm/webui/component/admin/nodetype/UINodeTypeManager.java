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
package org.exoplatform.ecm.webui.component.admin.nodetype;

import org.exoplatform.ecm.webui.nodetype.selector.UINodeTypeSearch;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 20, 2006
 * 2:20:55 PM
 */
@ComponentConfig (
    lifecycle = UIContainerLifecycle.class
)
public class UINodeTypeManager extends UIAbstractManager {

  final static public String IMPORT_POPUP = "NodeTypeImportPopup" ;
  final static public String EXPORT_POPUP = "NodeTypeExportPopup" ;

  public UINodeTypeManager() throws Exception {
    addChild(UINodeTypeSearchForm.class, null, "NodeTypeSearchForm") ;
    addChild(UINodeTypeList.class, null, "ListNodeType") ;
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    UINodeTypeSearch uiNodeTypeSearch = getChild(UINodeTypeSearchForm.class).getChild(UINodeTypeSearch.class);
    uiNodeTypeSearch.init();
    UINodeTypeList uiNodeTypeList = getChild(UINodeTypeList.class);
    uiNodeTypeList.refresh(uiNodeTypeList.getUIPageIterator().getCurrentPage());
  }
  public void setExportPopup() throws Exception {
    removeChildById(EXPORT_POPUP) ;
    UIPopupWindow  uiPopup = addChild(UIPopupWindow.class, null, EXPORT_POPUP);
    uiPopup.setWindowSize(500, 400);
    UINodeTypeExport uiExport = uiPopup.createUIComponent(UINodeTypeExport.class, null, null) ;
    uiExport.update() ;
    uiPopup.setUIComponent(uiExport) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void setImportPopup() throws Exception {
    removeChildById(IMPORT_POPUP) ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, IMPORT_POPUP);
    uiPopup.setWindowSize(500, 400);
    UINodeTypeImportPopup uiImportPopup =
      uiPopup.createUIComponent(UINodeTypeImportPopup.class, null, null) ;
    uiPopup.setUIComponent(uiImportPopup) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public void initPopup(boolean isView) throws Exception {
    String popupId = "NodeTypePopup" ;
    if(isView) popupId = "ViewNodeTypePopup" ;
    removeChildById("NodeTypePopup") ;
    removeChildById("ViewNodeTypePopup") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, popupId) ;
    uiPopup.setShowMask(true);
    UINodeTypeForm uiForm = createUIComponent(UINodeTypeForm.class, null, null) ;
    uiForm.update(null, false) ;
    uiPopup.setWindowSize(660, 400) ;
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }
}
