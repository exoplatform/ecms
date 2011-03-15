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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 25, 2009
 * 11:33:01 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/admin/folksonomy/UITagStyleAddAction.gtmpl"
    )
public class UITagStyleAddAction extends UIComponent {

  private static String[] ACTIONS = {"AddStyle"} ;

  public UITagStyleAddAction() {
    super();
  }

  public String[] getActions() {
    return ACTIONS;
  }

}
