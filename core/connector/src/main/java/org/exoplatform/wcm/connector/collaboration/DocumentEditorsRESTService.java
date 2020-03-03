/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.connector.collaboration;

import java.util.Arrays;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.exception.EditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.model.EditorProvider;
import org.exoplatform.services.cms.documents.model.Link;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class DocumentEditorsRESTService is REST endpoint for working with editable documents.
 * Its used to set prefered editor for specific user/document.
 *
 */
@Path("/documents/editors")
public class DocumentEditorsRESTService implements ResourceContainer {

  /** The Constant PROVIDER_NOT_REGISTERED. */
  private static final String PROVIDER_NOT_REGISTERED = "DocumentEditors.error.EditorProviderNotRegistered";

  /** The Constant EMPTY_REQUEST. */
  private static final String EMPTY_REQUEST           = "DocumentEditors.error.EmptyRequest";

  /** The Constant LOG. */
  protected static final Log  LOG                     = ExoLogger.getLogger(DocumentEditorsRESTService.class);

  /** The document service. */
  protected DocumentService   documentService;

  /**
   * Instantiates a new document editors REST service.
   *
   * @param documentService the document service
   */
  public DocumentEditorsRESTService(DocumentService documentService) {
    this.documentService = documentService;

  }

  /**
   * Return all available editor providers.
   *
   * @param uriInfo the uri info
   * @return the response
   */
  @GET
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEditors(@Context UriInfo uriInfo) {
    List<EditorProvider> providers = documentService.getEditorProviders();
    providers.forEach(provider -> initLinks(provider, uriInfo));
    try {
      String json = new JsonGeneratorImpl().createJsonArray(providers).toString();
      return Response.status(Status.OK).entity("{\"editors\":" + json + "}").build();
    } catch (Exception e) {
      LOG.error("Cannot get editors, error: {}", e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets the preferred editor for specific user/document.
   *
   * @param uriInfo the uri info
   * @param provider the provider
   * @return the response
   */
  @GET
  @Path("/{provider}")
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEditor(@Context UriInfo uriInfo, @PathParam("provider") String provider) {
    try {
      EditorProvider editorProvider = documentService.getEditorProvider(provider);
      initLinks(editorProvider, uriInfo);
      return Response.status(Status.OK).entity(editorProvider).build();
    } catch (EditorProviderNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("{ \"message\":\"" + PROVIDER_NOT_REGISTERED + "\"}").build();
    }
  }

  /**
   * Saves the editor provider.
   *
   * @param provider the provider
   * @param active the active
   * @param permissions the permissions
   * @return the response
   */
  @POST
  @Path("/{provider}")
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateEditor(@PathParam("provider") String provider,
                               @FormParam("active") Boolean active,
                               @FormParam("permissions") List<String> permissions) {
    if (active == null && permissions == null) {
      return Response.status(Status.BAD_REQUEST).entity("{ \"message\":\"" + EMPTY_REQUEST + "\"}").build();
    }
    EditorProvider editorProvider = new EditorProvider(provider, active, permissions);
    try {
      documentService.updateEditorProvider(editorProvider);
      return Response.status(Status.OK).build();
    } catch (EditorProviderNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity("{ \"message\":\"" + PROVIDER_NOT_REGISTERED + "\"}").build();
    }
  }

  /**
   * Sets the prefered editor for specific user/document.
   *
   * @param fileId the file id
   * @param userId the user id
   * @param provider the provider
   * @param workspace the workspace
   * @return the response
   */
  @POST
  @Path("/prefered/{fileId}")
  @RolesAllowed("users")
  public Response preferedEditor(@PathParam("fileId") String fileId,
                                 @FormParam("userId") String userId,
                                 @FormParam("provider") String provider,
                                 @FormParam("workspace") String workspace) {
    try {
      documentService.setPreferedEditor(userId, provider, fileId, workspace);
    } catch (Exception e) {
      LOG.error("Cannot set prefered editor for user {} and node {}: {}", userId, fileId, e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok().build();
  }

  /**
   * Inits the links.
   *
   * @param provider the provider
   * @param uriInfo the uri info
   */
  protected void initLinks(EditorProvider provider, UriInfo uriInfo) {
    String path = uriInfo.getAbsolutePath().toString();
    if (!uriInfo.getPathParameters().containsKey("provider")) {
      StringBuilder pathBuilder = new StringBuilder(path);
      if (!path.endsWith("/")) {
        pathBuilder.append("/");
      }
      path = pathBuilder.append(provider.getProvider()).toString();
    }
    Link self = new Link("self", path.toString());
    Link update = new Link("update", path.toString());
    provider.setLinks(Arrays.asList(self, update));
  }

}
