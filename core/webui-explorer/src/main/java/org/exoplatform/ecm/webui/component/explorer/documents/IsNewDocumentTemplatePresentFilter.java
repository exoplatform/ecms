package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;


/**
 * The filter checks if at least one NewDocumentTemplateProvider is registered.
 */
public class IsNewDocumentTemplatePresentFilter extends UIExtensionAbstractFilter {

  /**
   * Instantiates a new checks if is template plugin present filter.
   */
  public IsNewDocumentTemplatePresentFilter() {
    this(null);
  }

  /**
   * Instantiates a new checks if is template plugin present filter.
   *
   * @param messageKey the message key
   */
  public IsNewDocumentTemplatePresentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /**
   * Accept.
   *
   * @param context the context
   * @return true, if successful
   * @throws Exception the exception
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    DocumentService documentService = ExoContainerContext.getCurrentContainer()
                                                            .getComponentInstanceOfType(DocumentService.class);

    if (documentService != null) {
      return documentService.getNewDocumentTemplateProviders().size() > 0;
    }
    return false;
  }

  /**
   * On deny.
   *
   * @param context the context
   * @throws Exception the exception
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

}
