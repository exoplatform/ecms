
package org.exoplatform.clouddrive.webui;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.filter.Filter;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveContextFilter.java 00000 Nov 17, 2016 pnedonosko $
 * 
 */
public class CloudDriveContextFilter implements Filter {

  protected static final Logger LOG = LoggerFactory.getLogger(CloudDriveContextFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {

    PortalContainer container = PortalContainer.getInstance();
    WebAppController controller = (WebAppController) container.getComponentInstanceOfType(WebAppController.class);
    PortalApplication app = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);

    final CloudDriveLifecycle lifecycle = new CloudDriveLifecycle();
    try {
      app.getApplicationLifecycle().add(lifecycle);
      chain.doFilter(request, response);
    } finally {
      app.getApplicationLifecycle().remove(lifecycle);
    }
  }

}
