/*
* Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.document.service.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.WebAppController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentViewerRESTServiceTest {

  private static final String      WORKSPACE = "collaboration";

  @Mock
  private RepositoryService        repositoryService;

  @Mock
  private ManageableRepository     repository;

  @Mock
  private ExtendedSession          session;

  private ContentViewerRESTService contentViewerRESTService;

  @Before
  public void setUp() throws Exception {
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    when(repositoryEntry.getName()).thenReturn("repository");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repository.login(WORKSPACE)).thenReturn(session);
    contentViewerRESTService = new ContentViewerRESTService(mock(WebAppController.class),
                                                            repositoryService,
                                                            mock(LinkManager.class));
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  public void testTheContentIsNotRenderedForAUserWhoCannotReadTheDocument() throws Exception {
    when(session.getNodeByIdentifier("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    Response response = getContent();

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(repository, never()).getSystemSession(any());
    verify(session).logout();
  }

  @Test
  public void testTheContentOfAMissingDocumentIsNotFound() throws Exception {
    when(session.getNodeByIdentifier("uuid")).thenThrow(new ItemNotFoundException());
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getContent().getStatus());
  }

  @Test
  public void testAnAnonymousCallerIsAskedToSignIn() throws Exception {
    when(session.getNodeByIdentifier("uuid")).thenThrow(new AccessDeniedException());
    ConversationState.setCurrent(new ConversationState(new Identity(IdentityConstants.ANONIM)));

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getContent().getStatus());
    verify(repository, never()).getSystemSession(any());
  }

  @Test
  public void testACallWithoutIdentityIsRefused() throws Exception {
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getContent().getStatus());
    verify(repository, never()).getSystemSession(any());
  }

  private Response getContent() throws Exception {
    return contentViewerRESTService.getContent(mock(HttpServletRequest.class),
                                               mock(HttpServletResponse.class),
                                               "repository",
                                               WORKSPACE,
                                               "uuid");
  }
}
