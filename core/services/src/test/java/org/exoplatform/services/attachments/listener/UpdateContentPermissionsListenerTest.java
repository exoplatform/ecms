/*
 * Copyright (C) 2024 eXo Platform SAS.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;

public class UpdateContentPermissionsListenerTest {

  private static final String               WORKSPACE = "collaboration";

  private SessionProvider                   threadSessionProvider;

  private SessionProviderService            sessionProviderService;

  private RepositoryService                 repositoryService;

  private AttachmentStorage                 attachmentStorage;

  private ManageableRepository              repository;

  private Session                           systemSession;

  private UpdateContentPermissionsListener  listener;

  private MockedStatic<Utils>               utils;

  private MockedStatic<CommonsUtils>        commonsUtils;

  private MockedStatic<PortalContainer>     portalContainer;

  private MockedStatic<ExoContainerContext> exoContainerContext;

  @Before
  public void setUp() {
    sessionProviderService = new ThreadLocalSessionProviderService();
    threadSessionProvider = sessionProviderService.getSessionProvider(null);
    sessionProviderService.setSessionProvider(null, null);
    repositoryService = mock(RepositoryService.class);
    attachmentStorage = mock(AttachmentStorage.class);
    repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setName("repository");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    systemSession = mock(Session.class);
    utils = mockStatic(Utils.class);
    commonsUtils = mockStatic(CommonsUtils.class);
    portalContainer = mockStatic(PortalContainer.class);
    exoContainerContext = mockStatic(ExoContainerContext.class);
    exoContainerContext.when(() -> ExoContainerContext.getService(SessionProviderService.class))
                       .thenReturn(sessionProviderService);
    commonsUtils.when(CommonsUtils::getRestContextName).thenReturn("rest");
    commonsUtils.when(CommonsUtils::getCurrentDomain).thenReturn("https://exoplatform.com");
    portalContainer.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    utils.when(() -> Utils.getSystemSession(sessionProviderService, repositoryService)).thenReturn(systemSession);
    listener = new UpdateContentPermissionsListener(repositoryService, attachmentStorage);
  }

  @After
  public void tearDown() {
    sessionProviderService.setSessionProvider(null, threadSessionProvider);
    utils.close();
    commonsUtils.close();
    portalContainer.close();
    exoContainerContext.close();
  }

  @Test
  public void testUpdateAttachmentsAndContentImagesPermissions() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/test");
    ExtendedNode attachmentNode = mock(ExtendedNode.class);
    when(attachmentNode.getPath()).thenReturn("/Groups/spaces/test/Documents/attachment");
    when(systemSession.getNodeByUUID("123")).thenReturn(attachmentNode);
    ExtendedNode uuidImageNode = mock(ExtendedNode.class);
    when(uuidImageNode.getPath()).thenReturn("/Groups/spaces/test/Documents/uuidImage");
    when(systemSession.getNodeByUUID("123456")).thenReturn(uuidImageNode);
    ExtendedNode pathImageNode = mock(ExtendedNode.class);
    when(pathImageNode.getPath()).thenReturn("/Groups/spaces/test/testimage");
    when(pathImageNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(true);
    when(systemSession.getItem("/Groups/spaces/test/testimage")).thenReturn(pathImageNode);
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_PAGE_VERSIONS")).thenReturn(List.of(attachment("123")));

    listener.onEvent(new Event<>("content.update.permissions",
                                 null,
                                 Map.of("entityId",
                                        "1",
                                        "entityType",
                                        "WIKI_PAGE_VERSIONS",
                                        "audience",
                                        "all",
                                        "spaces",
                                        List.of(space),
                                        "content",
                                        "test <p> <img src=\"/portal/rest/images/repository/collaboration/123456\">"
                                            + "<img src=\"https://exoplatform.com/portal/rest/jcr/repository/collaboration/Groups/spaces/test/testimage\">")));

    for (ExtendedNode node : List.of(attachmentNode, uuidImageNode, pathImageNode)) {
      verify(node).setPermission("*:/spaces/test", new String[] { PermissionType.READ });
      verify(node).setPermission("any", new String[] { PermissionType.READ });
      verify(node, times(2)).save();
    }
    verify(pathImageNode).addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
  }

  @Test
  public void testUpdatePermissionsWithSystemProviderAndRestoreCallerOne() throws Exception {
    SessionProvider callerProvider = SessionProvider.createAnonimProvider();
    sessionProviderService.setSessionProvider(null, callerProvider);
    List<SessionProvider> providersDuringUpdate = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringUpdate.add(sessionProviderService.getSessionProvider(null));
      return List.of();
    });

    listener.onEvent(new Event<>("content.update.permissions", null, eventData()));

    assertEquals(1, providersDuringUpdate.size());
    SessionProvider updateProvider = providersDuringUpdate.get(0);
    assertNotSame(callerProvider, updateProvider);
    assertSame(callerProvider, sessionProviderService.getSessionProvider(null));
    assertTrue("the provider used for the update is closed", isClosed(updateProvider));
    assertFalse("the caller's provider stays open", isClosed(callerProvider));
  }

  @Test
  public void testUpdatePermissionsUsesSystemRights() throws Exception {
    SessionProvider callerProvider = mock(SessionProvider.class);
    when(callerProvider.getSession(WORKSPACE, repository)).thenReturn(mock(ExtendedSession.class));
    sessionProviderService.setSessionProvider(null, callerProvider);
    ExtendedSession repositorySystemSession = mock(ExtendedSession.class);
    when(repository.getSystemSession(WORKSPACE)).thenReturn(repositorySystemSession);
    List<Object> sessionsDuringUpdate = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      sessionsDuringUpdate.add(sessionProviderService.getSessionProvider(null).getSession(WORKSPACE, repository));
      return List.of();
    });

    listener.onEvent(new Event<>("content.update.permissions", null, eventData()));

    assertEquals(List.of(repositorySystemSession), sessionsDuringUpdate);
  }

  @Test
  public void testRestoreCallerProviderWhenUpdateFails() throws Exception {
    SessionProvider callerProvider = SessionProvider.createAnonimProvider();
    sessionProviderService.setSessionProvider(null, callerProvider);
    List<SessionProvider> providersDuringUpdate = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringUpdate.add(sessionProviderService.getSessionProvider(null));
      return List.of(attachment("123"));
    });
    when(systemSession.getNodeByUUID("123")).thenThrow(new RepositoryException("node failure"));

    assertThrows(RepositoryException.class,
                 () -> listener.onEvent(new Event<>("content.update.permissions", null, eventData())));

    assertSame(callerProvider, sessionProviderService.getSessionProvider(null));
    assertEquals(1, providersDuringUpdate.size());
    assertTrue("the provider used for the update is closed", isClosed(providersDuringUpdate.get(0)));
  }

  @Test
  public void testRestoreNoProviderWhenCallerHadNone() throws Exception {
    List<SessionProvider> providersDuringUpdate = new ArrayList<>();
    when(attachmentStorage.getAttachmentsByEntity(1L, "WIKI_DRAFT_PAGES")).thenAnswer(invocation -> {
      providersDuringUpdate.add(sessionProviderService.getSessionProvider(null));
      return List.of();
    });

    listener.onEvent(new Event<>("content.update.permissions", null, eventData()));

    assertEquals(1, providersDuringUpdate.size());
    assertNotNull(providersDuringUpdate.get(0));
    assertNull(sessionProviderService.getSessionProvider(null));
  }

  private Map<String, Object> eventData() {
    return Map.of("entityId", "1", "entityType", "WIKI_DRAFT_PAGES", "audience", "space", "spaces", List.of());
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
