/*
 * Copyright (C) 2021 eXo Platform SAS.
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
import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.rest.model.AttachmentEntity;
import org.exoplatform.services.attachments.service.AttachmentsService;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.manager.IdentityManager;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/attachments")
@Api(value = "/attachments", description = "Managing attachments")
public class AttachmentsRestService implements ResourceContainer {
  private static final Log     LOG = ExoLogger.getLogger(AttachmentsRestService.class.getName());

  protected AttachmentsService attachmentsService;

  protected IdentityManager    identityManager;

  public AttachmentsRestService(AttachmentsService attachmentsService, IdentityManager identityManager) {
    this.attachmentsService = attachmentsService;
    this.identityManager = identityManager;
  }

  @POST
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Link an existing attachments to the given entity (Event, Task, Wiki,...)", httpMethod = "POST", response = Response.class, consumes = "application/json")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response linkAttachmentsToEntity(@ApiParam(value = "entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                          @ApiParam(value = "entity type", required = true) @PathParam("entityType") String entityType,
                                          @ApiParam(value = "list of files uuid stored in jcr attached to the provided entity", required = true) @QueryParam("attachmentIds") List<String> attachmentIds) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    if (attachmentIds.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("You must link at least one attachment to the entity").build();
    }

    try {
      attachmentsService.linkAttachmentsToEntity(entityId, entityType, attachmentIds);
    } catch (Exception e) {
      LOG.error("Error when trying to link attachments to a context: ", e);
    }
    return Response.ok().build();
  }

  @PUT
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Update entity's attachments list", httpMethod = "PUT", response = Response.class, consumes = "application/json")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response updateAttachmentsLinkedToContext(@ApiParam(value = "entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                                   @ApiParam(value = "entity type", required = true) @PathParam("entityType") String entityType,
                                                   @ApiParam(value = "list of files uuid stored in jcr attached to the provided entity", required = true) @QueryParam("attachmentIds") List<String> attachmentIds) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    if (attachmentIds.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("You must link at least one attachment to the entity").build();
    }

    try {
      attachmentsService.updateEntityAttachments(entityId, entityType, attachmentIds);
    } catch (Exception e) {
      LOG.error("Error when trying to link attachments to a context: ", e);
    }
    return Response.ok().build();
  }

  @GET
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get the list of attachments linked to the given entity", httpMethod = "GET", response = Response.class, consumes = "application/json")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response getAttachmentsByEntity(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity identifier must be a positive integer").build();
    }
    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type must not be empty").build();
    }

    List<Attachment> attachments = attachmentsService.getAttachmentsByEntity(entityId, entityType);
    List<AttachmentEntity> attachmentsEntities = new ArrayList<>();
    if (!attachments.isEmpty()) {
      attachmentsEntities = attachments.stream()
                                       .map(attachment -> EntityBuilder.fromAttachment(identityManager, attachment))
                                       .collect(Collectors.toList());
    }
    return Response.ok(attachmentsEntities).build();
  }

  @DELETE
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Delete the list of attachments linked to the given entity", httpMethod = "DELETE", response = Response.class, consumes = "application/json")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response DeleteAttachmentsByEntity(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                            @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    try {
      attachmentsService.deleteAllEntityAttachments(entityId, entityType);
      return Response.noContent().build();
    } catch (ObjectNotFoundException e) {
      LOG.error("Error when trying to delete all attachments from entity from entity with id '{}' ", entityId, e);
      return Response.status(Response.Status.NOT_FOUND).entity("AttachmentContext not found").build();
    }
  }

  @DELETE
  @Path("{entityType}/{entityId}/{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Delete an attachment linked to the given entity", httpMethod = "DELETE", response = Response.class, consumes = "application/json", notes = "returns empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response DeleteEntityAttachment(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType,
                                         @ApiParam(value = "Attachment id", required = true) @PathParam("attachmentId") String attachmentId) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    try {
      attachmentsService.deleteAttachmentItemById(entityId, entityType, attachmentId);
      return Response.noContent().build();
    } catch (ObjectNotFoundException e) {
      LOG.error("Error when trying to delete the attachment with id '{}' from entity with id '{}'", attachmentId, entityId, e);
      return Response.status(Response.Status.NOT_FOUND).entity("AttachmentContext not found").build();
    }
  }

}
