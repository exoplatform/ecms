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
package org.exoplatform.wcm.ext.component.document.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ShareDocumentServiceTest {

  private static final String                                          SHARER        = "mary";

  private static final String                                          WORKSPACE     = "collaboration";

  private static final String                                          READABLE_ID   = "readable";

  private static final String                                          UNREADABLE_ID = "unreadable";

  @Mock
  private RepositoryService                                            repositoryService;

  @Mock
  private LinkManager                                                  linkManager;

  @Mock
  private IdentityManager                                              identityManager;

  @Mock
  private ActivityManager                                              activityManager;

  @Mock
  private SpaceService                                                 spaceService;

  @Mock
  private SessionProviderService                                       sessionProviderService;

  private MockedStatic<WCMCoreUtils>                                   wcmCoreUtils;

  private MockedStatic<Utils>                                          activityUtils;

  private MockedStatic<org.exoplatform.services.wcm.utils.Utils>       wcmUtils;

  private ShareDocumentService                                         shareDocumentService;

  private Node                                                         readableNode;

  private Node                                                         unreadableNode;

  @Before
  public void setUp() throws Exception {
    shareDocumentService = spy(new ShareDocumentService(repositoryService,
                                                        linkManager,
                                                        identityManager,
                                                        activityManager,
                                                        spaceService,
                                                        sessionProviderService));
    doReturn(true).when(shareDocumentService).canRead(SHARER, WORKSPACE, READABLE_ID);
    doReturn(false).when(shareDocumentService).canRead(SHARER, WORKSPACE, UNREADABLE_ID);

    // the documents as the system session reads them, and the space's copy
    readableNode = mock(Node.class, RETURNS_DEEP_STUBS);
    unreadableNode = mock(Node.class, RETURNS_DEEP_STUBS);
    Node copy = mock(Node.class, RETURNS_DEEP_STUBS);
    when(readableNode.getSession().getNodeByUUID("copy")).thenReturn(copy);
    when(unreadableNode.getSession().getNodeByUUID("copy")).thenReturn(copy);
    when(copy.getSession().getRepository()).thenReturn(mock(ManageableRepository.class, RETURNS_DEEP_STUBS));
    doReturn("copy").when(shareDocumentService).publishDocumentToSpace(anyString(), any(), anyString(), any(), anyBoolean());
    ExtendedSession systemSession = mock(ExtendedSession.class);
    when(systemSession.getNodeByIdentifier(READABLE_ID)).thenReturn(readableNode);
    when(systemSession.getNodeByIdentifier(UNREADABLE_ID)).thenReturn(unreadableNode);
    SessionProvider systemProvider = mock(SessionProvider.class);
    when(systemProvider.getSession(any(), any())).thenReturn(systemSession);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(WCMCoreUtils::getSystemSessionProvider).thenReturn(systemProvider);
    activityUtils = mockStatic(Utils.class);
    wcmUtils = mockStatic(org.exoplatform.services.wcm.utils.Utils.class);

    Space space = new Space();
    space.setGroupId("/spaces/team");
    when(spaceService.getSpaceByPrettyName("team")).thenReturn(space);
    when(spaceService.canRedactOnSpace(space, SHARER)).thenReturn(true);
    Identity sharer = new Identity("1");
    sharer.setRemoteId(SHARER);
    when(identityManager.getIdentity("1")).thenReturn(sharer);
  }

  @After
  public void tearDown() {
    wcmUtils.close();
    activityUtils.close();
    wcmCoreUtils.close();
  }

  /**
   * Of the documents an activity names, only those its sharer can read are
   * copied to the target space and named by the shared activity.
   */
  @Test
  public void onlyTheDocumentsTheSharerCanReadAreSharedToTheSpace() throws Exception {
    ExoSocialActivity originalActivity = mock(ExoSocialActivity.class);
    when(originalActivity.getTemplateParams()).thenReturn(Map.of(ShareDocumentService.WORKSPACE,
                                                                 WORKSPACE + "|@|" + WORKSPACE,
                                                                 ShareDocumentService.ID,
                                                                 UNREADABLE_ID + "|@|" + READABLE_ID));
    when(activityManager.getActivity("original")).thenReturn(originalActivity);
    ExoSocialActivity sharedActivity = mock(ExoSocialActivity.class);
    ActivityStream stream = mock(ActivityStream.class);
    when(stream.getPrettyId()).thenReturn("team");
    when(sharedActivity.getActivityStream()).thenReturn(stream);
    when(sharedActivity.getPosterId()).thenReturn("1");
    when(sharedActivity.getTemplateParams()).thenReturn(new HashMap<>(Map.of("originalActivityId", "original")));

    shareDocumentService.shareDocumentActivityToSpace(sharedActivity);

    verify(shareDocumentService).publishDocumentToSpace(eq("/spaces/team"), eq(readableNode), anyString(), any(), anyBoolean());
    verify(shareDocumentService, never()).publishDocumentToSpace(anyString(), eq(unreadableNode), anyString(), any(), anyBoolean());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> templateParams = ArgumentCaptor.forClass(Map.class);
    verify(sharedActivity).setTemplateParams(templateParams.capture());
    assertEquals("copy", templateParams.getValue().get(ShareDocumentService.SHARED_TEMPLATE_PARAMS_PREFIX + "id"));
    verify(activityManager).updateActivity(sharedActivity);
  }
}
