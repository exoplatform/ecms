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
package org.exoplatform.wcm.webui.validator;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 15, 2009
 */
public class ZeroNumberValidator implements Validator {

  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput.getValue()==null || ((String)uiInput.getValue()).length()==0) return;
    UIComponent uiComponent = (UIComponent) uiInput ;
    UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class) ;
    String label;
    try{
      label = uiForm.getLabel(uiInput.getName());
    } catch(Exception e) {
      label = uiInput.getName();
    }
    label = label.trim();
    if(label.charAt(label.length() - 1) == ':') label = label.substring(0, label.length() - 1);
    if (((String)uiInput.getValue()).charAt(0) == '0') {
      Object[] args = { label, uiInput.getBindingField() };
      throw new MessageException(new ApplicationMessage("ZeroValidator.msg.Invalid-number", args)) ;
    }
  }

}
