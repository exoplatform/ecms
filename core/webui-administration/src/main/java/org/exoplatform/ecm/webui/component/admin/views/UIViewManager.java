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
package org.exoplatform.ecm.webui.component.admin.views;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@exoplatform.com
 * Sep 25, 2006
 * 11:45:11 AM
 */

@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")
public class UIViewManager extends UIAbstractManager {
  public UIViewManager() throws Exception{
    addChild(UIViewContainer.class, null, null) ;
    UITemplateContainer uiECMTemp = addChild(UITemplateContainer.class, null, "ECMTemplate") ;
    uiECMTemp.addChild(UIECMTemplateList.class, null, null) ;
    uiECMTemp.setRendered(false) ;
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    getChild(UIViewContainer.class).update() ;
    UIECMTemplateList uiECMTemplateList = ((UITemplateContainer)getChildById("ECMTemplate")).getChild(UIECMTemplateList.class);
    uiECMTemplateList.refresh(uiECMTemplateList.getUIPageIterator().getCurrentPage());
  }
}

