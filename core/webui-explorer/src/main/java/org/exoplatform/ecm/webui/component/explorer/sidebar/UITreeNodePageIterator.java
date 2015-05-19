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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.Set;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.pham@exoplatform.com
 * Sep 26, 2007
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/sidebar/UITreeNodePageIterator.gtmpl",
    events = @EventConfig(listeners = UITreeNodePageIterator.ShowPageActionListener.class )
)
public class UITreeNodePageIterator extends UIPageIterator {
  private String selectedPath_ ;

  public UITreeNodePageIterator() {
  }

  public String getSelectedPath() { return selectedPath_ ; }
  public void setSelectedPath(String path) { this.selectedPath_ = path ; }
  static  public class ShowPageActionListener extends EventListener<UITreeNodePageIterator> {
    public void execute(Event<UITreeNodePageIterator> event) throws Exception {
      UITreeNodePageIterator uiPageIterator = event.getSource() ;
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      if(uiPageIterator.getAvailablePage() < page) page = uiPageIterator.getAvailablePage();	    
      uiPageIterator.setCurrentPage(page) ;
      if(uiPageIterator.getParent() == null) return ;
      UIJCRExplorer uiExplorer = uiPageIterator.getAncestorOfType(UIJCRExplorer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
      UIDocumentWorkspace uiDocumentWorkspace = uiExplorer.findFirstComponentOfType(UIDocumentWorkspace.class) ;
      UISearchResult uiSearchResult = uiDocumentWorkspace.getChild(UISearchResult.class) ;
      if(uiSearchResult.isRendered()) return ;
      UIDocumentContainer uiDocumentContainer = uiDocumentWorkspace.getChild(UIDocumentContainer.class);
      UIDocumentInfo uiDocumentInfo = null ;
      if(uiExplorer.isShowViewFile()) {
        uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentWithTree") ;
      } else {
        Set<String> allItemByTypeFilterMap = uiExplorer.getAllItemByTypeFilterMap();
        if (allItemByTypeFilterMap.size() > 0)
          uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentWithTree");
        else
          uiDocumentInfo = uiDocumentContainer.getChildById("UIDocumentInfo");
      }

      if (uiDocumentInfo == null) return;

      String currentPath = uiExplorer.getCurrentNode().getPath();
      if(!currentPath.equalsIgnoreCase(uiPageIterator.getSelectedPath())) return ;

      UIPageIterator iterator = uiDocumentInfo.getContentPageIterator();
      if(iterator.getAvailablePage() >= page) iterator.setCurrentPage(page);

      if (uiDocumentWorkspace.isRendered() && uiDocumentContainer.isRendered() && uiDocumentInfo.isRendered()) {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentInfo);
      }
    }
  }
}
