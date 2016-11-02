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
package org.exoplatform.ecm.webui.component.explorer ;

import org.exoplatform.ecm.webui.component.explorer.lifecycle.UIDocumentWorkspaceLifeCycle;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.ecm.webui.component.explorer.versions.UIDiff;
import org.exoplatform.ecm.webui.component.explorer.versions.UIVersionInfo;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : tran the  trong
 *          trongtt@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */
@ComponentConfig(lifecycle = UIDocumentWorkspaceLifeCycle.class)
public class UIDocumentWorkspace extends UIContainer {

  static public String SIMPLE_SEARCH_RESULT = "SimpleSearchResult" ;
  public UIDocumentWorkspace() throws Exception {
    addChild(UIDocumentContainer.class, null, null) ;
    addChild(UISearchResult.class, null, SIMPLE_SEARCH_RESULT).setRendered(false);
    addChild(UIVersionInfo.class, null, null).setRendered(false);
    addChild(UIDiff.class, null, null).setRendered(false);
  }
}
