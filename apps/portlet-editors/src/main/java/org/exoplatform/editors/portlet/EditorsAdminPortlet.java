package org.exoplatform.editors.portlet;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class EditorsAdminPortlet extends GenericPortlet {

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/editors-admin.jsp");
    prDispatcher.include(request, response);
  }
}
