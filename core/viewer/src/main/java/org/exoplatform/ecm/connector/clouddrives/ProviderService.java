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

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

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

  /** The Constant LOG. */
  protected static final Log        LOG = ExoLogger.getLogger(ProviderService.class);

  /** The cloud drives. */
  protected final CloudDriveService cloudDrives;

  /**
   * REST cloudDrives uses {@link CloudDriveService} for actual job.
   *
   * @param cloudDrives the cloud drives
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
