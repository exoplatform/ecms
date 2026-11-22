/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.wcm.connector.collaboration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.impl.EditorProvidersHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.connector.collaboration.editors.ErrorMessage;
import org.exoplatform.wcm.connector.collaboration.editors.PreviewInfo;

import jakarta.servlet.http.HttpServletRequest;

public class DocumentEditorsRESTServiceTest {

  private static final String        WORKSPACE        = "collaboration";

  private static final String        TARGET_WORKSPACE = "knowledge";

  private static final String        FILE_ID          = "shortcut-id";

  private static final String        TARGET_ID        = "target-id";

  private ManageableRepository       repository;

  private ExtendedSession            session;

  private ExtendedSession            targetSession;

  private DocumentService            documentService;

  private EditorProvidersHelper      editorProvidersHelper;

  private Object                     previousHelper;

  private UriInfo                    uriInfo;

  private HttpServletRequest         request;

  private DocumentEditorsRESTService documentEditorsRESTService;

  @Before
  public void setUp() throws Exception {
    RepositoryService repositoryService = Mockito.mock(RepositoryService.class);
    repository = Mockito.mock(ManageableRepository.class);
    Mockito.when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = Mockito.mock(RepositoryEntry.class);
    Mockito.when(repositoryEntry.getName()).thenReturn("repository");
    Mockito.when(repository.getConfiguration()).thenReturn(repositoryEntry);
    // A session provider of the caller logs in as the thread's user
    session = Mockito.mock(ExtendedSession.class);
    targetSession = Mockito.mock(ExtendedSession.class);
    Mockito.when(repository.login(WORKSPACE)).thenReturn(session);
    Mockito.when(repository.login(TARGET_WORKSPACE)).thenReturn(targetSession);

    documentService = Mockito.mock(DocumentService.class);
    Mockito.when(documentService.getDocumentEditorProviders()).thenReturn(Collections.emptyList());
    editorProvidersHelper = Mockito.mock(EditorProvidersHelper.class);
    Mockito.when(editorProvidersHelper.initPreview(Mockito.anyList(),
                                                   Mockito.any(),
                                                   Mockito.anyString(),
                                                   Mockito.anyString(),
                                                   Mockito.any(),
                                                   Mockito.any()))
           .thenReturn(Collections.emptyList());
    previousHelper = setHelper(editorProvidersHelper);

    uriInfo = Mockito.mock(UriInfo.class);
    Mockito.when(uriInfo.getRequestUri()).thenReturn(new URI("https://localhost/rest/documents/editors/preview"));
    request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getLocale()).thenReturn(Locale.ENGLISH);

    documentEditorsRESTService = new DocumentEditorsRESTService(documentService,
                                                                Mockito.mock(SpaceService.class),
                                                                Mockito.mock(OrganizationService.class),
                                                                Mockito.mock(IdentityManager.class),
                                                                repositoryService);
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  @After
  public void tearDown() throws Exception {
    ConversationState.setCurrent(null);
    setHelper(previousHelper);
  }

  @Test
  public void shouldRefuseAPreviewWithoutIdentity() throws Exception {
    ConversationState.setCurrent(null);

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(401, response.getStatus());
    assertNoStore(response);
    Mockito.verify(repository, Mockito.never()).login(Mockito.anyString());
  }

  @Test
  public void shouldRefuseAPreviewWithoutFileOrWorkspace() throws Exception {
    // Without the guard the request would read a file and initialize the providers: the pin fails on that, not on an error
    Mockito.when(repository.login(Mockito.nullable(String.class))).thenReturn(session);
    Mockito.when(session.getNodeByIdentifier(Mockito.nullable(String.class))).thenReturn(Mockito.mock(Node.class));
    assertEquals(400, documentEditorsRESTService.initPreview(uriInfo, request, null, WORKSPACE).getStatus());
    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, " ");

