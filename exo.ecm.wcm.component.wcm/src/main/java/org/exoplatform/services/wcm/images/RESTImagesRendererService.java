/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.images;

import java.io.InputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.version.VersionHistory;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 31, 2009
 */
@Path("/images/")
public class RESTImagesRendererService implements ResourceContainer{

  /** The session provider service. */
  private SessionProviderService sessionProviderService;
  
  /** The repository service. */
  private RepositoryService repositoryService;
  
  /** The log. */
  static Log log = ExoLogger.getLogger(RESTImagesRendererService.class);

  /**
   * Instantiates a new rEST images renderer service.
   * 
   * @param repositoryService the repository service
   * @param sessionProviderService the session provider service
   */
  public RESTImagesRendererService(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  /**
   * Serve image.
   * 
   * @param repository the repository
   * @param workspace the workspace
   * @param nodeIdentifier the node identifier
   * 
   * @return the response
   */
  @GET
  @Path("/{repositoryName}/{workspaceName}/{nodeIdentifier}")
  public Response serveImage(@PathParam("repositoryName") String repository, 
                                     @PathParam("workspaceName") String workspace,
                                     @PathParam("nodeIdentifier") String nodeIdentifier,
                                     @QueryParam("param") @DefaultValue("file") String param) { 
    try {
      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      WCMService wcmService = WCMCoreUtils.getService(WCMService.class);
      Node node = wcmService.getReferencedContent(sessionProvider, repository, workspace, nodeIdentifier);
      if (node == null) return Response.status(HTTPStatus.NOT_FOUND).build();
      
      
      if ("file".equals(param)) {
        Node dataNode = null; 
        if(node.isNodeType("nt:file")) {
          dataNode = node;
        }else if(node.isNodeType("nt:versionedChild")) {
          VersionHistory versionHistory = (VersionHistory)node.getProperty("jcr:childVersionHistory").getNode();
          String versionableUUID = versionHistory.getVersionableUUID();
          dataNode = sessionProvider.getSession(workspace,repositoryService.getRepository(repository)).getNodeByUUID(versionableUUID);
        }else {
          return Response.status(HTTPStatus.NOT_FOUND).build();
        }     
        InputStream jcrData = dataNode.getNode("jcr:content").getProperty("jcr:data").getStream();
        return Response.ok(jcrData, "image").build();  
      } else {
        InputStream jcrData = node.getProperty(param).getStream();
        return Response.ok(jcrData, "image").build();        
      }
      
    } catch (PathNotFoundException e) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    }catch (ItemNotFoundException e) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    }catch (Exception e) {
      log.error("Error when serveImage: ", e.fillInStackTrace());
      return Response.serverError().build(); 
    }
  }

  /**
   * Generate uri.
   * 
   * @param file the node
   * @param propertyName the image property name, null if file is an image node
   * 
   * @return the string
   * 
   * @throws Exception the exception
   */
  public String generateImageURI(Node file, String propertyName) throws Exception {
    StringBuilder builder = new StringBuilder();
    NodeLocation fielLocation = NodeLocation.make(file);
    String repository = fielLocation.getRepository();
    String workspaceName = fielLocation.getWorkspace();
    String nodeIdentifiler = file.isNodeType("mix:referenceable") ? file.getUUID() : file.getPath().replaceFirst("/","");
    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    
    if (propertyName == null) {
      if(!file.isNodeType("nt:file")) throw new UnsupportedOperationException("The node isn't nt:file");
      InputStream stream = file.getNode("jcr:content").getProperty("jcr:data").getStream();
      if (stream.available() == 0) return null;
      stream.close();
      builder.append("/").append(portalName).append("/")
             .append(restContextName).append("/")
             .append("images/")
             .append(repository).append("/")
             .append(workspaceName).append("/")
             .append(nodeIdentifiler)
             .append("?param=file");
      return builder.toString();
    } else {
      builder.append("/").append(portalName).append("/")
             .append(restContextName).append("/")
             .append("images/")
             .append(repository).append("/")
             .append(workspaceName).append("/")
             .append(nodeIdentifiler)
             .append("?param=").append(propertyName);
      return builder.toString();
    }
  }
  
  @Deprecated
  public String generateURI(Node file) throws Exception {
    return generateImageURI(file, null); 
  }
  
  @Deprecated
  public String generateURI(Node file, String propertyName) throws Exception {
    return generateImageURI(file, propertyName); 
  }
  
}
