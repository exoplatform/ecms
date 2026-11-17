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
package org.exoplatform.services.cms.documents.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;

import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Pins on the repository what the guard reads and writes there: the restore
 * path a trashed document's space is resolved from, read through a system
 * session from the node trashed with it, the access list put back after a
 * version restore, which the next version carries, and the entries a document
 * holds as its own.
 */
public class DocumentSpaceGuardRepositoryTest extends BaseWCMTestCase {

  private TrashService       trashService;

  private DocumentSpaceGuard guard;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    applySystemSession();
    trashService = container.getComponentInstanceOfType(TrashService.class);
    guard = new DocumentSpaceGuard(null,
                                   null,
                                   container.getComponentInstanceOfType(NodeHierarchyCreator.class),
                                   null,
                                   trashService,
                                   repositoryService);
  }

  @Override
  public void tearDown() throws Exception {
    session.logout();
    super.tearDown();
  }

  public void testTheRestorePathOfATrashedFolderIsReadThroughASystemSession() throws Exception {
    Node trashHome = addNewNode("Trash");
    Node testNode = addNewNode("GuardTestNode");
    Node folder = testNode.addNode("folder");
    folder.addNode("a.docx");
    session.save();
    trashService.moveToTrash(folder, sessionProvider, 0);
    session.save();

    String workspace = trashService.getTrashHomeNode().getSession().getWorkspace().getName();
    String trashedPath = trashService.getTrashHomeNode().getPath() + "/folder";
    assertEquals("/GuardTestNode/folder", guard.getRestorePath(workspace, trashedPath));
    assertNull(guard.getRestorePath(workspace, trashService.getTrashHomeNode().getPath() + "/missing"));

    trashHome.remove();
    testNode.remove();
    session.save();
  }

  /**
   * The version restored predates the document's own access list: the restore
   * takes it away, and the guard puts it back.
   */
  public void testAnAccessListTakenAwayByAVersionRestoreIsPutBack() throws Exception {
    Node testNode = addNewNode("GuardVersionNode");
    Node document = versionedDocument(testNode);
    Version firstVersion = document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermission("*:/spaces/team", new String[] { "read" });
    ((ExtendedNode) document).removePermission("any");
    document.save();
    Map<String, List<String>> accessList = guard.getAccessList(document);
    assertEquals(List.of("read"), accessList.get("*:/spaces/team"));

    // the restore leaves the node checked in, with the version's mixins: none
    document.restore(firstVersion, true);
    document = session.getRootNode().getNode("GuardVersionNode/document");
    assertFalse(document.isCheckedOut());
    assertNull(guard.getAccessList(document));
    guard.restoreAccessList(document, accessList);

    assertEquals(accessList, guard.getAccessList(session.getRootNode().getNode("GuardVersionNode/document")));
    testNode.remove();
    session.save();
  }

  /**
   * The version restored has an access list of its own, the document has
   * none: the guard lets it read its parent's again.
   */
  public void testAnAccessListGivenByAVersionRestoreIsTakenAway() throws Exception {
    Node testNode = addNewNode("GuardVersionNode");
    Node document = versionedDocument(testNode);
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermission("*:/spaces/team", new String[] { "read" });
    document.save();
    Version firstVersion = document.checkin();
    document.checkout();
    document.removeMixin("exo:privilegeable");
    document.save();
    assertNull(guard.getAccessList(document));

    document.restore(firstVersion, true);
    document = session.getRootNode().getNode("GuardVersionNode/document");
    assertNotNull(guard.getAccessList(document));
    guard.restoreAccessList(document, null);

    assertNull(guard.getAccessList(session.getRootNode().getNode("GuardVersionNode/document")));
    testNode.remove();
    session.save();
  }

  /**
   * The documents storage restores a version in the user's session, puts the
   * access list back through a system session, then checks the document in
   * from the user's session: the new version carries the list put back, so a
   * later restore of it brings back the kept list, not the one the restore
   * set. Mutant: a put-back the restoring session does not see leaves the
   * version with no access list of its own.
   */
  public void testAListPutBackThroughAnotherSessionReachesTheNextVersion() throws Exception {
    Node testNode = addNewNode("GuardVersionNode");
    Node document = versionedDocument(testNode);
    Version firstVersion = document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermission("*:/spaces/team", new String[] { "read" });
    ((ExtendedNode) document).removePermission("any");
    document.save();
    String uuid = document.getUUID();
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      Session otherSession = systemProvider.getSession(COLLABORATION_WS, repository);
      assertNotSame(session, otherSession);
      Map<String, List<String>> accessList = guard.getAccessList(otherSession.getNodeByUUID(uuid));
      document.restore(firstVersion, true);
      if (!document.isCheckedOut()) {
        document.checkout();
      }
      guard.restoreAccessList(otherSession.getNodeByUUID(uuid), accessList);

      Version nextVersion = document.checkin();

      List<String> versionPermissions = new ArrayList<>();
      for (Value value : nextVersion.getNode("jcr:frozenNode").getProperty("exo:permissions").getValues()) {
        versionPermissions.add(value.getString());
      }
      assertTrue(versionPermissions.toString(), versionPermissions.contains("*:/spaces/team read"));
      assertFalse(versionPermissions.toString(), versionPermissions.contains("any read"));
    } finally {
      systemProvider.close();
    }
    testNode.remove();
    session.save();
  }

  /**
   * A document without an access list of its own reads its folder's: the
   * folder's entry for a target is not the document's, so a grant to it is
   * decided. Giving the document a list of its own copies the folder's entries
   * as its own, which is why the grant sites read what the document holds
   * before adding it. Mutant: an access-list-only check counts the folder's
   * entry.
   */
  public void testAnEntryReadFromTheFolderIsNotHeld() throws Exception {
    Node testNode = addNewNode("GuardHeldNode");
    Node folder = testNode.addNode("folder", "nt:unstructured");
    folder.addMixin("exo:privilegeable");
    ((ExtendedNode) folder).setPermission("*:/spaces/team", new String[] { "read" });
    Node document = folder.addNode("document", "nt:unstructured");
    session.save();
    assertFalse(document.isNodeType("exo:privilegeable"));
    assertTrue(((ExtendedNode) document).getACL().getPermissions("*:/spaces/team").contains("read"));

    assertFalse(DocumentGrantOperation.holdsRead(document, "*:/spaces/team"));

    document.addMixin("exo:privilegeable");
    session.save();
    assertTrue(DocumentGrantOperation.holdsRead(document, "*:/spaces/team"));
    ((ExtendedNode) folder).removePermission("*:/spaces/team");
    session.save();
    assertTrue(DocumentGrantOperation.holdsRead(session.getRootNode().getNode("GuardHeldNode/folder/document"),
                                                "*:/spaces/team"));
    testNode.remove();
    session.save();
  }

  private Node versionedDocument(Node parent) throws Exception {
    Node document = parent.addNode("document", "nt:unstructured");
    document.addMixin("mix:versionable");
    session.save();
    return document;
  }

  private Node addNewNode(String name) throws Exception {
    while (session.getRootNode().hasNode(name)) {
      session.getRootNode().getNode(name).remove();
      session.save();
    }
    Node node = session.getRootNode().addNode(name);
    session.save();
    return node;
  }
}
