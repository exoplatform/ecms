/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.clouddrive.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.annotation.security.RolesAllowed;
import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveMessage;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.LocalCloudFile;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotConnectedException;
import org.exoplatform.clouddrive.NotYetCloudFileException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * REST service providing information about providers. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveService.java 00000 Oct 22, 2012 pnedonosko $
 */
@Path("/clouddrive/drive")
@Produces(MediaType.APPLICATION_JSON)
public class DriveService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG           = ExoLogger.getLogger(DriveService.class);

  /** The Constant CONTENT_SUFIX. */
  protected static final String          CONTENT_SUFIX = "/jcr:content";

  /** The cloud drives. */
  protected final CloudDriveService      cloudDrives;

  /** The jcr service. */
  protected final RepositoryService      jcrService;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /**
   * REST cloudDrives uses {@link CloudDriveService} for actual job.
   *
   * @param cloudDrives {@link CloudDriveService}
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   */
  public DriveService(CloudDriveService cloudDrives, RepositoryService jcrService, SessionProviderService sessionProviders) {
    this.cloudDrives = cloudDrives;
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
  }

  /**
   * Return drive information.
   * 
   * @param uriInfo {@link UriInfo}
   * @param workspace String
   * @param path String
   * @return set of {@link CloudProvider} currently available for connect
   */
  @GET
  @RolesAllowed("users")
  public Response getDrive(@Context HttpServletRequest request,
                           @Context UriInfo uriInfo,
                           @QueryParam("workspace") String workspace,
                           @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        Locale locale = request.getLocale();
        return readDrive(workspace, path, locale, false);
      } else {
        return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null path")).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null workspace")).build();
    }
  }

  /**
   * Synchronized cloud drive or its file/folder and return result for client refresh.
   *
   * @param uriInfo {@link UriInfo}
   * @param workspace {@link String} Drive Node workspace
   * @param path {@link String} Drive Node path
   * @return the response
   */
  @POST
  @Path("/synchronize/")
  @RolesAllowed("users")
  public Response synchronize(@Context HttpServletRequest request,
                              @Context UriInfo uriInfo,
                              @FormParam("workspace") String workspace,
                              @FormParam("path") String path) {

    if (workspace != null) {
      if (path != null) {
        Locale locale = request.getLocale();
        return readDrive(workspace, path, locale, true);
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  // *********************************** internals *************************************

  /**
   * Read cloud drive and optionally synchronized it before. Drive will contain a file from it will asked,
   * 
   * @param workspace {@link String} Drive workspace
   * @param path {@link String} path of a Node in the Drive
   * @param synchronize {@link Boolean} flag to synch before the read (true to force sync)
   * @return {@link Response} REST response
   */
  protected Response readDrive(String workspace, String path, Locale locale, boolean synchronize) {
    try {
      CloudDrive local = cloudDrives.findDrive(workspace, path);
      if (local != null) {
        Collection<CloudFile> files;
        Collection<String> removed;
        Collection<CloudDriveMessage> messages;
        if (synchronize) {
          try {
            Command sync = local.synchronize();
            sync.await(); // wait for sync process
            files = sync.getFiles();
            initModified(files, locale, local);
            removed = sync.getRemoved();
            messages = sync.getMessages();
          } catch (InterruptedException e) {
            LOG.warn("Caller of synchronization command interrupted.", e);
            Thread.currentThread().interrupt();
            return Response.status(Status.SERVICE_UNAVAILABLE)
                           .entity(ErrorEntiry.message("Synchrinization interrupted. Try again later."))
                           .build();
          } catch (ExecutionException e) {
            Throwable err = e.getCause();
            if (err instanceof RefreshAccessException) {
              Throwable cause = err.getCause();
              LOG.warn("Access to cloud drive expired, forbidden or revoked. " + err.getMessage()
                  + (cause != null ? ". " + cause.getMessage() : ""));
              // client should treat this status in special way and obtain new
              // credentials using given
              // provider
              return Response.status(Status.FORBIDDEN).entity(local.getUser().getProvider()).build();
            } else if (err instanceof NotConnectedException) {
              LOG.warn("Cannot synchronize not connected drive. " + err.getMessage(), err);
              return Response.status(Status.BAD_REQUEST)
                             .entity(ErrorEntiry.notCloudDrive("Drive not connected", workspace, path))
                             .build();
            } else if (err instanceof CloudDriveException) {
              LOG.error("Error synchrinizing the drive. " + err.getMessage(), err);
              return Response.status(Status.INTERNAL_SERVER_ERROR)
                             .entity(ErrorEntiry.message("Error synchrinizing the drive. Try again later."))
                             .build();
            } else if (err instanceof AccessDeniedException) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Not sufficient permissions. " + err.getMessage());
              }
              return Response.status(Status.FORBIDDEN)
                             .entity(ErrorEntiry.acessDenied("Access denied. Synchronization canceled."))
                             .build();
            } else if (err instanceof RepositoryException) {
              LOG.error("Storage error. " + err.getMessage(), err);
              return Response.status(Status.INTERNAL_SERVER_ERROR)
                             .entity(ErrorEntiry.message("Storage error. Synchronization canceled."))
                             .build();
            } else if (err instanceof RuntimeException) {
              LOG.error("Runtime error. " + err.getMessage(), err);
              return Response.status(Status.INTERNAL_SERVER_ERROR)
                             .entity(ErrorEntiry.message("Internal server error. Synchronization canceled. Try again later."))
                             .build();
            }
            LOG.error("Unexpected error. " + err.getMessage(), err);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(ErrorEntiry.message("Unexpected server error. Synchronization canceled. Try again later."))
                           .build();
          } catch (CloudDriveException e) {
            LOG.error("Error synchronizing drive " + workspace + ":" + path, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(ErrorEntiry.message("Error synchronizing drive. " + e.getMessage()))
                           .build();
          }
        } else {
          files = new ArrayList<CloudFile>();
          removed = Collections.emptyList();
          messages = Collections.emptyList();
          try {
            if (!local.getPath().equals(path)) {
              // if path not the drive itself
              CloudFile file = local.getFile(path);
              files.add(file);
              if (!file.getPath().equals(path)) {
                // it's symlink, add it also
                files.add(new LinkedCloudFile(file, path));
              }
              initModified(file, locale, local);
            }
          } catch (NotYetCloudFileException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Item " + workspace + ":" + path + " not yet a cloud file : " + e.getMessage());
            }
            files.add(new AcceptedCloudFile(path));
          } catch (NotCloudFileException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Item " + workspace + ":" + path + " not a cloud file : " + e.getMessage());
            }
          }
        }

        return Response.ok().entity(DriveInfo.create(workspace, local, files, removed, messages)).build();
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
        }
        return Response.status(Status.NOT_FOUND).entity(ErrorEntiry.notCloudDrive("Not connected", workspace, path)).build();
      }
    } catch (LoginException e) {
      LOG.warn("Error login to read drive " + workspace + ":" + path + ". " + e.getMessage());
      return Response.status(Status.UNAUTHORIZED).entity(ErrorEntiry.message("Authentication error")).build();
    } catch (DriveRemovedException e) {
      LOG.error("Drive removed " + workspace + ":" + path, e);
      return Response.status(Status.NOT_FOUND).entity(ErrorEntiry.driveRemoved("Drive removed", workspace, path)).build();
    } catch (PathNotFoundException e) {
      LOG.warn("Error reading file " + workspace + ":" + path + ". " + e.getMessage());
      return Response.status(Status.NOT_FOUND)
                     .entity(ErrorEntiry.nodeNotFound("File was removed or renamed", workspace, path))
                     .build();
    } catch (RepositoryException e) {
      LOG.error("Error reading drive " + workspace + ":" + path, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity(ErrorEntiry.message("Error reading drive: storage error"))
                     .build();
    } catch (Throwable e) {
      LOG.error("Error reading drive " + workspace + ":" + path, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity(ErrorEntiry.message("Error reading drive: runtime error"))
                     .build();
    }
  }

  /**
   * Return file information. Returned file may be not yet created in cloud (accepted for creation), then this service response
   * will be with status ACCEPTED, otherwise it's OK response.
   *
   * @param uriInfo the uri info
   * @param workspace {@link String} Drive Node workspace
   * @param path {@link String} File Node path
   * @return {@link Response} REST response
   */
  @GET
  @Path("/file/")
  @RolesAllowed("users")
  public Response getFile(@Context HttpServletRequest request,
                          @Context UriInfo uriInfo,
                          @QueryParam("workspace") String workspace,
                          @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        try {
          Locale locale = request.getLocale();
          CloudDrive local = cloudDrives.findDrive(workspace, path);
          if (local != null) {
            try {
              CloudFile file = local.getFile(path);
              if (!file.getPath().equals(path)) {
                file = new LinkedCloudFile(file, path); // it's symlink
              }
              initModified(file, locale, local);
              return Response.ok().entity(file).build();
            } catch (NotYetCloudFileException e) {
              return Response.status(Status.ACCEPTED).entity(new AcceptedCloudFile(path)).build();
            } catch (NotCloudFileException e) {
              return Response.status(Status.NOT_FOUND).entity(ErrorEntiry.notCloudFile(e.getMessage(), workspace, path)).build();
            }
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
          }
          return Response.status(Status.NOT_FOUND)
                         .entity(ErrorEntiry.notCloudDrive("Not a cloud file or drive not connected", workspace, path))
                         .build();
        } catch (LoginException e) {
          LOG.warn("Error login to read drive file " + workspace + ":" + path + ": " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity(ErrorEntiry.message("Authentication error")).build();
        } catch (CloudDriveException e) {
          LOG.warn("Error reading file " + workspace + ":" + path, e);
          return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Error reading file. " + e.getMessage())).build();
        } catch (RepositoryException e) {
          LOG.error("Error reading file " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity(ErrorEntiry.message("Error reading file: storage error."))
                         .build();
        } catch (Throwable e) {
          LOG.error("Error reading file " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity(ErrorEntiry.message("Error reading file: runtime error."))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null path")).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null workspace")).build();
    }
  }

  /**
   * Return list of files in given folder. Returned files may be not yet created in cloud (accepted for creation), then this
   * service response will be with status ACCEPTED, otherwise it's OK response. This service will not return files for nodes that
   * do not belong to the cloud drive associated with this path.
   *
   * @param uriInfo the uri info
   * @param workspace {@link String} Drive Node workspace
   * @param path {@link String} Folder Node path
   * @return {@link Response} REST response
   */
  @GET
  @Path("/files/")
  @RolesAllowed("users")
  public Response getFiles(@Context HttpServletRequest request,
                           @Context UriInfo uriInfo,
                           @QueryParam("workspace") String workspace,
                           @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        try {
          Locale locale = request.getLocale();
          CloudDrive local = cloudDrives.findDrive(workspace, path);
          if (local != null) {
            String parentPath;
            if (local.getPath().equals(path)) {
              parentPath = path;
            } else {
              try {
                // take node path from parent to unlink if the parent is symlink
                parentPath = local.getFile(path).getPath();
              } catch (NotYetCloudFileException e) {
                // use path as is
                parentPath = path;
              }
            }

            SessionProvider sp = sessionProviders.getSessionProvider(null);
            Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());
            Node parentNode = (Node) userSession.getItem(parentPath);

            List<CloudFile> files = new ArrayList<CloudFile>();
            boolean hasAccepted = false;
            for (NodeIterator childs = parentNode.getNodes(); childs.hasNext();) {
              Node fileNode = childs.nextNode();
              String filePath = fileNode.getPath();
              try {
                CloudFile file = local.getFile(filePath);
                if (file != null) {
                  if (!file.getPath().equals(filePath)) {
                    file = new LinkedCloudFile(file, filePath); // it's symlink
                  }
                  initModified(file, locale, local);
                  files.add(file);
                } // not a cloud file - skip it
              } catch (NotYetCloudFileException e) {
                hasAccepted = true;
                files.add(new AcceptedCloudFile(filePath));
              }
            }

            ResponseBuilder resp;
            if (hasAccepted) {
              resp = Response.status(Status.ACCEPTED);
            } else {
              resp = Response.ok();
            }
            return resp.entity(files).build();
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
          }
          return Response.status(Status.NOT_FOUND).entity(ErrorEntiry.notCloudDrive("Not connected", workspace, path)).build();
        } catch (LoginException e) {
          LOG.warn("Error login to read drive files in " + workspace + ":" + path + ": " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
        } catch (CloudDriveException e) {
          LOG.warn("Error reading files in " + workspace + ":" + path, e);
          return Response.status(Status.BAD_REQUEST).entity("Error reading files. " + e.getMessage()).build();
        } catch (RepositoryException e) {
          LOG.error("Error reading files in " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading files: storage error.").build();
        } catch (Throwable e) {
          LOG.error("Error reading files in " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading file: runtime error.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null path")).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null workspace")).build();
    }
  }

  /**
   * State of a drive pointed by given workspace and path.
   *
   * @param uriInfo - request info
   * @param workspace the workspace
   * @param path the path
   * @return {@link Response}
   */
  @GET
  @Path("/state/")
  @RolesAllowed("users")
  public Response getState(@Context UriInfo uriInfo, @QueryParam("workspace") String workspace, @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        try {
          CloudDrive local = cloudDrives.findDrive(workspace, path);
          if (local != null) {
            try {
              return Response.status(Status.OK).entity(local.getState()).build();
            } catch (RefreshAccessException e) {
              Throwable cause = e.getCause();
              LOG.warn("Access to cloud drive expired, forbidden or revoked. " + e.getMessage()
                  + (cause != null ? ". " + cause.getMessage() : ""));
              // client should treat this status in special way and obtain new
              // credentials using given
              // provider
              return Response.status(Status.FORBIDDEN).entity(local.getUser().getProvider()).build();
            } catch (CloudDriveException e) {
              LOG.error("Error getting changes link for drive " + workspace + ":" + path, e);
              return Response.status(Status.INTERNAL_SERVER_ERROR)
                             .entity("Error getting changes link. " + e.getMessage())
                             .build();
            }
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
            }
            return Response.status(Status.NOT_FOUND).entity(ErrorEntiry.notCloudDrive("Not connected", workspace, path)).build();
          }
        } catch (LoginException e) {
          LOG.warn("Error login to read drive " + workspace + ":" + path + ". " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
        } catch (RepositoryException e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading drive: storage error.").build();
        } catch (Throwable e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading drive: runtime error.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null path")).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity(ErrorEntiry.message("Null workspace")).build();
    }
  }

  private void initModified(Collection<CloudFile> files, Locale locale, CloudDrive drive) {
    for (CloudFile file : files) {
      initModified(file, locale, drive);
    }
  }

  private void initModified(CloudFile file, Locale locale, CloudDrive drive) {
    if (file.isConnected()) {
      try {
        LocalCloudFile.class.cast(file).initModified(locale, drive);
      } catch (ClassCastException e) {
        // safely ignore it, but let to know it was
        LOG.warn("Cannot initialize cloud file modified field for {} due to error: {}", file.getPath(), e.getMessage());
      }
    }
  }
}
