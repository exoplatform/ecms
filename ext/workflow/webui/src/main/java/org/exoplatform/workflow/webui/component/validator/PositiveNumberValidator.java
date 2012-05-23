package org.exoplatform.workflow.webui.component.validator;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

public class PositiveNumberValidator implements Validator{

  /* (non-Javadoc)
  * @see org.exoplatform.webui.form.validator.Validator#validate(org.exoplatform.webui.form.UIFormInput)
  */
  @Override
  public void validate(UIFormInput uiInput) throws Exception
  {
    String inputValue = ((String)uiInput.getValue());
    if (StringUtils.isNotBlank(inputValue))
    {
      boolean isValid = true;
      Double inputNumber = 0.0d;

      try
      {
        inputNumber = Double.parseDouble(inputValue);

        // Invalid if less than or equal zero
        if (inputNumber <= 0 )
        {
          isValid = false;
        }
      }
      // Invalid if not be number format compliant
      catch (NumberFormatException e) {
        isValid = false;
      }

      if (!isValid) {
        Object[] args = {uiInput.getName(), inputValue};

        throw new MessageException(
            new ApplicationMessage("PositiveNumberValidator.msg.invalid-input", args, ApplicationMessage.WARNING));
      }
    }
  }
}
