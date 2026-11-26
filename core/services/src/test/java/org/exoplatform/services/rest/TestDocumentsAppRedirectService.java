package org.exoplatform.services.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import jakarta.servlet.http.HttpServletRequest;

public class TestDocumentsAppRedirectService {

  private static final String         WORKSPACE = "collaboration";

  private HttpServletRequest          request;

  private RepositoryService           repositoryService;

  private ManageableRepository        repository;

  private DocumentService             documentService;

  private ExtendedSession             session;

  private DocumentsAppRedirectService documentsAppRedirectService;

  @Before
  public void setUp() throws Exception {
    request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getScheme()).thenReturn("https");
    Mockito.when(request.getServerName()).thenReturn("localhost");
    Mockito.when(request.getServerPort()).thenReturn(443);
    repositoryService = Mockito.mock(RepositoryService.class);
    repository = Mockito.mock(ManageableRepository.class);
    Mockito.when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = Mockito.mock(RepositoryEntry.class);
    Mockito.when(repositoryEntry.getName()).thenReturn("repository");
    Mockito.when(repository.getConfiguration()).thenReturn(repositoryEntry);
    // A session provider of the caller logs in as the thread's user
    session = Mockito.mock(ExtendedSession.class);
    Mockito.when(repository.login(WORKSPACE)).thenReturn(session);
    documentService = Mockito.mock(DocumentService.class);
    documentsAppRedirectService = new DocumentsAppRedirectService(repositoryService, documentService);
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  public void shouldReturnAnServerErrorWhenParametersAreMissing() throws Exception {
    Response response = documentsAppRedirectService.redirect(request, null, null);

    assertNotNull(response);
    assertEquals(500, response.getStatus());
    assertNoStore(response);
  }

  @Test
  public void shouldAnswerAServerErrorWhenTheRepositoryFails() throws Exception {
    Mockito.when(session.getNodeByIdentifier("123")).thenThrow(new javax.jcr.RepositoryException("repository down"));

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(500, response.getStatus());
    assertNoStore(response);
    Mockito.verify(documentService, Mockito.never()).getLinkInDocumentsApp(Mockito.anyString());
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldReturnNotFoundWhenNoNodeIsReturned() throws Exception {
    Mockito.when(session.getNodeByIdentifier("123")).thenReturn(null);

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    Mockito.verify(documentService, Mockito.never()).getLinkInDocumentsApp(Mockito.anyString());
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldRefuseACallWithoutIdentity() throws Exception {
    ConversationState.setCurrent(null);

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(401, response.getStatus());
    assertNoStore(response);
    Mockito.verify(repository, Mockito.never()).login(Mockito.anyString());
  }

  @Test
  public void shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {
    Mockito.when(session.getNodeByIdentifier("123")).thenThrow(new ItemNotFoundException());

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldReturnNotFoundForAnUnknownWorkspace() throws Exception {
    Mockito.when(repository.login("unknown")).thenThrow(new javax.jcr.NoSuchWorkspaceException("unknown"));

    Response response = documentsAppRedirectService.redirect(request, "unknown", "123");

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    Mockito.verify(documentService, Mockito.never()).getLinkInDocumentsApp(Mockito.anyString());
  }

  @Test
  public void shouldNeitherLocateNorRedirectToADocumentTheUserCannotRead() throws Exception {
    Mockito.when(session.getNodeByIdentifier("123")).thenThrow(new AccessDeniedException());

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(404, response.getStatus());
    assertNoStore(response);
    Mockito.verify(documentService, Mockito.never()).getLinkInDocumentsApp(Mockito.anyString());
    Mockito.verify(repository, Mockito.never()).getSystemSession(Mockito.anyString());
    Mockito.verify(session).logout();
  }

  @Test
  public void shouldRedirectToADocumentTheUserCanRead() throws Exception {
    Node node = Mockito.mock(Node.class);
    Mockito.when(node.getPath()).thenReturn("/Groups/spaces/team/Documents/report.pdf");
    Mockito.when(session.getNodeByIdentifier("123")).thenReturn(node);
    Mockito.when(documentService.getLinkInDocumentsApp("/Groups/spaces/team/Documents/report.pdf"))
           .thenReturn("/portal/g/:spaces:team/team/documents?documentPreviewId=123");

    Response response = documentsAppRedirectService.redirect(request, WORKSPACE, "123");

    assertEquals(307, response.getStatus());
    assertEquals("https://localhost:443/portal/g/:spaces:team/team/documents?documentPreviewId=123",
                 response.getMetadata().getFirst("Location").toString());
    assertNoStore(response);
    Mockito.verify(repository, Mockito.never()).getSystemSession(Mockito.anyString());
    Mockito.verify(session).logout();
  }

  private static void assertNoStore(Response response) {
    CacheControl cacheControl = (CacheControl) response.getMetadata().getFirst(HttpHeaders.CACHE_CONTROL);
    assertNotNull("the answer carries a cache policy", cacheControl);
    assertTrue("the answer is private", cacheControl.isPrivate());
    assertTrue("the answer is no-store", cacheControl.isNoStore());
  }
}
