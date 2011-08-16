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
package org.exoplatform.ecm.webui.form.validator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 *          anhvurz90@yahoo.com
 * Jul 13, 2011
 */
public class UploadFileMimeTypesValidator implements Validator {
  
  private String mimeTypes_;
  
  public UploadFileMimeTypesValidator() {
  }
  
  public UploadFileMimeTypesValidator(String mimeTypes) {
    this.mimeTypes_ = mimeTypes;
  }
  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput instanceof UIFormUploadInput) {
      UIFormUploadInput uploadInput = UIFormUploadInput.class.cast(uiInput);
      String mimeTypeInput = uploadInput.getUploadResource() == null ? null : uploadInput.getUploadResource().getMimeType();
      if (mimeTypes_ != null && mimeTypeInput != null) {
        if (mimeTypes_.contains(mimeTypeInput)) {
          return;
        }
        Pattern pattern = Pattern.compile(mimeTypes_.replace("*", ".*"));
        Matcher matcher = pattern.matcher(mimeTypeInput); 
        if (!matcher.find()) {
          Object[] args = { mimeTypeInput, uploadInput.getName() };
          throw new MessageException(
              new ApplicationMessage("UploadFileMimeTypesValidator.msg.wrong-mimetype",args, ApplicationMessage.WARNING));
        }
      }
    }
  }
}
