
package org.exoplatform.ecm.webui.component.explorer.documents;

import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * The Class DocumentSelectItemOption adds a template plugin to SelectItemOption.
 *
 * @param <T> the generic type
 */
public class DocumentSelectItemOption<T> extends SelectItemOption<T> {

  /** The template plugin. */
  protected final NewDocumentTemplatePlugin templatePlugin;
  
  /**
   * Instantiates a new document select item option.
   *
   * @param templatePlugin the template plugin
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
   * @param templatePlugin the template plugin
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
   * @param templatePlugin the template plugin
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
   * @param templatePlugin the template plugin
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
   * @param templatePlugin the template plugin
   */
  public DocumentSelectItemOption(String label, T value, NewDocumentTemplatePlugin templatePlugin) {
    super(label, value);
    this.templatePlugin = templatePlugin;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param value the value
   * @param templatePlugin the template plugin
   */
  public DocumentSelectItemOption(T value, NewDocumentTemplatePlugin templatePlugin) {
    super(value);
    this.templatePlugin = templatePlugin;
  }


  /**
   * Gets the template plugin.
   *
   * @return the template plugin
   */
  public NewDocumentTemplatePlugin getTemplatePlugin() {
    return templatePlugin;
  }

}
