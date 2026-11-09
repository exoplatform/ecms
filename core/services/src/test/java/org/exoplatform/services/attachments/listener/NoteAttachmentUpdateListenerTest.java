/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.exoplatform.services.attachments.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.Event;

public class NoteAttachmentUpdateListenerTest {

  private static final String          WORKSPACE = "collaboration";

  private SessionProvider                   threadSessionProvider;

  private SessionProviderService       sessionProviderService;

  private AttachmentStorage            attachmentStorage;

  private ManageableRepository         repository;

  private MockedStatic<ExoContainerContext> exoContainerContext;

  private NoteAttachmentUpdateListener listener;

  @Before
  public void setUp() {
    sessionProviderService = new ThreadLocalSessionProviderService();
    threadSessionProvider = sessionProviderService.getSessionProvider(null);
    sessionProviderService.setSessionProvider(null, null);
    attachmentStorage = mock(AttachmentStorage.class);
    repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setName("repository");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    exoContainerContext = mockStatic(ExoContainerContext.class);
    exoContainerContext.when(() -> ExoContainerContext.getService(SessionProviderService.class))
                       .thenReturn(sessionProviderService);
    exoContainerContext.when(() -> ExoContainerContext.getService(AttachmentStorage.class)).thenReturn(attachmentStorage);
    listener = new NoteAttachmentUpdateListener();
  }

  @After
  public void tearDown() {
    sessionProviderService.setSessionProvider(null, threadSessionProvider);
    exoContainerContext.close();
  }

  @Test
  public void testMoveAttachmentsWithSystemProviderAndRestoreCallerOne() throws Exception {
    SessionProvider callerProvider = SessionProvider.createAnonimProvider();
    sessionProviderService.setSessionProvider(null, callerProvider);
    List<SessionProvider> providersDuringMove = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringMove.add(sessionProviderService.getSessionProvider(null));
      return List.of(attachment("att1"));
    });

    listener.onEvent(new Event<>("note.page.version.created", null, Map.of("draftPageId", "1", "pageVersionId", "2")));

    verify(attachmentStorage).linkAttachmentToEntity(2L, "WIKI_PAGE_VERSIONS", "att1");
    verify(attachmentStorage).deleteAttachmentItemByIdByEntity(1L, "WIKI_DRAFT_PAGES", "att1");
    assertEquals(1, providersDuringMove.size());
    SessionProvider moveProvider = providersDuringMove.get(0);
    assertNotSame(callerProvider, moveProvider);
    assertSame(callerProvider, sessionProviderService.getSessionProvider(null));
    assertTrue("the provider used for the move is closed", isClosed(moveProvider));
    assertFalse("the caller's provider stays open", isClosed(callerProvider));
  }

  @Test
  public void testMoveAttachmentsUsesSystemRights() throws Exception {
    SessionProvider callerProvider = mock(SessionProvider.class);
    when(callerProvider.getSession(WORKSPACE, repository)).thenReturn(mock(ExtendedSession.class));
    sessionProviderService.setSessionProvider(null, callerProvider);
    ExtendedSession systemSession = mock(ExtendedSession.class);
    when(repository.getSystemSession(WORKSPACE)).thenReturn(systemSession);
    List<Object> sessionsDuringMove = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      sessionsDuringMove.add(sessionProviderService.getSessionProvider(null).getSession(WORKSPACE, repository));
      return List.of();
    });

    listener.onEvent(new Event<>("note.page.version.created", null, Map.of("draftPageId", "1", "pageVersionId", "2")));

    assertEquals(List.of(systemSession), sessionsDuringMove);
  }

  @Test
  public void testRestoreCallerProviderWhenMoveFails() throws Exception {
    SessionProvider callerProvider = SessionProvider.createAnonimProvider();
    sessionProviderService.setSessionProvider(null, callerProvider);
    List<SessionProvider> providersDuringMove = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringMove.add(sessionProviderService.getSessionProvider(null));
      throw new IllegalStateException("storage failure");
    });

    listener.onEvent(new Event<>("note.page.version.created", null, Map.of("draftPageId", "1", "pageVersionId", "2")));

    assertSame(callerProvider, sessionProviderService.getSessionProvider(null));
    assertEquals(1, providersDuringMove.size());
    assertTrue("the provider used for the move is closed", isClosed(providersDuringMove.get(0)));
  }

  @Test
  public void testRestoreNoProviderWhenCallerHadNone() throws Exception {
    List<SessionProvider> providersDuringMove = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringMove.add(sessionProviderService.getSessionProvider(null));
      return List.of();
    });

    listener.onEvent(new Event<>("note.page.version.created", null, Map.of("draftPageId", "1", "pageVersionId", "2")));

    assertEquals(1, providersDuringMove.size());
    assertNotNull(providersDuringMove.get(0));
    assertNull(sessionProviderService.getSessionProvider(null));
  }

  /**
   * {@link SessionProvider#getSession} checks that its provider is open before
   * anything else: only a closed provider throws this exception and message.
   */
  private boolean isClosed(SessionProvider sessionProvider) {
    try {
      sessionProvider.getSession(WORKSPACE, repository);
      return false;
    } catch (IllegalStateException e) {
      return "Session provider already closed".equals(e.getMessage());
    } catch (Exception e) {
      return false;
    }
  }

  private Attachment attachment(String id) {
    Attachment attachment = new Attachment();
    attachment.setId(id);
    return attachment;
  }
}
