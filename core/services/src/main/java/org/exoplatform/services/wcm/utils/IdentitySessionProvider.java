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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.RepositoryContainer;
import org.exoplatform.services.jcr.impl.RepositoryServiceImpl;
import org.exoplatform.services.jcr.impl.WorkspaceContainer;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionReference;
import org.exoplatform.services.jcr.impl.core.TrackedSession;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

/**
 * A {@link SessionProvider} whose sessions are opened as the identity it is
 * created for. The stock {@code new SessionProvider(ConversationState)} does
 * not keep the state: its sessions log in as the thread's
 * {@code ConversationState.getCurrent()}, whoever that is. This provider opens
 * each session with the identity's own state, whatever the thread's state is,
 * null included. The caller closes it as any {@link SessionProvider}.
 * <p>
 * The sessions are built directly, as {@code SessionFactory#createSession}
 * does: a {@link TrackedSession} when {@code exo.jcr.session.tracking.active}
 * is on, so a session left open is reported, a {@link SessionImpl} otherwise.
 */
public class IdentitySessionProvider extends SessionProvider {

  private final RepositoryServiceImpl        repositoryService;

  private final ConversationState            state;

  private final Map<String, ExtendedSession> sessions = new HashMap<>();

  private boolean                            closed;

  /**
   * @param repositoryService the repository service, which must be the
   *          repository's own implementation
   * @param identity the identity the sessions are opened as
   * @throws IllegalArgumentException when the identity is missing or is the
   *           system's, whose sessions come from
   *           {@link SessionProvider#createSystemProvider()}
   * @throws IllegalStateException when the repository service cannot open a
   *           session for a given identity
   */
  public IdentitySessionProvider(RepositoryService repositoryService, Identity identity) {
    this(repositoryService, new ConversationState(requireIdentity(identity)));
  }

  private IdentitySessionProvider(RepositoryService repositoryService, ConversationState state) {
    super(state);
    if (!(repositoryService instanceof RepositoryServiceImpl repositoryServiceImpl)) {
      throw new IllegalStateException("A session of a given identity needs the repository's own service");
    }
    this.repositoryService = repositoryServiceImpl;
    this.state = state;
  }

  @Override
  public synchronized Session getSession(String workspaceName, ManageableRepository repository) throws RepositoryException {
    if (closed) {
      throw new IllegalStateException("Session provider already closed");
    }
    if (workspaceName == null) {
      throw new IllegalArgumentException("Workspace Name is null");
    }
    String repositoryName = repository.getConfiguration().getName();
    String key = repositoryName + workspaceName;
    ExtendedSession session = sessions.get(key);
    if (session == null) {
      RepositoryContainer repositoryContainer = repositoryService.getRepositoryContainer(repositoryName);
      WorkspaceContainer workspaceContainer = repositoryContainer == null ? null
                                                                          : repositoryContainer.getWorkspaceContainer(workspaceName);
      if (workspaceContainer == null || !repository.isWorkspaceInitialized(workspaceName)) {
        throw new NoSuchWorkspaceException("Workspace '" + workspaceName + "' not found");
      }
      session = SessionReference.isStarted() ? new TrackedSession(workspaceName, state, workspaceContainer)
                                             : new SessionImpl(workspaceName, state, workspaceContainer);
      session.registerLifecycleListener(this);
      sessions.put(key, session);
    }
    return session;
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    for (ExtendedSession session : sessions.values().toArray(new ExtendedSession[0])) {
      session.logout();
    }
    sessions.clear();
  }

  @Override
  public synchronized void onCloseSession(ExtendedSession session) {
    sessions.values().remove(session);
  }

  private static Identity requireIdentity(Identity identity) {
    if (identity == null) {
      throw new IllegalArgumentException("The identity to open a session for is missing");
    }
    if (IdentityConstants.SYSTEM.equals(identity.getUserId())) {
      throw new IllegalArgumentException("A system session is opened with SessionProvider.createSystemProvider()");
    }
    return identity;
  }

}
