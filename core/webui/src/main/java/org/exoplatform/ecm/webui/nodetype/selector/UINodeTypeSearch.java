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
package org.exoplatform.ecm.webui.nodetype.selector;

import org.exoplatform.ecm.webui.form.validator.NodeTypeNameValidator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputContainer;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 23, 2009
 */

@ComponentConfig(
    template =  "classpath:groovy/ecm/webui/nodetype/selector/UINodeTypeSearch.gtmpl"
)

public class UINodeTypeSearch extends UIFormInputContainer<String> {

  public void init() throws Exception {
    if (getChild(UIFormStringInput.class) != null) {
      removeChild(UIFormStringInput.class);
    }
    addChild(new UIFormStringInput("NodeTypeText", "NodeTypeText", "*"));
    addValidator(NodeTypeNameValidator.class);
  }

  public String event(String name) throws Exception {
    return getAncestorOfType(UIForm.class).event(name);
  }

  public String getResource(String key) {
    try {
      return Utils.getResourceBundle(Utils.LOCALE_WEBUI_DMS, key, getClass().getClassLoader());
    } catch (Exception e) {
      return key;
    }
  }

  @Override
  public String getValue() throws Exception {
    return getChild(UIFormStringInput.class).getValue();
 }

  public Class getTypeValue() {
    return String.class;
  }
}
