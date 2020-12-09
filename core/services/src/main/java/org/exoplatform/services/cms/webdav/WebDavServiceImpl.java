/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.cms.webdav;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.webdav.util.InitParamsDefaults;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.ext.webdav.method.OPTIONS;
import org.exoplatform.services.rest.ext.webdav.method.*;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This class is used to override the default WebDavServiceImpl in order to support symlinks
 *
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 9 avr. 2009
 */
@Path("/jcr/")
public class WebDavServiceImpl extends org.exoplatform.services.jcr.webdav.WebDavServiceImpl {

  /**
   * Logger.
   */
  private static final Log LOG = ExoLogger.getLogger(WebDavServiceImpl.class.getName());

  private final String POST_UPLOAD_CONTENT_EVENT = "WebDavService.event.postUpload";

  private final String PERSONAL_DRIVE_PREFIX = "/Users/${userId}/Private";

  private final String GROUP_DRIVE_PREFIX = "/Groups${groupId}/Documents";

  private final String PERSONAL_GROUP_DRIVE_WORKSPACE = "collaboration";


   private final NodeFinder nodeFinder;

  private final RepositoryService repositoryService;

  private ListenerService listenerService;

  private final MimeTypeResolver mimeTypeResolver;

   public WebDavServiceImpl(InitParams params,
                            RepositoryService repositoryService,
                            ThreadLocalSessionProviderService sessionProviderService,
                            NodeFinder nodeFinder, AutoVersionService autoVersionService, ManageDriveService manageDriveService) throws Exception
   {
      super(params, repositoryService, sessionProviderService);
      this.repositoryService = repositoryService;
      this.nodeFinder = nodeFinder;
      this.listenerService = WCMCoreUtils.getService(ListenerService.class);
      this.mimeTypeResolver = new MimeTypeResolver();
      this.mimeTypeResolver.setDefaultMimeType(InitParamsDefaults.FILE_MIME_TYPE);

      List<String> lstDriveAutoVersion = autoVersionService.getDriveAutoVersion();
      MultivaluedMap<String, String> allowedAutoVersionPath = new MultivaluedMapImpl();
      if (!lstDriveAutoVersion.isEmpty())
      {
         for (String driverName : lstDriveAutoVersion)
         {
            DriveData driveData = manageDriveService.getDriveByName(StringUtils.trim(driverName));
            if (driveData != null)
            {
               String driveHome = driveData.getHomePath();
               String workspace = driveData.getWorkspace();

               if (driveHome.startsWith(PERSONAL_DRIVE_PREFIX) && PERSONAL_GROUP_DRIVE_WORKSPACE.equals(workspace))
               {
                  allowedAutoVersionPath.add(driveData.getWorkspace(), "/Users");
               }
               else if (driveHome.startsWith(GROUP_DRIVE_PREFIX) && PERSONAL_GROUP_DRIVE_WORKSPACE.equals(workspace))
               {
                  allowedAutoVersionPath.add(driveData.getWorkspace(), "/Groups");
               }
               else
               {
                  allowedAutoVersionPath.add(driveData.getWorkspace(), driveHome);
               }
            }
         }
      }
      webDavServiceInitParams.setAllowedAutoVersionPath(allowedAutoVersionPath);
      webDavServiceInitParams.setEnableAutoVersion(true);
   }

