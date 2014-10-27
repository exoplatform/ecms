/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.form.validator;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 8, 2008 3:50:44 PM
 */
public class ECMNameValidator implements Validator {

  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput.getValue()==null || ((String)uiInput.getValue()).trim().length()==0) return;
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
    String s = (String)uiInput.getValue();
    if(s == null || s.trim().length() == 0) {
      Object[] args = { uiInput.getLabel() };
      throw new MessageException(new ApplicationMessage("ECMNameValidator.msg.empty-input", args, ApplicationMessage.WARNING)) ;
    }
    for(int i = 0; i < s.length(); i ++){
      char c = s.charAt(i);
      if(Character.isLetter(c) || Character.isDigit(c) || Character.isSpaceChar(c) || c=='_'
        || c=='-' || c=='.' || c==':' || c=='@' || c=='^' || c==',' || c=='%' || c=='\'') {
        continue ;
      }
      Object[] args = { label };
      throw new MessageException(new ApplicationMessage("ECMNameValidator.msg.Invalid-char", args, ApplicationMessage.WARNING)) ;
    }
  }
}
