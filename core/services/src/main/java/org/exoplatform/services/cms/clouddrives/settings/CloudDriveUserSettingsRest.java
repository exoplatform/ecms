/*
 * Copyright (C) 2022 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.cms.clouddrives.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.manager.IdentityManager;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/clouddrive/settings")
@Tag(name = "/clouddrive/settings", description = "Manages clouddrive connectors settings associated to users")
public class CloudDriveUserSettingsRest implements ResourceContainer {

  private static final Log                LOG = ExoLogger.getLogger(CloudDriveUserSettingsRest.class);

  private CloudDriveUserSettingsService   cloudDriveUserSettingsService;

  private IdentityManager                 identityManager;

  public CloudDriveUserSettingsRest(CloudDriveUserSettingsService cloudDriveUserSettingsService, IdentityManager identityManager) {
    this.identityManager = identityManager;
    this.cloudDriveUserSettingsService = cloudDriveUserSettingsService;
  }


  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Saves clouddrive connectors settings for authenticated user",
          method = "PUT"
  )
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                  @ApiResponse(responseCode = "400", description = "Invalid query input"),
                  @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
                  @ApiResponse(responseCode = "500", description = "Internal server error")
          }
  )
  public Response saveUserSettings(
          @Parameter(
                  description = "User's clouddrive connectors settings to update",
                  required = true
          )
                  CloudDriveSettingsRestEntity cloudDriveSettingsRestEntity) {
    if (cloudDriveSettingsRestEntity == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("clouddrive connectors settings object is mandatory").build();
    }
    long identityId = RestUtils.getCurrentUserIdentityId(identityManager);
    try {
      cloudDriveUserSettingsService.saveCloudDriveUserSettings(identityId, cloudDriveSettingsRestEntity);
      return Response.noContent().build();
    } catch (Exception e) {
      LOG.warn("Error saving clouddrive connectors settings for user with id '{}'", identityId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Get clouddrive connectors settings for authenticated user",
          method = "GET"
  )
  @ApiResponses(
          value = {
                  @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                  @ApiResponse(responseCode = "400", description = "Invalid query input"),
                  @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
                  @ApiResponse(responseCode = "500", description = "Internal server error")
          }
  )
  public Response getUserSettings() {
    long identityId = RestUtils.getCurrentUserIdentityId(identityManager);
    try {
      String cloudDriveSettingsRestEntity = cloudDriveUserSettingsService.getCloudDriveUserSettings(identityId);
      return Response.ok(cloudDriveSettingsRestEntity).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving clouddrive connectors settings for user with id '{}'", identityId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }
}
