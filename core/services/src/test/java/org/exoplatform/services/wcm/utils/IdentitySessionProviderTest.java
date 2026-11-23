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
package org.exoplatform.services.wcm.utils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.mockito.MockedStatic;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Session;

import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionReference;
import org.exoplatform.services.jcr.impl.core.TrackedSession;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.BaseWCMTestCase;

public class IdentitySessionProviderTest extends BaseWCMTestCase {

  private static final String NAMED_USER   = "named-user";

  private static final String THREAD_USER  = "thread-user";

  private ConversationState   initialState;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    initialState = ConversationState.getCurrent();
  }

  @Override
  public void tearDown() throws Exception {
    ConversationState.setCurrent(initialState);
    super.tearDown();
  }

  public void testSessionOpensAsNamedUserWhenThreadIsAnotherUser() throws Exception {
    ConversationState.setCurrent(new ConversationState(new Identity(THREAD_USER)));
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      Session userSession = provider.getSession(COLLABORATION_WS, repository);
      assertEquals(NAMED_USER, userSession.getUserID());
    } finally {
      provider.close();
    }
  }

  public void testSessionOpensAsNamedUserWhenThreadHasNoState() throws Exception {
    ConversationState.setCurrent(null);
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      assertEquals(NAMED_USER, provider.getSession(COLLABORATION_WS, repository).getUserID());
    } finally {
      provider.close();
    }
  }

  public void testAttachmentsHelperOpensAsGivenIdentity() throws Exception {
    ConversationState.setCurrent(new ConversationState(new Identity(THREAD_USER)));
    SessionProvider provider = Utils.getUserSessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      assertEquals(NAMED_USER, provider.getSession(COLLABORATION_WS, repository).getUserID());
    } finally {
      provider.close();
    }
  }

  public void testAttachmentsHelperKeepsThreadStateForTheRequestsOwnUser() throws Exception {
    MembershipEntry threadMembership = new MembershipEntry("/platform/thread-only-group", "member");
    ConversationState.setCurrent(new ConversationState(new Identity(NAMED_USER, List.of(threadMembership))));
    SessionProvider provider = Utils.getUserSessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      SessionImpl userSession = (SessionImpl) provider.getSession(COLLABORATION_WS, repository);
      assertEquals(NAMED_USER, userSession.getUserID());
      assertTrue(userSession.getUserState().getIdentity().getMemberships().contains(threadMembership));
    } finally {
      provider.close();
    }
  }

  /**
   * No current caller reaches this branch: Utils#getSession and processes both
   * need a thread state.
   */
  public void testAttachmentsHelperOpensAsGivenIdentityWhenThreadHasNoState() throws Exception {
    ConversationState.setCurrent(null);
    SessionProvider provider = Utils.getUserSessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      assertEquals(NAMED_USER, provider.getSession(COLLABORATION_WS, repository).getUserID());
    } finally {
      provider.close();
    }
  }

  public void testRefusesSystemIdentity() {
    try {
      new IdentitySessionProvider(repositoryService, new Identity(IdentityConstants.SYSTEM));
      fail("The system identity must be refused");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testAttachmentsHelperKeepsSystemThreadOnTheStockProvider() {
    Identity system = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(system));
    SessionProvider provider = Utils.getUserSessionProvider(repositoryService, system);
    assertFalse(provider instanceof IdentitySessionProvider);
  }

  /**
   * The jcr module has no way to stop its tracking once started, so the test
   * switches it on for its own duration: the started flag is stubbed and the
   * registry the tracked sessions register in is set, then cleared.
   */
  public void testSessionIsTrackedWhenSessionTrackingIsActive() throws Exception {
    Field objects = SessionReference.class.getDeclaredField("objects");
    objects.setAccessible(true);
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try (MockedStatic<SessionReference> tracking = mockStatic(SessionReference.class)) {
      tracking.when(SessionReference::isStarted).thenReturn(true);
      objects.set(null, new ConcurrentHashMap<>());
      Session userSession = provider.getSession(COLLABORATION_WS, repository);
      assertTrue(userSession instanceof TrackedSession);
      assertEquals(NAMED_USER, userSession.getUserID());
    } finally {
      provider.close();
      objects.set(null, null);
    }
  }

  public void testSessionIsNotTrackedWhenSessionTrackingIsInactive() throws Exception {
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try (MockedStatic<SessionReference> tracking = mockStatic(SessionReference.class)) {
      tracking.when(SessionReference::isStarted).thenReturn(false);
      Session userSession = provider.getSession(COLLABORATION_WS, repository);
      assertFalse(userSession instanceof TrackedSession);
    } finally {
      provider.close();
    }
  }

  public void testRefusesUnknownWorkspace() {
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      provider.getSession("no-such-workspace", repository);
      fail("An unknown workspace must be refused");
    } catch (NoSuchWorkspaceException e) {
      // expected
    } catch (Exception e) {
      fail("Expected NoSuchWorkspaceException but got " + e);
    } finally {
      provider.close();
    }
  }

  public void testRefusesUninitializedWorkspace() throws Exception {
    ManageableRepository uninitializedRepository = spy(repository);
    doReturn(false).when(uninitializedRepository).isWorkspaceInitialized(COLLABORATION_WS);
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    try {
      provider.getSession(COLLABORATION_WS, uninitializedRepository);
      fail("An uninitialized workspace must be refused");
    } catch (NoSuchWorkspaceException e) {
      // expected
    } finally {
      provider.close();
    }
  }

  public void testCloseLogsOutSessionsAndRefusesFurtherOnes() throws Exception {
    SessionProvider provider = new IdentitySessionProvider(repositoryService, new Identity(NAMED_USER));
    Session userSession = provider.getSession(COLLABORATION_WS, repository);
    assertTrue(userSession.isLive());
    assertSame(userSession, provider.getSession(COLLABORATION_WS, repository));
    provider.close();
    assertFalse(userSession.isLive());
    try {
      provider.getSession(COLLABORATION_WS, repository);
      fail("A closed provider must not open a session");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testRefusesMissingIdentity() {
    try {
      new IdentitySessionProvider(repositoryService, null);
      fail("A missing identity must be refused");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testRefusesRepositoryServiceThatCannotOpenForAnIdentity() {
    try {
      new IdentitySessionProvider(mock(RepositoryService.class), new Identity(NAMED_USER));
      fail("A repository service of another kind must be refused");
    } catch (IllegalStateException e) {
      // expected
    }
  }

}
