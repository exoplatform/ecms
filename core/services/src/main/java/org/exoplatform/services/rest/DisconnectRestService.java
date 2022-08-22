package org.exoplatform.services.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.exoplatform.services.cms.clouddrives.*;
import org.exoplatform.services.rest.resource.ResourceContainer;

@Path("/clouddrive/disconnect")
public class DisconnectRestService implements ResourceContainer {

  private static final Log            LOG = ExoLogger.getLogger(DisconnectRestService.class.getName());

  private DisconnectCloudDriveService disconnectCloudDriveService;

  public DisconnectRestService(DisconnectCloudDriveService disconnectCloudDriveService) {
    this.disconnectCloudDriveService = disconnectCloudDriveService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Disconnect From Cloud Drive", method = "POST", description = "Disconnect From Cloud Drive")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "404", description = "Not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response disconnect(@Parameter(description = "workspace", required = true)
  @QueryParam("workspace")
  String workspace,
                             @Parameter(description = "path", required = true)
                             @QueryParam("path")
                             String path) {
    try {
      disconnectCloudDriveService.disconnectCloudDrive(workspace, path);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error disconnecting from cloud drive", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }
}
