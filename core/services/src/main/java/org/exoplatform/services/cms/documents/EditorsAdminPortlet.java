package org.exoplatform.services.cms.documents;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EditorsAdminPortlet extends GenericPortlet {

  private static final Log LOG = ExoLogger.getLogger(EditorsAdminPortlet.class);

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/editorsadmin.jsp");
    prDispatcher.include(request, response);
    // TODO: call JS code
  }
}
