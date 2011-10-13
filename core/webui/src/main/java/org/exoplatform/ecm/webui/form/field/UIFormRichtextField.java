/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.form.field;

import org.exoplatform.ecm.webui.form.DialogFormField;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.wcm.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * May 10, 2010
 */
public class UIFormRichtextField extends DialogFormField {

  private final String TOOLBAR = "toolbar";
  private final String WIDTH = "width";
  private final String HEIGHT = "height";

  private String toolbar;
  private String width;
  private String height;

  public UIFormRichtextField(String name, String label, String[] arguments) {
    super(name, label, arguments);
  }

  @SuppressWarnings("unchecked")
  public <T extends UIFormInputBase> T createUIFormInput() throws Exception {
    UIFormRichtextInput richtext = new UIFormRichtextInput(name, name, defaultValue);
    setPredefineOptions();
    richtext.setToolbar(toolbar);
    richtext.setWidth(width);
    richtext.setHeight(height);
    if(validateType != null) {
      DialogFormUtil.addValidators(richtext, validateType);
    }
    return (T)richtext;
  }

  private void setPredefineOptions() {
    if (options == null) return;
    for(String option: options.split(",")) {
      String[] entry = option.split(":");
      if(TOOLBAR.equals(entry[0])) {
        toolbar = entry[1];
      } else if(WIDTH.equals(entry[0])) {
        width = entry[1];
      } else if(HEIGHT.equals(entry[0])) {
        height = entry[1];
      }
    }
  }

}
