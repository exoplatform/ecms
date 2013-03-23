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
package org.exoplatform.wcm.webui.form;

import java.io.Writer;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIFormInputSet;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 23, 2013  
 */
public class UIFormInputSetWithNoLabel extends UIFormInputSet {
  
  public UIFormInputSetWithNoLabel(String name) throws Exception {
    super(name);
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (getComponentConfig() != null) {
        super.processRender(context);
        return;
    }
    Writer w = context.getWriter();
    w.write("<div class=\"UIFormInputSet\">");
    w.write("<div class=\"form-horizontal\">");
    for (UIComponent inputEntry : getChildren()) {
        if (inputEntry.isRendered()) {
            w.write("<div class=\"control-group\">");
            w.write("<div class=\"controls-full\">");
            renderUIComponent(inputEntry);
            w.write("</div>");
            w.write("</div>");
        }
    }
    w.write("</div>");
    w.write("</div>");
  }
}
