package org.exoplatform.services.rest.transferRules;

import io.swagger.annotations.*;
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
  @Path("/getTransfertRulesDocumentStatus")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the status of the transfert rules documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns the status of the transfert rules documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse (code = 500, message = "Internal server error due to data encoding")})
  public Response getTransfertRulesDocumentStatus() {
    SettingValue<?> sharedDocumentSettingValue = settingService.get(Context.GLOBAL.id("sharedDocumentStatus"),
                                                      Scope.APPLICATION.id("sharedDocumentStatus"),
                                                      "exo:sharedDocumentStatus");
    SettingValue<?> downloadDocumentsettingValue = settingService.get(Context.GLOBAL.id("downloadDocumentStatus"),
                                                      Scope.APPLICATION.id("downloadDocumentStatus"),
                                                      "exo:downloadDocumentStatus");
    boolean isSharedDocumentActivated = sharedDocumentSettingValue != null && !sharedDocumentSettingValue.getValue().toString().isEmpty() ? Boolean.valueOf(sharedDocumentSettingValue.getValue().toString()) : false;
    boolean isDownloadDocumentActivated = downloadDocumentsettingValue != null && !downloadDocumentsettingValue.getValue().toString().isEmpty() ? Boolean.valueOf(downloadDocumentsettingValue.getValue().toString()) : false;
    return Response.ok().entity(new TransferRulesStatusModel(Boolean.toString(isSharedDocumentActivated), Boolean.toString(isDownloadDocumentActivated))).build();
  }

  @PUT
  @Path("/saveSharedDocumentStatus")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Updates the shared document status",
      httpMethod = "PUT",
      response = Response.class,
      notes = "Updates the shared document status.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"),
      @ApiResponse (code = 500, message = "Internal server error due to data encoding")})
  public Response saveSharedDocumentStatus(String sharedDocumentStatus) {
    settingService.set(Context.GLOBAL.id("sharedDocumentStatus"), Scope.APPLICATION.id("sharedDocumentStatus"),
                       "exo:sharedDocumentStatus",
                       SettingValue.create(sharedDocumentStatus));
    return Response.ok(sharedDocumentStatus).build();
  }

  @PUT
  @Path("/saveDownloadDocumentStatus")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Updates the download document status",
      httpMethod = "PUT",
      response = Response.class,
      notes = "Updates the download document status.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"),
      @ApiResponse (code = 500, message = "Internal server error due to data encoding")})
  public Response saveDownloadDocumentStatus(String downloadDocumentStatus) {
    settingService.set(Context.GLOBAL.id("downloadDocumentStatus"), Scope.APPLICATION.id("downloadDocumentStatus"),
                       "exo:downloadDocumentStatus",
                       SettingValue.create(downloadDocumentStatus));
    return Response.ok(downloadDocumentStatus).build();
  }
}
