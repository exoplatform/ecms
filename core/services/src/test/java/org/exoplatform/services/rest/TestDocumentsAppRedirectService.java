package org.exoplatform.services.rest;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

/**
 * Tests for DocumentsAppRedirectService
 */
public class TestDocumentsAppRedirectService {

  @Before
  public void setUp() throws Exception {


  }

  @Test
  public void shouldReturnAnServerErrorWhenParametersAreMissing() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    RepositoryService repositoryService = Mockito.mock(RepositoryService.class);
    DocumentService documentService = Mockito.mock(DocumentService.class);
    SessionProviderService sessionProviderService = Mockito.mock(SessionProviderService.class);

    DocumentsAppRedirectService documentsAppRedirectService = new DocumentsAppRedirectService(sessionProviderService, repositoryService, documentService);
    Response response = documentsAppRedirectService.redirect(request, null, null);

    assertNotNull(response);
    assertEquals(500, response.getStatus());
  }

  @Test
  public void shouldReturnAnServerErrorWhenDocumentDoesNotExist() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    SessionProviderService sessionProviderService = Mockito.mock(SessionProviderService.class);
    RepositoryService repositoryService = Mockito.mock(RepositoryService.class);
    DocumentService documentService = Mockito.mock(DocumentService.class);
    SessionProvider sessionProvider = Mockito.mock(SessionProvider.class);
    ExtendedSession session = Mockito.mock(ExtendedSession.class);
    Mockito.when(sessionProviderService.getSystemSessionProvider(Mockito.any())).thenReturn(sessionProvider);
    Mockito.when(sessionProvider.getSession(Mockito.anyString(), Mockito.any())).thenReturn(session);

    Mockito.when(session.getNodeByIdentifier(Mockito.anyString())).thenThrow(new ItemNotFoundException());

    DocumentsAppRedirectService documentsAppRedirectService = new DocumentsAppRedirectService(sessionProviderService, repositoryService, documentService);
    Response response = documentsAppRedirectService.redirect(request, "collaboration", "123");

    assertNotNull(response);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void shouldReturnRedirectUrlWhenDocumentExists() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    SessionProviderService sessionProviderService = Mockito.mock(SessionProviderService.class);
    RepositoryService repositoryService = Mockito.mock(RepositoryService.class);
    DocumentService documentService = Mockito.mock(DocumentService.class);
    SessionProvider sessionProvider = Mockito.mock(SessionProvider.class);
    ExtendedSession session = Mockito.mock(ExtendedSession.class);
    Node node = Mockito.mock(Node.class);
    Mockito.when(sessionProviderService.getSystemSessionProvider(Mockito.any())).thenReturn(sessionProvider);
    Mockito.when(sessionProvider.getSession(Mockito.anyString(), Mockito.any())).thenReturn(session);

    Mockito.when(session.getNodeByIdentifier(Mockito.anyString())).thenReturn(node);

    DocumentsAppRedirectService documentsAppRedirectService = new DocumentsAppRedirectService(sessionProviderService, repositoryService, documentService);
    Response response = documentsAppRedirectService.redirect(request, "collaboration", "123");

    assertNotNull(response);
    assertEquals(307, response.getStatus());
  }
}