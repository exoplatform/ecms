package org.exoplatform.wcm.webui.newsletter.viewer;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * thang.do@exoplatform.com Jun 25, 2009
 */
public class UINewsletterViewerEmailAddressValidator implements Validator {

  /** The Constant EMAIL_REGEX. */
  static private final String EMAIL_REGEX = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[_A-Za-z0-9-]+(.([a-zA-Z0-9])+)*";

  /* (non-Javadoc)
   * @see org.exoplatform.webui.form.validator.Validator#validate(org.exoplatform.webui.form.UIFormInput)
   */
  @SuppressWarnings("unchecked")
  public void validate(UIFormInput uiInput) throws Exception {
    UIComponent uiComponent = (UIComponent) uiInput;
    UIForm uiForm = uiComponent.getAncestorOfType(UIForm.class);
    String label;
    try {
      label = uiForm.getLabel(uiInput.getName());
    } catch (Exception e) {
      label = uiInput.getName();
    }
    label = label.trim();
    if (label.charAt(label.length() - 1) == ':')
      label = label.substring(0, label.length() - 1);
    if (uiInput.getValue() == null
        || ((String) uiInput.getValue()).trim().length() == 0)
      return;
    String s = (String) uiInput.getValue();
    if (s.matches(EMAIL_REGEX))
      return;
    Object[] args = { label, uiInput.getBindingField() };
    throw new MessageException(new ApplicationMessage(
        "EmailAddressValidator.msg.Invalid-input", args,
        ApplicationMessage.WARNING));
  }
}
