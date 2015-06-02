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
package org.exoplatform.wcm.connector.collaboration;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.version.VersionHistory;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Gets the image binary data of a given image node.
 *
 * @LevelAPI Provisional
 * 
 * @anchor RESTImagesRendererService
 */
@Path("/images/")
public class RESTImagesRendererService implements ResourceContainer{

  /** The session provider service. */
  private SessionProviderService sessionProviderService;

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(RESTImagesRendererService.class.getName());

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  /** Default mime type **/
  private static String DEFAULT_MIME_TYPE = "image/jpg";

  /** Mime type property **/
  private static String PROPERTY_MIME_TYPE = "jcr:mimeType";

  /**
   * Instantiates a new REST images renderer service.
   *
   * @param repositoryService The repository service.
   * @param sessionProviderService The session provider service.
   */
  public RESTImagesRendererService(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
  }

  /**
   * Gets the image binary data of a given image node.
   * @param repositoryName The repository.
   * @param workspaceName The workspace.
   * @param nodeIdentifier The node identifier.
   * @param param Checks if the document is a file or not. The default value is "file".
   * @param ifModifiedSince Checks the modification date.
   * @return The response
   *
   * @anchor RESTImagesRendererService.serveImage
   */
  @GET
  @Path("/{repositoryName}/{workspaceName}/{nodeIdentifier}")
  public Response serveImage(@PathParam("repositoryName") String repositoryName,
                                     @PathParam("workspaceName") String workspaceName,
                                     @PathParam("nodeIdentifier") String nodeIdentifier,
                                     @QueryParam("param") @DefaultValue("file") String param,
                                     @HeaderParam("If-Modified-Since") String ifModifiedSince) {
    try {
      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      WCMService wcmService = WCMCoreUtils.getService(WCMService.class);
      Node node = wcmService.getReferencedContent(sessionProvider, workspaceName, nodeIdentifier);
      if (node == null) return Response.status(HTTPStatus.NOT_FOUND).build();

      if ("file".equals(param)) {
        Node dataNode = null;
        if(WCMCoreUtils.isNodeTypeOrFrozenType(node, NodetypeConstant.NT_FILE)) {
          dataNode = node;
        }else if(node.isNodeType("nt:versionedChild")) {
          VersionHistory versionHistory = (VersionHistory)node.getProperty("jcr:childVersionHistory").getNode();
          String versionableUUID = versionHistory.getVersionableUUID();
          dataNode = sessionProvider.getSession(workspaceName,
                                                repositoryService.getCurrentRepository())
                                    .getNodeByUUID(versionableUUID);
        }else {
          return Response.status(HTTPStatus.NOT_FOUND).build();
        }

        if (ifModifiedSince != null && isModified(ifModifiedSince, dataNode) == false) {
          return Response.notModified().build();
        }

        DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);

        Node jcrContentNode = dataNode.getNode("jcr:content");
        String mimeType = DEFAULT_MIME_TYPE;
        if (jcrContentNode.hasProperty(PROPERTY_MIME_TYPE)) {
          mimeType = jcrContentNode.getProperty(PROPERTY_MIME_TYPE).getString();
        }
        InputStream jcrData = jcrContentNode.getProperty("jcr:data").getStream();
        return Response.ok(jcrData, mimeType).header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
      }

      if (ifModifiedSince != null && isModified(ifModifiedSince, node) == false) {
        return Response.notModified().build();
      }

      DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
      InputStream jcrData = node.getProperty(param).getStream();
      return Response.ok(jcrData, DEFAULT_MIME_TYPE).header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

    } catch (PathNotFoundException e) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    }catch (ItemNotFoundException e) {
      return Response.status(HTTPStatus.NOT_FOUND).build();
    }catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when serveImage: ", e);
      }
      return Response.serverError().build();
    }
  }

  /**
   * Gets the last modified date of a node.
  + * @param node A specific node.
  + * @return The last modified date.
  + * @throws Exception
  + */
  private Date getLastModifiedDate(Node node) throws Exception {
     Date lastModifiedDate = null;
     if (node.hasNode("jcr:content") && node.getNode("jcr:content").hasProperty("jcr:lastModified")) {
       lastModifiedDate = node.getNode("jcr:content").getProperty("jcr:lastModified").getDate().getTime();
     } else if (node.hasProperty("exo:dateModified")) {
         lastModifiedDate = node.getProperty("exo:dateModified").getDate().getTime();
     } else if (node.hasProperty("jcr:created")){
       lastModifiedDate = node.getProperty("jcr:created").getDate().getTime();
     }
     return lastModifiedDate;
  }

  /**
   * Checks if resources were modified or not.
   * @param ifModifiedSince The date when the node is modified.
   * @param node A specific node.
   * @return
   * @throws Exception
   */
  private boolean isModified(String ifModifiedSince, Node node) throws Exception {
     // get last-modified-since from header
     DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
     if(ifModifiedSince == null || ifModifiedSince.length() == 0)
       return false;
     try {
       Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);
       // get last modified date of node
       Date lastModifiedDate = getLastModifiedDate(node);
       // Check if cached resource has not been modifed, return 304 code
       if (lastModifiedDate != null && ifModifiedSinceDate != null &&
           ifModifiedSinceDate.getTime() >= lastModifiedDate.getTime()) {
         return false;
       }
       return true;
     } catch(ParseException pe) {
       return false;
     }

  }

}
