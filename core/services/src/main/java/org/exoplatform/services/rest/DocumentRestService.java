/**
 * 
 */
package org.exoplatform.services.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

/**
 * @author Ayoub Zayati
 */
@Path("/documents")
@Api(tags = "/documents", value = "/documents", description = "Managing documents")
public class DocumentRestService implements ResourceContainer {

  private DocumentService  documentService;

  private static final int DEFAULT_LIMIT = 20;

  public DocumentRestService(DocumentService documentService) {
    this.documentService = documentService;
  }

  @GET
  @Path("/favorite")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets favorite documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns favorite documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getFavoriteDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    return Response.ok(documentService.getFavoriteDocuments(userId, limit)).build();
  }

  @GET
  @Path("/folder")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets documents by folder",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns documents by folder")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response getDocumentsByFolder(@ApiParam(value = "Folder from which documents to be retrieved", required = true) @QueryParam("folder") String folder,
                                       @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (folder == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    return Response.ok(documentService.getDocumentsByFolder(folder, null, limit)).build();
  }

  @GET
  @Path("/query")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets documents by quey",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns documents by quey")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input") })
  public Response getDocumentsByQuery(@ApiParam(value = "Query from which documents to be retrieved", required = true) @QueryParam("query") String query,
                                      @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (query == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    return Response.ok(documentService.getDocumentsByQuery(query, limit)).build();
  }

  @GET
  @Path("/shared")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets shared documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns shared documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getSharedDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    return Response.ok(documentService.getSharedDocuments(userId, limit)).build();
  }

  @GET
  @Path("/recent")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets recent documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns recent documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getRecentDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    return Response.ok(documentService.getMyWorkDocuments(userId, limit)).build();
  }
  
  @GET
  @Path("/recentSpaces")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets recent spaces documents",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns recent spaces documents")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getRecentSpacesDocuments(@ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam("limit") int limit) throws Exception {
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    return Response.ok(documentService.getRecentSpacesDocuments(limit)).build();
  }
}
