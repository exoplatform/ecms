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
package org.exoplatform.ecm.webui.component.explorer.search;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UITabPane;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : le bien thuy
 *          lebienthuyt@gmail.com
 * Oct 2, 2006
 * 10:08:51 AM
 * Editor: pham tuan Oct 27, 2006
 */

@ComponentConfig(template = "system:/groovy/webui/core/UITabPane_New.gtmpl", 
                 events = { @EventConfig(listeners = UIECMSearch.SelectTabActionListener.class) })
public class UIECMSearch extends UITabPane implements UIPopupComponent {

  static public String ADVANCED_RESULT = "AdvancedSearchResult" ;

  public UIECMSearch() throws Exception {
    addChild(UIContentNameSearch.class,null,null);
    setSelectedTab("UIContentNameSearch");
    addChild(UISearchContainer.class, null, null) ;
    addChild(UIJCRAdvancedSearch.class, null, null);
    addChild(UISavedQuery.class, null, null);
    UISearchResult uiSearchResult = addChild(UISearchResult.class, null, ADVANCED_RESULT);
    UIPageIterator uiPageIterator = uiSearchResult.getChild(UIPageIterator.class) ;
    uiPageIterator.setId("AdvanceSearchIterator") ;
  }

  public void activate() throws Exception {
    UIJCRAdvancedSearch advanceSearch = getChild(UIJCRAdvancedSearch.class);
    advanceSearch.update(null);
    UISavedQuery uiQuery = getChild(UISavedQuery.class);
    uiQuery.updateGrid(1);
  }

  public void deActivate() throws Exception {
  }
  
  static public class SelectTabActionListener extends EventListener<UIECMSearch>
  {
     public void execute(Event<UIECMSearch> event) throws Exception
     {
        WebuiRequestContext context = event.getRequestContext();
        String renderTab = context.getRequestParameter(UIComponent.OBJECTID);
        if (renderTab == null)
           return;
        event.getSource().setSelectedTab(renderTab);
        context.setResponseComplete(true);
        context.addUIComponentToUpdateByAjax(event.getSource().getParent());
     }
  }
}
