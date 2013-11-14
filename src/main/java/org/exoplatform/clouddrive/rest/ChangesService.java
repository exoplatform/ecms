/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.clouddrive.CloudDriveAccessException;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ChangesService.java 00000 Nov 10, 2013 pnedonosko $
 * 
 */
@Path("/clouddrive/changes/")
@Produces(MediaType.APPLICATION_JSON)
public class ChangesService implements ResourceContainer {

  protected static final Log             LOG = ExoLogger.getLogger(ChangesService.class);

  protected final CloudDriveService      cloudDrives;

  protected final RepositoryService      jcrService;

  protected final SessionProviderService sessionProviders;

  protected final NodeFinder             finder;

  /**
   * 
   */
  public ChangesService(CloudDriveService cloudDrives,
                        RepositoryService jcrService,
                        SessionProviderService sessionProviders,
                        NodeFinder finder) {
    this.cloudDrives = cloudDrives;
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
  }

  /**
   * Start asynchronous synchronization of a drive described by given workspace and path.
   * 
   * @param uriInfo - request info TODO need it?
   * @param workspace
   * @param path
   * @return {@link Response}
   */
  @POST
  @RolesAllowed("users")
  public Response asyncSynchronization(@Context UriInfo uriInfo,
                                       @FormParam("workspace") String workspace,
                                       @FormParam("path") String path) {

    if (workspace != null) {
      if (path != null) {
        try {
          SessionProvider sp = sessionProviders.getSessionProvider(null);
          Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

          try {
            Item item = finder.getItem(userSession, path, true);
            // item = userSession.getItem(path);
            if (item.isNode()) {
              Node userNode = (Node) item;

              CloudDrive local = cloudDrives.findDrive(userNode);
              if (local != null) {
                try {
                  Command sync = local.synchronize(true);

                  // wait a bit here to let the sync to init and do empty check
                  try {
                    Thread.sleep(1500);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }

                  return Response.status(Status.ACCEPTED)
                                 .entity("{\"syncDone\":\"" + sync.isDone() + "\",\"startTime\":\""
                                     + sync.getStartTime() + "\",\"finishTime\":\"" + sync.getFinishTime()
                                     + "\"}")
                                 .build();
                } catch (CloudDriveAccessException e) {
                  LOG.warn("Request to cloud drive forbidden or revoked.", e);
                  // XXX client should treat this status in special way and obtain new credentials using
                  // given provider
                  return Response.status(Status.FORBIDDEN).entity(local.getUser().getProvider()).build();
                } catch (DriveRemovedException e) {
                  LOG.warn("Cannot run asynchronous syncronization on removed drive " + workspace + ":"
                      + path + ". " + e.getMessage());
                  return Response.status(Status.NOT_FOUND)
                                 .entity("Synchronization canceled. " + e.getMessage())
                                 .build();
                } catch (CloudDriveException e) {
                  LOG.error("Error starting asynchronous synchronization on drive " + workspace + ":" + path,
                            e);
                  return Response.status(Status.INTERNAL_SERVER_ERROR)
                                 .entity("Error starting asynchronous synchronization. " + e.getMessage())
                                 .build();
                }
              } else {
                LOG.warn("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
                return Response.status(Status.NO_CONTENT).build();
              }
            } else {
              LOG.warn("Item " + workspace + ":" + path + " not a node.");
              return Response.status(Status.PRECONDITION_FAILED).entity("Not a node.").build();
            }
          } finally {
            userSession.logout();
          }
        } catch (LoginException e) {
          LOG.warn("Error login to read drive " + workspace + ":" + path + ". " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
        } catch (RepositoryException e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity("Error reading drive: storage error.")
                         .build();
        } catch (Throwable e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity("Error reading drive: runtime error.")
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  /**
   * Start asynchronous synchronization of a drive described by given workspace and path.
   * 
   * @param uriInfo - request info TODO need it?
   * @param workspace
   * @param path
   * @return {@link Response}
   */
  @GET
  @Path("/link/")
  @RolesAllowed("users")
  public Response getChangesLink(@Context UriInfo uriInfo,
                                 @FormParam("workspace") String workspace,
                                 @FormParam("path") String path) {

    if (workspace != null) {
      if (path != null) {
        try {
          SessionProvider sp = sessionProviders.getSessionProvider(null);
          Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

          try {
            Item item = finder.getItem(userSession, path, true);
            // item = userSession.getItem(path);
            if (item.isNode()) {
              Node userNode = (Node) item;

              CloudDrive local = cloudDrives.findDrive(userNode);
              if (local != null) {
                try {
                  local.updateChangesLink();
                  String link = local.getChangesLink();
                  if (link != null) {
                    return Response.status(Status.OK).entity("{changesLink:\"" + link + "\"}").build();
                  } else {
                    return Response.status(Status.NOT_FOUND).entity("Changes link not provided").build();
                  }
                } catch (CloudDriveException e) {
                  LOG.error("Error getting changes link for drive " + workspace + ":" + path, e);
                  return Response.status(Status.INTERNAL_SERVER_ERROR)
                                 .entity("Error getting changes link. " + e.getMessage())
                                 .build();
                }
              } else {
                LOG.warn("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
                return Response.status(Status.NO_CONTENT).build();
              }
            } else {
              LOG.warn("Item " + workspace + ":" + path + " not a node.");
              return Response.status(Status.PRECONDITION_FAILED).entity("Not a node.").build();
            }
          } finally {
            userSession.logout();
          }
        } catch (LoginException e) {
          LOG.warn("Error login to read drive " + workspace + ":" + path + ". " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
        } catch (RepositoryException e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity("Error reading drive: storage error.")
                         .build();
        } catch (Throwable e) {
          LOG.error("Error reading drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity("Error reading drive: runtime error.")
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

}
