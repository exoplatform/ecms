/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.rest;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


/**
 * REST service providing information about providers. Created by The eXo
 * Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ProviderService.java 00000 Oct 13, 2012 pnedonosko $
 */
@Path("/clouddrive/provider")
@Produces(MediaType.APPLICATION_JSON)
public class ProviderService implements ResourceContainer {

  protected static final Log        LOG = ExoLogger.getLogger(ProviderService.class);

  protected final CloudDriveService cloudDrives;

  /**
   * REST cloudDrives uses {@link CloudDriveService} for actual job.
   * 
   * @param {@link CloudDriveService} cloudDrives
   */
  public ProviderService(CloudDriveService cloudDrives) {
    this.cloudDrives = cloudDrives;
  }

  /**
   * Return available providers.
   * 
   * @return set of {@link CloudProvider} currently available for connect
   */
  @GET
  @RolesAllowed("users")
  @Path("/all")
  public Set<CloudProvider> getAll() {
    return cloudDrives.getProviders();
  }

  /**
   * Return provider by its id.
   * 
   * @param providerId - provider name see more in {@link CloudProvider}
   * @return response with asked {@link CloudProvider} json
   */
  @GET
  @RolesAllowed("users")
  @Path("/{providerid}")
  public Response getById(@PathParam("providerid") String providerId) {

    try {
      return Response.ok().entity(cloudDrives.getProvider(providerId)).build();
    } catch (CloudDriveException e) {
      LOG.warn("Cannot return prvider by id " + providerId + ": " + e.getMessage());
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

}
