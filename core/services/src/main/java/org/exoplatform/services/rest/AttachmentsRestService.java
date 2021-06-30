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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.download.DownloadService;
import org.exoplatform.services.attachments.model.*;
import org.exoplatform.services.attachments.rest.model.AttachmentEntity;
import org.exoplatform.services.attachments.service.AttachmentService;
import org.exoplatform.services.attachments.utils.EntityBuilder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

import io.swagger.annotations.*;

@Path("/attachments")
@Api(value = "/attachments", description = "Managing attachments") // NOSONAR deprecated attribute
public class AttachmentsRestService implements ResourceContainer {
  private static final Log    LOG = ExoLogger.getLogger(AttachmentsRestService.class.getName());

  protected AttachmentService attachmentService;

  protected IdentityManager   identityManager;

  protected DownloadService   downloadService;

  public AttachmentsRestService(AttachmentService attachmentService,
                                IdentityManager identityManager,
                                DownloadService downloadService) {
    this.attachmentService = attachmentService;
    this.identityManager = identityManager;
    this.downloadService = downloadService;
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
                                          @ApiParam(value = "list of files uuid stored in jcr attached to the provided entity", required = true) @QueryParam("attachmentIds") List<String> attachmentIds) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    if (attachmentIds.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("You must link at least one attachment to the entity").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      attachmentService.linkAttachmentsToEntity(userIdentityId, entityId, entityType, attachmentIds);
    } catch (Exception e) {
      LOG.error("Error when trying to link attachments to entity with type {} and id {}: ", entityType, entityId, e);
      return Response.serverError().entity(e.getMessage()).build();
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
                                                   @ApiParam(value = "list of files uuid stored in jcr attached to the provided entity", required = true) @QueryParam("attachmentIds") List<String> attachmentIds) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      attachmentService.updateEntityAttachments(userIdentityId, entityId, entityType, attachmentIds);
    } catch (IllegalAccessException e) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when trying to update attachments of entity type {} with id {}: ", entityType, entityId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.ok().build();
  }

  @GET
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get the list of attachments linked to the given entity", httpMethod = "GET", response = Response.class, produces = "application/json")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error")
  })
  public Response getAttachmentsByEntity(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType) throws Exception {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity identifier must be a positive integer").build();
    }
    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type must not be empty").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      List<Attachment> attachments = attachmentService.getAttachmentsByEntity(userIdentityId, entityId, entityType);
      List<AttachmentEntity> attachmentsEntities = new ArrayList<>();
      if (!attachments.isEmpty()) {
        attachmentsEntities = attachments.stream()
          .map(attachment -> EntityBuilder.fromAttachment(identityManager, attachment))
          .filter(attachmentEntity -> attachmentEntity.getAcl().isCanView())
          .collect(Collectors.toList());
      }
      return Response.ok(attachmentsEntities).build();
    } catch (IllegalAccessException e) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when trying to get attachments of entity type {} with id {}: ", entityType, entityId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get the list of attachments linked to the given entity", httpMethod = "GET", response = Response.class, produces = "application/json")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error")
  })
  public Response getAttachmentById(@ApiParam(value = "Attachment technical identifier", required = true) @PathParam("attachmentId") String attachmentId) throws Exception {
    if (StringUtils.isBlank(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment identifier is mandatory").build();
    }
    try {
      Attachment attachment = attachmentService.getAttachmentById(attachmentId, WCMCoreUtils.getUserSessionProvider());
      if (attachment == null) {
        return Response.status(Status.NOT_FOUND).build();
      } else {
        return Response.ok(EntityBuilder.fromAttachment(identityManager, attachment)).build();
      }
    } catch (Exception e) {
      LOG.error("Error when trying to get attachment with id {}: ", attachmentId, e);
      return Response.serverError().build();
    }
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
  public Response deleteAttachmentsByEntity(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                            @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      attachmentService.deleteAllEntityAttachments(userIdentityId, entityId, entityType);
      return Response.noContent().build();
    } catch (ObjectNotFoundException e) {
      LOG.error("Error when trying to delete all attachments from entity with type {} and with id '{}' ", entityType, entityId, e);
      return Response.status(Response.Status.NOT_FOUND).entity("AttachmentContext not found").build();
    } catch (IllegalAccessException e) {
      LOG.error("User '{}' attempts to delete a non authorized {} entity with id {}", userIdentityId, entityType, entityId);
      return Response.status(Response.Status.UNAUTHORIZED).build();
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
  public Response deleteEntityAttachment(@ApiParam(value = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @ApiParam(value = "Entity type", required = true) @PathParam("entityType") String entityType,
                                         @ApiParam(value = "Attachment id", required = true) @PathParam("attachmentId") String attachmentId) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      attachmentService.deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
      return Response.noContent().build();
    } catch (ObjectNotFoundException e) {
      LOG.error("Error when trying to delete the attachment with id '{}' from entity with type {} and id '{}'", attachmentId, entityType, entityId, e);
      return Response.status(Response.Status.NOT_FOUND).entity("AttachmentContext not found").build();
    } catch (IllegalAccessException e) {
      LOG.error("User '{}' attempts to delete a non authorized {} entity with id {}", userIdentityId, entityType, entityId);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
  }

  @POST
  @Path("/downloadByPath")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Downloads a list of attachments by it paths", httpMethod = "POST", response = Response.class, consumes = "application/x-www-form-urlencoded", notes = "redirects to download URL of binary that contains the list of attachments in a Zip file if multiple, else the selected file")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.SEE_OTHER, message = "Request Redirect"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response downloadActivityAttachments(@ApiParam(value = "Filename to use for download", required = true) @QueryParam("fileName") String fileName,
                                              @ApiParam(value = "List of file attachments to download", required = true) List<ActivityFileAttachment> activityFileAttachments) throws URISyntaxException {
    if (activityFileAttachments == null || activityFileAttachments.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("attachments param is mandatory").build();
    }
    if (StringUtils.isBlank(fileName)) {
      return Response.status(Status.BAD_REQUEST).entity("fileName param is mandatory").build();
    }

    String downloadLink = getDownloadLink(activityFileAttachments, fileName);
    URI location = new URI(downloadLink);
    return Response.seeOther(location).build();
  }

  @POST
  @Path("/{attachmentId}/move")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Move an attachment to a destination path", httpMethod = "POST", response = Response.class, consumes = "application/json", notes = "returns empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response moveAttachmentToNewPath(@ApiParam(value = "New path", required = true) @QueryParam("newPath") String newPath,
                                          @ApiParam(value = "New destination path's drive", required = true) @QueryParam("newPathDrive") String newPathDrive,
                                          @ApiParam(value = "Attachment id", required = true) @PathParam("attachmentId") String attachmentId) {

    if (StringUtils.isEmpty(newPathDrive)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New destination path's drive is mandatory").build();
    }

    if (StringUtils.isEmpty(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment id is mandatory").build();
    }

    long userIdentityId = getCurrentUserIdentityId();
    try {
      attachmentService.moveAttachmentToNewPath(userIdentityId, attachmentId, newPathDrive, newPath);
    } catch (Exception e) {
      LOG.error("Error when trying to move attachment with id {} to new destination path {} ", attachmentId, newPath, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.noContent().build();
  }

  public Identity getCurrentUserIdentity() {
    return ConversationState.getCurrent().getIdentity();
  }

  public long getCurrentUserIdentityId() {
    String currentUser = getCurrentUserIdentity().getUserId();
    org.exoplatform.social.core.identity.model.Identity identity =
                                                                 identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                     currentUser);
    return identity == null ? 0 : Long.parseLong(identity.getId());
  }

  public String getDownloadLink(List<ActivityFileAttachment> activityFileAttachments, String fileName) {
    NodeLocation[] nodeLocations = new NodeLocation[activityFileAttachments.size()];
    for (int i = 0; i < activityFileAttachments.size(); i++) {
      nodeLocations[i] = activityFileAttachments.get(i).getNodeLocation();
    }
    ActivityFilesDownloadResource dresource = new ActivityFilesDownloadResource(nodeLocations);
    dresource.setDownloadName(fileName);
    String downloadResource = downloadService.addDownloadResource(dresource);
    return downloadService.getDownloadLink(downloadResource);
  }

}
