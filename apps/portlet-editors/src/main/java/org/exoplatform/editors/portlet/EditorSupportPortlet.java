package org.exoplatform.editors.portlet;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.connector.collaboration.cometd.CometdConfig;
import org.exoplatform.wcm.connector.collaboration.cometd.CometdDocumentsService;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;

public class EditorSupportPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(EditorSupportPortlet.class);

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    try {
      PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/editors-admin.jsp");
      prDispatcher.include(request, response);
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      CometdDocumentsService cometdService = ExoContainerContext.getCurrentContainer()
                                                                .getComponentInstanceOfType(CometdDocumentsService.class);
      JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
      CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(),
                                                 cometdService.getUserToken(context.getRemoteUser()),
                                                 PortalContainer.getCurrentPortalContainerName());
      js.require("SHARED/editorsupport", "editorsupport")
        .addScripts("editorsupport.initConfig('" + context.getRemoteUser() + "' ," + cometdConf.toJSON() + ");");
    } catch (Exception e) {
      LOG.error("Error processing editor support portlet for user " + request.getRemoteUser(), e);
    }
  }

}
