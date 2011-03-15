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
package org.exoplatform.ecm.webui.form.field;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.ecm.webui.form.DialogFormField;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormTextAreaInput;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
public class UIFormTextAreaField extends DialogFormField {

  public final static int DEFAULT_ROW = 10;
  public final static int DEFAULT_COL = 30;

  public UIFormTextAreaField(String name, String label, String[] arguments) {
    super(name, label, arguments);
  }

  @SuppressWarnings("unchecked")
  public <T extends UIFormInputBase> T createUIFormInput() throws Exception {
    UIFormTextAreaInput uiTextArea = new UIFormTextAreaInput(name, name, defaultValue) ;
    if(validateType != null) {
      DialogFormUtil.addValidators(uiTextArea, validateType);
    }
    if(label != null) uiTextArea.setLabel(label) ;
    return (T)uiTextArea;
  }

  public Map<String, String> getSelectorInfo() {
    Map<String, String> map = new HashMap<String, String>() ;
    map.put("selectorClass", selectorClass) ;
    map.put("returnField", name) ;
    map.put("selectorIcon", selectorIcon) ;
    map.put("workspaceField", workspaceField) ;
    if(selectorParams != null) map.put("selectorParams", selectorParams) ;
    return map;
  }

  public boolean useSelector() { return selectorClass != null; }
}
