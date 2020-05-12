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
package org.exoplatform.ecm.connector.clouddrives;

import javax.annotation.security.RolesAllowed;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.services.cms.clouddrives.CannotCreateDriveException;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.cms.clouddrives.ProviderNotAvailableException;
import org.exoplatform.services.cms.clouddrives.features.CloudDriveFeatures;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

/**
 * REST service providing discovery of Cloud Drive features.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FeaturesService.java 00000 Jan 31, 2014 pnedonosko $
 */
@Path("/clouddrive/features")
@Produces(MediaType.APPLICATION_JSON)
public class FeaturesService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG = ExoLogger.getLogger(FeaturesService.class);

  /** The features. */
  protected final CloudDriveFeatures     features;

  /** The cloud drives. */
  protected final CloudDriveService      cloudDrives;

  /** The jcr service. */
  protected final RepositoryService      jcrService;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /**
   * Instantiates a new features service.
   *
   * @param cloudDrives the cloud drives
   * @param features the features
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   */
  public FeaturesService(CloudDriveService cloudDrives,
                         CloudDriveFeatures features,
                         RepositoryService jcrService,
                         SessionProviderService sessionProviders) {
    this.cloudDrives = cloudDrives;
    this.features = features;

    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
  }

  /**
   * Can create drive.
   *
   * @param uriInfo the uri info
   * @param workspace the workspace
   * @param path the path
   * @param providerId the provider id
   * @return the response
   */
  @GET
  @Path("/can-create-drive/")
  @RolesAllowed("users")
  public Response canCreateDrive(@Context UriInfo uriInfo,
                                 @QueryParam("workspace") String workspace,
                                 @QueryParam("path") String path,
                                 @QueryParam("provider") String providerId) {
    if (workspace != null) {
      if (path != null) {
        CloudProvider provider;
        if (providerId != null && providerId.length() > 0) {
          try {
            provider = cloudDrives.getProvider(providerId);
          } catch (ProviderNotAvailableException e) {
            LOG.warn("Unknown provider: " + providerId);
            return Response.status(Status.BAD_REQUEST).entity("Unknown provider.").build();
          }
        } else {
          provider = null;
        }

        ConversationState currentConvo = ConversationState.getCurrent();
        try {
          boolean result =
                         features.canCreateDrive(workspace,
                                                 path,
                                                 currentConvo != null ? currentConvo.getIdentity().getUserId() : null,
                                                 provider);
          return Response.ok().entity("{\"result\":\"" + result + "\"}").build();
        } catch (CannotCreateDriveException e) {
          return Response.ok().entity("{\"result\":\"false\", \"message\":\"" + e.getMessage() + "\"}").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

  /**
   * Checks if is autosync enabled.
   *
   * @param uriInfo the uri info
   * @param workspace the workspace
   * @param path the path
   * @return the response
   */
  @GET
  @Path("/is-autosync-enabled/")
  @RolesAllowed("users")
  public Response isAutosyncEnabled(@Context UriInfo uriInfo,
                                    @QueryParam("workspace") String workspace,
                                    @QueryParam("path") String path) {
    if (workspace != null) {
      if (path != null) {
        try {
          CloudDrive local = cloudDrives.findDrive(workspace, path);
          if (local != null) {
            boolean result = features.isAutosyncEnabled(local);
            return Response.ok().entity("{\"result\":\"" + result + "\"}").build();
          } else {
            LOG.warn("Item " + workspace + ":" + path + " not a cloud file or drive not connected.");
            return Response.status(Status.NO_CONTENT).build();
          }
        } catch (LoginException e) {
          LOG.warn("Error login to read drive " + workspace + ":" + path + ". " + e.getMessage());
          return Response.status(Status.UNAUTHORIZED).entity("Authentication error.").build();
        } catch (RepositoryException e) {
          LOG.error("Error getting autosync status of a drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading drive: storage error.").build();
        } catch (Throwable e) {
          LOG.error("Error getting autosync status of a drive " + workspace + ":" + path, e);
          return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading drive: runtime error.").build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST).entity("Null path.").build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity("Null workspace.").build();
    }
  }

}
