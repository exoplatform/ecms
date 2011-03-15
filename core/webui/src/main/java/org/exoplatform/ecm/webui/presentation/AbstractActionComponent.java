/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.presentation;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Sep 18, 2009
 */
public abstract class AbstractActionComponent extends UIComponent {

  private List<Class> lstComponentupdate = new ArrayList<Class>();

  public void setLstComponentupdate(List<Class> lstComponentupdate) {
    this.lstComponentupdate = lstComponentupdate;
  }

  public List<Class> getLstComponentupdate() {
    return lstComponentupdate;
  }

  public void updateAjax(WebuiRequestContext requestcontext) {
    for (Class clazz : lstComponentupdate) {
      requestcontext.addUIComponentToUpdateByAjax(this.getAncestorOfType((Class<UIComponent>)clazz));
    }
  }
}
