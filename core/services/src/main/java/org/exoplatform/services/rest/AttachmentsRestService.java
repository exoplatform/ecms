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
import javax.jcr.ItemExistsException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang.StringUtils;

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
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;


@Path("/attachments")
@Tag(name = "/attachments", description = "Managing attachments")
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
  @Path("{entityType}/{entityId}/{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Link an attachment to an entity", description = "Link an existing attachment to the given entity (Event, Task, Wiki,...)", method = "POST")
  @ApiResponses(value = { 
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response linkAttachmentToEntity(@Parameter(description = "entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @Parameter(description = "entity type", required = true) @PathParam("entityType") String entityType,
                                         @Parameter(description = "file uuid stored in jcr to be attached to the provided entity", required = true) @PathParam("attachmentId") String attachmentId) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }

    if (StringUtils.isEmpty(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment id must not be empty").build();
    }

    long userIdentityId = getCurrentUserIdentityId();
    Attachment attachment = new Attachment();
    try {
      attachment = attachmentService.linkAttachmentToEntity(userIdentityId, entityId, entityType, attachmentId);
    } catch (Exception e) {
      LOG.error("Error when trying to link attachments to entity with type {} and id {}: ", entityType, entityId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.ok(attachment).build();
  }

  @PUT
  @Path("{entityType}/{entityId}")
  @Consumes("application/x-www-form-urlencoded")
  @RolesAllowed("users")
  @Operation(summary = "Update entity's attachments list", method = "PUT", description = "Update entity's attachments list")
  @ApiResponses(value = { 
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response updateAttachmentsLinkedToContext(@Parameter(description = "entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                                   @Parameter(description = "entity type", required = true) @PathParam("entityType") String entityType,
                                                   @Parameter(description = "list of files uuid stored in jcr attached to the provided entity", required = true) @FormParam("attachmentId") List<String> attachmentIds) {

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
  @Operation(summary = "Get list of attachments linked to an entity", method = "GET", description = "Get the list of attachments linked to the given entity")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getAttachmentsByEntity(@Parameter(description = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @Parameter(description = "Entity type", required = true) @PathParam("entityType") String entityType) throws Exception {

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
                                         .toList();
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
  @Path("{entityType}/{entityId}/{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get list of attachments linked to the given entity", method = "GET", description = "Get the list of attachments linked to the given entity")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getAttachmentByIdByEntity(@Parameter(description = "Attachment technical identifier", required = true) @PathParam("attachmentId") String attachmentId,
                                            @Parameter(description = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                            @Parameter(description = "Entity type", required = true) @PathParam("entityType") String entityType) throws Exception {
    if (StringUtils.isBlank(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment identifier is mandatory").build();
    }
    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity identifier must be a positive integer").build();
    }
    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type must not be empty").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      Attachment attachment = attachmentService.getAttachmentByIdByEntity(entityType, entityId, attachmentId, userIdentityId);
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

  @GET
  @Path("{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get an attachment with its jcr uuid", method = "GET", description = "Get an attachment with its jcr uuid")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getAttachmentById(@Parameter(description = "Attachment technical identifier", required = true) @PathParam("attachmentId") String attachmentId) throws Exception {
    if (StringUtils.isBlank(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment identifier is mandatory").build();
    }

    try {
      Attachment attachment = attachmentService.getAttachmentById(attachmentId);
      if (attachment == null) {
        return Response.status(Status.NOT_FOUND).build();
      } else {
        return Response.ok(EntityBuilder.fromAttachment(identityManager, attachment)).build();
      }
    } catch (ObjectNotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      LOG.error("Error when trying to get attachment with id {}: ", attachmentId, e);
      return Response.serverError().build();
    }
  }

  @DELETE
  @Path("{entityType}/{entityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Delete a list of attachments", description = "Delete the list of attachments linked to the given entity", method = "DELETE")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteAttachmentsByEntity(@Parameter(description = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                            @Parameter(description = "Entity type", required = true) @PathParam("entityType") String entityType) {

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
      LOG.error("Error when trying to delete all attachments from entity with type {} and with id '{}' ",
                entityType,
                entityId,
                e);
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
  @Operation(summary = "Delete an attachment linked to the given entity", method = "DELETE",  description = "Delete an attachment linked to the given entity and returns the deleted attachment")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteEntityAttachment(@Parameter(description = "Entity technical identifier", required = true) @PathParam("entityId") long entityId,
                                         @Parameter(description = "Entity type", required = true) @PathParam("entityType") String entityType,
                                         @Parameter(description = "Attachment id", required = true) @PathParam("attachmentId") String attachmentId) {

    if (entityId <= 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity technical identifier must be positive").build();
    }

    if (StringUtils.isEmpty(entityType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Entity type is mandatory").build();
    }
    long userIdentityId = getCurrentUserIdentityId();
    try {
      AttachmentEntity attachmentEntity =
                                        EntityBuilder.fromAttachment(identityManager,
                                                                     attachmentService.getAttachmentByIdByEntity(entityType,
                                                                                                                 entityId,
                                                                                                                 attachmentId,
                                                                                                                 userIdentityId));

      attachmentService.deleteAttachmentItemById(userIdentityId, entityId, entityType, attachmentId);
      return Response.ok(attachmentEntity).build();
    } catch (ObjectNotFoundException e) {
      LOG.error("Error when trying to delete the attachment with id '{}' from entity with type {} and id '{}'",
                attachmentId,
                entityType,
                entityId,
                e);
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
  @Operation(summary = "Downloads a list of attachments by it paths", method = "POST", description = "redirects to download URL of binary that contains the list of attachments in a Zip file if multiple, else the selected file")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "303", description = "Request Redirect"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response downloadActivityAttachments(@Parameter(description = "Filename to use for download", required = true) @QueryParam("fileName") String fileName,
                                              @RequestBody(description = "List of file attachments to download", required = true) List<ActivityFileAttachment> activityFileAttachments) throws URISyntaxException {
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
  @Consumes("application/x-www-form-urlencoded")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Move an attachment to a destination path", method = "POST", description = "Move an attachment to a destination path and returns empty response")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response moveAttachmentToNewPath(@Parameter(description = "New path", required = true) @FormParam("newPath") String newPath,
                                          @Parameter(description = "New destination path's drive", required = true) @FormParam("newPathDrive") String newPathDrive,
                                          @Parameter(description = "New destination path's drive", required = true) @FormParam("entityType") String entityType,
                                          @Parameter(description = "Entity technical identifier", required = true) @FormParam("entityId") long entityId,
                                          @Parameter(description = "Entity type", required = true) @PathParam("attachmentId") String attachmentId) {

    if (StringUtils.isEmpty(newPathDrive)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New destination path's drive is mandatory").build();
    }

    if (StringUtils.isEmpty(attachmentId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Attachment id is mandatory").build();
    }

    long userIdentityId = getCurrentUserIdentityId();
    try {
      Attachment attachment = attachmentService.moveAttachmentToNewPath(userIdentityId,
                                                                        attachmentId,
                                                                        newPathDrive,
                                                                        newPath,
                                                                        entityType,
                                                                        entityId);
      return Response.ok(EntityBuilder.fromAttachment(identityManager, attachment)).build();
    } catch (Exception e) {
      LOG.error("Error when trying to move attachment with id {} to new destination path {} ", attachmentId, newPath, e);
      return Response.serverError()
                     .entity("Error when trying to move attachment with id " + attachmentId + " to new destination path {} "
                         + newPath)
                     .build();
    }
  }


  @POST
  @Path("/newDoc")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "create new document",
          method = "POST",
          description = "create new document and returns a new created document")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public Response createNewDocument(@Parameter(description = "title") @Schema(defaultValue = "20") @FormParam("title") String title,
                                    @Parameter(description = "path of new document", required = true) @FormParam("path") String path,
                                    @Parameter(description = "New destination path's drive", required = true) @FormParam("pathDrive") String pathDrive,
                                    @Parameter(description = "template name of new document") @Schema(defaultValue = "20") @FormParam("templateName") String templateName) throws Exception {
    if (StringUtils.isEmpty(title)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New document title is mandatory").build();
    }
    if (StringUtils.isEmpty(templateName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New document template name is mandatory").build();
    }
    if (StringUtils.isEmpty(path)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New document path is mandatory").build();
    }
    if (StringUtils.isEmpty(pathDrive)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New destination path's drive is mandatory").build();
    }

    try {
      Identity userIdentity = getCurrentUserIdentity();
      Attachment attachment = attachmentService.createNewDocument(userIdentity, title, path, pathDrive, templateName);
      return Response.ok(EntityBuilder.fromAttachment(identityManager, attachment)).build();
    } catch (ItemExistsException e) {
      return Response.status(Status.CONFLICT).entity("Document with the same name already exist in this current path").build();
    } catch (Exception e) {
      LOG.error("Error when trying to a new document with type ", templateName, e);
      return Response.serverError()
              .entity("Error when trying to a new document with type "
                      + templateName)
              .build();
    }
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
      ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
      nodeLocations[i] = new NodeLocation(activityFileAttachment.getRepository(),
                                          activityFileAttachment.getWorkspace(),
                                          activityFileAttachment.getDocPath(),
                                          activityFileAttachment.getId(),
                                          false);
    }
    ActivityFilesDownloadResource dresource = new ActivityFilesDownloadResource(nodeLocations);
    dresource.setDownloadName(fileName);
    String downloadResource = downloadService.addDownloadResource(dresource);
    return downloadService.getDownloadLink(downloadResource);
  }

}
