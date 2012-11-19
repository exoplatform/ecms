package org.exoplatform.ecm.webui.form.validator;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.Validator;

public class CategoryValidator implements Validator{

  public void validate(UIFormInput uiInput) throws Exception {
    if (uiInput.getValue() == null) return;
    if (uiInput instanceof UIFormStringInput) {
      TaxonomyService taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
      try{
        int index = 0;
        String categoryPath = (String) uiInput.getValue();
        index = categoryPath.indexOf("/");
        if (index < 0) {
          taxonomyService.getTaxonomyTree(categoryPath);
        } else {
          taxonomyService.getTaxonomyTree(categoryPath.substring(0, index)).getNode(categoryPath.substring(index + 1));
        }
      }catch (Exception e) {
        throw new MessageException(
                                   new ApplicationMessage(
                                                          "CategoryValidator.msg.non-categories",
                                                          null,
                                                          ApplicationMessage.WARNING));
      }
    }
  }
}
