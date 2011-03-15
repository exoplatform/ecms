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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.List;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 24, 2009
 * 5:20:02 PM
 */
@ComponentConfig(
     template = "app:/groovy/webui/component/explorer/popup/action/UIFormInputSetWithActionForTaggingForm.gtmpl"
  )
public class UIFormInputSetWithActionForTaggingForm extends UIFormInputSetWithAction {

  public UIFormInputSetWithActionForTaggingForm(String name) {
    super(name);
  }

  public List<String> getTagNames() throws Exception {
    return this.getAncestorOfType(UITaggingForm.class).getAllTagNames();
  }

}
