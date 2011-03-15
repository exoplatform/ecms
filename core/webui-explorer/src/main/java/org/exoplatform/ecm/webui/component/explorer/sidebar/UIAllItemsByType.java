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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.List;
import java.util.Set;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 29, 2009
 * 7:08:57 AM
 */
@ComponentConfig(
  template = "app:/groovy/webui/component/explorer/sidebar/UIAllItemsByType.gtmpl",
  events = {
    @EventConfig(listeners = UIAllItemsByType.ShowDocumentTypeActionListener.class),
    @EventConfig(listeners = UIAllItemsByType.DocumentFilterActionListener.class)
  }
)

public class UIAllItemsByType extends UIComponent {

  public UIAllItemsByType() {
  }

  public List<String> getAllSupportedType() {
    DocumentTypeService documentTypeService = getApplicationComponent(DocumentTypeService.class);
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiJCRExplorer.isFilterSave())
      return uiJCRExplorer.getCheckedSupportType();
    return documentTypeService.getAllSupportedType();
  }

  static public class ShowDocumentTypeActionListener extends EventListener<UIAllItemsByType> {
    public void execute(Event<UIAllItemsByType> event) throws Exception {
      UIAllItemsByType uiViewDocumentTypes = event.getSource();
      String supportedType = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = uiViewDocumentTypes.getAncestorOfType(UIJCRExplorer.class);
      Set<String> allItemByTypeFilterMap = uiExplorer.getAllItemByTypeFilterMap();
      if (allItemByTypeFilterMap.contains(supportedType)) {
        allItemByTypeFilterMap.remove(supportedType);
      } else {
        allItemByTypeFilterMap.add(supportedType);
      }
//      uiExplorer.setSupportedType(supportType);
//      uiExplorer.setViewDocument(true);
      uiExplorer.setIsViewTag(false);
//      uiExplorer.setSelectRootNode();

//      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
//      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
//      if(uiDocumentWorkspace.isRendered()) {
//        UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class) ;
//        UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
//        uiDocumentInfo.setDocumentSourceType(DocumentProviderUtils.CURRENT_NODE_ITEMS);
//      }
      uiExplorer.updateAjax(event);
    }
  }

  static public class DocumentFilterActionListener extends EventListener<UIAllItemsByType> {
    public void execute(Event<UIAllItemsByType> event) throws Exception {
      UIAllItemsByType uiSideBar = event.getSource();
      UIJCRExplorer uiJCRExplorer = uiSideBar.getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer popupAction = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIDocumentFilterForm uiDocumentFilter = popupAction.activate(UIDocumentFilterForm.class,300);
      uiDocumentFilter.invoke(uiSideBar.getAllSupportedType());

//      UIWorkingArea uiWorkingArea = uiJCRExplorer.getChild(UIWorkingArea.class);
//      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
//      if(uiDocumentWorkspace.isRendered()) {
//        UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class) ;
//        UIDocumentInfo uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo") ;
//        uiDocumentInfo.setDocumentSourceType(DocumentProviderUtils.CURRENT_NODE_ITEMS);
//      }
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
}
