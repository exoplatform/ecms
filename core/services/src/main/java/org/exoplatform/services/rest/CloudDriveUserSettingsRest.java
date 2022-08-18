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

package org.exoplatform.services.rest;

import io.swagger.annotations.*;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.cms.clouddrives.settings.CloudDriveSettingsRestEntity;
import org.exoplatform.services.cms.clouddrives.settings.CloudDriveUserSettingsService;
import org.exoplatform.services.cms.clouddrives.settings.RestUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.manager.IdentityManager;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/clouddrive/settings")
@Api(value = "/clouddrive/settings", description = "Manages clouddrive connectors settings associated to users") // NOSONAR
public class CloudDriveUserSettingsRest {

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
  @ApiOperation(
          value = "Saves clouddrive connectors settings for authenticated user",
          httpMethod = "PUT",
          response = Response.class,
          consumes = "application/json"
  )
  @ApiResponses(
          value = {
                  @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
                  @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Bad request"),
                  @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
                  @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error"),
          }
  )
  public Response saveUserSettings(
          @ApiParam(
                  value = "User's clouddrive connectors settings to update",
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
}
