/**
 *
 */
package org.exoplatform.services.rest.transferRules;

import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/transferRules")
@Api(tags = "/transferRules", value = "/transferRules", description = "Managing transfer rules")
public class TransferRulesRestService implements ResourceContainer {

  private SettingService settingService;

  public TransferRulesRestService(SettingService settingService) {
    this.settingService = settingService;
  }

  @GET
  @Path("/getSharedDocumentStatus")
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets shared documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns shared documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getSharedDocumentStatus() {
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id("sharedDocumentStatus"),
                                                      Scope.APPLICATION.id("sharedDocumentStatus"),
                                                      "exo:sharedDocumentStatus");
    boolean isSharedDocumentActivated = settingValue != null && !settingValue.getValue().toString().isEmpty() ? Boolean.valueOf(settingValue.getValue().toString()) : true;
    return Response.ok().entity("{\"isSharedDocumentActivated\":\"" + isSharedDocumentActivated + "\"}").build();
  }

  @PUT
  @Path("/saveSharedDocumentStatus")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Updates the share document status",
      httpMethod = "PUT",
      response = Response.class,
      notes = "Updates the share document status.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response saveSharedDocumentStatus(TransferRulesStatusModel transferRulesStatusModel) {
    if (StringUtils.isBlank(transferRulesStatusModel.getSharedDocumentStatus())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Status must not be null or blank").build();
    }
    settingService.set(Context.GLOBAL.id("sharedDocumentStatus"),
                       Scope.APPLICATION.id("sharedDocumentStatus"),
                       "exo:sharedDocumentStatus",
                       SettingValue.create(transferRulesStatusModel.getSharedDocumentStatus()));
    return Response.ok(transferRulesStatusModel.getSharedDocumentStatus()).build();
  }
}
