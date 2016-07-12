/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS Author : Tran Hung Phong
 * phongth@exoplatform.com Jul 24, 2012
 */
public class XSSValidator implements Validator {

  @Override
  public void validate(UIFormInput uiInput) throws Exception {
    String inputValue = ((String) uiInput.getValue());
    if (inputValue == null || inputValue.trim().length() == 0) {
      return;
    }

    inputValue = HTMLSanitizer.sanitize(inputValue);
    if (StringUtils.isEmpty(inputValue)) {
      String message = "UIActionForm.msg.xss-vulnerability-character";
      if (uiInput.getLabel() == null)
        message = "UIActionForm.msg.xss-vulnerability-character-wo-label";
      Object[] args = { uiInput.getLabel() };
      throw new MessageException(new ApplicationMessage(message, args, ApplicationMessage.WARNING));
    }
  }
}
