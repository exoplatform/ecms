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
package org.exoplatform.wcm.webui.form;

import java.io.Writer;
import java.util.ResourceBundle;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputSet;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Aug 15, 2012
 * 10:52:32 AM  
 */
public class UIFormInputSetWithColspan extends UIFormInputSet {
  
  private boolean isColspan = false;
  
  public UIFormInputSetWithColspan(String name) throws Exception {
    super(name);
  }
  
  public void allowColspan(boolean allowColspan) {
    isColspan = allowColspan;
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception
  {
     if (getComponentConfig() != null)
     {
        super.processRender(context);
        return;
     }
     Writer w = context.getWriter();
     w.write("<div class=\"UIFormInputSet\">");
     w.write("<table class=\"UIFormGrid " +this.getId()+ "\">");
     ResourceBundle res = context.getApplicationResourceBundle();
     UIForm uiForm = getAncestorOfType(UIForm.class);
     for (UIComponent inputEntry : getChildren())
     {
        if (inputEntry.isRendered())
        {
           String label = "";
           boolean hasLabel = false;
           if (inputEntry instanceof UIFormInputBase)
           {
              UIFormInputBase formInputBase = (UIFormInputBase) inputEntry;
              if (formInputBase.getLabel() != null)
              {
                 label = uiForm.getLabel(res, formInputBase.getLabel());
              }
              else
              {
                 label = uiForm.getLabel(res, formInputBase.getId());
              }
              if (formInputBase.getLabel() != null || (label != formInputBase.getId()))
              {
                 hasLabel = true;
              }
           }
           w.write("<tr>");
           if(isColspan) {
             w.write("<td class=\"FieldLabel\" colspan=\"2\">");
           } else {
             w.write("<td class=\"FieldLabel\">");
             
             // if missing resource and the label hasn't been set before, don't print out the label.
             if (hasLabel)
             {
               w.write(label);
             }
           }
           w.write("</td>");
           w.write("<td class=\"FieldComponent\">");
           renderUIComponent(inputEntry);
           w.write("</td>");
           w.write("</tr>");
        }
     }
     w.write("</table>");
     w.write("</div>");
  }

}
