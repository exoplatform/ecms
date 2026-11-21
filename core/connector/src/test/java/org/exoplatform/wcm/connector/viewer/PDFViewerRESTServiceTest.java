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
package org.exoplatform.wcm.connector.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PDFViewerRESTServiceTest {

  private static final String         WORKSPACE = "collaboration";

  @Mock
  private RepositoryService           repositoryService;

  @Mock
  private ManageableRepository        repository;

  @Mock
  private ExtendedSession             session;

  @Mock
  private CacheService                cacheService;

  @Mock
  private ExoCache<Serializable, Object> pdfCache;

  private MockedStatic<WCMCoreUtils>  wcmCoreUtils;

  private PDFViewerRESTService        pdfViewerRESTService;

  @Before
  public void setUp() throws Exception {
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(PDFViewerService.class)).thenReturn(null);
    when(cacheService.getCacheInstance(anyString())).thenReturn(pdfCache);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn(WORKSPACE);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repository.login(WORKSPACE)).thenReturn(session);
    pdfViewerRESTService = new PDFViewerRESTService(repositoryService, cacheService);
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
    wcmCoreUtils.close();
  }

  @Test
  public void testAPageIsNotRenderedForAUserWhoCannotReadTheDocument() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = pdfViewerRESTService.getCoverImage("repository", WORKSPACE, "uuid", "1", "0", "1.0");

    assertNoStore(response);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(repository, never()).getSystemSession(any());
    verify(session).logout();
  }

  @Test
  public void testAPageOfAMissingDocumentIsNotFound() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new ItemNotFoundException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = pdfViewerRESTService.getCoverImage("repository", WORKSPACE, "uuid", "1", "0", "1.0");

    assertNoStore(response);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testAnAnonymousCallerIsAskedToSignIn() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity(IdentityConstants.ANONIM)));

    Response response = pdfViewerRESTService.getCoverImage("repository", WORKSPACE, "uuid", "1", "0", "1.0");

    assertNoStore(response);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    verify(repository, never()).getSystemSession(any());
  }

  @Test
  public void testACallWithoutIdentityIsRefused() throws Exception {
    Response response = pdfViewerRESTService.getCoverImage("repository", WORKSPACE, "uuid", "1", "0", "1.0");

    assertNoStore(response);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    verify(repository, never()).getSystemSession(any());
    verify(repository, never()).login(any(String.class));
  }

  @Test
  public void testAReadablePageIsPrivateButNotNoStore() throws Exception {
    File page = File.createTempFile("pdfviewer-page", ".png");
    page.deleteOnExit();
    when(pdfCache.get(any())).thenReturn(page.getPath());
    when(session.getNodeByUUID("uuid")).thenReturn(mock(Node.class));
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = pdfViewerRESTService.getCoverImage("repository", WORKSPACE, "uuid", "1", "0", "1.0");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CacheControl cacheControl = (CacheControl) response.getMetadata().getFirst(HttpHeaders.CACHE_CONTROL);
    assertTrue(cacheControl.isPrivate());
    assertFalse(cacheControl.isNoStore());
    verify(session).logout();
    ((InputStream) response.getEntity()).close();
  }

  @Test
  public void testThePdfFileIsNotFoundForAUserWhoCannotReadTheDocument() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = pdfViewerRESTService.getPDFFile("repository", WORKSPACE, "uuid");

    assertNoStore(response);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(session).logout();
  }

  @Test
  public void testThePdfFileOfAMissingDocumentIsNotFound() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new ItemNotFoundException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = pdfViewerRESTService.getPDFFile("repository", WORKSPACE, "uuid");

    assertNoStore(response);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals("Document not found", response.getEntity());
    verify(session).logout();
  }

  @Test
  public void testAReadablePdfFileIsPrivateButNotNoStore() throws Exception {
    File pdf = File.createTempFile("pdfviewer-file", ".pdf");
    pdf.deleteOnExit();
    Node node = mock(Node.class);
    when(session.getNodeByUUID("uuid")).thenReturn(node);
    PDFViewerRESTService service = spy(pdfViewerRESTService);
    doReturn(pdf).when(service).getPDFDocumentFile(node, "repository");
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
      utils.when(() -> Utils.getTitle(node)).thenReturn("document.pdf");
      Response response = service.getPDFFile("repository", WORKSPACE, "uuid");

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      CacheControl cacheControl = (CacheControl) response.getMetadata().getFirst(HttpHeaders.CACHE_CONTROL);
      assertTrue(cacheControl.isPrivate());
      assertFalse(cacheControl.isNoStore());
      verify(session).logout();
      ((InputStream) response.getEntity()).close();
    }
  }

  @Test
  public void testThePdfFileAnswersAFixedTextWhenItCannotBeRead() throws Exception {
    Node node = mock(Node.class);
    when(session.getNodeByUUID("uuid")).thenReturn(node);
    PDFViewerRESTService service = spy(pdfViewerRESTService);
    doThrow(new IllegalStateException("/internal/path")).when(service).getPDFDocumentFile(node, "repository");
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = service.getPDFFile("repository", WORKSPACE, "uuid");

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertEquals("Cannot read the document", response.getEntity());
    assertNoStore(response);
    verify(session).logout();
  }

  @Test
  public void testThePdfFileIsRefusedWithoutIdentity() throws Exception {
    Response response = pdfViewerRESTService.getPDFFile("repository", WORKSPACE, "uuid");

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertNoStore(response);
    verify(repository, never()).login(any(String.class));
  }

  @Test
  public void testThePdfFileAsksAnAnonymousCallerToSignIn() throws Exception {
    when(session.getNodeByUUID("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity(IdentityConstants.ANONIM)));

    Response response = pdfViewerRESTService.getPDFFile("repository", WORKSPACE, "uuid");

    assertNoStore(response);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  private static void assertNoStore(Response response) {
    CacheControl cacheControl = (CacheControl) response.getMetadata().getFirst(HttpHeaders.CACHE_CONTROL);
    assertTrue(cacheControl.isPrivate());
    assertTrue(cacheControl.isNoStore());
  }
}
