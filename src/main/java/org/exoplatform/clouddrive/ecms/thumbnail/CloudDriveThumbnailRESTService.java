/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms.thumbnail;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.wcm.connector.collaboration.ThumbnailRESTService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveThumbnailRESTService.java 00000 May 21, 2014 pnedonosko $
 */
@Path("/thumbnailImage/")
public class CloudDriveThumbnailRESTService extends ThumbnailRESTService {

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String       LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final DateFormat   DATE_FORMAT            = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

  /** The cloud drives. */
  protected final CloudDriveService cloudDrives;

  /**
   * Instantiates a new cloud drive thumbnail REST service.
   *
   * @param repositoryService the repository service
   * @param thumbnailService the thumbnail service
   * @param nodeFinder the node finder
   * @param linkManager the link manager
   * @param cloudDrives the cloud drives
   */
  public CloudDriveThumbnailRESTService(RepositoryService repositoryService,
                                        ThumbnailService thumbnailService,
                                        NodeFinder nodeFinder,
                                        LinkManager linkManager,
                                        CloudDriveService cloudDrives) {
    super(repositoryService, thumbnailService, nodeFinder, linkManager);
    this.cloudDrives = cloudDrives;
  }

  /**
   * {@inheritDoc}
   */
  @Path("/medium/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  @Override
  public Response getThumbnailImage(@PathParam("repoName") String repoName,
                                    @PathParam("workspaceName") String workspaceName,
                                    @PathParam("nodePath") String nodePath,
                                    @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getThumbnailImage(repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Path("/big/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  @Override
  public Response getCoverImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getCoverImage(repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Path("/large/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  @Override
  public Response getLargeImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getLargeImage(repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Path("/small/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  public Response getSmallImage(@PathParam("repoName") String repoName,
                                @PathParam("workspaceName") String workspaceName,
                                @PathParam("nodePath") String nodePath,
                                @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getSmallImage(repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Path("/custom/{size}/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  @Override
  public Response getCustomImage(@PathParam("size") String size,
                                 @PathParam("repoName") String repoName,
                                 @PathParam("workspaceName") String workspaceName,
                                 @PathParam("nodePath") String nodePath,
                                 @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getCustomImage(size, repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Path("/origin/{repoName}/{workspaceName}/{nodePath:.*}/")
  @GET
  @Override
  public Response getOriginImage(@PathParam("repoName") String repoName,
                                 @PathParam("workspaceName") String workspaceName,
                                 @PathParam("nodePath") String nodePath,
                                 @HeaderParam("If-Modified-Since") String ifModifiedSince) throws Exception {
    if (accept(workspaceName, nodePath)) {
      return super.getOriginImage(repoName, workspaceName, nodePath, ifModifiedSince);
    } else {
      return ok();
    }
  }

  // internals

  /**
   * Accept.
   *
   * @param workspaceName the workspace name
   * @param nodePath the node path
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  protected boolean accept(String workspaceName, String nodePath) throws RepositoryException {
    return cloudDrives.findDrive(workspaceName, getNodePath(nodePath)) == null;
  }

  /**
   * Ok.
   *
   * @return the response
   */
  protected Response ok() {
    return Response.ok().header(LAST_MODIFIED_PROPERTY, DATE_FORMAT.format(new Date())).build();
  }

  /**
   * Method copied from {@link ThumbnailRESTService}.
   *
   * @param nodePath the node path
   * @return the node path
   */
  private String getNodePath(String nodePath) {
    ArrayList<String> encodeNameArr = new ArrayList<String>();
    if (!nodePath.equals("/")) {
      for (String name : nodePath.split("/")) {
        if (name.length() > 0) {
          encodeNameArr.add(Text.escapeIllegalJcrChars(name));
        }
      }
      StringBuilder encodedPath = new StringBuilder();
      for (String encodedName : encodeNameArr) {
        encodedPath.append("/").append(encodedName);
      }
      nodePath = encodedPath.toString();
    }
    return nodePath;
  }

}
