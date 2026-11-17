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
package org.exoplatform.wcm.ext.component.document.service;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cms.documents.security.DocumentGrantOperation;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * An activity shared to another space gives that space access to its
 * documents only when each document's space lets the sharer manage its access.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ShareDocumentServiceTest {

  private static final String         SHARER       = "mary";

  private static final String         TARGET_GROUP = "/spaces/target";

  @Mock
  private RepositoryService           repositoryService;

  @Mock
  private LinkManager                 linkManager;

  @Mock
  private IdentityManager             identityManager;

  @Mock
  private ActivityManager             activityManager;

  @Mock
  private SpaceService                spaceService;

  @Mock
  private SessionProviderService      sessionProviderService;

  @Mock
  private DocumentSpaceGuard          guard;

  @Mock
  private DocumentGrantOperation      grantOperation;

  @Mock
  private Node                        document;

  private ExtendedSession             session;

  private MockedStatic<WCMCoreUtils>  wcmCoreUtils;

  private ShareDocumentService        shareDocumentService;

  private ExoSocialActivity           sharedActivity;

  @Before
  public void setUp() throws Exception {
    Space targetSpace = new Space();
    targetSpace.setGroupId(TARGET_GROUP);
    when(spaceService.getSpaceByPrettyName("target")).thenReturn(targetSpace);
    Identity sharer = new Identity("organization", SHARER);
    when(identityManager.getIdentity("sharerIdentityId")).thenReturn(sharer);
    when(spaceService.canRedactOnSpace(targetSpace, SHARER)).thenReturn(true);
    ExoSocialActivity originalActivity = new ExoSocialActivityImpl();
    originalActivity.setTemplateParams(new HashMap<>(Map.of(ShareDocumentService.WORKSPACE,
                                                            "collaboration",
                                                            ShareDocumentService.ID,
                                                            "documentId")));
    when(activityManager.getActivity("originalActivityId")).thenReturn(originalActivity);
    ManageableRepository repository = mock(ManageableRepository.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    session = mock(ExtendedSession.class);
    when(systemSessionProvider.getSession("collaboration", repository)).thenReturn(session);
    when(session.getNodeByIdentifier("documentId")).thenReturn(document);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(WCMCoreUtils::getSystemSessionProvider).thenReturn(systemSessionProvider);
    when(guard.newGrantOperation(SHARER, DocumentGrantOperation.SOURCE_ACTIVITY_SHARE)).thenReturn(grantOperation);

    shareDocumentService = spy(new ShareDocumentService(repositoryService,
                                                        linkManager,
                                                        identityManager,
                                                        activityManager,
                                                        spaceService,
                                                        sessionProviderService));
    doReturn(guard).when(shareDocumentService).getDocumentSpaceGuard();

    sharedActivity = new ExoSocialActivityImpl();
    sharedActivity.setId("sharedActivityId");
    sharedActivity.setPosterId("sharerIdentityId");
    sharedActivity.setTemplateParams(new HashMap<>(Map.of("originalActivityId", "originalActivityId")));
    ActivityStreamImpl stream = new ActivityStreamImpl();
    stream.setPrettyId("target");
    sharedActivity.setActivityStream(stream);
  }

  @After
  public void tearDown() {
    wcmCoreUtils.close();
  }

  @Test
  public void testNoGrantWhenTheDocumentSpaceRefusesTheSharer() throws Exception {
    when(grantOperation.canGrant(document, "*:" + TARGET_GROUP)).thenReturn(false);

    shareDocumentService.shareDocumentActivityToSpace(sharedActivity);

    verify(shareDocumentService, never()).publishDocumentToSpace(anyString(), any(), anyString(), anyString(), anyBoolean());
  }

  @Test
  public void testGrantWhenTheDocumentSpaceLetsTheSharerManageAccess() throws Exception {
    when(grantOperation.canGrant(document, "*:" + TARGET_GROUP)).thenReturn(true);
    // the grant is the publication of the document to the target space: what
    // follows it is out of this test's reach
    IllegalStateException published = new IllegalStateException("published");
    doThrow(published).when(shareDocumentService).publishDocumentToSpace(TARGET_GROUP, document, "", PermissionType.READ, false);

    assertSame(published, assertThrows(IllegalStateException.class,
                                       () -> shareDocumentService.shareDocumentActivityToSpace(sharedActivity)));
  }

  /**
   * A target space whose own READ entry the document holds is due no grant:
   * nothing is decided for it, and the document is published to it. Mutant:
   * deciding before reading the access list asks the space, which refuses,
   * and publishes nothing.
   */
  @Test
  public void testATargetSpaceThatAlreadyReadsTheDocumentNeedsNoDecision() throws Exception {
    ExtendedNode readDocument = mock(ExtendedNode.class);
    when(readDocument.isNodeType("exo:privilegeable")).thenReturn(true);
    when(readDocument.getACL()).thenReturn(new AccessControlList("root",
                                                                 List.of(new AccessControlEntry("*:" + TARGET_GROUP,
                                                                                                PermissionType.READ))));
    when(session.getNodeByIdentifier("documentId")).thenReturn(readDocument);
    IllegalStateException published = new IllegalStateException("published");
    doThrow(published).when(shareDocumentService)
                      .publishDocumentToSpace(TARGET_GROUP, readDocument, "", PermissionType.READ, false);

    assertSame(published, assertThrows(IllegalStateException.class,
                                       () -> shareDocumentService.shareDocumentActivityToSpace(sharedActivity)));
    verify(grantOperation, never()).canGrant(any(), anyString());
  }

  /**
   * An "any" entry does not exempt the target space from the decision: the
   * space's own entry, written on that strength, would outlive the removal of
   * "any". Mutant: counting "any" as the space's entry publishes the document
   * for a refused sharer.
   */
  @Test
  public void testAnAnyEntryDoesNotExemptTheTargetSpaceFromTheDecision() throws Exception {
    ExtendedNode publicDocument = mock(ExtendedNode.class);
    when(publicDocument.isNodeType("exo:privilegeable")).thenReturn(true);
    when(publicDocument.getACL()).thenReturn(new AccessControlList("root",
                                                                   List.of(new AccessControlEntry("any", PermissionType.READ))));
    when(session.getNodeByIdentifier("documentId")).thenReturn(publicDocument);
    when(grantOperation.canGrant(publicDocument, "*:" + TARGET_GROUP)).thenReturn(false);

    shareDocumentService.shareDocumentActivityToSpace(sharedActivity);

    verify(grantOperation).canGrant(publicDocument, "*:" + TARGET_GROUP);
    verify(shareDocumentService, never()).publishDocumentToSpace(anyString(), any(), anyString(), anyString(), anyBoolean());
  }

  /**
   * A document without an access list of its own reads its folder's: the
   * target space's entry there is not the document's, so the space is
   * decided. Mutant: counting the entries of a document without its own list
   * publishes the document for a refused sharer.
   */
  @Test
  public void testAnEntryReadFromTheFolderDoesNotExemptTheTargetSpaceFromTheDecision() throws Exception {
    ExtendedNode document = mock(ExtendedNode.class);
    when(document.getACL()).thenReturn(new AccessControlList("root",
                                                             List.of(new AccessControlEntry("*:" + TARGET_GROUP,
                                                                                            PermissionType.READ))));
    when(session.getNodeByIdentifier("documentId")).thenReturn(document);
    when(grantOperation.canGrant(document, "*:" + TARGET_GROUP)).thenReturn(false);
    // a publication fails the test with this marker
    doThrow(new IllegalStateException("published")).when(shareDocumentService)
                                                   .publishDocumentToSpace(anyString(),
                                                                           any(),
                                                                           anyString(),
                                                                           anyString(),
                                                                           anyBoolean());

    shareDocumentService.shareDocumentActivityToSpace(sharedActivity);

    verify(grantOperation).canGrant(document, "*:" + TARGET_GROUP);
  }
}
