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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 11, 2007
 * 2:22:08 PM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")
public class UIFolksonomyManager extends UIAbstractManager {

  public UIFolksonomyManager() throws Exception {
    addChild(UITagManager.class, null, null);
    addChild(UITagPermissionManager.class, null, null).setRendered(false);
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    getChild(UITagManager.class).update();
  }

}
