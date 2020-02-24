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
package org.exoplatform.wcm.ext.component.document.service;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.exception.EditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.model.EditorProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * The Class DocumentEditorsRESTService is REST endpoint for working with editable documents.
 * Its used to set prefered editor for specific user/document.
 *
 */
@Path("/documents/editors")
public class DocumentEditorsRESTService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(DocumentEditorsRESTService.class);

  /** The document service. */
  protected DocumentService  documentService;

  /**
   * Instantiates a new document editors REST service.
   *
   * @param documentService the document service
   */
  public DocumentEditorsRESTService(DocumentService documentService) {
    this.documentService = documentService;
  }

  /**
   * Sets the prefered editor for specific user/document.
   *
   * @param fileId the file id
   * @param data the data
   * @return the response
   */
  @GET
  //@RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEditors() {
    List<EditorProvider> providers = documentService.getEditorProviders();
    return Response.status(Status.OK).entity(providers).build();
  }

  /**
   * Sets the prefered editor for specific user/document.
   *
   * @param fileId the file id
   * @param data the data
   * @return the response
   */
  @GET
  @Path("/{provider}")
 //@RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEditor(@PathParam("provider") String provider) {
    try {
      EditorProvider editorProvider = documentService.getEditorProvider(provider);
      return Response.status(Status.OK).entity(editorProvider).build();
    } catch (EditorProviderNotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  /**
   * Sets the prefered editor for specific user/document.
   *
   * @param fileId the file id
   * @param data the data
   * @return the response
   */
  @POST
  @Path("/{provider}")
  //@RolesAllowed("users")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateEditor(@PathParam("provider") String provider, EditorProvider editorProvider) {
    if (editorProvider != null) {
      editorProvider.setProvider(provider);
      try {
        documentService.updateEditorProvider(editorProvider);
        return Response.status(Status.OK).build();
      } catch (EditorProviderNotFoundException e) {
        return Response.status(Status.NOT_FOUND).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Sets the prefered editor for specific user/document.
   *
   * @param fileId the file id
   * @param data the data
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
}
