/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.ecm.webui.form.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Validates whether this number is in a correct format
 */
public class PhoneFormatValidator implements Validator {

    @Override
    public void validate(UIFormInput uiInput) throws Exception {
      // TODO Auto-generated method stub
      String rex = "(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]‌​)\\s*)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)([2-9]1[02-9]‌​|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})\\s*(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+)\\s*)?$";  
      String value = (String)uiInput.getValue();
      if(StringUtils.isEmpty(value)) return;
      Pattern pattern = Pattern.compile(rex);;
      Matcher matcher = pattern.matcher(value);;
      if(!matcher.matches()){
        
        Object[] args = { uiInput.getName() };
        throw new MessageException(new ApplicationMessage("ECMPhoneFormatValidator.msg.Invalid", args, ApplicationMessage.WARNING)) ;
      }
    }


}
