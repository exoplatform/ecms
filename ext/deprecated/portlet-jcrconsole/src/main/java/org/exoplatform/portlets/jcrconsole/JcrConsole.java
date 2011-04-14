/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portlets.jcrconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.frameworks.jcr.cli.CliAppContext;
import org.exoplatform.services.command.impl.CommandService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


/**
 * Created by The eXo Platform SARL .
 *
 * @author <a href="mailto:uy7c@yahoo.com">Max Shaposhnik</a>
 * @version $Id$
 */

public class JcrConsole extends GenericPortlet {

  //private CliAppContext context;
  private static final Log LOG  = ExoLogger.getLogger("JcrConsole");

  protected void doView(RenderRequest renderRequest,
      RenderResponse renderResponse) throws PortletException, IOException {

    ExoContainer container = ExoContainerContext.getCurrentContainer();

    renderResponse.setContentType("text/html; charset=UTF-8");
    PortletContext context = getPortletContext();

    ResourceURL resourceURL = renderResponse.createResourceURL();
    String resourceString = resourceURL.toString();


    while (resourceString.indexOf("&amp;") != -1 )
    {
      resourceString = resourceString.replace("&amp;", "&");
    }

    PrintWriter w = renderResponse.getWriter();
    w.println("<SCRIPT LANGUAGE=\"JavaScript\" TYPE=\"text/javascript\" SRC=\"/jcr-console/scripts/console.js\"></SCRIPT>");
    w.println("<LINK REL=\"stylesheet\"  HREF=\"/jcr-console/styles/styles.css\" TYPE=\"text/css\">");
    w.println("<DIV ID=\"termDiv\" STYLE=\"position:relative; top:20px; left:100px;\"></DIV>");
    w.println("<SCRIPT LANGUAGE=\"JavaScript\">");
    w.println("var action =\"" + resourceString + "\";");
    w.println("termOpen();");
    w.println("</SCRIPT>");
  }

  private void parseQuery(String query, ArrayList params) {
    try {
      params.clear();
      if (query.indexOf("\"") == -1) {
        while (!query.equals("")) {
          String item = query.substring(0, (query.indexOf(" ") < 0) ? query.length() : query
              .indexOf(" "));
          params.add(item);
          query = query.substring(query.indexOf(item) + item.length());
          query = query.trim();
        }
      } else {
        while (!query.equals("")) {
          String item = "";
          if (query.startsWith("\"")) {
            item = query.substring(query.indexOf("\"") + 1, (query.indexOf("\"", 1) < 0) ? query
                .length() : query.indexOf("\"", 1));
          } else {
            item = query.substring(0, (query.indexOf(" ") < 0) ? query.length() : query
                .indexOf(" "));
          }
          item = item.trim();
          if (item != null && !(item.equals(""))) {
            params.add(item);
          }
          int index = query.indexOf(item) + item.length() + 1;
          if (query.length() > index) {
            query = query.substring(query.indexOf(item) + item.length() + 1);
            query = query.trim();
          } else {
            query = "";
          }
        }
      }
    } catch (Exception exception) {
      LOG.error("Unexpected error", exception);
    }
  }

  public void processAction(ActionRequest actionRequest,
      ActionResponse actionResponse) throws PortletException, IOException {
  }

  public void serveResource (ResourceRequest resourceRequest, ResourceResponse resourceResponse)
  throws PortletException, IOException {

    CliAppContext context = (CliAppContext) resourceRequest.getPortletSession().getAttribute("context");

    //CliAppContext context = null;

    ArrayList<String> params = new ArrayList<String>();
    String PARAMETERS_KEY = "parameterss";
    resourceResponse.setContentType("text/html");
    PrintWriter printWriter = resourceResponse.getWriter();
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();

      String commandLine = resourceRequest.getParameter("myaction").trim();
      String commandFromCommandLine = commandLine.substring(0,
          (commandLine.indexOf(" ") < 0) ? commandLine.length() : commandLine.indexOf(" "));

      commandLine = commandLine.substring(commandLine.indexOf(commandFromCommandLine)
          + commandFromCommandLine.length());
      commandLine = commandLine.trim();
      CommandService cservice = (CommandService) container
          .getComponentInstanceOfType(CommandService.class);
      Catalog catalog = cservice.getCatalog("CLI");

      parseQuery(commandLine, params);

      if (context == null) {
        RepositoryService repService = (RepositoryService) container
            .getComponentInstanceOfType(RepositoryService.class);

        String workspace = repService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();

        context = new CliAppContext(repService.getCurrentRepository(), PARAMETERS_KEY);
        context.setCurrentWorkspace(workspace);
        context.setCurrentItem(context.getSession().getRootNode());
      }
      Command commandToExecute = catalog.getCommand(commandFromCommandLine);
      context.put(PARAMETERS_KEY, params);
      if (commandToExecute != null) {
          commandToExecute.execute(context);
          printWriter.print(context.getOutput());
      } else {
        printWriter.print("Command not found \n");
      }
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
      LOG.error("[ERROR] [jcr-concole] Can't execute command - " + e.getMessage());
      printWriter.print("Invalid command\n");
    }
    finally {
      resourceRequest.getPortletSession().setAttribute("context", context);
    }
  }
}
