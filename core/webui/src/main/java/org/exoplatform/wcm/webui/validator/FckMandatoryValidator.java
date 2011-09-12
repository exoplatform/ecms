/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

/**
 * Created by The eXo Platform SAS
 * Author : Dung Khuong
 *          dung.khuong@exoplatform.com
 * Jul 6, 2010
 */
@SuppressWarnings("serial")
public class FckMandatoryValidator extends org.exoplatform.webui.form.validator.MandatoryValidator {

  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    if ((uiInput.getValue() != null) && (((String) uiInput.getValue()).trim().length() > 0)
        && (!uiInput.getValue().toString().trim().equals("<br />"))) {
      return;
    } else {
      UIComponent uiComponent = (UIComponent)uiInput;
      UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class);
      String label;
      try
      {
         label = uiForm.getId() + ".label." + uiInput.getName();
      }
      catch (Exception e)
      {
         label = uiInput.getName();
      }
      label = label.trim();
      Object[] args = {label};      
      throw new MessageException(new ApplicationMessage("EmptyFieldValidator.msg.empty-input",
                                                        args,
                                                        ApplicationMessage.WARNING));
    }
  }
}
