package org.exoplatform.ecm.webui.core;
import org.exoplatform.webui.ext.UIExtension;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 2/2/15
 * Custom UIExtension to add new properties.
 */
public class UIECMExtension extends UIExtension{
  private String view;

  public String getView() {
    return view;
  }

  public void setView(String view) {
    this.view = view;
  }
}
