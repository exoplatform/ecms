package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;


/**
 * The Class IsTemplatePluginPresentFilter.
 */
public class IsTemplatePluginPresentFilter extends UIExtensionAbstractFilter {

  /**
   * Instantiates a new checks if is template plugin present filter.
   */
  public IsTemplatePluginPresentFilter() {
    this(null);
  }

  /**
   * Instantiates a new checks if is template plugin present filter.
   *
   * @param messageKey the message key
   */
  public IsTemplatePluginPresentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /**
   * Accept.
   *
   * @param context the context
   * @return true, if successful
   * @throws Exception the exception
   */
  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#accept(java.util.Map)
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    NewDocumentServiceImpl documentService = ExoContainerContext.getCurrentContainer()
                                                            .getComponentInstanceOfType(NewDocumentServiceImpl.class);

    if (documentService != null) {
      return documentService.hasDocumentTemplatePlugins();
    }
    return false;
  }

  /**
   * On deny.
   *
   * @param context the context
   * @throws Exception the exception
   */
  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#onDeny(java.util.Map)
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

}
