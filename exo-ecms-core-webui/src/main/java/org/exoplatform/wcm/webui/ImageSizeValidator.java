package org.exoplatform.wcm.webui;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Mar 30, 2009
 */
public class ImageSizeValidator implements Validator {

  /* (non-Javadoc)
   * @see org.exoplatform.webui.form.validator.Validator#validate(org.exoplatform.webui.form.UIFormInput)
   */
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
    String s = (String)uiInput.getValue();
    int size = s.length();
    for(int i = 0; i < size; i ++){
      char c = s.charAt(i);
      if (Character.isDigit(c) || (s.charAt(0) == '-' && i == 0 && s.length() > 1)
                               || (s.charAt(size-1) == '%')){
        continue;
      }
      Object[] args = { label, uiInput.getBindingField() };
      throw new MessageException(new ApplicationMessage("NumberFormatValidator.msg.Invalid-number", args)) ;
    }
  }
}
