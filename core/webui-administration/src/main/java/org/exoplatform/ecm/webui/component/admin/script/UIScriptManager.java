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
package org.exoplatform.ecm.webui.component.admin.script;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 27, 2006
 * 09:13:15 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")

public class UIScriptManager extends UIAbstractManager {

  public UIScriptManager() throws Exception {
    addChild(UIECMScripts.class, null , null) ;
  }

  public void refresh()throws Exception {
    getChild(UIECMScripts.class).refresh(1);
  }

  public void removeECMScripForm() {
    getChild(UIECMScripts.class).removeChild(UIPopupWindow.class) ;
  }

}
