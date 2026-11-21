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
package org.exoplatform.wcm.ext.component.activity.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

@RunWith(MockitoJUnitRunner.class)
public class ActivityListenerTest {

  private static final String    POSTER          = "mary";

  /** The user whose session publishes the activity */
  private static final String    PUBLISHER       = "john";

  private static final String    POSTER_IDENTITY = "2";

  private static final String    SPACE_GROUP     = "/spaces/team";

  private static final String    WORKSPACE       = "collaboration";

  private static final String    READABLE_ID     = "readable";

  private static final String    UNREADABLE_ID   = "unreadable";

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
  private IdentityManager        identityManager;

  private ActivityListener       activityListener;

  private ExtendedNode           readableNode;

  private ExtendedNode           unreadableNode;

  @Before
  public void setUp() throws Exception {
    ConversationState.setCurrent(new ConversationState(new Identity(PUBLISHER)));
    org.exoplatform.social.core.identity.model.Identity poster =
                                                               new org.exoplatform.social.core.identity.model.Identity(POSTER_IDENTITY);
    poster.setRemoteId(POSTER);
    lenient().when(identityManager.getIdentity(POSTER_IDENTITY)).thenReturn(poster);
    Space space = new Space();
    space.setGroupId(SPACE_GROUP);
    when(spaceService.getSpaceByPrettyName("team")).thenReturn(space);

    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry configuration = mock(RepositoryEntry.class);
    when(configuration.getDefaultWorkspaceName()).thenReturn(WORKSPACE);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    SessionProvider systemProvider = mock(SessionProvider.class);
    Session systemSession = mock(Session.class);
    when(systemProvider.getSession(WORKSPACE, repository)).thenReturn(systemSession);
    when(sessionProviderService.getSystemSessionProvider(null)).thenReturn(systemProvider);
    readableNode = nodeWithoutSpaceRight();
    unreadableNode = nodeWithoutSpaceRight();
    lenient().when(systemSession.getNodeByUUID(READABLE_ID)).thenReturn(readableNode);
    // stubbed so that only the read check decides
    lenient().when(systemSession.getNodeByUUID(UNREADABLE_ID)).thenReturn(unreadableNode);

    Membership membership = mock(Membership.class);
    when(membership.getGroupId()).thenReturn(SPACE_GROUP);
    when(membership.getMembershipType()).thenReturn("member");
    MembershipHandler membershipHandler = mock(MembershipHandler.class);
    when(membershipHandler.findMembershipsByUserAndGroup(PUBLISHER, SPACE_GROUP)).thenReturn(List.of(membership));
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);

    activityListener = new ActivityListener(spaceService,
                                            sessionProviderService,
                                            repositoryService,
                                            shareDocumentService,
                                            organizationService,
                                            identityManager);
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * A document an activity names is given to the space only when its poster
   * can read it, whoever publishes the activity; the activity's other
   * documents still are.
   */
  @Test
  public void aDocumentThePosterCannotReadIsNotGivenToTheSpace() {
    when(shareDocumentService.canRead(POSTER, WORKSPACE, READABLE_ID)).thenReturn(true);
    when(shareDocumentService.canRead(POSTER, WORKSPACE, UNREADABLE_ID)).thenReturn(false);
    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setId("1");
    activity.setPosterId(POSTER_IDENTITY);
    activity.setStreamOwner("team");
    activity.setTemplateParams(Map.of("id", UNREADABLE_ID + "|@|" + READABLE_ID));

    activityListener.saveActivity(new ActivityLifeCycleEvent(ActivityLifeCycleEvent.Type.SAVE_ACTIVITY, activity));

    verify(shareDocumentService).publishDocumentToSpace(eq(SPACE_GROUP), eq(readableNode), anyString(), anyString(), anyBoolean());
    verify(shareDocumentService, never()).publishDocumentToSpace(any(), eq(unreadableNode), any(), any(), any());
  }

  /**
   * An activity whose poster is unknown gives the space none of its documents.
   */
  @Test
  public void anActivityOfAnUnknownPosterGivesNoDocument() {
    // whoever is asked can read: only the unknown poster refuses the grant
    lenient().when(shareDocumentService.canRead(any(), any(), any())).thenReturn(true);
    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setId("1");
    activity.setPosterId("unknown");
    activity.setStreamOwner("team");
    activity.setTemplateParams(Map.of("id", READABLE_ID));

    activityListener.saveActivity(new ActivityLifeCycleEvent(ActivityLifeCycleEvent.Type.SAVE_ACTIVITY, activity));

    verify(shareDocumentService, never()).publishDocumentToSpace(any(), any(), any(), any(), any());
  }

  private static ExtendedNode nodeWithoutSpaceRight() throws Exception {
    ExtendedNode node = mock(ExtendedNode.class);
    AccessControlList acl = mock(AccessControlList.class);
    when(acl.getPermissionEntries()).thenReturn(Collections.emptyList());
    when(node.getACL()).thenReturn(acl);
    return node;
  }
}
