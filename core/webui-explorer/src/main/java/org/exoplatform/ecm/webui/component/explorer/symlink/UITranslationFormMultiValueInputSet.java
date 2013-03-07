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
package org.exoplatform.ecm.webui.component.explorer.symlink;

import java.io.Writer;
import java.util.ResourceBundle;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 7, 2013
 */
public class UITranslationFormMultiValueInputSet extends UIFormMultiValueInputSet {

  /*
   * (non-Javadoc)
   * @see org.exoplatform.webui.form.UIFormMultiValueInputSet#processRender(org.
   * exoplatform.webui.application.WebuiRequestContext)
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (getChildren() == null || getChildren().size() < 1)
      createUIFormInput(0);

    Writer writer = context.getWriter();

    UIForm uiForm = getAncestorOfType(UIForm.class);
    int size = getChildren().size();

    ResourceBundle res = context.getApplicationResourceBundle();
    String addItem = res.getString("UIAddTranslationForm.action.SelectDocument");

    writer.append("<div class=\"input-append\">");
    for (int i = 0; i < size; i++) {
      UIFormInputBase uiInput = getChild(i);

      uiInput.setReadOnly(readonly_);
      uiInput.setDisabled(!enable_);

      uiInput.processRender(context);

      if (i == size - 1) {
        writer.append("<button type=\"button\" onclick=\"");
        writer.append(uiForm.event("SelectDocument", getId())).append("\" title=\"" + addItem + "\"");
        writer.append(" class=\"btn\" >" + addItem + "</button>");
      }
    }
    writer.append("</div>");
  }
}
