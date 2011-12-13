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
package org.exoplatform.ecm.jcr;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * @author hai_lethanh
 *
 */
public class SimpleSearchValidator implements Validator {

  @Override
  public void validate(UIFormInput uiInput) throws Exception {
    String inputValue = ((String) uiInput.getValue());
    if (inputValue == null || inputValue.trim().length() == 0) {
      throw new MessageException(new ApplicationMessage("SearchValidator.msg.empty-input",
                                                        new Object[] { uiInput.getName() },
                                                        ApplicationMessage.WARNING));
    }
  }
}
