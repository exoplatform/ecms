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
package org.exoplatform.wcm.ext.component.activity.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cms.documents.security.DocumentGrantOperation;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.MembershipImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

/**
 * A document posted in another space's stream gives that space access only
 * when the document's space lets the poster manage its access.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ActivityListenerTest {

  private static final String    POSTER       = "mary";

  private static final String    TARGET_GROUP = "/spaces/target";

  @Mock
  private SpaceService           spaceService;

  @Mock
  private SessionProviderService sessionProviderService;

  @Mock
  private RepositoryService      repositoryService;

  @Mock
  private IShareDocumentService  shareDocumentService;

  @Mock
  private OrganizationService    organizationService;

  @Mock
  private DocumentSpaceGuard     guard;

  @Mock
  private DocumentGrantOperation grantOperation;

  @Mock
  private Node                   document;

  private ActivityListener       listener;

  private ActivityLifeCycleEvent event;

  private Session                session;

  @Before
  public void setUp() throws Exception {
    Space targetSpace = new Space();
    targetSpace.setGroupId(TARGET_GROUP);
    when(spaceService.getSpaceByPrettyName("target")).thenReturn(targetSpace);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    when(sessionProviderService.getSystemSessionProvider(null)).thenReturn(sessionProvider);
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setDefaultWorkspaceName("collaboration");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    session = mock(Session.class);
    when(sessionProvider.getSession("collaboration", repository)).thenReturn(session);
    when(session.getNodeByUUID("documentId")).thenReturn(document);
    MembershipHandler membershipHandler = mock(MembershipHandler.class);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);
    MembershipImpl membership = new MembershipImpl();
    membership.setGroupId(TARGET_GROUP);
    membership.setMembershipType("member");
    when(membershipHandler.findMembershipsByUserAndGroup(POSTER, TARGET_GROUP)).thenReturn(List.<Membership> of(membership));
    ConversationState.setCurrent(new ConversationState(new Identity(POSTER)));
    when(guard.newGrantOperation(POSTER, DocumentGrantOperation.SOURCE_ACTIVITY_POST)).thenReturn(grantOperation);

    listener = spy(new ActivityListener(spaceService,
                                        sessionProviderService,
                                        repositoryService,
                                        shareDocumentService,
                                        organizationService));
    doReturn(guard).when(listener).getDocumentSpaceGuard();

    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setTemplateParams(Map.of("id", "documentId"));
    activity.setStreamOwner("target");
    event = new ActivityLifeCycleEvent(null, activity);
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  public void testNoGrantWhenTheDocumentSpaceRefusesThePoster() {
    when(grantOperation.canGrant(document, "*:" + TARGET_GROUP)).thenReturn(false);

    listener.saveActivity(event);

    verify(shareDocumentService, never()).publishDocumentToSpace(anyString(), any(), anyString(), anyString(), anyBoolean());
  }

  /**
   * The target space already reads the document: no grant is due, so none is
   * decided and no skipped grant is reported. Mutant: deciding before checking
   * the access asks the space's decision.
   */
  @Test
  public void testNoGrantDecisionWhenTheTargetSpaceAlreadyReadsTheDocument() throws Exception {
    ExtendedNode readable = mock(ExtendedNode.class);
    AccessControlList accessList = mock(AccessControlList.class);
    when(accessList.getPermissionEntries()).thenReturn(List.of(new AccessControlEntry("*:" + TARGET_GROUP, PermissionType.READ)));
    when(readable.getACL()).thenReturn(accessList);
    when(session.getNodeByUUID("documentId")).thenReturn(readable);

    listener.saveActivity(event);

    verify(grantOperation, never()).canGrant(any(), anyString());
    verify(shareDocumentService, never()).publishDocumentToSpace(anyString(), any(), anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testGrantWhenTheDocumentSpaceLetsThePosterManageAccess() {
    when(grantOperation.canGrant(document, "*:" + TARGET_GROUP)).thenReturn(true);

    listener.saveActivity(event);

    verify(shareDocumentService).publishDocumentToSpace(TARGET_GROUP, document, "", PermissionType.READ, false);
  }
}
