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
package org.exoplatform.wcm.connector.collaboration;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.cms.thumbnail.impl.ThumbnailUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.connector.viewer.PDFViewerRESTService;

/**
 * Returns a responding data as a thumbnail image.
 * {{{{repoName}}}}: The name of repository.
 * {{{{workspaceName}}}}: The name of workspace.
 * {{{{nodePath}}}}: The node path.
 *
 * {{{{portalname}}}}: The name of the portal.
 * {{{{restcontextname}}}}: The context name of REST web application which is deployed to the "{{{{portalname}}}}" portal.
 *
 * @LevelAPI Provisional
 * @anchor ThumbnailRESTService
 */
@Path("/thumbnailImage/")
public class ThumbnailRESTService implements ResourceContainer {

  /** The log **/
  private static final Log LOG  = ExoLogger.getLogger(ThumbnailRESTService.class.getName());
  
  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  private final RepositoryService repositoryService_;
  private final ThumbnailService thumbnailService_;
  private final NodeFinder nodeFinder_;
  private final LinkManager linkManager_;

  public ThumbnailRESTService(RepositoryService repositoryService,
                              ThumbnailService thumbnailService,
                              NodeFinder nodeFinder,
                              LinkManager linkManager) {
    repositoryService_ = repositoryService;
    thumbnailService_ = thumbnailService;
    nodeFinder_ = nodeFinder;
    linkManager_ = linkManager;
  }

/**
 * Returns an image with a medium size (64x64).
 * For example: /portal/rest/thumbnailImage/medium/repository/collaboration/test.gif/
 *
 * @param repoName The repository name.
 * @param workspaceName The workspace name.
 * @param nodePath The node path.
 * @return Response inputstream.
 * @throws Exception
 *
 * @anchor ThumbnailRESTService.getThumbnailImage
 */
  @Path("/medium/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getThumbnailImage(@PathParam("repoName") String repoName,
                                    @PathParam("workspaceName") String workspaceName,
                                    @PathParam("nodePath") String nodePath,
                                    @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    return getThumbnailByType(workspaceName,
                              nodePath,
                              ThumbnailService.MEDIUM_SIZE,
                              ifModifiedSince);
  }

/**
 * Returns an image with a big size.
 *
 * @param repoName The repository name.
 * @param workspaceName The workspace name.
 * @param nodePath The node path.
 * @return Response inputstream.
 * @throws Exception
 *
 * @anchor ThumbnailRESTService.getCoverImage
 */
  @Path("/big/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getCoverImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    return getThumbnailByType(workspaceName, nodePath, ThumbnailService.BIG_SIZE, ifModifiedSince);
  }

/**
 * Returns an image with a large size (300x300).
 *
 * @param repoName The repository name.
 * @param workspaceName The workspace name.
 * @param nodePath The node path.
 * @return Response inputstream.
 * @throws Exception
 *
 * @anchor ThumbnailRESTService.getLargeImage
 */
  @Path("/large/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getLargeImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    return getThumbnailByType(workspaceName, nodePath, ThumbnailService.BIG_SIZE, ifModifiedSince);
  }

/**
 * Returns an image with a small size (32x32).
 *
 * @param repoName The repository name.
 * @param workspaceName The workspace name.
 * @param nodePath The node path.
 * @return Response inputstream.
 * @throws Exception
 *
 * @anchor ThumbnailRESTService.getSmallImage
 */
  @Path("/small/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getSmallImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    return getThumbnailByType(workspaceName, nodePath, ThumbnailService.SMALL_SIZE, ifModifiedSince);
  }

/**
 * Returns an image with a custom size.
 *
 * @param size The customized size of the image.
 * @param repoName The repository name.
 * @param workspaceName The workspace name.
 * @param nodePath The node path.
 * @return Response inputstream.
 * @throws Exception
 *
 * @anchor ThumbnailRESTService.getCustomImage
 */
  @Path("/custom/{size}/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getCustomImage(@PathParam("size") String size,
                                @PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    return getThumbnailByType(workspaceName, nodePath, "exo:"+size, ifModifiedSince);
  }

