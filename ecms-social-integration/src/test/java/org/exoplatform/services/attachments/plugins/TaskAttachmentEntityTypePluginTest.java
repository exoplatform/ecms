/*
 * Copyright (C) 2022 eXo Platform SAS.
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
package org.exoplatform.services.attachments.plugins;

import static org.mockito.Mockito.lenient;
import org.exoplatform.services.cms.documents.security.DocumentGrantOperation;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.social.core.space.model.Space;
import junit.framework.TestCase;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.WorkspaceImpl;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;

import static org.exoplatform.services.wcm.core.NodetypeConstant.EXO_PRIVILEGEABLE;
import static org.exoplatform.services.wcm.core.NodetypeConstant.NT_FOLDER;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskAttachmentEntityTypePluginTest extends TestCase {

  public void testGetAttachmentOrLinkId() throws Exception {
    long entityId = 1;

    // Mock services
    TaskService taskService = mock(TaskService.class);
    ProjectService projectService = mock(ProjectService.class);
    NodeHierarchyCreator nodeHierarchyCreator = mock(NodeHierarchyCreator.class);
    RepositoryService repositoryService = mock(RepositoryService.class);
    SessionProviderService sessionProviderService = mock(SessionProviderService.class);

    // Mock SessionProvider
    SessionProvider sessionProvider = mock(SessionProvider.class);
    when(sessionProviderService.getSessionProvider(null)).thenReturn(sessionProvider);

    // Mock Repository service and JCR session
    ManageableRepository repository = mock(ManageableRepository.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repository.getConfiguration().getDefaultWorkspaceName()).thenReturn("collaboration");
    ExtendedSession extendedSession = mock(ExtendedSession.class);
    when(sessionProvider.getSession(any(), any())).thenReturn(extendedSession);

    // Mock Task
    TaskDto task = new TaskDto();
    task.setId(1);
    ProjectDto project = new ProjectDto();
    project.setId(1);
    StatusDto status = new StatusDto();
    status.setProject(project);
    task.setStatus(status);
    when(taskService.getTask(1)).thenReturn(task);
    when(projectService.getParticipator(anyLong())).thenReturn(new HashSet<>(Arrays.asList("user1",
            "/platform/users", "member:/spaces/space1")));

    // Instantiate the TaskAttachmentEntityTypePlugin
    TaskAttachmentEntityTypePlugin taskAttachmentEntityTypePlugin = newPlugin(taskService,
                                                                              projectService,
                                                                              nodeHierarchyCreator,
                                                                              sessionProviderService,
                                                                              repositoryService,
                                                                              mock(IdentityRegistry.class),
                                                                              mock(Authenticator.class),
                                                                              true,
                                                                              mock(DocumentSpaceGuard.class));

    // Node does not exist, we return the same attachmentId
    String attachmentId = "123456789Azerty";
    String attachmentName = "testFile.docx";
    assertEquals(1, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", entityId, attachmentId).size());
    assertEquals(attachmentId, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", entityId, attachmentId).get(0));

    // Node exist
    NodeImpl node = mock(NodeImpl.class);
    when(node.getIdentifier()).thenReturn(attachmentId);
    when(node.getName()).thenReturn(attachmentName);
    NodeType nodeType = mock(NodeType.class);
    when(nodeType.getName()).thenReturn("nt:file");
    when(node.getPrimaryNodeType()).thenReturn(nodeType);
    when(extendedSession.getNodeByIdentifier(anyString())).thenReturn(node);
    when(extendedSession.itemExists(anyString())).thenReturn(true);

    // Return original node ID if it is not under space and user can not access the group's target folder
    when(node.getPath()).thenReturn("/Users/user1/documents/testFile.docx");
    assertEquals(1, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).size());
    assertEquals(attachmentId, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).get(0));

    // Create different nodes
    NodeImpl rootNode = mock(NodeImpl.class);
    NodeImpl taskParentNode = mock(NodeImpl.class);
    NodeImpl taskNode = mock(NodeImpl.class);
    NodeImpl linkNode = mock(NodeImpl.class);
    String linkNodeIdentifier = "link_identifier_123456789Azerty";
    SessionImpl session = mock(SessionImpl.class);
    WorkspaceImpl workspace = mock(WorkspaceImpl.class);
    when(workspace.getName()).thenReturn("collaboration");
    when(session.getWorkspace()).thenReturn(workspace);
    when(node.getSession()).thenReturn(session);
    when(node.getPath()).thenReturn("/Groups/spaces/spaceOne/documents/testFile.docx");
    when(linkNode.getIdentifier()).thenReturn(linkNodeIdentifier);
    when(taskNode.addNode(anyString(), anyString())).thenReturn(linkNode);
    when(taskParentNode.getNode(String.valueOf(anyLong()))).thenReturn(taskNode);
    when(taskParentNode.addNode(String.valueOf(entityId), NT_FOLDER)).thenReturn(taskNode);
    when(rootNode.addNode("task", NT_FOLDER)).thenReturn(taskParentNode);
    when(extendedSession.getItem(anyString())).thenReturn(rootNode);

    // Will return link ID instead of the original attached file
    assertEquals(1, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).size());
    assertEquals(linkNodeIdentifier, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).get(0));


    when(projectService.getParticipator(anyLong())).thenReturn(new HashSet<>(Arrays.asList("user1",
            "/platform/users", "member:/spaces/space1", "member:/spaces/spaceOne")));

    // Will return link ID instead of the original attached file
    assertEquals(2, taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).size());
    assertTrue(taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).contains(attachmentId));
    assertTrue(taskAttachmentEntityTypePlugin.getlinkedAttachments("task", 1, attachmentId).contains(linkNodeIdentifier));

  }

  /**
   * "Limit Access Management" of the attachment's space refuses the user
   * attaching it: the task's audience gets no access to it, neither a grant on
   * the file nor a symlink in its space. Mutant: dropping the decision lets
   * the grant and the symlink through.
   */
  public void testNoGrantNorSymlinkForAMemberTheDocumentSpaceRefuses() throws Exception {
    Fixture fixture = new Fixture("requester", false);
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("file-id"), linked);
    verify(fixture.node, never()).setPermission(anyString(), any(String[].class));
    verify(fixture.linkNode, never()).setPermission(anyString(), any(String[].class));
  }

  /**
   * A grant to the document's own space is decided too: on a document its
   * space restricted, a read grant to the whole space is a new access.
   * Mutant: exempting the own space writes the grant.
   */
  public void testAGrantToTheDocumentsOwnSpaceIsDecidedToo() throws Exception {
    Fixture fixture = new Fixture("requester", false);
    when(fixture.node.getPath()).thenReturn("/Groups/spaces/alpha/Documents/manager-file.docx");
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("file-id"), linked);
    verify(fixture.node, never()).setPermission(anyString(), any(String[].class));
  }

  /**
   * A document of the task's own space is linked itself, with no symlink, and
   * read by the space when its decision allows it. Mutant: deciding the own
   * space on anything but the document's space creates a symlink.
   */
  public void testADocumentOfTheTaskSpaceIsLinkedItself() throws Exception {
    Fixture fixture = new Fixture("requester", true);
    when(fixture.node.getPath()).thenReturn("/Groups/spaces/alpha/Documents/manager-file.docx");
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("file-id"), linked);
    verify(fixture.node).setPermission(eq("member:/spaces/alpha"), any(String[].class));
    verify(fixture.linkNode, never()).setPermission(anyString(), any(String[].class));
  }

  /**
   * The grants are decided on behalf of the user the attachment service names,
   * never one read from the thread. Mutant: deciding for the thread's user, or
   * for none, fails the verify.
   */
  public void testTheGrantsAreDecidedForTheUserLinkingTheAttachment() throws Exception {
    Fixture fixture = new Fixture("requester", true);
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    fixture.plugin.getlinkedAttachments("task", 7, "file-id", "mary");

    verify(fixture.guard).newGrantOperation("mary", DocumentGrantOperation.SOURCE_TASK);
  }

  /**
   * A document of another space is not of the task's space because a folder
   * of its drive is named after that space: the task's space gets a symlink
   * to it. Mutant: deciding the own space on the path's text links the
   * document itself.
   */
  public void testADocumentUnderAFolderNamedAfterTheTaskSpaceIsNotOfThatSpace() throws Exception {
    Fixture fixture = new Fixture("requester", true);
    when(fixture.node.getPath()).thenReturn("/Groups/spaces/alpha/Documents/spaces/beta/manager-file.docx");
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/beta")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
  }

  /**
   * An identity the document's access list already holds needs no grant:
   * nothing is decided for it, so no skipped grant is logged, and its space
   * gets its link. Mutant: deciding before checking the access list asks the
   * space, which refuses, and links nothing.
   */
  public void testAnIdentityThatAlreadyReadsTheDocumentNeedsNoDecision() throws Exception {
    Fixture fixture = new Fixture("requester", false);
    when(fixture.node.isNodeType(EXO_PRIVILEGEABLE)).thenReturn(true);
    when(fixture.node.getACL()).thenReturn(new AccessControlList("manager",
                                                                 List.of(new AccessControlEntry("member:/spaces/alpha",
                                                                                                PermissionType.READ))));
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
    DocumentGrantOperation grantOperation = fixture.guard.newGrantOperation("requester", DocumentGrantOperation.SOURCE_TASK);
    verify(grantOperation, never()).canGrant(any(), eq("member:/spaces/alpha"));
    verify(fixture.node, never()).setPermission(anyString(), any(String[].class));
  }

  /**
   * A document without an access list of its own reads its folder's: an
   * identity the folder's list holds is not exempt from the decision, and a
   * refusal writes and saves nothing, the folder's entries included. Mutant:
   * counting the entries of a document without its own list links the
   * document and makes them its own.
   */
  public void testAnEntryReadFromTheFolderDoesNotExemptAnIdentityFromTheDecision() throws Exception {
    Fixture fixture = new Fixture(null, false);
    inheritsItsFolderList(fixture.node, "member:/spaces/alpha");
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("file-id"), linked);
    DocumentGrantOperation grantOperation = fixture.guard.newGrantOperation(null, DocumentGrantOperation.SOURCE_TASK);
    verify(grantOperation).canGrant(fixture.node, "member:/spaces/alpha");
    verify(fixture.node, never()).addMixin(EXO_PRIVILEGEABLE);
    verify(fixture.node, never()).setPermission(anyString(), any(String[].class));
    verify(fixture.node, never()).save();
  }

  /**
   * The first grant gives the document an access list of its own, which
   * copies its folder's entries: which identities the document holds is read
   * before it, so the next identity is still decided. Mutant: reading it in
   * the loop, after the first grant, lets the refused identity through.
   */
  public void testTheIdentitiesHeldAreReadBeforeTheFirstGrant() throws Exception {
    Fixture fixture = new Fixture(null, false);
    Set<String> participants = new HashSet<>(List.of("member:/spaces/alpha", "member:/spaces/beta"));
    Iterator<String> order = participants.iterator();
    String allowed = order.next();
    String refused = order.next();
    inheritsItsFolderList(fixture.node, allowed, refused);
    DocumentGrantOperation grantOperation = fixture.guard.newGrantOperation(null, DocumentGrantOperation.SOURCE_TASK);
    when(grantOperation.canGrant(fixture.node, allowed)).thenReturn(true);
    when(fixture.projectService.getParticipator(3L)).thenReturn(participants);

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
    verify(grantOperation).canGrant(fixture.node, refused);
  }

  /**
   * The node has no access list of its own and reads one holding each
   * identity's READ entry; adding its own makes it privilegeable.
   */
  private static void inheritsItsFolderList(NodeImpl node, String... identities) throws Exception {
    AtomicBoolean privilegeable = new AtomicBoolean(false);
    when(node.isNodeType(EXO_PRIVILEGEABLE)).thenAnswer(invocation -> privilegeable.get());
    when(node.canAddMixin(EXO_PRIVILEGEABLE)).thenAnswer(invocation -> !privilegeable.get());
    doAnswer(invocation -> {
      privilegeable.set(true);
      return null;
    }).when(node).addMixin(EXO_PRIVILEGEABLE);
    when(node.getACL()).thenReturn(new AccessControlList("manager",
                                                         Arrays.stream(identities)
                                                               .map(identity -> new AccessControlEntry(identity,
                                                                                                       PermissionType.READ))
                                                               .toList()));
  }

  /**
   * The requester of a process, creator of the task, is not a participant of
   * the project's space: a file a manager attaches from their personal drive
   * stays unreadable for them unless the plugin grants it, on the file and on
   * the symlink it creates in the space. Mutants: dropping the creator from
   * the permitted identities, or the grant on the symlink, fails the matching
   * verify.
   */
  public void testTaskCreatorCanReadTheAttachmentAndItsSymlink() throws Exception {
    Fixture fixture = new Fixture("requester");
    Set<String> participators = new HashSet<>(List.of("member:/spaces/alpha"));
    when(fixture.projectService.getParticipator(3L)).thenReturn(participators);

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
    // the project's space reads the file and its symlink, as before
    verify(fixture.node).setPermission(eq("member:/spaces/alpha"), aryEq(new String[] { PermissionType.READ }));
    verify(fixture.linkNode).setPermission(eq("member:/spaces/alpha"), aryEq(new String[] { PermissionType.READ }));
    // the task creator, outside the space, reads them too
    verify(fixture.node).setPermission(eq("requester"), aryEq(new String[] { PermissionType.READ }));
    verify(fixture.linkNode).setPermission(eq("requester"), aryEq(new String[] { PermissionType.READ }));
    // the participants the project service holds are not modified
    assertEquals(Set.of("member:/spaces/alpha"), participators);
  }

  /**
   * A task with no creator grants nothing beyond the project participants.
   */
  public void testTaskWithoutCreatorGrantsNoExtraPermission() throws Exception {
    Fixture fixture = new Fixture(null);
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
    verify(fixture.node).setPermission(eq("member:/spaces/alpha"), aryEq(new String[] { PermissionType.READ }));
    verify(fixture.node, never()).setPermission(isNull(), any());
    verify(fixture.linkNode, never()).setPermission(isNull(), any());
  }

  /**
   * A creator who is a member of a participant space already reads the file
   * and its symlink through that membership: no personal entry is added, so
   * that leaving the space still revokes their access. Mutant: without the
   * coverage check the creator is granted like a requester.
   */
  public void testCreatorCoveredByAParticipantGroupGetsNoPersonalGrant() throws Exception {
    Fixture fixture = new Fixture("spacemember", new Identity("spacemember", List.of(new MembershipEntry("/spaces/alpha", "member"))));
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    List<String> linked = fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    assertEquals(List.of("link-id"), linked);
    verify(fixture.node).setPermission(eq("member:/spaces/alpha"), aryEq(new String[] { PermissionType.READ }));
    verify(fixture.node, never()).setPermission(eq("spacemember"), any());
    verify(fixture.linkNode, never()).setPermission(eq("spacemember"), any());
  }

  /**
   * A creator the registry does not hold (not logged in) is resolved through
   * the authenticator; a creator nobody can resolve is granted, since the ACL
   * plugin admits them whatever their memberships.
   */
  public void testCreatorMembershipsComeFromTheAuthenticatorWhenNotRegistered() throws Exception {
    Fixture fixture = new Fixture("offline", null);
    when(fixture.authenticator.createIdentity("offline")).thenReturn(new Identity("offline",
                                                                                  List.of(new MembershipEntry("/spaces/alpha", "member"))));
    when(fixture.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    fixture.plugin.getlinkedAttachments("task", 7, "file-id");

    verify(fixture.node, never()).setPermission(eq("offline"), any());

    Fixture unresolved = new Fixture("unknown", null);
    when(unresolved.projectService.getParticipator(3L)).thenReturn(new HashSet<>(List.of("member:/spaces/alpha")));

    unresolved.plugin.getlinkedAttachments("task", 7, "file-id");

    verify(unresolved.node).setPermission(eq("unknown"), aryEq(new String[] { PermissionType.READ }));
  }

  /**
   * A task of project 3 whose attachment "file-id" sits in a personal drive,
   * so that the plugin creates the symlink "link-id" in the space's Documents
   */
  private static class Fixture {
    final ProjectService                 projectService = mock(ProjectService.class);

    final NodeImpl                       node           = mock(NodeImpl.class);

    final NodeImpl                       linkNode       = mock(NodeImpl.class);

    final Authenticator                  authenticator  = mock(Authenticator.class);

    final TaskAttachmentEntityTypePlugin plugin;

    final IdentityRegistry               identityRegistry = mock(IdentityRegistry.class);

    final boolean grantAllowed;

    final DocumentSpaceGuard             guard            = mock(DocumentSpaceGuard.class);

    Fixture(String taskCreator) throws Exception {
      this(taskCreator, taskCreator == null ? null : new Identity(taskCreator));
    }

    Fixture(String taskCreator, boolean grantAllowed) throws Exception {
      this(taskCreator, taskCreator == null ? null : new Identity(taskCreator), grantAllowed);
    }

    /**
     * @param taskCreator the task's creator
     * @param creatorIdentity the creator's security identity, as the registry
     *          returns it (null for a user it does not hold)
     */
    Fixture(String taskCreator, Identity creatorIdentity) throws Exception {
      this(taskCreator, creatorIdentity, true);
    }

    Fixture(String taskCreator, Identity creatorIdentity, boolean grantAllowed) throws Exception {
      this.grantAllowed = grantAllowed;
      TaskService taskService = mock(TaskService.class);
      NodeHierarchyCreator nodeHierarchyCreator = mock(NodeHierarchyCreator.class);
      RepositoryService repositoryService = mock(RepositoryService.class);
      SessionProviderService sessionProviderService = mock(SessionProviderService.class);
      SessionProvider sessionProvider = mock(SessionProvider.class);
      when(sessionProviderService.getSessionProvider(null)).thenReturn(sessionProvider);
      ManageableRepository repository = mock(ManageableRepository.class);
      when(repositoryService.getCurrentRepository()).thenReturn(repository);
      RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
      when(repository.getConfiguration()).thenReturn(repositoryEntry);
      when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
      ExtendedSession extendedSession = mock(ExtendedSession.class);
      when(sessionProvider.getSession(any(), any())).thenReturn(extendedSession);

      TaskDto task = new TaskDto();
      task.setId(7);
      task.setCreatedBy(taskCreator);
      ProjectDto project = new ProjectDto();
      project.setId(3);
      StatusDto status = new StatusDto();
      status.setProject(project);
      task.setStatus(status);
      when(taskService.getTask(7)).thenReturn(task);

      when(node.getIdentifier()).thenReturn("file-id");
      when(node.getName()).thenReturn("manager-file.docx");
      when(node.getPath()).thenReturn("/Users/m___/ma___/man___/manager/Private/Documents/manager-file.docx");
      NodeType nodeType = mock(NodeType.class);
      when(nodeType.getName()).thenReturn("nt:file");
      when(node.getPrimaryNodeType()).thenReturn(nodeType);
      SessionImpl session = mock(SessionImpl.class);
      WorkspaceImpl workspace = mock(WorkspaceImpl.class);
      when(workspace.getName()).thenReturn("collaboration");
      when(session.getWorkspace()).thenReturn(workspace);
      when(node.getSession()).thenReturn(session);
      when(extendedSession.getNodeByIdentifier("file-id")).thenReturn(node);
      when(extendedSession.itemExists(anyString())).thenReturn(true);

      NodeImpl rootNode = mock(NodeImpl.class);
      NodeImpl taskParentNode = mock(NodeImpl.class);
      NodeImpl taskNode = mock(NodeImpl.class);
      when(linkNode.getIdentifier()).thenReturn("link-id");
      when(taskNode.addNode(anyString(), anyString())).thenReturn(linkNode);
      when(taskParentNode.addNode("7", NT_FOLDER)).thenReturn(taskNode);
      when(rootNode.addNode("task", NT_FOLDER)).thenReturn(taskParentNode);
      when(extendedSession.getItem(anyString())).thenReturn(rootNode);

      if (taskCreator != null) {
        when(identityRegistry.getIdentity(taskCreator)).thenReturn(creatorIdentity);
      }
      plugin = newPlugin(taskService,
                         projectService,
                         nodeHierarchyCreator,
                         sessionProviderService,
                         repositoryService,
                         identityRegistry,
                         authenticator,
                         grantAllowed,
                         guard);
    }
  }

  private static TaskAttachmentEntityTypePlugin newPlugin(TaskService taskService, // NOSONAR
                                                          ProjectService projectService,
                                                          NodeHierarchyCreator nodeHierarchyCreator,
                                                          SessionProviderService sessionProviderService,
                                                          RepositoryService repositoryService,
                                                          IdentityRegistry identityRegistry,
                                                          Authenticator authenticator,
                                                          boolean grantAllowed,
                                                          DocumentSpaceGuard guard) {
    DocumentGrantOperation grantOperation = mock(DocumentGrantOperation.class);
    lenient().when(guard.newGrantOperation(any(), anyString())).thenReturn(grantOperation);
    lenient().when(grantOperation.canGrant(any(), anyString())).thenReturn(grantAllowed);
    try {
      // the space of the drive, from the stored path's first spaces segment, as DocumentSpaceGuardTest pins it
      lenient().when(guard.getDocumentSpace(any())).thenAnswer(invocation -> {
        String path = ((Node) invocation.getArgument(0)).getPath();
        if (!path.startsWith("/Groups/spaces/")) {
          return null;
        }
        Space space = new Space();
        space.setGroupId("/spaces/" + path.substring("/Groups/spaces/".length()).split("/")[0]);
        return space;
      });
    } catch (RepositoryException e) {
      throw new IllegalStateException(e);
    }
    return new TaskAttachmentEntityTypePlugin(taskService,
                                              projectService,
                                              nodeHierarchyCreator,
                                              sessionProviderService,
                                              repositoryService,
                                              identityRegistry,
                                              authenticator) {
      @Override
      protected DocumentSpaceGuard getDocumentSpaceGuard() {
        return guard;
      }
    };
  }

}
