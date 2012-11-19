package org.exoplatform.services.handler;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;

public class SitemapHandler extends WebRequestHandler {

  private String portalName = null;
  public String getHandlerName()
  {
    return "sitemap";
  }
  @Override
  public boolean execute(ControllerContext context) throws Exception
  {
     portalName = context.getParameter(QualifiedName.create("gtn", "sitename"));
     execute(context.getController(), context.getRequest(), context.getResponse());
     return true;
  }
  public void execute(WebAppController controller, HttpServletRequest req, HttpServletResponse res) throws Exception  {
  String sitemapContent = "";
    if(portalName != null && portalName.length() > 0) {
      SEOService seoService = WCMCoreUtils.getService(SEOService.class);
      sitemapContent = seoService.getSitemap(portalName);
      res.setContentType("text/xml");
      PrintWriter out = res.getWriter();
      out.println(sitemapContent);
    }
  }

  public boolean getRequiresLifeCycle() {
    return true;
  }
}
