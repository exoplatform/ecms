package org.exoplatform.wcm.webui.friendly;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class FriendlyServlet extends HttpServlet {

  /**
   *
   */
  private static final long serialVersionUID = 440086446956963128L;

  public void destroy() {
  }

  public ServletConfig getServletConfig() {
    return null;
  }

  public String getServletInfo() {
    return null;
  }

  public void init(ServletConfig arg0) throws ServletException {
  }

  protected void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {

    FriendlyService fs = WCMCoreUtils.getService(FriendlyService.class);

    String path = request.getRequestURI();
    path = fs.getUnfriendlyUri(path);
    request.getRequestDispatcher(path).forward(request, response);

  }

}
