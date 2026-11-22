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
package org.exoplatform.services.wcm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;

public class WCMCoreUtilsTest {

  private static final String               USER_ID = "john";

  private MockedStatic<ExoContainerContext> containerContext;

  private MockedConstruction<SessionProvider> providers;

  private SessionProviderService            sessionProviderService;

  private SessionProvider                   callerProvider;

  private ConversationState                 callerState;

  @Before
  public void setUp() {
    ExoContainer container = mock(ExoContainer.class);
    IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
    sessionProviderService = mock(SessionProviderService.class);
    callerProvider = mock(SessionProvider.class);
    callerState = new ConversationState(new Identity("caller"));
    when(identityRegistry.getIdentity(USER_ID)).thenReturn(new Identity(USER_ID));
    when(sessionProviderService.getSessionProvider(null)).thenReturn(callerProvider);
    when(container.getComponentInstanceOfType(IdentityRegistry.class)).thenReturn(identityRegistry);
    when(container.getComponentInstanceOfType(SessionProviderService.class)).thenReturn(sessionProviderService);
    containerContext = Mockito.mockStatic(ExoContainerContext.class);
    containerContext.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
    providers = Mockito.mockConstruction(SessionProvider.class);
    ConversationState.setCurrent(callerState);
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
    providers.close();
    containerContext.close();
  }

  @Test
  public void invokeUserSessionClosesTheProviderItCreated() throws Exception {
    AtomicReference<SessionProvider> received = new AtomicReference<>();

    WCMCoreUtils.invokeUserSession(USER_ID, received::set);

    assertEquals(1, providers.constructed().size());
    SessionProvider created = providers.constructed().get(0);
    assertSame(created, received.get());
    verify(created).close();
    verify(sessionProviderService).setSessionProvider(null, callerProvider);
    assertSame(callerState, ConversationState.getCurrent());
  }

  @Test
  public void invokeUserSessionClosesTheProviderWhenTheHandlerFails() {
    assertThrows(IllegalStateException.class, () -> WCMCoreUtils.invokeUserSession(USER_ID, provider -> {
      throw new IllegalStateException("handler failure");
    }));

    assertEquals(1, providers.constructed().size());
    verify(providers.constructed().get(0)).close();
    verify(sessionProviderService).setSessionProvider(null, callerProvider);
    assertSame(callerState, ConversationState.getCurrent());
  }

  @Test
  public void invokeUserSessionDoesNotCloseTheCallerProvider() throws Exception {
    WCMCoreUtils.invokeUserSession(USER_ID, provider -> {
      // nothing to do
    });

    verify(callerProvider, never()).close();
  }
}
