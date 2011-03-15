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
package org.exoplatform.wcm.webui.wiki;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Mar 4, 2009
 */

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class
)

public class UIWikiPortlet extends UIPortletApplication {

  /**
   * Instantiates a new uI wiki portlet.
   *
   * @throws Exception the exception
   */
  public UIWikiPortlet() throws Exception {
    addChild(UIWikiContentForm.class, null, null);
  }

}
