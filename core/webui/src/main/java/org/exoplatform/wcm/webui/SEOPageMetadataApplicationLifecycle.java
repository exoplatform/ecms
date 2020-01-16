package org.exoplatform.wcm.webui;

import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.user.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.web.application.*;
import org.exoplatform.webui.application.WebuiRequestContext;

public class SEOPageMetadataApplicationLifecycle extends BaseComponentPlugin
    implements ApplicationLifecycle<WebuiRequestContext> {

  private static final Log LOG = ExoLogger.getLogger(SEOPageMetadataApplicationLifecycle.class.toString());

  public void onInit(Application app) {
  }

  public void onStartRequest(final Application app, final WebuiRequestContext context) throws Exception {
    PortalRequestContext pcontext = (PortalRequestContext) context;
    String requestPath = pcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
    String siteName = pcontext.getSiteName();
    try {
      if (pcontext.getSiteType().equals(SiteType.PORTAL)) {
        // Get page
        SEOService seoService = CommonsUtils.getService(SEOService.class);
        ArrayList<String> paramArray = null;
        if (!pcontext.useAjax()) {
          Enumeration<String> params = pcontext.getRequest().getParameterNames();
          if (params.hasMoreElements()) {
            paramArray = new ArrayList<>();
            while (params.hasMoreElements()) {
              String contentParam = params.nextElement();
              paramArray.add(pcontext.getRequestParameter(contentParam));
            }
          }
        }

        UserPortal userPortal = pcontext.getUserPortal();
        UserNavigation navigation = userPortal.getNavigation(SiteKey.portal(siteName));
        UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
        String nodePath = pcontext.getNodePath();
        UserNode currentNode = null;
        if (StringUtils.isBlank(nodePath)) {
          currentNode = userPortal.getDefaultPath(builder.build());
        } else {
          currentNode = userPortal.resolvePath(navigation, builder.build(), nodePath);
        }
        if (currentNode != null && currentNode.getPageRef() != null) {
          String pageReference = currentNode.getPageRef().format();
          PageMetadataModel metaModel = seoService.getMetadata(paramArray, pageReference, pcontext.getLocale().getLanguage());
          if (metaModel != null) {
            pcontext.setAttribute("PAGE_METADATA", metaModel);
            if (StringUtils.isNotBlank(metaModel.getTitle())) {
              pcontext.getRequest().setAttribute(PortalRequestContext.REQUEST_TITLE, metaModel.getTitle());
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error getting page SEO of site {} with path {}", siteName, requestPath, e);
    }
  }

  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
  }

  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
  }

  public void onDestroy(Application app) {
  }
}
