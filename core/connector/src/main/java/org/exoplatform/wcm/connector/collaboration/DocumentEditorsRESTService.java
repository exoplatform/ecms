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

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.exception.PermissionValidationException;
import org.exoplatform.services.cms.documents.impl.EditorProvidersHelper;
import org.exoplatform.services.cms.documents.impl.EditorProvidersHelper.ProviderInfo;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.connector.collaboration.editors.DocumentEditorData;
import org.exoplatform.wcm.connector.collaboration.editors.EditorPermission;
import org.exoplatform.wcm.connector.collaboration.editors.ErrorMessage;
import org.exoplatform.wcm.connector.collaboration.editors.HypermediaLink;
import org.exoplatform.wcm.connector.collaboration.editors.PreviewInfo;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class DocumentEditorsRESTService is REST endpoint for working with editable documents.
 *
 */
@Path("/documents/editors")
public class DocumentEditorsRESTService implements ResourceContainer {

  private static final String         EXO_SYMLINK                  = "exo:symlink";

  /** The Constant PROVIDER_NOT_REGISTERED. */
  private static final String         PROVIDER_NOT_REGISTERED      = "EditorProviderNotRegistered";

  /** The Constant EMPTY_REQUEST. */
  private static final String         EMPTY_REQUEST                = "EmptyRequest";

  /** The Constant CANNOT_GET_PROVIDERS. */
  private static final String         CANNOT_GET_PROVIDERS         = "CannotGetProviders";

  /** The Constant PERMISSION_NOT_VALID. */
  private static final String         PERMISSION_NOT_VALID         = "PermissionNotValid";

  /** The Constant CANNOT_SAVE_PREFERRED_EDITOR. */
  private static final String         CANNOT_SAVE_PREFERRED_EDITOR = "CannotSavePreferredEditor";

  /** The Constant SELF. */
  private static final String         SELF                         = "self";

  /** The Constant LOG. */
  protected static final Log          LOG                          = ExoLogger.getLogger(DocumentEditorsRESTService.class);

  /** The document service. */
  protected final DocumentService     documentService;

  /** The document service. */
  protected final OrganizationService organization;

  /** The document service. */
  protected final SpaceService        spaceService;

  /** The identity manager. */
  protected final IdentityManager     identityManager;

  /** The repository service. */
  protected final RepositoryService   repositoryService;

  /** The link manager. */
  protected final LinkManager         linkManager;

  /**
   * Instantiates a new document editors REST service.
   *
   * @param documentService the document service
   * @param spaceService the space service
   * @param organizationService the organization service
   * @param identityManager the identity manager
   * @param repositoryService the repository service
   * @param linkManager the link manager
   */
  public DocumentEditorsRESTService(DocumentService documentService,
                                    SpaceService spaceService,
                                    OrganizationService organizationService,
                                    IdentityManager identityManager,
                                    RepositoryService repositoryService,
                                    LinkManager linkManager) {
    this.documentService = documentService;
    this.identityManager = identityManager;
    this.organization = organizationService;
    this.spaceService = spaceService;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
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
    List<DocumentEditorData> providers = documentService.getDocumentEditorProviders()
                                                        .stream()
                                                        .map(this::convertToDTO)
                                                        .collect(Collectors.toList());
    providers.forEach(provider -> initLinks(provider, uriInfo));
    try {
      String json = new JsonGeneratorImpl().createJsonArray(providers).toString();
      return Response.status(Status.OK).entity("{\"editors\":" + json + "}").build();
    } catch (JsonException e) {
      LOG.error("Cannot get providers JSON, error: {}", e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorMessage(e.getMessage(), CANNOT_GET_PROVIDERS)).build();
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
      DocumentEditorProvider editorProvider = documentService.getEditorProvider(provider);
      DocumentEditorData providerData = convertToDTO(editorProvider);
      initLinks(providerData, uriInfo);
      return Response.status(Status.OK).entity(providerData).build();
    } catch (DocumentEditorProviderNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity(new ErrorMessage(e.getMessage(), PROVIDER_NOT_REGISTERED)).build();
    }
  }

