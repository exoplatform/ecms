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
package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;


/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 15, 2007 10:10:03 AM
 */
@ComponentConfig(
    events = {
        @EventConfig(listeners = UIDocumentInfo.ChangeNodeActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ViewNodeActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SortActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.VoteActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ChangeLanguageActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.DownloadActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ShowPageActionListener.class)
    }
)
public class UIDocumentWithTree extends UIDocumentInfo {

  public UIDocumentWithTree() throws Exception {
    getChildById(CONTENT_PAGE_ITERATOR_ID).setId("PageIteratorWithTreeView");
    getChildById(CONTENT_TODAY_PAGE_ITERATOR_ID).setId("TodayPageIteratorWithTreeView");
    getChildById(CONTENT_YESTERDAY_PAGE_ITERATOR_ID).setId("YesterdayPageIteratorWithTreeView");
    getChildById(CONTENT_WEEK_PAGE_ITERATOR_ID).setId("WeekPageIteratorWithTreeView");
    getChildById(CONTENT_MONTH_PAGE_ITERATOR_ID).setId("MonthPageIteratorWithTreeView");
    getChildById(CONTENT_YEAR_PAGE_ITERATOR_ID).setId("YearPageIteratorWithTreeView");    
  }

  public String getTemplate() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return uiExplorer.getDocumentInfoTemplate();
  }
}
