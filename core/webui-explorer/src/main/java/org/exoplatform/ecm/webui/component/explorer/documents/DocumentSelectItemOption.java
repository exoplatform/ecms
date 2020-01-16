
package org.exoplatform.ecm.webui.component.explorer.documents;

import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * The Class DocumentSelectItemOption adds a provider to SelectItemOption.
 *
 * @param <T> the generic type
 */
public class DocumentSelectItemOption<T> extends SelectItemOption<T> {

  /** The provider. */
  protected final NewDocumentTemplatePlugin templatePlugin;
 
  /**
   * Instantiates a new document select item option.
   *
   * @param provider the provider
   */
  public DocumentSelectItemOption(NewDocumentTemplatePlugin templatePlugin) {
    super();
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param icon the icon
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, String icon, NewDocumentTemplatePlugin templatePlugin) {
    super(label, value, icon);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param desc the desc
   * @param icon the icon
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, String desc, String icon, NewDocumentTemplatePlugin templatePlugin) {
    super(label, value, desc, icon);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param desc the desc
   * @param icon the icon
   * @param selected the selected
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, String desc, String icon, boolean selected, NewDocumentTemplatePlugin templatePlugin) {
    super(label, value, desc, icon, selected);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, NewDocumentTemplatePlugin templatePlugin) {
    super(label, value);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param value the value
   * @param provider the provider
   */
  public DocumentSelectItemOption(T value, NewDocumentTemplatePlugin templatePlugin) {
    super(value);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public NewDocumentTemplatePlugin getTemplatePlugin() {
    return templatePlugin;
  }

}