    assertEquals(400, response.getStatus());
    assertNoStore(response);
    Mockito.verify(repository, Mockito.never()).login(Mockito.anyString());
    assertNoProviderInitialized();
  }

  @Test
  public void shouldNotPreviewAFileTheUserCannotRead() throws Exception {
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenThrow(new AccessDeniedException());

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldAskAnAnonymousVisitorToSignInForAFileTheVisitorCannotRead() throws Exception {
    // An unauthenticated request carries the anonymous identity, not a null state
    ConversationState.setCurrent(new ConversationState(new Identity(IdentityConstants.ANONIM)));
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenThrow(new AccessDeniedException());

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(401, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(repository, Mockito.never()).getSystemSession(Mockito.anyString());
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldNotPreviewAMissingFile() throws Exception {
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenThrow(new ItemNotFoundException());

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldNotPreviewAFileOfAnUnknownWorkspace() throws Exception {
    Mockito.when(repository.login("unknown")).thenThrow(new javax.jcr.NoSuchWorkspaceException("unknown"));

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, "unknown");

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
  }

  @Test
  public void shouldNotRevealTheTargetOfAShortcutTheUserCannotRead() throws Exception {
    mockShortcut();
    Mockito.when(targetSession.getNodeByUUID(TARGET_ID)).thenThrow(new AccessDeniedException());

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(repository, Mockito.never()).getSystemSession(Mockito.anyString());
    Mockito.verify(session).logout();
    Mockito.verify(targetSession).logout();
  }

  @Test
  public void shouldGiveTheTargetOfAShortcutTheUserCanRead() throws Exception {
    mockShortcut();
    Node target = Mockito.mock(Node.class);
    Mockito.when(target.getUUID()).thenReturn(TARGET_ID);
    Mockito.when(targetSession.getNodeByUUID(TARGET_ID)).thenReturn(target);

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(200, response.getStatus());
    assertNoStore(response);
    assertEquals(TARGET_ID, ((PreviewInfo) response.getEntity()).getFileId());
    Mockito.verify(repository, Mockito.never()).getSystemSession(Mockito.anyString());
    Mockito.verify(session).logout();
    Mockito.verify(targetSession).logout();
  }

  @Test
  public void shouldGiveTheIdOfAFileThatIsNotAShortcut() throws Exception {
    Node node = Mockito.mock(Node.class);
    Mockito.when(node.isNodeType("exo:symlink")).thenReturn(false);
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenReturn(node);

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(200, response.getStatus());
    assertNoStore(response);
    assertEquals(FILE_ID, ((PreviewInfo) response.getEntity()).getFileId());
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldAnswerAnErrorWhenTheRepositoryFails() throws Exception {
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenThrow(new RepositoryException("repository down"));

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(500, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldAnswerAnErrorForAShortcutWithoutTarget() throws Exception {
    mockShortcut();
    Node shortcut = session.getNodeByIdentifier(FILE_ID);
    Mockito.when(shortcut.getProperty("exo:workspace")).thenThrow(new PathNotFoundException("exo:workspace"));

    Response response = documentEditorsRESTService.initPreview(uriInfo, request, FILE_ID, WORKSPACE);

    assertEquals(500, response.getStatus());
    assertNoStore(response);
    assertNoProviderInitialized();
    Mockito.verify(session).logout();
  }

  @Test
  public void testPreferredEditorSavedForSignedInUserOnly() throws Exception {
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "collaboration");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    verify(documentService).savePreferredEditor("mary", "onlyoffice", "file1", "collaboration");
    verify(documentService, never()).savePreferredEditor(eq("john"), any(), any(), any());
  }

  @Test
  public void testPreferredEditorSavedForSignedInUserWithoutUserParameter() throws Exception {
    Response response = documentEditorsRESTService.preferredEditor("file1", null, "onlyoffice", "collaboration");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    verify(documentService).savePreferredEditor("mary", "onlyoffice", "file1", "collaboration");
  }

  @Test
  public void testPreferredEditorAccessDeniedAnswersFixedText() throws Exception {
    doThrow(new AccessDeniedException("internal repository detail")).when(documentService)
                                                                    .savePreferredEditor(anyString(), any(), any(), any());
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "collaboration");
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    ErrorMessage message = (ErrorMessage) response.getEntity();
    assertEquals("Access denied error.", message.getErrorMessage());
    assertFalse(message.getErrorMessage().contains("internal repository detail"));
  }

  @Test
  public void testPreferredEditorFailureAnswersFixedText() throws Exception {
    doThrow(new RepositoryException("internal repository detail")).when(documentService)
                                                                  .savePreferredEditor(anyString(), any(), any(), any());
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "collaboration");
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    ErrorMessage message = (ErrorMessage) response.getEntity();
    assertEquals("Cannot save the preferred editor.", message.getErrorMessage());
    assertFalse(message.getErrorMessage().contains("internal repository detail"));
  }

  @Test
  public void testPreferredEditorOfMissingFileAnswersNotFound() throws Exception {
    doThrow(new ItemNotFoundException("internal repository detail")).when(documentService)
                                                                    .savePreferredEditor(anyString(), any(), any(), any());
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "collaboration");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    ErrorMessage message = (ErrorMessage) response.getEntity();
    assertEquals("Document not found.", message.getErrorMessage());
  }

  @Test
  public void testPreferredEditorOfUnknownWorkspaceAnswersNotFound() throws Exception {
    doThrow(new NoSuchWorkspaceException("internal repository detail")).when(documentService)
                                                                       .savePreferredEditor(anyString(), any(), any(), any());
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "unknown");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    ErrorMessage message = (ErrorMessage) response.getEntity();
    assertEquals("Document not found.", message.getErrorMessage());
  }

  /**
   * Covers a bypass of the role check only: the method is restricted to the
   * {@code users} role, so an anonymous HTTP request is refused with 403 by the
   * REST role filter before it reaches the method.
   */
  @Test
  public void testPreferredEditorWithoutSignedInUser() throws Exception {
    ConversationState.setCurrent(null);
    Response response = documentEditorsRESTService.preferredEditor("file1", "john", "onlyoffice", "collaboration");
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertNull(response.getEntity());
    verifyNoInteractions(documentService);
  }

  private void mockShortcut() throws Exception {
    Node shortcut = Mockito.mock(Node.class);
    Mockito.when(shortcut.isNodeType("exo:symlink")).thenReturn(true);
    Property workspaceProperty = Mockito.mock(Property.class);
    Mockito.when(workspaceProperty.getString()).thenReturn(TARGET_WORKSPACE);
    Mockito.when(shortcut.getProperty("exo:workspace")).thenReturn(workspaceProperty);
    Property uuidProperty = Mockito.mock(Property.class);
    Mockito.when(uuidProperty.getString()).thenReturn(TARGET_ID);
    Mockito.when(shortcut.getProperty("exo:uuid")).thenReturn(uuidProperty);
    Mockito.when(session.getNodeByIdentifier(FILE_ID)).thenReturn(shortcut);
  }

  private void assertNoProviderInitialized() {
    Mockito.verify(editorProvidersHelper, Mockito.never())
           .initPreview(Mockito.anyList(),
                         Mockito.any(),
                         Mockito.nullable(String.class),
                         Mockito.nullable(String.class),
                         Mockito.any(),
                         Mockito.any());
  }

  private static Object setHelper(Object helper) throws Exception {
    Field field = EditorProvidersHelper.class.getDeclaredField("helper");
    field.setAccessible(true);
    Object previous = field.get(null);
    field.set(null, helper);
    return previous;
  }

  private static void assertNoStore(Response response) {
    CacheControl cacheControl = (CacheControl) response.getMetadata().getFirst(HttpHeaders.CACHE_CONTROL);
    assertNotNull("the answer carries a cache policy", cacheControl);
    assertTrue("the answer is private", cacheControl.isPrivate());
    assertTrue("the answer is no-store", cacheControl.isNoStore());
  }
}
