package org.exoplatform.wcm.webui.administration;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * ngoc.tran@exoplatform.com Jan 28, 2010
 */
@ComponentConfig(
                 lifecycle = UIApplicationLifecycle.class,
                 template = "app:/groovy/Editing/UIEditingPortlet.gtmpl"
               )
public class UIEditingPortlet extends UIPortletApplication {

  public UIEditingPortlet() throws Exception {
    addChild(UIEditingForm.class, null, null);
  }
}
