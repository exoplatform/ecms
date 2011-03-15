package org.exoplatform.wcm.webui.authoring;

import javax.portlet.PortletMode;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
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

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context;
    PortletMode newMode = pContext.getApplicationMode();

    super.processRender(app, context);
  }

}
