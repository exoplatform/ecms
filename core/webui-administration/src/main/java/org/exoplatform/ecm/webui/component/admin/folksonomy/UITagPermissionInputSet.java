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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 14, 2009
 * 4:46:20 PM
 */
@ComponentConfig(template = "classpath:groovy/ecm/webui/form/UIFormInputSetWithAction.gtmpl")
public class UITagPermissionInputSet extends UIFormInputSetWithAction {
  final static public String FIELD_USERORGROUP = "userOrGroup";

  public UITagPermissionInputSet(String name) throws Exception {
    super(name);
    setComponentConfig(getClass(), null) ;
    UIFormStringInput userGroup = new UIFormStringInput(FIELD_USERORGROUP, FIELD_USERORGROUP, null) ;
    userGroup.addValidator(MandatoryValidator.class) ;
    userGroup.setEditable(false) ;
    addUIFormInput(userGroup) ;
    setActionInfo(FIELD_USERORGROUP, new String[] {"SelectMember"}) ;
  }

}
