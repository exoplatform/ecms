package org.exoplatform.wcm.webui.authoring;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
        template = "app:/groovy/authoring/UIDashboardPortlet.gtmpl"
  )
public class UIWCMDashboardPortlet extends UIPortletApplication {

  public UIWCMDashboardPortlet() throws Exception {
      addChild(UIDashboardForm.class, null, null);
  }

}