  /**
   * Returns an image with an original size.
   *
   * @param repoName The repository name.
   * @param workspaceName The workspace name.
   * @param nodePath The node path.
   * @return Response data stream.
   * @throws Exception
   *
   * @anchor ThumbnailRESTService.getOriginImage
   */
  @Path("/origin/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getOriginImage(@PathParam("repoName") String repoName,
                                 @PathParam("workspaceName") String workspaceName,
                                 @PathParam("nodePath") String nodePath,
                                 @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    if (!thumbnailService_.isEnableThumbnail())
      return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
    Node showingNode = getShowingNode(workspaceName, getNodePath(nodePath));
    Node targetNode = getTargetNode(showingNode);
    if (targetNode.isNodeType("nt:file") || targetNode.isNodeType("nt:resource")) {
      Node content = targetNode;
      if (targetNode.isNodeType("nt:file"))
        content = targetNode.getNode("jcr:content");
      if (ifModifiedSince != null) {
        // get last-modified-since from header
        Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);

        // get last modified date of node
        Date lastModifiedDate = content.getProperty("jcr:lastModified").getDate().getTime();

        // Check if cached resource has not been modifed, return 304 code
        if (ifModifiedSinceDate.getTime() >= lastModifiedDate.getTime()) {
          return Response.notModified().build();
        }
      }

      String mimeType = content.getProperty("jcr:mimeType").getString();
      for (ComponentPlugin plugin : thumbnailService_.getComponentPlugins()) {
        if (plugin instanceof ThumbnailPlugin) {
          ThumbnailPlugin thumbnailPlugin = (ThumbnailPlugin) plugin;
          if (thumbnailPlugin.getMimeTypes().contains(mimeType)) {
            InputStream inputStream = content.getProperty("jcr:data").getStream();
            return Response.ok(inputStream, "image").header(LAST_MODIFIED_PROPERTY,
                                                            dateFormat.format(new Date())).build();
          }
        }
      }
    }

    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }
  
  /**
   * Return an image at an original size from local machine.
   *
   * @param nodePath The node path.
   * @return Response data stream.
   * @throws Exception
   *
   * @anchor ThumbnailRESTService.getOriginImage
   */
  @Path("/originImage/{nodePath:.*}/")
  @GET
  public Response getLocalImage(@PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    if (!thumbnailService_.isEnableThumbnail())
      return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

    
    try {
      File file = new File(nodePath);
      FileInputStream inputStream = new FileInputStream(file);
      BufferedInputStream buf = new BufferedInputStream(inputStream);
      
      return Response.ok(buf, "image").header(LAST_MODIFIED_PROPERTY,
                                                          dateFormat.format(new Date())).build();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  private Response getThumbnailByType(String workspaceName, String nodePath,
      String propertyName, String ifModifiedSince) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    if (!thumbnailService_.isEnableThumbnail())
      return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
    Node showingNode = getShowingNode(workspaceName, getNodePath(nodePath));
    Node parentNode = showingNode.getParent();
    String identifier = ((NodeImpl) showingNode).getInternalIdentifier();
    Node targetNode = getTargetNode(showingNode);
    if(targetNode.isNodeType("nt:file")) {
      Node content = targetNode.getNode("jcr:content");
      String mimeType = content.getProperty("jcr:mimeType").getString();
      for(ComponentPlugin plugin : thumbnailService_.getComponentPlugins()) {
        if(plugin instanceof ThumbnailPlugin) {
          ThumbnailPlugin thumbnailPlugin = (ThumbnailPlugin) plugin;
          if(thumbnailPlugin.getMimeTypes().contains(mimeType)) {
            Node thumbnailFolder = ThumbnailUtils.getThumbnailFolder(parentNode);

            Node thumbnailNode = ThumbnailUtils.getThumbnailNode(thumbnailFolder, identifier);

            if(!thumbnailNode.hasProperty(propertyName)) {
              try {
                BufferedImage image = thumbnailPlugin.getBufferedImage(content, targetNode.getPath());
                thumbnailService_.addThumbnailImage(thumbnailNode, image, propertyName);
              } catch (Exception e) {
            	throw new Exception("Failed to get image.", e);            	
              }
            }

            if(ifModifiedSince != null && thumbnailNode.hasProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED)) {
              // get last-modified-since from header
              Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);

              // get last modified date of node
              Date lastModifiedDate = thumbnailNode.getProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED)
                                                   .getDate()
                                                   .getTime();

              // Check if cached resource has not been modifed, return 304 code
              if (ifModifiedSinceDate.getTime() >= lastModifiedDate.getTime()) {
                return Response.notModified().build();
              }
            }
            InputStream inputStream = null;
            if(thumbnailNode.hasProperty(propertyName)) {
              inputStream = thumbnailNode.getProperty(propertyName).getStream();
            }
            return Response.ok(inputStream, "image").header(LAST_MODIFIED_PROPERTY,
                                                            dateFormat.format(new Date())).build();
          }
        }
      }
    }
    return getThumbnailRes(parentNode, identifier, propertyName, ifModifiedSince);
  }

  private Response getThumbnailRes(Node parentNode,
                                   String identifier,
                                   String propertyName,
                                   String ifModifiedSince) throws Exception {

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    if(parentNode.hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER)) {
      Node thumbnailFolder = parentNode.getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
      if(thumbnailFolder.hasNode(identifier)) {
        Node thumbnailNode = thumbnailFolder.getNode(identifier);
        if (ifModifiedSince != null && thumbnailNode.hasProperty("exo:dateModified")) {
          // get last-modified-since from header
          Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);

          // get last modified date of node
          Date lastModifiedDate = thumbnailNode.getProperty("exo:dateModified").getDate().getTime();

          // Check if cached resource has not been modified, return 304 code
          if (ifModifiedSinceDate.getTime() >= lastModifiedDate.getTime()) {
            return Response.notModified().build();
          }
        }

        if(thumbnailNode.hasProperty(propertyName)) {
          InputStream inputStream = thumbnailNode.getProperty(propertyName).getStream();
          return Response.ok(inputStream, "image").header(LAST_MODIFIED_PROPERTY,
                                                          dateFormat.format(new Date())).build();
        }
      }
    }
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  private String getNodePath(String nodePath) throws Exception {
    ArrayList<String> encodeNameArr = new ArrayList<String>();
    if(!nodePath.equals("/")) {
      for(String name : nodePath.split("/")) {
        if(name.length() > 0) {
          encodeNameArr.add(Text.escapeIllegalJcrChars(name));
        }
      }
      StringBuilder encodedPath = new StringBuilder();
      for(String encodedName : encodeNameArr) {
        encodedPath.append("/").append(encodedName);
      }
      nodePath = encodedPath.toString();
    }
    return nodePath;
  }

  private Node getTargetNode(Node showingNode) throws Exception {
    Node targetNode = null;
    if (linkManager_.isLink(showingNode)) {
      try {
        targetNode = linkManager_.getTarget(showingNode);
      } catch (ItemNotFoundException e) {
        targetNode = showingNode;
      }
    } else {
      targetNode = showingNode;
    }
    return targetNode;
  }

  private Node getShowingNode(String workspaceName, String nodePath) throws Exception {
    ManageableRepository repository = repositoryService_.getCurrentRepository();
    Session session = getSystemProvider().getSession(workspaceName, repository);
    Node showingNode = null;
    if(nodePath.equals("/")) showingNode = session.getRootNode();
    else {
      showingNode = (Node) nodeFinder_.getItem(session, nodePath);
    }
    return showingNode;
  }

  private SessionProvider getSystemProvider() {
    SessionProviderService service = WCMCoreUtils.getService(SessionProviderService.class);
    return service.getSystemSessionProvider(null) ;
  }
}
