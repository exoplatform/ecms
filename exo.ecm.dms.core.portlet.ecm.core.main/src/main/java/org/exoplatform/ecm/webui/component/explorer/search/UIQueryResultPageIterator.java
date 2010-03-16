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

import java.util.ArrayList;

import org.exoplatform.ecm.jcr.model.ExtensiblePageList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 12, 2008  
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/search/UIQueryResultPageIterator.gtmpl",
    events = @EventConfig(listeners = UIQueryResultPageIterator.ShowPageActionListener.class )    
)
public class UIQueryResultPageIterator extends UIPageIterator {

  private ExtensiblePageList searchRsultPageList_ = new SearchResultPageList(null, new ArrayList(), 10, false);
  
  public void  setSearchResultPageList(SearchResultPageList resultPageList) {
    searchRsultPageList_ = resultPageList ;
  }
  
  public long getEstimateNumberOfNodes() { return searchRsultPageList_.getPageNumberEstimate() ; }
  
  public int getRealNumberOfNodes() { return searchRsultPageList_.getRealNumberNodes() ; }
  
  public boolean isEndOfIterator() { return searchRsultPageList_.isEndOfIterator() ; }
  
  static  public class ShowPageActionListener extends EventListener<UIQueryResultPageIterator> {
    public void execute(Event<UIQueryResultPageIterator> event) throws Exception {
      UIQueryResultPageIterator uiPageIterator = event.getSource() ;
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      UISearchResult uiSearchResult = uiPageIterator.getParent() ;
      int totalPageInThisIterator = uiSearchResult.getCurrentAvaiablePage() ; 
      if(totalPageInThisIterator - page <= 2) uiSearchResult.updateGrid(false) ;
      uiPageIterator.setCurrentPage(page) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult);
    }
  }
}
