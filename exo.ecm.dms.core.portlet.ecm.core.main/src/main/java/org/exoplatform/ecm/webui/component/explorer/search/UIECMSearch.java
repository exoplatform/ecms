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

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : le bien thuy  
 *          lebienthuyt@gmail.com
 * Oct 2, 2006
 * 10:08:51 AM 
 * Editor: pham tuan Oct 27, 2006
 */

@ComponentConfig( template = "system:/groovy/webui/core/UITabPane.gtmpl" )
public class UIECMSearch extends UIContainer implements UIPopupComponent {
  
  static public String ADVANCED_RESULT = "AdvancedSearchResult" ;
  
  public UIECMSearch() throws Exception {
    addChild(UIContentNameSearch.class,null,null);
    addChild(UISearchContainer.class, null, null).setRendered(false) ;
    addChild(UIJCRAdvancedSearch.class, null, null).setRendered(false);
    addChild(UISavedQuery.class, null, null).setRendered(false) ;
    UISearchResult uiSearchResult = addChild(UISearchResult.class, null, ADVANCED_RESULT).setRendered(false) ;
    UIQueryResultPageIterator uiPageIterator = uiSearchResult.getChild(UIQueryResultPageIterator.class) ;
    uiPageIterator.setId("AdvanceSearchIterator") ;
  }

  public void activate() throws Exception {
    UIJCRAdvancedSearch advanceSearch = getChild(UIJCRAdvancedSearch.class);
    advanceSearch.update(null);
    UISavedQuery uiQuery = getChild(UISavedQuery.class);
    uiQuery.setRepositoryName(getAncestorOfType(UIJCRExplorer.class).getRepositoryName()) ;
    uiQuery.updateGrid(1);
  }

  public void deActivate() throws Exception {
  }
}