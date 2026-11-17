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
package org.exoplatform.services.attachments.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.cms.documents.security.DocumentGrantOperation;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.space.model.Space;

/**
 * The news attachments permission listener's grants under a space's "Limit
 * Access Management" setting: each grant is decided for the acting user, a
 * target whose own entry the attachment holds is due none, and a refused
 * grant writes nothing on the attachment.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateContentPermissionsListenerGrantTest {
  @Mock
  private SessionProviderService          sessionProviderService;

  @Mock
  private RepositoryService               repositoryService;

  @Mock
  private AttachmentStorage               attachmentStorage;

  @Mock
  private DocumentSpaceGuard              documentSpaceGuard;

  @Mock
  private DocumentGrantOperation          grantOperation;

  private UpdateContentPermissionsListener shareAttachmentsToSpaceListener;

  private MockedStatic<Utils>             UTILS = mockStatic(Utils.class);

  private MockedStatic<CommonsUtils>      COMMONS_UTILS = mockStatic(CommonsUtils.class);

  private MockedStatic<PortalContainer>   PORTAL_CONTAINER = mockStatic(PortalContainer.class);

  private MockedStatic<ExoContainerContext> EXO_CONTAINER_CONTEXT = mockStatic(ExoContainerContext.class);

  @Before
  public void setUp() throws Exception {
    lenient().when(documentSpaceGuard.newGrantOperation(any(), anyString())).thenReturn(grantOperation);
    lenient().when(grantOperation.canGrant(any(), anyString())).thenReturn(true);
    this.shareAttachmentsToSpaceListener = new UpdateContentPermissionsListener(repositoryService, attachmentStorage) {
      @Override
      protected DocumentSpaceGuard getDocumentSpaceGuard() {
        return documentSpaceGuard;
      }
    };
  }

  @After
  public void tearDown() throws Exception {
    UTILS.close();
    COMMONS_UTILS.close();
    PORTAL_CONTAINER.close();
    EXO_CONTAINER_CONTEXT.close();
  }

  @Test
  public void testGrantsSkippedForARefusedUserAndTheRemovalStillRuns() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("other", space, "mary");
    when(attachmentNode.getACL()).thenReturn(new AccessControlList("root",
                                                                   List.of(new AccessControlEntry("any", PermissionType.READ))));
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("other", space, "mary")));

    verify(attachmentNode, never()).setPermission(anyString(), any());
    verify(attachmentNode).removePermission("any", PermissionType.READ);
    verify(documentSpaceGuard).newGrantOperation("mary", DocumentGrantOperation.SOURCE_NEWS);
    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/target");
  }

  @Test
  public void testAllAudienceGrantSkippedForARefusedUser() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("all", space, "mary");
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("all", space, "mary")));

    verify(attachmentNode, never()).setPermission(anyString(), any());
    verify(grantOperation).canGrant(attachmentNode, "any");
  }

  /**
   * A target whose own READ entry the attachment holds is due no grant:
   * nothing is decided for it, so no skipped grant is logged, and its entry is
   * written as before. Mutant: deciding before reading the access list asks
   * the space.
   */
  @Test
  public void testATargetThatAlreadyReadsTheAttachmentNeedsNoDecision() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("all", space, "mary");
    when(attachmentNode.isNodeType(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(true);
    when(attachmentNode.getACL()).thenReturn(new AccessControlList("root",
                                                                   List.of(new AccessControlEntry("*:/spaces/target",
                                                                                                  PermissionType.READ),
                                                                           new AccessControlEntry("any", PermissionType.READ))));

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("all", space, "mary")));

    verify(grantOperation, never()).canGrant(any(), anyString());
    verify(attachmentNode).setPermission("*:/spaces/target", new String[] { PermissionType.READ });
    verify(attachmentNode).setPermission("any", new String[] { PermissionType.READ });
  }

  /**
   * An "any" entry does not exempt a target from the decision: its own entry,
   * written on that strength, would outlive the removal of "any". Mutant:
   * counting "any" as the target's entry writes the target's grant for a
   * refused user.
   */
  @Test
  public void testAnAnyEntryDoesNotExemptATargetFromTheDecision() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("other", space, "mary");
    when(attachmentNode.isNodeType(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(true);
    when(attachmentNode.getACL()).thenReturn(new AccessControlList("root",
                                                                   List.of(new AccessControlEntry("any", PermissionType.READ))));
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("other", space, "mary")));

    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/target");
    verify(attachmentNode, never()).setPermission(anyString(), any());
    verify(attachmentNode).removePermission("any", PermissionType.READ);
  }

  /**
   * An attachment without an access list of its own reads its folder's: the
   * entries it reads there are not its own, so each target is decided, and a
   * refusal writes and saves nothing. Adding the access list copies the
   * folder's entries as the attachment's own, so they are read before it.
   * Mutants: counting the entries of an attachment without its own list, or
   * reading them after the list is added, writes the grants for a refused
   * user.
   */
  @Test
  public void testEntriesReadFromTheFolderDoNotExemptATargetFromTheDecision() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("all", space, "mary");
    inheritsItsFolderList(attachmentNode, "*:/spaces/target", "any");
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("all", space, "mary")));

    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/target");
    verify(grantOperation).canGrant(attachmentNode, "any");
    verify(attachmentNode, never()).addMixin(anyString());
    verify(attachmentNode, never()).setPermission(anyString(), any());
    verify(attachmentNode, never()).save();
  }

  /**
   * A refused grant under a narrowed audience leaves an attachment that reads
   * its folder's access list, and no "any" entry, as it is: it gets no list of
   * its own, which would copy the folder's entries as its own. Mutants: adding
   * the list before the decisions, or removing "any" from an attachment that
   * does not read it, saves the copy.
   */
  @Test
  public void testARefusedGrantLeavesAnAttachmentReadingItsFolderListAsItIs() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("space", space, "mary");
    inheritsItsFolderList(attachmentNode, "*:/spaces/source");
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("space", space, "mary")));

    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/target");
    verify(attachmentNode, never()).addMixin(anyString());
    verify(attachmentNode, never()).setPermission(anyString(), any());
    verify(attachmentNode, never()).removePermission(anyString(), anyString());
    verify(attachmentNode, never()).save();
  }

  /**
   * A narrowed audience takes away the "any" entry an attachment reads from
   * its folder: the attachment gets an access list of its own, then loses
   * the entry. Mutant: removing it from an attachment without its own list
   * fails on the engine.
   */
  @Test
  public void testANarrowedAudienceRemovesTheAnyEntryReadFromTheFolder() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("space", space, "mary");
    inheritsItsFolderList(attachmentNode, "any");
    when(grantOperation.canGrant(any(), anyString())).thenReturn(false);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("space", space, "mary")));

    InOrder inOrder = inOrder(attachmentNode);
    inOrder.verify(attachmentNode).addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
    inOrder.verify(attachmentNode).removePermission("any", PermissionType.READ);
    inOrder.verify(attachmentNode).save();
  }

  /**
   * The first grant gives the attachment an access list of its own, which
   * copies its folder's entries: which targets it holds is read before it, so
   * the next target is still decided. Mutant: reading it per target, after
   * the first grant, writes the refused target's grant.
   */
  @Test
  public void testTheTargetsHeldAreReadBeforeTheFirstGrant() throws Exception {
    Space allowedSpace = new Space();
    allowedSpace.setGroupId("/spaces/allowed");
    Space refusedSpace = new Space();
    refusedSpace.setGroupId("/spaces/refused");
    ExtendedNode attachmentNode = mockAttachment("space", allowedSpace, "mary");
    inheritsItsFolderList(attachmentNode, "*:/spaces/allowed", "*:/spaces/refused");
    when(grantOperation.canGrant(attachmentNode, "*:/spaces/allowed")).thenReturn(true);
    when(grantOperation.canGrant(attachmentNode, "*:/spaces/refused")).thenReturn(false);
    Map<String, Object> data = eventData("space", allowedSpace, "mary");
    data.put("spaces", List.of(allowedSpace, refusedSpace));

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, data));

    verify(attachmentNode).setPermission("*:/spaces/allowed", new String[] { PermissionType.READ });
    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/refused");
    verify(attachmentNode, never()).setPermission(eq("*:/spaces/refused"), any());
  }

  /**
   * The node has no access list of its own and reads its folder's, holding
   * each identity's READ entry; adding its own makes it privilegeable. The
   * stubs model the engine: a test the listener does not need them in leaves
   * them unused.
   */
  private static void inheritsItsFolderList(ExtendedNode node, String... identities) throws Exception {
    AtomicBoolean privilegeable = new AtomicBoolean(false);
    lenient().when(node.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenAnswer(invocation -> !privilegeable.get());
    lenient().doAnswer(invocation -> {
      privilegeable.set(true);
      return null;
    }).when(node).addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
    lenient().when(node.isNodeType(NodetypeConstant.EXO_PRIVILEGEABLE)).thenAnswer(invocation -> privilegeable.get());
    lenient().when(node.getACL())
             .thenReturn(new AccessControlList("root",
                                               Arrays.stream(identities)
                                                     .map(identity -> new AccessControlEntry(identity, PermissionType.READ))
                                                     .toList()));
  }

  /**
   * An event without an audience, a new article's draft for one, gets its
   * space grants decided and written, and leaves the "any" entry as it is.
   * Mutants: reading the audience as given fails before the space grants or
   * after them; reading a missing audience as a narrowed one removes "any".
   */
  @Test
  public void testAnEventWithoutAudienceGetsItsSpaceGrantsAndLeavesAnyAsItIs() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment(null, space, "mary");
    // an "any" entry the listener must leave: it does not read the list
    lenient().when(attachmentNode.getACL())
             .thenReturn(new AccessControlList("root", List.of(new AccessControlEntry("any", PermissionType.READ))));
    when(grantOperation.canGrant(any(), anyString())).thenReturn(true);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData(null, space, "mary")));

    verify(grantOperation).canGrant(attachmentNode, "*:/spaces/target");
    verify(attachmentNode).setPermission("*:/spaces/target", new String[] { PermissionType.READ });
    verify(attachmentNode, never()).setPermission(eq("any"), any());
    verify(attachmentNode, never()).removePermission(anyString(), anyString());
  }

  @Test
  public void testGrantsWrittenWhenAllowed() throws Exception {
    Space space = new Space();
    space.setGroupId("/spaces/target");
    ExtendedNode attachmentNode = mockAttachment("all", space, null);

    shareAttachmentsToSpaceListener.onEvent(new Event("content.update.permissions", null, eventData("all", space, null)));

    verify(documentSpaceGuard).newGrantOperation(null, DocumentGrantOperation.SOURCE_NEWS);
    verify(attachmentNode).setPermission("*:/spaces/target", new String[] { PermissionType.READ });
    verify(attachmentNode).setPermission("any", new String[] { PermissionType.READ });
  }

  private ExtendedNode mockAttachment(String audience, Space space, String username) throws Exception {
    Session session = mock(Session.class);
    ExtendedNode attachmentNode = mock(ExtendedNode.class);
    lenient().when(attachmentNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)).thenReturn(false);
    when(attachmentNode.getPath()).thenReturn("/Groups/spaces/source/Documents/a.docx");
    lenient().when(attachmentNode.getACL()).thenReturn(new AccessControlList("root", new ArrayList<>()));
    when(session.getNodeByUUID("123")).thenReturn(attachmentNode);
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(SessionProviderService.class)).thenReturn(sessionProviderService);
    Attachment attachment = new Attachment();
    attachment.setId("123");
    when(attachmentStorage.getAttachmentsByEntity(1, "WIKI_PAGE_VERSIONS")).thenReturn(List.of(attachment));
    UTILS.when(() -> Utils.getSystemSession(sessionProviderService, repositoryService)).thenReturn(session);
    return attachmentNode;
  }

  private Map<String, Object> eventData(String audience, Space space, String username) {
    Map<String, Object> data = new java.util.HashMap<>();
    data.put("entityType", "WIKI_PAGE_VERSIONS");
    data.put("entityId", "1");
    data.put("audience", audience);
    data.put("spaces", List.of(space));
    if (username != null) {
      data.put(UpdateContentPermissionsListener.USERNAME, username);
    }
    return data;
  }

}
