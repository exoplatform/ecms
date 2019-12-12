
package org.exoplatform.ecm.webui.component.explorer.documents;

import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * The Class DocumentSelectItemOption.
 *
 * @param <T> the generic type
 */
public class DocumentSelectItemOption<T> extends SelectItemOption<T> {

  /** The provider. */
  protected String provider;

  public DocumentSelectItemOption() {
    super();
  }

  public DocumentSelectItemOption(String label, T value, String icon) {
    super(label, value, icon);
  }

  public DocumentSelectItemOption(String label, T value, String desc, String icon) {
    super(label, value, desc, icon);
  }

  public DocumentSelectItemOption(String label, T value, String desc, String icon, boolean selected) {
    super(label, value, desc, icon, selected);
  }

  public DocumentSelectItemOption(String label, T value) {
    super(label, value);
  }

  public DocumentSelectItemOption(T value) {
    super(value);
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

}
