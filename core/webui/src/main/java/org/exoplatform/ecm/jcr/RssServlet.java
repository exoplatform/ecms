/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
/**
 * Created by The eXo Platform SARL        .
 * @author: Nguyen Quang Hung
 * @email: nguyenkequanghung@yahoo.com
 */
@SuppressWarnings({"serial","unused"})
public class RssServlet extends HttpServlet {
  private static final Log LOG  = ExoLogger.getLogger(RssServlet.class);
  public void init(ServletConfig config) throws ServletException {}
  public void service(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setHeader("Cache-Control", "private max-age=600, s-maxage=120");
    String wsName = null ;
    String path = null ;
    String portalName = null ;
    String pathInfo = request.getPathInfo() ;
    portalName = pathInfo.substring(1, pathInfo.indexOf("/", 1)) ;
    wsName = pathInfo.substring(portalName.length() + 2, pathInfo.indexOf("/", portalName.length() + 2)) ;
    path = pathInfo.substring(pathInfo.indexOf(wsName)+ wsName.length() + 1);
    PortalContainer pcontainer =  RootContainer.getInstance().getPortalContainer(portalName);
    PortalContainer.setInstance(pcontainer) ;
    RepositoryService repositoryService =
      (RepositoryService)pcontainer.getComponentInstanceOfType(RepositoryService.class);
    TemplateService tservice =
      (TemplateService)pcontainer.getComponentInstanceOfType(TemplateService.class);
    Session session = null ;
    try{
      session = repositoryService.getCurrentRepository().getSystemSession(wsName) ;
      String repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
      Node rootNode = session.getRootNode() ;
      Node file = null ;
      if(rootNode.hasNode(path))
        file = rootNode.getNode(path) ;
      else if(rootNode.hasNode(path.substring(0, path.lastIndexOf("/")))){
        Node parentNode = rootNode.getNode(path.substring(0, path.lastIndexOf("/"))) ;
        String name = path.substring(path.lastIndexOf("/") + 1) ;
          if(name.indexOf(".") > -1 && parentNode.hasNode(name.substring(0, name.indexOf("."))))
            file = parentNode.getNode(name.substring(0, name.indexOf("."))) ;
      }
      if (file == null) throw new Exception("Node " + path + " not found. ");
      Node content;
      if(file.isNodeType("nt:file")) {
        content = file.getNode("jcr:content");
        Property data = content.getProperty("jcr:data");
        String mimeType = content.getProperty("jcr:mimeType").getString();
        response.setContentType(mimeType) ;
        InputStream is = data.getStream();
        byte[] buf = new byte[is.available()];
        is.read(buf);
        ServletOutputStream os = response.getOutputStream();
        os.write(buf);
      } else if (file.isNodeType("exo:rss-enable")){
        List documentNodeType = tservice.getDocumentTemplates() ;
        String nodeType = file.getPrimaryNodeType().getName() ;
        if(documentNodeType.contains(nodeType)){
          String templateName = tservice.getTemplatePath(false, nodeType, "view1") ;
          request.setAttribute("portalName", portalName) ;
          request.setAttribute("wsName", wsName) ;
          request.setAttribute("templateName", "jcr:"+templateName) ;
          request.setAttribute("curNode", file) ;
          RequestDispatcher rd = request.getRequestDispatcher("/viewcontent");
          rd.forward(request, response);
        }else {
          throw new Exception("This node type is not document node");
        }
      } else throw new Exception("Invalid node type, expected nt:file or exo:rss-enable type");
    }catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      throw new ServletException(e) ;
    }finally{
      if(session != null) {
        session.logout() ;
      }
    }
  }
}
