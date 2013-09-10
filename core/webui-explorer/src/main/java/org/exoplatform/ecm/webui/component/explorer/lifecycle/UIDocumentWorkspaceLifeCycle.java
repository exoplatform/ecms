/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.lifecycle;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS
 *  Author : eXoPlatform exo@exoplatform.com Apr
 * 4, 2013
 */
public class UIDocumentWorkspaceLifeCycle extends Lifecycle<UIContainer> {
  public void processRender(UIContainer uicomponent, WebuiRequestContext context) throws Exception {
    UIWorkingArea uiWorkingArea = uicomponent.getAncestorOfType(UIWorkingArea.class);
    UIActionBar uiActionBar = uiWorkingArea.getChild(UIActionBar.class);
    boolean isUISelectDocumentTemplateTitleRendered =
        uiWorkingArea.isUISelectDocumentTemplateTitleRendered();
    context.getWriter()
           .append("<div class=\"")
           .append(uicomponent.getId())
           .append(isUISelectDocumentTemplateTitleRendered || uiActionBar.isRendered()
                   ? StringUtils.EMPTY : " uiDocumentWorkspaceBox") 
           .append("\" id=\"")
           .append(uicomponent.getId())
           .append("\">");
    uicomponent.renderChildren(context);
    context.getWriter().append("</div>");
  }
}
