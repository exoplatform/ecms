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
package org.exoplatform.services.cms.clouddrives.webui.rest;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.ecm.connector.clouddrives.AcceptedCloudFile;
import org.exoplatform.ecm.connector.clouddrives.ErrorEntiry;
import org.exoplatform.ecm.connector.clouddrives.LinkedCloudFile;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.NotCloudFileException;
import org.exoplatform.services.cms.clouddrives.NotYetCloudFileException;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudFile;
import org.exoplatform.services.cms.clouddrives.webui.action.CloudFileActionService;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * REST service providing information about cloud files in Documents (ECMS) app.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DriveService.java 00000 Jul 11, 2018 pnedonosko $
 */
@Path("/clouddrive/document/")
@Produces(MediaType.APPLICATION_JSON)
public class FileService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG = ExoLogger.getLogger(FileService.class);

  /** The cloud drives. */
  protected final CloudDriveService      cloudDrives;

  /** The jcr service. */
  protected final RepositoryService      jcrService;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /** The document service. */
  protected final DocumentService        documentService;

  /** The link manager. */
  protected final CloudFileActionService cloudActions;

  /**
   * REST cloudDrives uses {@link CloudDriveService} for actual job.
   *
   * @param cloudDrives {@link CloudDriveService}
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param documentService the document service
   * @param cloudActions the cloud actions
   */
  public FileService(CloudDriveService cloudDrives,
                     RepositoryService jcrService,
                     SessionProviderService sessionProviders,
                     DocumentService documentService,
                     CloudFileActionService cloudActions) {
    this.cloudDrives = cloudDrives;
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.documentService = documentService;
    this.cloudActions = cloudActions;
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
  public Response getFile(@Context UriInfo uriInfo, @QueryParam("workspace") String workspace, @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        try {
          CloudDrive local = cloudDrives.findDrive(workspace, path);
          if (local != null) {
            try {
              CloudFile file = local.getFile(path);
              if (!file.getPath().equals(path)) {
                file = new LinkedCloudFile(file, path); // it's symlink
              } else {
                Identity currentIdentity = ConversationState.getCurrent().getIdentity();
                if (!local.getLocalUser().equals(currentIdentity.getUserId())) {
                  // XXX for shared file we need return also a right open link
                  // It's a workaround for PLF-8078
                  ExtendedNode fileNode = fileNode(file, workspace);
                  if (fileNode != null) {
                    // Find is the node shared via a group or via a personal
                    // documents to current user - generate open link
                    // accordingly
                    // We'll use a first one as in search context it has less
                    // sense.
                    String openLink = null;
                    String filePath = null;
                    nextAce: for (AccessControlEntry ace : fileNode.getACL().getPermissionEntries()) {
                      MembershipEntry me = ace.getMembershipEntry();
                      if (me != null) {
                        // it's group - url to group docs with the link
                        if (currentIdentity.getGroups().contains(me.getGroup())) {
                          DriveData groupDrive = cloudActions.getGroupDrive(me.getGroup());
                          if (groupDrive != null) {
                            for (NodeIterator niter = cloudActions.getCloudFileLinks(fileNode,
                                                                                     me.getGroup(),
                                                                                     groupDrive.getHomePath(),
                                                                                     false); niter.hasNext();) {
                              Node linkNode = niter.nextNode();
                              filePath = linkNode.getPath();
                              openLink = documentService.getLinkInDocumentsApp(linkNode.getPath(), groupDrive);
                              break nextAce;
                            }
                          }
                        }
                      } else if (ace.getIdentity().equals(currentIdentity.getUserId())) {
                        // user, url to the symlink in current user docs
                        Node profileNode = cloudActions.getUserProfileNode(currentIdentity.getUserId());
                        String userPath = profileNode.getPath();
                        for (NodeIterator niter = cloudActions.getCloudFileLinks(fileNode,
                                                                                 currentIdentity.getUserId(),
                                                                                 userPath,
                                                                                 false); niter.hasNext();) {
                          Node linkNode = niter.nextNode();
                          filePath = linkNode.getPath();
                          DriveData linkDrive = documentService.getDriveOfNode(filePath);
                          if (linkDrive != null) {
                            openLink = documentService.getLinkInDocumentsApp(linkNode.getPath(), linkDrive);
                            break nextAce;
                          } else {
                            LOG.warn("Cannot find Documents drive for shared Cloud File: " + filePath);
                          }
                        }
                      }
                    }
                    if (openLink != null) {
                      file = new SharedCloudFile(file, filePath, openLink);
                    }
                  } else {
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                                   .entity(ErrorEntiry.message("File node cannot be found."))
                                   .build();
                  }
                }
              }
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

  @GET
  @RolesAllowed("users")
  @Path("/drive/personal")
  public Response getUserDrive(@Context HttpServletRequest request, @Context UriInfo uriInfo) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      DriveData drive = cloudActions.getUserDrive(currentIdentity.getUserId());
      return Response.ok().entity(drive).build();
    } catch (Exception e) {
      LOG.error("Error reading user drive for " + currentIdentity, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ErrorEntiry.message("Error reading user drive: ")).build();
    }
  }

  /**
   * Read cloud file node.
   *
   * @param file the file
   * @param workspace the workspace
   * @return the node
   * @throws PathNotFoundException the path not found exception
   * @throws LoginException the login exception
   * @throws NoSuchWorkspaceException the no such workspace exception
   * @throws RepositoryException the repository exception
   */
  protected ExtendedNode fileNode(CloudFile file, String workspace) throws PathNotFoundException,
                                                                    LoginException,
                                                                    NoSuchWorkspaceException,
                                                                    RepositoryException {
    ExtendedNode node;
    if (JCRLocalCloudFile.class.isAssignableFrom(file.getClass())) {
      node = ExtendedNode.class.cast(JCRLocalCloudFile.class.cast(file).getNode());
    } else {
      node = null;
    }
    if (node == null) {
      SessionProvider sp = sessionProviders.getSessionProvider(null);
      if (sp != null) {
        Item item = sp.getSession(workspace, jcrService.getCurrentRepository()).getItem(file.getPath());
        if (ExtendedNode.class.isAssignableFrom(item.getClass())) {
          node = ExtendedNode.class.cast(item);
        } else {
          LOG.warn("Cannot cast cloud file node from item: " + item);
        }
      } else {
        LOG.warn("Cannot get session provider to read cloud file node: " + file.getPath());
      }
    }
    return node;
  }

}
