
package org.exoplatform.ecm.webui.component.explorer.documents;

import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * The Class DocumentSelectItemOption adds a provider to SelectItemOption.
 *
 * @param <T> the generic type
 */
public class DocumentSelectItemOption<T> extends SelectItemOption<T> {

  /** The provider. */
  protected final String provider;

  /**
   * Instantiates a new document select item option.
   *
   * @param provider the provider
   */
  public DocumentSelectItemOption(String provider) {
    super();
    this.provider = provider;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param icon the icon
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, String icon, String provider) {
    super(label, value, icon);
    this.provider = provider;
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
  public DocumentSelectItemOption(String label, T value, String desc, String icon, String provider) {
    super(label, value, desc, icon);
    this.provider = provider;
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
  public DocumentSelectItemOption(String label, T value, String desc, String icon, boolean selected, String provider) {
    super(label, value, desc, icon, selected);
    this.provider = provider;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param provider the provider
   */
  public DocumentSelectItemOption(String label, T value, String provider) {
    super(label, value);
    this.provider = provider;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param value the value
   * @param provider the provider
   */
  public DocumentSelectItemOption(T value, String provider) {
    super(value);
    this.provider = provider;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
  }

}