  private String getRealDestinationHeader(String baseURI, String repoName, String destinationHeader) {
    String serverURI = baseURI + "/jcr/" + repoName;

    destinationHeader = TextUtil.unescape(destinationHeader, '%');

    if (!destinationHeader.startsWith(serverURI)) {
      return null;
    }

    String destPath = destinationHeader.substring(serverURI.length() + 1);

    try {
      Item item = nodeFinder.getItem(workspaceName(destPath),
                                     LinkUtils.getParentPath(path(destPath)),
                                     true);
      return item.getSession().getWorkspace().getName()
          + LinkUtils.createPath(item.getPath(), LinkUtils.getItemName(path(destPath)));
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + destPath, e);
      }
      return null;
    }
  }

  @CHECKIN
  @Path("/{repoName}/{repoPath:.*}/")
  public Response checkin(@PathParam("repoName") String repoName,
                          @PathParam("repoPath") String repoPath,
                          @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                          @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.checkin(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  @CHECKOUT
  @Path("/{repoName}/{repoPath:.*}/")
  public Response checkout(@PathParam("repoName") String repoName,
                           @PathParam("repoPath") String repoPath,
                           @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                           @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {
    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.checkout(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  @COPY
  @Path("/{repoName}/{repoPath:.*}/")
  public Response copy(@PathParam("repoName") String repoName,
                       @PathParam("repoPath") String repoPath,
                       @HeaderParam(ExtHttpHeaders.DESTINATION) String destinationHeader,
                       @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                       @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                       @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
                       @HeaderParam(ExtHttpHeaders.OVERWRITE) String overwriteHeader,
                       @Context UriInfo uriInfo,
                       HierarchicalProperty body) {

    try {
      repoPath = convertRepoPath(repoPath, false);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    String realDestinationHeader = getRealDestinationHeader(uriInfo.getPath(), repoName, destinationHeader);
    if (realDestinationHeader != null) {
      destinationHeader = realDestinationHeader;
    }
    return super.copy(repoName,
                      repoPath,
                      destinationHeader,
                      lockTokenHeader,
                      ifHeader,
                      depthHeader,
                      overwriteHeader,
                      uriInfo,
                      body);
  }

  @GET
  @Path("/{repoName}/{repoPath:.*}/")
  public Response get(@PathParam("repoName") String repoName,
                      @PathParam("repoPath") String repoPath,
                      @HeaderParam(ExtHttpHeaders.RANGE) String rangeHeader,
                      @HeaderParam(ExtHttpHeaders.IF_MODIFIED_SINCE) String ifModifiedSince,
                      @HeaderParam(ExtHttpHeaders.IF_NONE_MATCH) String ifNoneMatch,
                      @QueryParam("version") String version,
                      @Context UriInfo uriInfo) {

    String fileName;
     boolean isFile;
    try {
      repoPath = convertRepoPath(repoPath, true);
      Session session = nodeFinder.getItem(workspaceName(repoPath),LinkUtils.getParentPath(path(normalizePath(repoPath))),true).getSession();
      Node currentNode = (Node) session.getItem(path(repoPath));
      fileName = decodeValue(currentNode.getName());
      isFile = currentNode.isNodeType(VersionHistoryUtils.NT_FILE);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    Response response = super.get(repoName, repoPath, rangeHeader, ifModifiedSince, ifNoneMatch, version, uriInfo);
    if (HTTPStatus.OK == response.getStatus()) {
      Response.ResponseBuilder responseBuilder = Response.fromResponse(response);
      if (isFile) {
        responseBuilder.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
      }
      return responseBuilder
              .header("Access-Control-Allow-Origin", uriInfo.getRequestUri().getHost())
              .header("Access-Control-Allow-Credentials", true)
              .header("Access-Control-Allow-Methods", "ACL, CANCELUPLOAD, CHECKIN, CHECKOUT, COPY, DELETE, GET, HEAD, LOCK, MKCALENDAR, MKCOL, " +
                      "MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT, SEARCH, UNCHECKOUT, UNLOCK, UPDATE, VERSION-CONTROL")
              .header("Access-Control-Allow-Headers", "Overwrite, Destination, Content-Type, Depth, User-Agent, Translate, Range, Content-Range," +
                      " Timeout, X-File-Size, X-Requested-With, If-Modified-Since, X-File-Name, Cache-Control, Location, Lock-Token, If")
              .header("Access-Control-Expose-Header", "DAV, content-length, Allow")
              .header("Access-Control-Max-Age", 3600)
              .build();
    }
    return response;
  }

  @HEAD
  @Path("/{repoName}/{repoPath:.*}/")
  public Response head(@PathParam("repoName") String repoName,
                       @PathParam("repoPath") String repoPath,
                       @Context UriInfo uriInfo) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.head(repoName, repoPath, uriInfo);
  }

  @LOCK
  @Path("/{repoName}/{repoPath:.*}/")
  public Response lock(@PathParam("repoName") String repoName,
                       @PathParam("repoPath") String repoPath,
                       @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                       @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                       @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
                       HierarchicalProperty body) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.lock(repoName, repoPath, lockTokenHeader, ifHeader, depthHeader, body);
  }

  @UNLOCK
  @Path("/{repoName}/{repoPath:.*}/")
  public Response unlock(@PathParam("repoName") String repoName,
                         @PathParam("repoPath") String repoPath,
                         @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                         @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.unlock(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  @OPTIONS
  @Path("/{repoName}/{path:.*}/")
  public Response options(@PathParam("path") String path) {
    return super.options(path);
  }

  @ORDERPATCH
  @Path("/{repoName}/{repoPath:.*}/")
  public Response order(@PathParam("repoName") String repoName,
                        @PathParam("repoPath") String repoPath,
                        @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                        @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                        @Context UriInfo uriInfo,
                        HierarchicalProperty body) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.order(repoName, repoPath, lockTokenHeader, ifHeader, uriInfo, body);
  }

  @PROPFIND
  @Path("/{repoName}/{repoPath:.*}/")
  public Response propfind(@PathParam("repoName") String repoName,
                           @PathParam("repoPath") String repoPath,
                           @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
                           @Context UriInfo uriInfo,
                           HierarchicalProperty body) {

    try {
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.propfind(repoName, repoPath, depthHeader, uriInfo, body);
  }

  @PROPPATCH
  @Path("/{repoName}/{repoPath:.*}/")
  public Response proppatch(@PathParam("repoName") String repoName,
                            @PathParam("repoPath") String repoPath,
                            @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                            @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                            @Context UriInfo uriInfo,
                            HierarchicalProperty body) {

    try {
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.proppatch(repoName, repoPath, lockTokenHeader, ifHeader, uriInfo, body);
  }

  @PUT
  @Path("/{repoName}/{repoPath:.*}/")
  public Response put(@PathParam("repoName") String repoName,
                      @PathParam("repoPath") String repoPath,
                      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                      @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                      @HeaderParam(ExtHttpHeaders.FILE_NODETYPE) String fileNodeTypeHeader,
                      @HeaderParam(ExtHttpHeaders.CONTENT_NODETYPE) String nodeTypeHeader,
                      @HeaderParam(ExtHttpHeaders.CONTENT_MIXINTYPES) String mixinTypes,
                      @HeaderParam(ExtHttpHeaders.CONTENTTYPE) MediaType mediaType,
                      @HeaderParam(ExtHttpHeaders.USER_AGENT) String userAgent,
                      InputStream inputStream,
                      @Context UriInfo uriInfo) {
    Session session = null;
    Item item = null;
    boolean isCreating = false;
    ActivityCommonService activityService = null;
    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      try {
        item = nodeFinder.getItem(workspaceName(repoPath),
                                  LinkUtils.getParentPath(path(normalizePath(repoPath))),
                                  true);
        repoPath = item.getSession().getWorkspace().getName()
            + LinkUtils.createPath(item.getPath(), Text.escapeIllegalJcrChars(LinkUtils.getItemName(path(repoPath))));
        session = item.getSession();
      } catch (PathNotFoundException e) {
        item = nodeFinder.getItem(workspaceName(repoPath),
                                  LinkUtils.getParentPath(path(Text.escapeIllegalJcrChars(repoPath))),
                                  true);
        repoPath = item.getSession().getWorkspace().getName()
            + LinkUtils.createPath(item.getPath(), Text.escapeIllegalJcrChars(LinkUtils.getItemName(path(repoPath))));
        session = item.getSession();
      }
      activityService = WCMCoreUtils.getService(ActivityCommonService.class);
      if (!session.itemExists(path(repoPath))) {
        isCreating = true;
      }
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }

    Response res = super.put(repoName,
                             repoPath,
                             lockTokenHeader,
                             ifHeader,
                             null,
                             nodeTypeHeader,
                             mixinTypes,
                             mediaType,
                             userAgent,
                             inputStream,
                             uriInfo);
    try {

      boolean pushAs = markTempFilesToHidden(repoPath);
      Node currentNode = (Node) session.getItem(path(repoPath));
      if (isCreating) {
        if (userAgent!= null && userAgent.contains("Microsoft")) {
          activityService.setCreating(currentNode, true);
        }
      }else {
        activityService.setCreating(currentNode, false);
      }

      try {
        if(isCreating && pushAs)
          listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, currentNode);
        
        if (currentNode.isCheckedOut() && !activityService.isCreating(currentNode) && pushAs)
          listenerService.broadcast(this.POST_UPLOAD_CONTENT_EVENT, this, currentNode);

        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Cannot broadcast file create activity for the item at " + currentNode.getPath(), e);
          }
        }

    } catch (PathNotFoundException npfe) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(npfe.getMessage()).build();
    } catch (RepositoryException re) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(re.getMessage()).build();
    } catch (Exception e) {
      return Response.serverError().build();
    }

    return res;
  }

  @REPORT
  @Path("/{repoName}/{repoPath:.*}/")
  public Response report(@PathParam("repoName") String repoName,
                         @PathParam("repoPath") String repoPath,
                         @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
                         @Context UriInfo uriInfo,
                         HierarchicalProperty body) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.report(repoName, repoPath, depthHeader, uriInfo, body);
  }

  @SEARCH
  @Path("/{repoName}/{repoPath:.*}/")
  public Response search(@PathParam("repoName") String repoName,
                         @PathParam("repoPath") String repoPath,
                         @Context UriInfo uriInfo,
                         HierarchicalProperty body) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.search(repoName, repoPath, uriInfo, body);
  }

  @UNCHECKOUT
  @Path("/{repoName}/{repoPath:.*}/")
  public Response uncheckout(@PathParam("repoName") String repoName,
                             @PathParam("repoPath") String repoPath,
                             @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                             @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.uncheckout(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  @VERSIONCONTROL
  @Path("/{repoName}/{repoPath:.*}/")
  public Response versionControl(@PathParam("repoName") String repoName,
                                 @PathParam("repoPath") String repoPath,
                                 @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                                 @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {

    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.versionControl(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  @ACL
  @Path("/{repoName}/{repoPath:.*}/")
  public Response acl(@PathParam("repoName") String repoName,
                      @PathParam("repoPath") String repoPath,
                      @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                      @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                      HierarchicalProperty body) {
    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.acl(repoName, repoPath, lockTokenHeader, ifHeader, body);
  }

  @MOVE
  @Path("/{repoName}/{repoPath:.*}/")
  public Response move(@PathParam("repoName") String repoName,
                       @PathParam("repoPath") String repoPath,
                       @HeaderParam(ExtHttpHeaders.DESTINATION) String destinationHeader,
                       @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                       @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                       @HeaderParam(ExtHttpHeaders.DEPTH) String depthHeader,
                       @HeaderParam(ExtHttpHeaders.OVERWRITE) String overwriteHeader,
                       @Context UriInfo uriInfo,
                       HierarchicalProperty body) {
    try {
      repoPath = convertRepoPath(repoPath, true);
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    Response response = super.move(repoName,
                                   repoPath,
                                   destinationHeader,
                                   lockTokenHeader,
                                   ifHeader,
                                   depthHeader,
                                   overwriteHeader,
                                   uriInfo,
                                   body);

    if (response.getStatus() == HTTPStatus.CREATED) {
      updateProperties(destinationHeader, repoName);
    }
    markTempFilesToHidden(repoPath);
    return response;
  }

  /**
   * update exo:name, exo:title and jcr:mimeType when rename a node
   * 
   * @param destinationHeader
   * @param repoName
   */
  private void updateProperties(String destinationHeader, String repoName) {
    try {
      URI dest = buildURI(destinationHeader);
      String destPath = dest.getPath();
      int repoIndex = destPath.indexOf(repoName);
      destPath = normalizePath(repoIndex == -1 ? destPath : destPath.substring(repoIndex + repoName.length() + 1));
      String destNodePath = path(destPath);
      Node destNode = (Node) nodeFinder.getItem(workspaceName(destPath), path(normalizePath(destNodePath)), true);
      String nodeName = Text.escapeIllegalJcrChars(destNode.getName());
      destNode.setProperty("exo:name", nodeName);
      destNode.setProperty("exo:title", nodeName);
      if (!Utils.isFolder(destNode)) {
        Node content = destNode.getNode("jcr:content");
        String mimeType = mimeTypeResolver.getMimeType(nodeName);
        content.setProperty("jcr:mimeType", mimeType);
        // Change publication status
        ListenerService listenerService =  WCMCoreUtils.getService(ListenerService.class);
        if (destNode.isNodeType("exo:datetime")) {
          destNode.setProperty("exo:dateModified", new GregorianCalendar());
        }
        listenerService.broadcast(CmsService.POST_EDIT_CONTENT_EVENT, destNode.getParent(), destNode);
      }
      destNode.save();     
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot change property of destNode" + destinationHeader, e);
      }
    }
  }

  /** 
   * Build URI from string. 
   */
  private URI buildURI(String path) throws URISyntaxException {
    try {
      return new URI(path);
    }
    catch (URISyntaxException e) {
      return new URI(TextUtil.escape(path, '%', true));
    }
  }

  /**
   * {@inheritDoc}
   */
  @MKCOL
  @Path("/{repoName}/{repoPath:.*}/")
  public Response mkcol(@PathParam("repoName") String repoName,
                        @PathParam("repoPath") String repoPath,
                        @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                        @HeaderParam(ExtHttpHeaders.IF) String ifHeader,
                        @HeaderParam(ExtHttpHeaders.CONTENT_NODETYPE) String nodeTypeHeader,
                        @HeaderParam(ExtHttpHeaders.CONTENT_MIXINTYPES) String mixinTypesHeader,
                        @Context UriInfo uriInfo) {
    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      Item item = nodeFinder.getItem(workspaceName(repoPath), LinkUtils.getParentPath(path(normalizePath(repoPath))), true);
      repoPath =
          item.getSession().getWorkspace().getName() + LinkUtils.createPath(item.getPath(), 
                                                                            LinkUtils.getItemName(path(repoPath)));
    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }
    return super.mkcol(repoName,
                       repoPath,
                       lockTokenHeader,
                       ifHeader,
                       nodeTypeHeader,
                       mixinTypesHeader,
                       uriInfo);
  }

  @DELETE
  @Path("/{repoName}/{repoPath:.*}/")
  public Response delete(@PathParam("repoName") String repoName,
                         @PathParam("repoPath") String repoPath,
                         @HeaderParam(ExtHttpHeaders.LOCKTOKEN) String lockTokenHeader,
                         @HeaderParam(ExtHttpHeaders.IF) String ifHeader) {
    Item item = null;
    try {
      repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      repoPath = convertRepoPath(repoPath, false);

      try {
        item = nodeFinder.getItem(workspaceName(repoPath),
                                  path(normalizePath(repoPath)),
                                  true);        
      } catch (PathNotFoundException e) {
        item = nodeFinder.getItem(workspaceName(repoPath),
                                  path(Text.escapeIllegalJcrChars(repoPath)),
                                  true);        
      }     

    } catch (PathNotFoundException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (NoSuchWorkspaceException exc) {
      return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot find the item at " + repoName + "/" + repoPath, e);
      }
      return Response.serverError().build();
    }    

    try {
      //Broadcast the event when user move node to Trash
      Node node = (Node)item;
      ListenerService listenerService =  WCMCoreUtils.getService(ListenerService.class);
      ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
      Node parent = node.getParent();
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {        
        if (activityService.isBroadcastNTFileEvents(node)) {
          listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, parent, node);
        }
      } else if(!WCMCoreUtils.isDocumentNodeType(node)){
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(node);

        //Broadcast event to remove file activities
        Node tempNode = null;
        try {
          while (!queue.isEmpty()) {
            tempNode = queue.poll();
            if (WCMCoreUtils.isDocumentNodeType(tempNode) 
                || tempNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
              listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, tempNode.getParent(), tempNode);
            } else {
              for (NodeIterator iter = tempNode.getNodes(); iter.hasNext(); ) {
                Node childNode = iter.nextNode();
                if(WCMCoreUtils.isDocumentNodeType(childNode) || childNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED) 
                    || childNode.isNodeType(NodetypeConstant.NT_FOLDER))
                  queue.add(childNode);
              }
            }
          }
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }         
      }
      //Remove the symlinks of deleted node. 
      Utils.removeSymlinks(node);
    } catch(Exception ex) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(ex.getMessage());
      }
    }    
    return super.delete(repoName, repoPath, lockTokenHeader, ifHeader);
  }

  private String convertRepoPath(String repoPath, boolean giveTarget) throws Exception{
    try {
      Item item = nodeFinder.getItem(workspaceName(repoPath), path(normalizePath(repoPath)), giveTarget);
      return item.getSession().getWorkspace().getName() + item.getPath();
    } catch (PathNotFoundException e) {
      Item item = nodeFinder.getItem(workspaceName(repoPath), path(Text.escapeIllegalJcrChars(repoPath)), giveTarget);
      return item.getSession().getWorkspace().getName() + item.getPath();
    }
  }

  /**
   * hidden temporary files/folders
   * @param repoPath
   */
  private boolean markTempFilesToHidden(String repoPath){
    if(StringUtils.isBlank(repoPath)) return false;
    String tempNodeFolder      = ".TemporaryItems";
    String tempNodeFileChild = "._folders.501";
    String tempNodeFile        = "._.TemporaryItems";
    String txtTempRegex        = "/._";
    try {
      String txtTemp = repoPath.substring(repoPath.lastIndexOf("/"), repoPath.length());
      boolean isTxtTemp = txtTemp.startsWith(txtTempRegex)?true:false;
      if(repoPath.contains(tempNodeFile) || isTxtTemp){
        Node _tempNodeFile = (Node)nodeFinder.getItem(workspaceName(repoPath), path(repoPath), true);
        _tempNodeFile.remove();
        _tempNodeFile.getSession().save();
        return false;
      }else if(repoPath.contains(tempNodeFolder)) {
        String currentNodePath = repoPath.substring(0, repoPath.indexOf(tempNodeFolder));
        Node currentNode = (Node)nodeFinder.getItem(workspaceName(repoPath), path(currentNodePath), true);
        //make tmp folder to hidden
        if(currentNode.hasNode(tempNodeFolder)){
          Node _tmpFolderNode = currentNode.getNode(tempNodeFolder);
          if(_tmpFolderNode.canAddMixin(NodetypeConstant.EXO_HIDDENABLE)) _tmpFolderNode.addMixin(NodetypeConstant.EXO_HIDDENABLE);
          if (_tmpFolderNode.hasNode(tempNodeFileChild)){
            Node _tempNodeFileChild = _tmpFolderNode.getNode(tempNodeFileChild);
            _tempNodeFileChild.remove();
          }
          _tmpFolderNode.save();
        }
        return false;
      }
    }catch(RepositoryException ex){
      if (LOG.isWarnEnabled()) {
        LOG.warn("The hidden temp files has been ignored " + ex.getMessage());
      }
    }
    return true;
  }

  public String decodeValue(String value) {
    String currentValue;
    do {
      currentValue = value;
      try {
        value = URLDecoder.decode(value, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        LOG.warn("Unable to decode value: ", e.getMessage());
        return value;
      }
    } while (!StringUtils.equals(currentValue, value));
    return value;
  }

}
