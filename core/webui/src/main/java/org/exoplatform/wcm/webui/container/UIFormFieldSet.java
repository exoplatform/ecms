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
package org.exoplatform.wcm.webui.container;

import java.io.Writer;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 26, 2009
 */
public class UIFormFieldSet extends UIContainer {

  /**
   * Instantiates a new uI form field set.
   *
   * @param name the name
   */
  public UIFormFieldSet(String name) {
    setId(name) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processDecode(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processDecode(WebuiRequestContext context) throws Exception {
    for(UIComponent child : getChildren())  {
      child.processDecode(context) ;
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    if (getComponentConfig() != null) {
      super.processRender(context);
      return;
    }
    UIForm uiForm = getAncestorOfType(UIForm.class);
    Writer writer = context.getWriter() ;
    writer.write("<div class=\"" + getId() + "\">") ;
    writer.write("<fieldset>") ;
    writer.write("<legend>" + uiForm.getLabel(getId()) + "</legend>") ;
    writer.write("<table class=\"UIFormGrid\">") ;
    for(UIComponent component : getChildren()) {
      if(component.isRendered()) {
        writer.write("<tr>") ;
        String componentName = uiForm.getLabel(component.getId());
        if(componentName != null && componentName.length() > 0 && !componentName.equals(getId())) {
          writer.write("<td class=\"FieldLabel\"><label for=\"" + component.getId() + "\">" + componentName + "</td>");
          writer.write("<td class=\"FieldComponent\">") ;
          renderUIComponent(component) ;
          writer.write("</td>") ;
        } else {
          writer.write("<td class=\"FieldComponent\" colspans=\"2\">") ;
          renderUIComponent(component) ;
          writer.write("</td>") ;
        }
        writer.write("</tr>") ;
      }
    }
    writer.write("</table>") ;
    writer.write("</fieldset>") ;
    writer.write("</div>") ;
  }
}
