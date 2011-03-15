/*
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
 */
package org.exoplatform.ecm.webui.component.admin.listener;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminControlPanel;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminWorkingArea;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.UIExtensionEventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 15 mai 2009
 */
public abstract class UIECMAdminControlPanelActionListener<T extends UIComponent> extends UIExtensionEventListener<T> {

  /**
   * {@inheritDoc}
   */
  @Override
  protected Map<String, Object> createContext(Event<T> event) throws Exception {
    Map<String, Object> context = new HashMap<String, Object>();
    UIECMAdminPortlet portlet = event.getSource().getAncestorOfType(UIECMAdminPortlet.class);
    UIECMAdminWorkingArea uiWorkingArea = portlet.getChild(UIECMAdminWorkingArea.class);
    UIECMAdminControlPanel controlPanel = event.getSource().getAncestorOfType(UIECMAdminControlPanel.class);
    WebuiRequestContext requestContext = event.getRequestContext();
    UIApplication uiApp = requestContext.getUIApplication();
    context.put(UIECMAdminPortlet.class.getName(), portlet);
    context.put(UIECMAdminWorkingArea.class.getName(), uiWorkingArea);
    context.put(UIECMAdminControlPanel.class.getName(), controlPanel);
    context.put(UIApplication.class.getName(), uiApp);
    context.put(WebuiRequestContext.class.getName(), requestContext);
    return context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getExtensionType() {
    return UIECMAdminControlPanel.EXTENSION_TYPE;
  }
}