  /**
   * Saves the editor provider.
   *
   * @param provider the provider
   * @param documentEditorData the editor provider DTO
   * @return the response
   */
  @PUT
  @Path("/{provider}")
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateEditor(@PathParam("provider") String provider, DocumentEditorData documentEditorData) {
    if (documentEditorData == null || documentEditorData.getActive() == null && documentEditorData.getPermissions() == null) {
      return Response.status(Status.BAD_REQUEST)
                     .entity(new ErrorMessage("The request should contain active or/and permissions fields.", EMPTY_REQUEST))
                     .build();
    }
    try {
      DocumentEditorProvider editorProvider = documentService.getEditorProvider(provider);
      if (documentEditorData.getActive() != null) {
        editorProvider.updateActive(documentEditorData.getActive());
      }
      if (documentEditorData.getPermissions() != null) {
        List<String> permissions = documentEditorData.getPermissions()
                                                     .stream()
                                                     .map(permission -> permission.getId())
                                                     .collect(Collectors.toList());
        editorProvider.updatePermissions(permissions);
      }
      return Response.status(Status.OK).build();
    } catch (DocumentEditorProviderNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity(new ErrorMessage(e.getMessage(), PROVIDER_NOT_REGISTERED)).build();
    } catch (PermissionValidationException e) {
      return Response.status(Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage(), PERMISSION_NOT_VALID)).build();
    }
  }

  /**
   * Sets the preferred editor for specific user/document.
   *
   * @param fileId the file id
   * @param userId the user id
   * @param provider the provider
   * @param workspace the workspace
   * @return the response
   */
  @POST
  @Path("/preferred/{fileId}")
  @RolesAllowed("users")
  public Response preferredEditor(@PathParam("fileId") String fileId,
                                  @FormParam("userId") String userId,
                                  @FormParam("provider") String provider,
                                  @FormParam("workspace") String workspace) {
    try {
      documentService.savePreferredEditor(userId, provider, fileId, workspace);
    } catch (AccessDeniedException e) {
      LOG.error("Access denied to set preferred editor for user {} and node {}: {}", userId, fileId, e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity(new ErrorMessage("Access denied error.", CANNOT_SAVE_PREFERRED_EDITOR))
                     .build();
    } catch (RepositoryException e) {
      LOG.error("Cannot set preferred editor for user {} and node {}: {}", userId, fileId, e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR)
                     .entity(new ErrorMessage(e.getMessage(), CANNOT_SAVE_PREFERRED_EDITOR))
                     .build();
    }
    return Response.ok().build();
  }

  /**
   * Inits preview.
   *
   * @param uriInfo the uri info
   * @param request the request
   * @param fileId the file id
   * @param workspace the workspace
   * @return the response
   */
  @POST
  @Path("/preview")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response initPreview(@Context UriInfo uriInfo,
                              @Context HttpServletRequest request,
                              @FormParam("fileId") String fileId,
                              @FormParam("workspace") String workspace) {
    org.exoplatform.services.security.Identity identity = ConversationState.getCurrent().getIdentity();
    List<DocumentEditorProvider> providers = documentService.getDocumentEditorProviders();
    List<ProviderInfo> providersInfo = EditorProvidersHelper.getInstance()
                                                            .initPreview(providers,
                                                                         identity,
                                                                         fileId,
                                                                         workspace,
                                                                         uriInfo.getRequestUri(),
                                                                         request.getLocale());

    String targetFileId = fileId;
    try {
      targetFileId = getTargetFileId(fileId, workspace);
    } catch (RepositoryException e) {
      LOG.warn("Cannot get fileId from symlink taget.", e.getMessage());
    }
    PreviewInfo previewInfo = new PreviewInfo(targetFileId, providersInfo);
    return Response.ok().entity(previewInfo).build();
  }

  /**
   * Gets the symlink target file id.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @return the target file id
   * @throws RepositoryException the repository exception
   */
  protected String getTargetFileId(String fileId, String workspace) throws RepositoryException {
    ExtendedSession systemSession = (ExtendedSession) repositoryService.getCurrentRepository().getSystemSession(workspace);
    Node node = systemSession.getNodeByIdentifier(fileId);
    if (node.isNodeType(EXO_SYMLINK)) {
      node = linkManager.getTarget(node, true);
      if (node != null) {
        return node.getUUID();
      }
    }
    return fileId;
  }

  /**
   * Inits the links.
   *
   * @param provider the provider
   * @param uriInfo the uri info
   */
  protected void initLinks(DocumentEditorData provider, UriInfo uriInfo) {
    String path = uriInfo.getAbsolutePath().toString();
    if (!uriInfo.getPathParameters().containsKey("provider")) {
      StringBuilder pathBuilder = new StringBuilder(path);
      if (!path.endsWith("/")) {
        pathBuilder.append("/");
      }
      path = pathBuilder.append(provider.getProvider()).toString();
    }
    provider.addLink(SELF, new HypermediaLink(path.toString()));
  }

  /**
   * Convert to DTO.
   *
   * @param provider the provider
   * @return the document editor provider data
   */
  protected DocumentEditorData convertToDTO(DocumentEditorProvider provider) {
    List<EditorPermission> permissions = provider.getPermissions().stream().map(permission -> {
      String[] temp = permission.split(":");
      if (temp.length < 2) {
        // user permission
        String userId = temp[0];
        Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
        if (identity != null) {
          Profile profile = identity.getProfile();
          String avatarUrl = profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
          return new EditorPermission(userId, profile.getFullName(), avatarUrl);
        }
        return new EditorPermission(userId);
      } else {
        // space
        String groupId = temp[1];
        Space space = spaceService.getSpaceByGroupId(groupId);
        if (space != null) {
          String displayName = space.getDisplayName();
          String avatarUrl = space != null && space.getAvatarUrl() != null ? space.getAvatarUrl()
                                                                           : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
          return new EditorPermission(groupId, displayName, avatarUrl);
        } else {
          // group
          Group group = null;
          try {
            group = organization.getGroupHandler().findGroupById(groupId);
          } catch (Exception e) {
            LOG.error("Cannot get group by id {}. {}", groupId, e.getMessage());
          }
          if (group != null) {
            String displayName = group.getLabel();
            String avatarUrl = LinkProvider.SPACE_DEFAULT_AVATAR_URL;
            return new EditorPermission(groupId, displayName, avatarUrl);
          }
        }
        return new EditorPermission(groupId);
      }
    }).collect(Collectors.toList());

    return new DocumentEditorData(provider.getProviderName(), provider.isActive(), permissions);
  }

}
