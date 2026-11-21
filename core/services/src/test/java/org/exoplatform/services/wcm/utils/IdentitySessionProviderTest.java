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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.utils;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import javax.jcr.Session;

import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.impl.DocumentServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
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

  public void testDocumentServiceHelperOpensAsNamedUser() throws Exception {
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);
    identityRegistry.register(new Identity(NAMED_USER));
    try {
      ConversationState.setCurrent(new ConversationState(new Identity(THREAD_USER)));
      Method helper = DocumentServiceImpl.class.getDeclaredMethod("getUserSessionProvider", String.class);
      helper.setAccessible(true);
      SessionProvider provider = (SessionProvider) helper.invoke(getService(DocumentService.class), NAMED_USER);
      try {
        assertEquals(NAMED_USER, provider.getSession(COLLABORATION_WS, repository).getUserID());
      } finally {
        provider.close();
      }
    } finally {
      identityRegistry.unregister(NAMED_USER);
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
