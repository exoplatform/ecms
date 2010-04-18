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
package org.exoplatform.services.cms.thumbnail.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 23, 2008 11:09:39 AM
 */
/**
 * Provide the request which will be used to get the response data
 * {repoName} Repository name
 * {workspaceName} Name of workspace
 * {nodePath} The node path
 * Example: 
 * <img src="/portal/rest/thumbnailImage/repository/collaboration/test.gif" />
 */
@Path("/thumbnailImage/")
public class ThumbnailRESTService implements ResourceContainer {
  
  private static final String LASTMODIFIED = "Last-Modified";
  
  private final RepositoryService repositoryService_;
  private final ThumbnailService thumbnailService_;
  private final NodeFinder nodeFinder_;
  private final LinkManager linkManager_;
  
  public ThumbnailRESTService(RepositoryService repositoryService, ThumbnailService thumbnailService, NodeFinder nodeFinder, LinkManager linkManager) {
    repositoryService_ = repositoryService;
    thumbnailService_ = thumbnailService;
    nodeFinder_ = nodeFinder;
    linkManager_ = linkManager;
  }
/**
 * Get the image with medium size
 * ex: /portal/rest/thumbnailImage/medium/repository/collaboration/test.gif/
 * @param repoName Repository name
 * @param wsName Workspace name
 * @param nodePath Node path
 * @return Response inputstream
 * @throws Exception
 */  
  
  @Path("/medium/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getThumbnailImage(@PathParam("repoName") String repoName, 
                                    @PathParam("workspaceName") String wsName,
                                    @PathParam("nodePath") String nodePath) throws Exception {
    return getThumbnailByType(repoName, wsName, nodePath, ThumbnailService.MEDIUM_SIZE);
  }
  
/**
 * Get the image with big size
 * ex: /portal/rest/thumbnailImage/big/repository/collaboration/test.gif/
 * @param repoName Repository name
 * @param wsName Workspace name
 * @param nodePath Node path
 * @return Response inputstream
 * @throws Exception
 */   
  @Path("/big/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getCoverImage(@PathParam("repoName") String repoName, 
                                @PathParam("workspaceName") String wsName,
                                @PathParam("nodePath") String nodePath) throws Exception {
    return getThumbnailByType(repoName, wsName, nodePath, ThumbnailService.BIG_SIZE);
  }
  
/**
 * Get the image with small size
 * ex: /portal/rest/thumbnailImage/small/repository/collaboration/test.gif/
 * @param repoName Repository name
 * @param wsName Workspace name
 * @param nodePath Node path
 * @return Response inputstream
 * @throws Exception
 */   
  @Path("/small/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getSmallImage(@PathParam("repoName") String repoName, 
                                @PathParam("workspaceName") String wsName,
                                @PathParam("nodePath") String nodePath) throws Exception {
    return getThumbnailByType(repoName, wsName, nodePath, ThumbnailService.SMALL_SIZE);
  }
  
  /**
   * Get the image with origin data
   * ex: /portal/rest/thumbnailImage/origin/repository/collaboration/test.gif/
   * @param repoName Repository name
   * @param wsName Workspace name
   * @param nodePath Node path
   * @return Response data stream
   * @throws Exception
   */   
  @Path("/origin/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getOriginImage(@PathParam("repoName") String repoName,
                                 @PathParam("workspaceName") String wsName, 
                                 @PathParam("nodePath") String nodePath) throws Exception {
    if (!thumbnailService_.isEnableThumbnail())
      return Response.ok().build();
    Node showingNode = getShowingNode(repoName, wsName, getNodePath(nodePath));
    Node targetNode = getTargetNode(showingNode);
    if (targetNode.getPrimaryNodeType().getName().equals("nt:file")) {
      Node content = targetNode.getNode("jcr:content");
      String mimeType = content.getProperty("jcr:mimeType").getString();
      for (ComponentPlugin plugin : thumbnailService_.getComponentPlugins()) {
        if (plugin instanceof ThumbnailPlugin) {
          ThumbnailPlugin thumbnailPlugin = (ThumbnailPlugin) plugin;
          if (thumbnailPlugin.getMimeTypes().contains(mimeType)) {
            String lastModified = content.getProperty("jcr:lastModified").getDate().getTime()
            .toString();
            InputStream inputStream = content.getProperty("jcr:data").getStream();
            return Response.ok(inputStream, "image").header(LASTMODIFIED, lastModified).build();
          }
        }
      }
    }
    return Response.ok().build();
  }
    
  private Response getThumbnailByType(String repoName, String wsName, String nodePath, 
      String propertyName) throws Exception {
    if(!thumbnailService_.isEnableThumbnail()) return Response.ok().build();
    Node showingNode = getShowingNode(repoName, wsName, getNodePath(nodePath));
    Node parentNode = showingNode.getParent();
    String identifier = ((NodeImpl) showingNode).getInternalIdentifier();
    Node targetNode = getTargetNode(showingNode);
    if(targetNode.getPrimaryNodeType().getName().equals("nt:file")) {
      Node content = targetNode.getNode("jcr:content");
      String mimeType = content.getProperty("jcr:mimeType").getString();
      for(ComponentPlugin plugin : thumbnailService_.getComponentPlugins()) {
        if(plugin instanceof ThumbnailPlugin) {
          ThumbnailPlugin thumbnailPlugin = (ThumbnailPlugin) plugin;
          if(thumbnailPlugin.getMimeTypes().contains(mimeType)) {
            Node thumbnailFolder = ThumbnailUtils.getThumbnailFolder(parentNode);
            
            Node thumbnailNode = ThumbnailUtils.getThumbnailNode(thumbnailFolder, identifier);
            
            if(!thumbnailNode.hasProperty(propertyName)) {
              BufferedImage image = thumbnailPlugin.getBufferedImage(content, targetNode.getPath());
              thumbnailService_.addThumbnailImage(thumbnailNode, image, propertyName);
            }
            String lastModified = null;
            if(thumbnailNode.hasProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED)) {
              lastModified = thumbnailNode.getProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED).getString();
            }
            InputStream inputStream = null;
            if(thumbnailNode.hasProperty(propertyName)) {
              inputStream = thumbnailNode.getProperty(propertyName).getStream();
            }
            return Response.ok(inputStream, "image").header(LASTMODIFIED, lastModified).build();
          }
        }
      }
    }
    return getThumbnailRes(parentNode, identifier, propertyName);
  }
  
  private Response getThumbnailRes(Node parentNode, String identifier, String propertyName) throws Exception{
    if(parentNode.hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER)) {
      Node thumbnailFolder = parentNode.getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
      if(thumbnailFolder.hasNode(identifier)) {
        Node thumbnailNode = thumbnailFolder.getNode(identifier);
        if(thumbnailNode.hasProperty(propertyName)) {
          InputStream inputStream = thumbnailNode.getProperty(propertyName).getStream();
          return Response.ok(inputStream, "image").build();
        }
      }
    }
    return Response.ok().build();
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
  
  private Node getShowingNode(String repoName, String wsName, String nodePath) throws Exception {
    ManageableRepository repository = repositoryService_.getRepository(repoName);
    Session session = getSystemProvider().getSession(wsName, repository);
    Node showingNode = null;
    Node root = session.getRootNode();
    root.getNodes();
    if(nodePath.equals("/")) showingNode = session.getRootNode();
    else {
      showingNode = (Node) nodeFinder_.getItem(session, nodePath);
    }
    return showingNode;
  }

  private SessionProvider getSystemProvider() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    return service.getSystemSessionProvider(null) ;  
  }
}
