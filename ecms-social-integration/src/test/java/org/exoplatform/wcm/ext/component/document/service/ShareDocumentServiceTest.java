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
 */
package org.exoplatform.wcm.ext.component.document.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

public class ShareDocumentServiceTest {

  private static final String                                          SPACE_GROUP = "/spaces/test";

  private static final String                                          WORKSPACE   = "collaboration";

  private RepositoryService                                            repositoryService;

  private LinkManager                                                  linkManager;

  private IdentityManager                                              identityManager;

  private ActivityManager                                              activityManager;

  private SpaceService                                                 spaceService;

  private SessionProviderService                                       sessionProviderService;

  private TrashService                                                 trashService;

  private ManageableRepository                                         repository;

  private ShareDocumentService                                         shareDocumentService;

  private MockedStatic<WCMCoreUtils>                                   wcmCoreUtils;

  private MockedStatic<Utils>                                          activityUtils;

  private MockedStatic<org.exoplatform.services.wcm.utils.Utils>       wcmUtils;

  @Before
  public void setUp() {
    repositoryService = mock(RepositoryService.class);
    linkManager = mock(LinkManager.class);
    identityManager = mock(IdentityManager.class);
    activityManager = mock(ActivityManager.class);
    spaceService = mock(SpaceService.class);
    sessionProviderService = mock(SessionProviderService.class);
    trashService = mock(TrashService.class);
    repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setName("repository");
    repositoryEntry.setDefaultWorkspaceName(WORKSPACE);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(TrashService.class)).thenReturn(trashService);
    activityUtils = mockStatic(Utils.class);
    wcmUtils = mockStatic(org.exoplatform.services.wcm.utils.Utils.class);
    shareDocumentService = new ShareDocumentService(repositoryService,
                                                    linkManager,
                                                    identityManager,
                                                    activityManager,
                                                    spaceService,
                                                    sessionProviderService);
  }

  @After
  public void tearDown() {
    wcmCoreUtils.close();
    activityUtils.close();
    wcmUtils.close();
  }

  @Test
  public void testADocumentOutsideTheTrashIsSharedToTheSpace() throws Exception {
    Node sharedFolder = spaceSharedFolder();
    ExtendedNode document = document("/Users/john/Private/report.docx");
    Node link = mock(Node.class);
    when(linkManager.createLink(sharedFolder, document)).thenReturn(link);
    when(link.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)).thenReturn(true);
    when(link.getUUID()).thenReturn("link-uuid");

    String sharedId = shareDocumentService.publishDocumentToSpace(SPACE_GROUP, document, "", PermissionType.READ, false);

    assertEquals("link-uuid", sharedId);
    verify(document).setPermission("*:" + SPACE_GROUP, new String[] { PermissionType.READ });
  }

  @Test
  public void testADocumentInTheTrashIsSharedToNoSpace() throws Exception {
    ExtendedNode document = document("/Trash/Users/john/report.docx");
    when(trashService.isInTrash(document)).thenReturn(true);

    String sharedId = shareDocumentService.publishDocumentToSpace(SPACE_GROUP, document, "", PermissionType.READ, true);

    assertEquals("", sharedId);
    verify(document, never()).setPermission(anyString(), any(String[].class));
    verify(linkManager, never()).createLink(any(Node.class), any(Node.class));
    verifyNoInteractions(sessionProviderService, repositoryService, activityManager);
  }

  @Test
  public void testAShortcutToADocumentInTheTrashIsSharedToNoSpace() throws Exception {
    Node shortcut = mock(Node.class);
    when(shortcut.getPath()).thenReturn("/Users/john/Private/report.docx");
    when(shortcut.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(true);
    ExtendedNode document = document("/Trash/Users/mary/report.docx");
    when(linkManager.getTarget(shortcut)).thenReturn(document);
    when(trashService.isInTrash(document)).thenReturn(true);

    String sharedId = shareDocumentService.publishDocumentToSpace(SPACE_GROUP, shortcut, "", PermissionType.READ, true);

    assertEquals("", sharedId);
    verify(document, never()).setPermission(anyString(), any(String[].class));
    verify(linkManager, never()).createLink(any(Node.class), any(Node.class));
    verifyNoInteractions(sessionProviderService, repositoryService, activityManager);
  }

  @Test
  public void testAnActivityIsSharedToASpaceWithoutItsDocumentInTheTrash() throws Exception {
    ShareDocumentService service = spy(shareDocumentService);
    ExoSocialActivity originalActivity = new ExoSocialActivityImpl();
    originalActivity.setTemplateParams(Map.of("WORKSPACE",
                                              WORKSPACE + ShareDocumentService.TEMPLATE_PARAMS_SEPARATOR + WORKSPACE,
                                              "id",
                                              "trashed-id" + ShareDocumentService.TEMPLATE_PARAMS_SEPARATOR + "live-id"));
    when(activityManager.getActivity("1")).thenReturn(originalActivity);
    ExoSocialActivity sharedActivity = sharedActivity("1");
    ExtendedSession systemSession = mock(ExtendedSession.class);
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    when(systemSessionProvider.getSession(WORKSPACE, repository)).thenReturn(systemSession);
    wcmCoreUtils.when(WCMCoreUtils::getSystemSessionProvider).thenReturn(systemSessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    ExtendedNode trashedDocument = document("/Trash/Users/john/old.docx");
    when(trashService.isInTrash(trashedDocument)).thenReturn(true);
    when(systemSession.getNodeByIdentifier("trashed-id")).thenReturn(trashedDocument);
    ExtendedNode liveDocument = document("/Users/john/Private/new.docx");
    when(systemSession.getNodeByIdentifier("live-id")).thenReturn(liveDocument);
    doReturn("live-link-uuid").when(service).publishDocumentToSpace(SPACE_GROUP, liveDocument, "", PermissionType.READ, false);
    Node liveLink = sharedLink("/Groups/spaces/test/Documents/Shared/new.docx");
    when(liveDocument.getSession().getNodeByUUID("live-link-uuid")).thenReturn(liveLink);

    service.shareDocumentActivityToSpace(sharedActivity);

    verify(activityManager).updateActivity(sharedActivity);
    Map<String, String> templateParams = sharedActivity.getTemplateParams();
    assertEquals("live-link-uuid", templateParams.get("Shared_id"));
    assertEquals("/Groups/spaces/test/Documents/Shared/new.docx", templateParams.get("Shared_DOCPATH"));
    assertFalse(templateParams.get("Shared_id").contains(ShareDocumentService.TEMPLATE_PARAMS_SEPARATOR));
  }

  /**
   * An activity of the space test, sharing the activity of the given id, posted
   * by john, who may post in that space
   */
  private ExoSocialActivity sharedActivity(String originalActivityId) {
    ExoSocialActivityImpl sharedActivity = new ExoSocialActivityImpl();
    sharedActivity.setId("2");
    sharedActivity.setPosterId("3");
    sharedActivity.setTemplateParams(new HashMap<>(Map.of("originalActivityId", originalActivityId)));
    ActivityStreamImpl activityStream = new ActivityStreamImpl();
    activityStream.setPrettyId("test");
    sharedActivity.setActivityStream(activityStream);
    Space space = new Space();
    space.setGroupId(SPACE_GROUP);
    when(spaceService.getSpaceByPrettyName("test")).thenReturn(space);
    Identity poster = new Identity("3");
    poster.setRemoteId("john");
    when(identityManager.getIdentity("3")).thenReturn(poster);
    when(spaceService.canRedactOnSpace(space, "john")).thenReturn(true);
    return sharedActivity;
  }

  private ExtendedNode document(String path) throws Exception {
    ExtendedNode document = mock(ExtendedNode.class);
    when(document.getPath()).thenReturn(path);
    when(document.getACL()).thenReturn(new AccessControlList());
    Session session = mock(Session.class);
    when(document.getSession()).thenReturn(session);
    return document;
  }

  /**
   * @return the Shared folder of the space test's Documents
   */
  private Node spaceSharedFolder() throws Exception {
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    when(sessionProviderService.getSystemSessionProvider(null)).thenReturn(systemSessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    Session systemSession = mock(Session.class);
    when(systemSessionProvider.getSession(WORKSPACE, repository)).thenReturn(systemSession);
    NodeHierarchyCreator nodeHierarchyCreator = mock(NodeHierarchyCreator.class);
    when(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH)).thenReturn("/Groups");
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(NodeHierarchyCreator.class)).thenReturn(nodeHierarchyCreator);
    Node spaceNode = mock(Node.class);
    when(systemSession.getItem("/Groups" + SPACE_GROUP)).thenReturn(spaceNode);
    Node documentsFolder = mock(Node.class);
    when(spaceNode.getNode("Documents")).thenReturn(documentsFolder);
    when(documentsFolder.hasNode("Shared")).thenReturn(true);
    Node sharedFolder = mock(Node.class);
    when(documentsFolder.getNode("Shared")).thenReturn(sharedFolder);
    return sharedFolder;
  }

  private Node sharedLink(String path) throws Exception {
    Node link = mock(Node.class);
    when(link.getPath()).thenReturn(path);
    Session linkSession = mock(Session.class);
    when(link.getSession()).thenReturn(linkSession);
    when(linkSession.getRepository()).thenReturn(repository);
    Workspace workspace = mock(Workspace.class);
    when(workspace.getName()).thenReturn(WORKSPACE);
    when(linkSession.getWorkspace()).thenReturn(workspace);
    return link;
  }

}
