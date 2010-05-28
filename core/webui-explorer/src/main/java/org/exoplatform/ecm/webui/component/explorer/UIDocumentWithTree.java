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
import org.exoplatform.webui.core.UIPageIterator;


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
    getChild(UIPageIterator.class).setId("PageIteratorWithTreeView") ;
  }
  
  public String getTemplate() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    return uiExplorer.getDocumentInfoTemplate(); 
  }
}
