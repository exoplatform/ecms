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
package org.exoplatform.wcm.webui.newsletter;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          ha.mai@exoplatform.com
 * Dec 29, 2009
 */
public class NoneHTMLValidator implements Validator {
  private String fullHtmltag = ".*<.*>.*</.*>.*";
  private String startHtmlTag = ".*<.*>.*";
  private String finishHtmlTag = ".*</.*>.*";

  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    if((uiInput.getValue() == null)) return;
    String input = ((String)uiInput.getValue()).trim();
    if(!input.matches(fullHtmltag) && !input.matches(startHtmlTag) && !input.matches(finishHtmlTag))
      return ;

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
    Object[]  args = {label, uiInput.getBindingField() } ;
    throw new MessageException(new ApplicationMessage("NoneHTMLValidator.msg.invalid", args, ApplicationMessage.WARNING)) ;
  }

}
