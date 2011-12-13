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
package org.exoplatform.ecm.webui.form.validator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;


/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 15, 2009
 */
public class DateValidator extends DateTimeValidator {

  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    String inputValue = ((String)uiInput.getValue());
    if (inputValue.trim().indexOf(" ") > 0) {
      super.validate(uiInput);
    } else {
      validateDateFormat(uiInput);
    }
  }
  
  private void validateDateFormat(UIFormInput uiInput) throws Exception {
    
    String inputValue = ((String)uiInput.getValue());
    try {
      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      DateFormat dateFormat_ = SimpleDateFormat.getDateInstance(DateFormat.SHORT, requestContext.getLocale());
      dateFormat_.setLenient(false);
      dateFormat_.parse(inputValue);
      ((UIFormDateTimeInput)uiInput).setValue(inputValue + " 00:00:00");
    } catch (ParseException parseEx) {
      //get label of datetime field
      UIComponent uiComponent = (UIComponent) uiInput ;
      UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class) ;
      String label;
      try{
        label = uiForm.getLabel(uiInput.getName());
      } catch(Exception ex) {
        label = uiInput.getName();
      }
      label = label.trim();
      if(label.charAt(label.length() - 1) == ':') label = label.substring(0, label.length() - 1);
      
      //throw exception with msg
      throw new MessageException(new ApplicationMessage("DateTimeValidator.msg.Invalid-input",
                                                        new Object[] { label, inputValue }));
    }
  }

}
