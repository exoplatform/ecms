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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Modified: - Extends from org.exoplatform.webui.form.validator.MandatoryValidator
 *             Use template name to generate a label for input instead of use form name
 *           - Only use for Form Generator feature
 * Nov 11, 2009
 */
public class MandatoryValidator extends org.exoplatform.webui.form.validator.MandatoryValidator {

  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    if((uiInput.getValue() != null) && ((String)uiInput.getValue()).trim().length() > 0) {
      return ;
    }

    UIComponent uiComponent = (UIComponent) uiInput ;
    UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class) ;
    String templatePath = uiForm.getTemplate();
    String nodetypeName = "";
    String[] temps = templatePath.split("/");
    for (String temp : temps){
      if (temp.contains("_fg_n")) {
        nodetypeName = temp;
        break;
      }
    }
    TemplateService templateService = uiForm.getApplicationComponent(TemplateService.class);
    String nodetypeLabel = templateService.getTemplateLabel(nodetypeName).trim();
    ResourceBundle res = Util.getPortalRequestContext().getApplicationResourceBundle();
    String label = "";
    try {
      label = res.getString(nodetypeLabel + ".label." + uiInput.getName());
    } catch (MissingResourceException e) {
      label = uiInput.getName();
    }
    Object[]  args = {label, uiInput.getBindingField() } ;
    throw new MessageException(new ApplicationMessage("EmptyFieldValidator.msg.empty-input", args, ApplicationMessage.WARNING)) ;
  }
}
