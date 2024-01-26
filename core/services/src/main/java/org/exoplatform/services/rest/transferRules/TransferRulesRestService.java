package org.exoplatform.services.rest.transferRules;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "/transferRules", description = "Managing transfer rules")
public class TransferRulesRestService implements ResourceContainer {

  private SettingService settingService;

  public TransferRulesRestService(SettingService settingService) {
    this.settingService = settingService;
  }

  @GET
  @Path("/getTransfertRulesDocumentStatus")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Gets the status of the transfert rules documents",
          method = "GET",
          description = "This returns the status of the transfert rules documents")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse (responseCode = "500", description = "Internal server error due to data encoding")})
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
  @Operation(
          summary = "Updates the shared document status",
          method = "PUT",
          description = "Updates the shared document status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse (responseCode = "500", description = "Internal server error due to data encoding")})
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
  @Operation(
          summary = "Updates the download document status",
          method = "PUT",
          description = "Updates the download document status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse (responseCode = "500", description = "Internal server error due to data encoding")})
  public Response saveDownloadDocumentStatus(String downloadDocumentStatus) {
    settingService.set(Context.GLOBAL.id("downloadDocumentStatus"), Scope.APPLICATION.id("downloadDocumentStatus"),
                       "exo:downloadDocumentStatus",
                       SettingValue.create(downloadDocumentStatus));
    return Response.ok(downloadDocumentStatus).build();
  }
}
