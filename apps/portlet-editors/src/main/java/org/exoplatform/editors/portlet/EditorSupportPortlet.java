package org.exoplatform.editors.portlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.wcm.connector.collaboration.cometd.CometdConfig;
import org.exoplatform.wcm.connector.collaboration.cometd.CometdDocumentsService;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class EditorSupportPortlet.
 */
public class EditorSupportPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log    LOG                    = ExoLogger.getLogger(EditorSupportPortlet.class);

  /** The Constant CLIENT_RESOURCE_PREFIX. */
  private static final String CLIENT_RESOURCE_PREFIX = "editors.";

  /**
   * Do view.
   *
   * @param request the request
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws PortletException the portlet exception
   */
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
      
      DocumentService documentService = ExoContainerContext.getCurrentContainer()
          .getComponentInstanceOfType(DocumentService.class);
      long idleTimeout = documentService.getEditorsIdleTimeout();
      js.require("SHARED/editorsupport", "editorsupport")
        .addScripts("editorsupport.initConfig('" + context.getRemoteUser() + "' ," + cometdConf.toJSON() + ", "
            + getI18n(request.getLocale()) + ", " + idleTimeout + ");");
    } catch (Exception e) {
      LOG.error("Error processing editor support portlet for user " + request.getRemoteUser(), e);
    }
  }

  /**
   * Gets the i 18 n.
   *
   * @param locale the locale
   * @return the i 18 n
   */
  protected String getI18n(Locale locale) {
    String messagesJson;
    try {
      ResourceBundleService i18nService = ExoContainerContext.getCurrentContainer()
                                                             .getComponentInstanceOfType(ResourceBundleService.class);
      ResourceBundle res = i18nService.getResourceBundle("locale.portlet.EditorsAdmin", locale);
      Map<String, String> resMap = new HashMap<String, String>();
      for (Enumeration<String> keys = res.getKeys(); keys.hasMoreElements();) {
        String key = keys.nextElement();
        String bundleKey;
        if (key.startsWith(CLIENT_RESOURCE_PREFIX)) {
          bundleKey = key.substring(CLIENT_RESOURCE_PREFIX.length());
        } else {
          bundleKey = key;
        }
        resMap.put(bundleKey, res.getString(key));
      }
      messagesJson = new JsonGeneratorImpl().createJsonObjectFromMap(resMap).toString();
    } catch (JsonException e) {
      LOG.warn("Cannot serialize messages bundle JSON", e);
      messagesJson = "{}";
    } catch (Exception e) {
      LOG.warn("Cannot build messages bundle", e);
      messagesJson = "{}";
    }
    return messagesJson;
  }

}
