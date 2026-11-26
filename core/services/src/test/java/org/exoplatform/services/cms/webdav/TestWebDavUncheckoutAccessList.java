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
package org.exoplatform.services.cms.webdav;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecms.test.DumpThreadLocalSessionProviderService;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;


/**
 * Cancelling the checkout of a document over the legacy WebDAV endpoint leaves
 * its access list as it is.
 */
public class TestWebDavUncheckoutAccessList extends BaseWCMTestCase {

  private static final String   MEMBER  = "member";

  private static final String   EDITOR  = "editor";

  private static final String   CANCELLER = "john";

  private static final String[] EDIT    = new String[] { PermissionType.READ, PermissionType.ADD_NODE,
      PermissionType.SET_PROPERTY, PermissionType.REMOVE };

  private static final String[] READ    = new String[] { PermissionType.READ };

  private Node                  usersRoot;

  private boolean               usersRootCreated;

  private Node                  folder;

  private WebDavServiceImpl     service;

  private ManageableRepository  currentRepository;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    usersRoot = session.getRootNode().hasNode("Users") ? session.getRootNode().getNode("Users") : null;
    if (usersRoot == null) {
      usersRoot = session.getRootNode().addNode("Users", "nt:unstructured");
      usersRootCreated = true;
    }
    folder = usersRoot.addNode("uncheckoutFolder", "nt:unstructured");
    session.save();
    currentRepository = repositoryService.getCurrentRepository();
    service = newService(repositoryService);
    ConversationState.setCurrent(new ConversationState(new Identity(CANCELLER)));
  }

  @Override
  public void tearDown() throws Exception {
    ConversationState.setCurrent(null);
    applySystemSession();
    session.refresh(false);
    session.getRootNode().getNode("Users/uncheckoutFolder").remove();
    if (usersRootCreated) {
      session.getRootNode().getNode("Users").remove();
    }
    session.save();
    super.tearDown();
  }

  public void testADocumentWithoutAListOfItsOwnStillReadsItsFoldersList() throws Exception {
    // The canceller edits the document through its folder's list
    folder.addMixin("exo:privilegeable");
    ((ExtendedNode) folder).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    Node document = addVersionedFile("no-own-list.txt");
    document.checkin();
    document.checkout();
    String uuid = document.getUUID();

    Response response = cancel("no-own-list.txt");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(uuid);
    assertFalse(cancelled.isCheckedOut());
    assertFalse(cancelled.isNodeType("exo:privilegeable"));
    assertEquals(entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder")), entriesOf(cancelled));
  }

  public void testANonVersionableDocumentStillAnswersConflict() throws Exception {
    Node file = folder.addNode("not-versionable.txt", "nt:file");
    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/plain");
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    resource.setProperty("jcr:data", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));
    session.save();

    Response response = cancel("not-versionable.txt");

    assertEquals(409, response.getStatus());
  }

  public void testAnAddedShareIsKept() throws Exception {
    Node document = addVersionedFile("share-added.txt");
    document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();

    String uuid = document.getUUID();
    Response response = cancel("share-added.txt");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(uuid);
    assertFalse(cancelled.isCheckedOut());
    assertTrue(cancelled.isNodeType("exo:privilegeable"));
    assertEquals(Set.of(CANCELLER + " read", CANCELLER + " add_node", CANCELLER + " set_property", CANCELLER + " remove",
                        MEMBER + " read"),
                 entriesOf(cancelled));
  }

  public void testARevokedShareStaysRevoked() throws Exception {
    Node document = addVersionedFile("share-revoked.txt");
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    document.checkin();
    document.checkout();
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, EDITOR, READ));
    session.save();

    String uuid = document.getUUID();
    Response response = cancel("share-revoked.txt");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(uuid);
    assertFalse(cancelled.isCheckedOut());
    Set<String> entries = entriesOf(cancelled);
    assertTrue(entries.contains(EDITOR + " read"));
    assertFalse(entries.contains(MEMBER + " read"));
  }

  public void testARemovedPrivateListStaysRemoved() throws Exception {
    Node document = addVersionedFile("list-removed.txt");
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    document.checkin();
    document.checkout();
    document.removeMixin("exo:privilegeable");
    session.save();

    String uuid = document.getUUID();
    Response response = cancel("list-removed.txt");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(uuid);
    assertFalse(cancelled.isCheckedOut());
    assertFalse(cancelled.isNodeType("exo:privilegeable"));
    assertEquals(entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder")), entriesOf(cancelled));
  }

  public void testACancellerGrantedOnlyByTheCurrentListKeepsTheAccess() throws Exception {
    Node document = addVersionedFile("granted-later.txt");
    document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, EDITOR, READ));
    session.save();

    String uuid = document.getUUID();
    Response response = cancel("granted-later.txt");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(uuid);
    assertTrue(cancelled.isNodeType("exo:privilegeable"));
    assertTrue(entriesOf(cancelled).containsAll(Set.of(CANCELLER + " add_node", CANCELLER + " set_property", EDITOR + " read")));
  }

  public void testADocumentLockedByAnotherUserAnswersLockedAndKeepsItsList() throws Exception {
    Node document = addVersionedFile("locked.txt");
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    document.checkin();
    document.checkout();
    document.addMixin("mix:lockable");
    session.save();
    document.lock(true, false);
    Set<String> before = entriesOf(document);

    String uuid = document.getUUID();
    Response response = cancel("locked.txt");

    assertEquals(423, response.getStatus());
    Node locked = reload(uuid);
    assertTrue(locked.isCheckedOut());
    assertEquals(before, entriesOf(locked));
  }

  public void testAFailureWhilePuttingTheListBackIsLoggedAndRethrown() throws Exception {
    Node document = addVersionedFile("put-back-fails.txt");
    String uuid = document.getUUID();
    document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    // The first system session reads the list, the second one has to put it back
    ManageableRepository failingRepository = spy(currentRepository);
    AtomicInteger systemSessions = new AtomicInteger();
    doAnswer(invocation -> {
      if (systemSessions.incrementAndGet() > 1) {
        throw new RepositoryException("refused");
      }
      return invocation.callRealMethod();
    }).when(failingRepository).getSystemSession(anyString());
    org.exoplatform.services.jcr.RepositoryService failingService = spy(repositoryService);
    doAnswer(invocation -> failingRepository).when(failingService).getCurrentRepository();
    WebDavServiceImpl failingWebDav = newService(failingService);
    Logger logger = (Logger) LoggerFactory.getLogger(WebDavServiceImpl.class.getName());
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    Level level = logger.getLevel();
    logger.setLevel(Level.ERROR);

    applyUserSession(CANCELLER, "gtn", COLLABORATION_WS);
    try {
      failingWebDav.uncheckout(REPO_NAME, "collaboration/Users/uncheckoutFolder/put-back-fails.txt", null, null);
      fail("The failure must reach the caller");
    } catch (IllegalStateException e) {
      assertEquals("refused", e.getCause().getMessage());
    } finally {
      applySystemSession();
      logger.detachAppender(appender);
      logger.setLevel(level);
    }
    assertTrue(appender.list.stream()
                            .anyMatch(event -> event.getLevel() == Level.ERROR
                                && event.getFormattedMessage().contains(uuid)
                                && event.getFormattedMessage().contains("access list could not be kept")));
  }

  /**
   * The cancel has to add the private-access mixin, so it checks the document
   * out; the write that fails comes after the document's own list is saved.
   */
  public void testAFailureAfterTheDocumentsOwnListIsSavedChecksTheDocumentInAgain() throws Exception {
    assertACancelFailingOnSaveChecksTheDocumentInAgain("checked-in-again.txt", 2);
  }

  /**
   * The document's own save is the one refused: its mixin and list are still
   * pending, which the check-in refuses until they are dropped.
   */
  public void testAFailureOnTheDocumentsOwnSaveChecksTheDocumentInAgain() throws Exception {
    assertACancelFailingOnSaveChecksTheDocumentInAgain("checked-in-again-own-save.txt", 1);
  }

  private void assertACancelFailingOnSaveChecksTheDocumentInAgain(String fileName, int refusedSave) throws Exception {
    Node document = addVersionedFile(fileName);
    String uuid = document.getUUID();
    document.checkin();
    document.checkout();
    document.addMixin("exo:privilegeable");
    ((ExtendedNode) document).setPermissions(Map.of(CANCELLER, EDIT, MEMBER, READ));
    session.save();
    // The first system session reads the lists; in the second one, the refusedSave-th save is refused
    ManageableRepository failingRepository = spy(currentRepository);
    AtomicInteger systemSessions = new AtomicInteger();
    AtomicInteger depth = new AtomicInteger();
    doAnswer(invocation -> {
      SessionImpl systemSession;
      depth.incrementAndGet();
      try {
        systemSession = (SessionImpl) invocation.callRealMethod();
      } finally {
        depth.decrementAndGet();
      }
      // the repository's own calls to its overload are not requests for a session
      if (depth.get() > 0 || systemSessions.incrementAndGet() == 1) {
        return systemSession;
      }
      SessionImpl failingSession = spy(systemSession);
      AtomicInteger saves = new AtomicInteger();
      doAnswer(save -> {
        if (saves.incrementAndGet() == refusedSave) {
          throw new RepositoryException("refused");
        }
        return save.callRealMethod();
      }).when(failingSession).save();
      return failingSession;
    }).when(failingRepository).getSystemSession(anyString());
    org.exoplatform.services.jcr.RepositoryService failingService = spy(repositoryService);
    doAnswer(invocation -> failingRepository).when(failingService).getCurrentRepository();
    WebDavServiceImpl failingWebDav = newService(failingService);
    Logger logger = (Logger) LoggerFactory.getLogger(WebDavServiceImpl.class.getName());
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    Level level = logger.getLevel();
    logger.setLevel(Level.ERROR);

    applyUserSession(CANCELLER, "gtn", COLLABORATION_WS);
    try {
      failingWebDav.uncheckout(REPO_NAME, "collaboration/Users/uncheckoutFolder/" + fileName, null, null);
      fail("The failure must reach the caller");
    } catch (IllegalStateException e) {
      assertEquals("refused", e.getCause().getMessage());
    } finally {
      applySystemSession();
      logger.detachAppender(appender);
      logger.setLevel(level);
    }
    assertFalse("the document was left checked out", reload(uuid).isCheckedOut());
    assertTrue(appender.list.stream()
                            .anyMatch(event -> event.getLevel() == Level.ERROR
                                && event.getFormattedMessage().contains(uuid)
                                && event.getFormattedMessage().contains("access list could not be kept")));
  }

  public void testAShareRevokedOnAFileOfAFolderStaysRevokedWhenItsCheckoutIsCancelled() throws Exception {
    Node versionableFolder = addVersionedFolder("revoked-folder");
    Node child = addFileWithList(versionableFolder, "child.txt", Map.of(CANCELLER, PermissionType.ALL, MEMBER, READ));
    Node deeper = addFileWithList(versionableFolder.addNode("sub", "nt:folder"),
                                  "deeper.txt",
                                  Map.of(CANCELLER, PermissionType.ALL, MEMBER, READ));
    String folderUuid = versionableFolder.getUUID();
    versionableFolder.checkin();
    versionableFolder.checkout();
    ((ExtendedNode) child).setPermissions(Map.of(CANCELLER, PermissionType.ALL, EDITOR, READ));
    ((ExtendedNode) deeper).setPermissions(Map.of(CANCELLER, PermissionType.ALL, EDITOR, READ));
    session.save();

    Response response = cancel("revoked-folder");

    assertEquals(200, response.getStatus());
    reload(folderUuid);
    Set<String> entries = entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder/revoked-folder/child.txt"));
    assertFalse("the revoked share came back: " + entries, entries.contains(MEMBER + " read"));
    assertTrue(entries.contains(EDITOR + " read"));
    Set<String> deeperEntries = entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder/revoked-folder/sub/deeper.txt"));
    assertFalse("the revoked share came back below: " + deeperEntries, deeperEntries.contains(MEMBER + " read"));
    assertTrue(deeperEntries.contains(EDITOR + " read"));
  }

  public void testAListOfItsOwnGainedByAFileOfAFolderAfterTheCheckoutIsKeptWhenItIsCancelled() throws Exception {
    Node versionableFolder = addVersionedFolder("gained-folder");
    Node child = versionableFolder.addNode("child.txt", "nt:file");
    Node resource = child.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/plain");
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    resource.setProperty("jcr:data", new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8)));
    session.save();
    String folderUuid = versionableFolder.getUUID();
    versionableFolder.checkin();
    versionableFolder.checkout();
    child.addMixin("exo:privilegeable");
    ((ExtendedNode) child).setPermissions(Map.of(CANCELLER, PermissionType.ALL, EDITOR, READ));
    session.save();

    Response response = cancel("gained-folder");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(folderUuid);
    assertTrue(entriesOf(cancelled.getNode("child.txt")).contains(EDITOR + " read"));
    assertFalse(cancelled.isCheckedOut());
  }

  public void testAListOfItsOwnRemovedFromAFileOfAFolderAfterTheCheckoutStaysRemovedWhenItIsCancelled() throws Exception {
    Node versionableFolder = addVersionedFolder("removed-below-folder");
    Node child = addFileWithList(versionableFolder, "child.txt", Map.of(CANCELLER, PermissionType.ALL, MEMBER, READ));
    String folderUuid = versionableFolder.getUUID();
    versionableFolder.checkin();
    versionableFolder.checkout();
    child.removeMixin("exo:privilegeable");
    session.save();

    Response response = cancel("removed-below-folder");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(folderUuid);
    Node cancelledChild = cancelled.getNode("child.txt");
    assertFalse(cancelledChild.isNodeType("exo:privilegeable"));
    assertEquals(entriesOf(cancelled), entriesOf(cancelledChild));
    assertFalse(cancelled.isCheckedOut());
  }

  public void testAListRemovedBelowAFolderThatReadsAChangedListStaysRemovedWhenItIsCancelled() throws Exception {
    Node versionableFolder = addVersionedFolder("stale-parent-folder");
    Node changed = versionableFolder.addNode("changed", "nt:folder");
    changed.addMixin("exo:privilegeable");
    ((ExtendedNode) changed).setPermissions(Map.of(CANCELLER, PermissionType.ALL, "bob", READ));
    session.save();
    Node inheriting = changed.addNode("inheriting", "nt:folder");
    Node removed = addFileWithList(inheriting, "removed.txt", Map.of(CANCELLER, PermissionType.ALL, "bob", READ));
    String folderUuid = versionableFolder.getUUID();
    versionableFolder.checkin();
    versionableFolder.checkout();
    ((ExtendedNode) changed).setPermissions(Map.of(CANCELLER, PermissionType.ALL));
    removed.removeMixin("exo:privilegeable");
    session.save();

    Response response = cancel("stale-parent-folder");

    assertEquals(200, response.getStatus());
    Node cancelled = reload(folderUuid);
    Node cancelledRemoved = cancelled.getNode("changed/inheriting/removed.txt");
    assertFalse(cancelledRemoved.isNodeType("exo:privilegeable"));
    assertFalse("the revoked share came back: " + entriesOf(cancelledRemoved), entriesOf(cancelledRemoved).contains("bob read"));
    assertEquals(entriesOf(cancelled.getNode("changed")), entriesOf(cancelledRemoved));
    assertFalse(cancelled.isCheckedOut());
  }

  public void testAFileOfAFolderKeepsItsListWhenTheCheckoutOfTheFolderIsCancelled() throws Exception {
    Node versionableFolder = addVersionedFolder("untouched-folder");
    Node child = addFileWithList(versionableFolder, "child.txt", Map.of(CANCELLER, PermissionType.ALL, MEMBER, READ));
    Node sub = versionableFolder.addNode("sub", "nt:folder");
    Node deeper = addFileWithList(sub, "deeper.txt", Map.of(CANCELLER, PermissionType.ALL, EDITOR, READ));
    session.save();
    String folderUuid = versionableFolder.getUUID();
    Set<String> childBefore = entriesOf(child);
    Set<String> deeperBefore = entriesOf(deeper);
    versionableFolder.checkin();
    versionableFolder.checkout();

    Response response = cancel("untouched-folder");

    assertEquals(200, response.getStatus());
    reload(folderUuid);
    assertEquals(childBefore, entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder/untouched-folder/child.txt")));
    assertEquals(deeperBefore,
                 entriesOf(session.getRootNode().getNode("Users/uncheckoutFolder/untouched-folder/sub/deeper.txt")));
  }

  private Node addVersionedFolder(String name) throws Exception {
    Node versionableFolder = folder.addNode(name, "nt:folder");
    versionableFolder.addMixin("mix:versionable");
    versionableFolder.addMixin("exo:privilegeable");
    ((ExtendedNode) versionableFolder).setPermissions(Map.of(CANCELLER, PermissionType.ALL));
    session.save();
    return versionableFolder;
  }

  private Node addFileWithList(Node parent, String name, Map<String, String[]> permissions) throws Exception {
    Node file = parent.addNode(name, "nt:file");
    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/plain");
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    resource.setProperty("jcr:data", new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8)));
    file.addMixin("exo:privilegeable");
    ((ExtendedNode) file).setPermissions(permissions);
    session.save();
    return file;
  }

  private WebDavServiceImpl newService(org.exoplatform.services.jcr.RepositoryService jcrRepositoryService) throws Exception {
    AutoVersionService autoVersionService = mock(AutoVersionService.class);
    when(autoVersionService.getDriveAutoVersion()).thenReturn(java.util.List.of());
    return new WebDavServiceImpl(new InitParams(),
                                 jcrRepositoryService,
                                 mock(ExoFeatureService.class),
                                 (DumpThreadLocalSessionProviderService) sessionProviderService_,
                                 container,
                                 WCMCoreUtils.getService(NodeFinder.class),
                                 autoVersionService,
                                 mock(ManageDriveService.class));
  }

  /**
   * The canceller's own session cancels the checkout
   */
  private Response cancel(String name) throws Exception {
    applyUserSession(CANCELLER, "gtn", COLLABORATION_WS);
    try {
      return service.uncheckout(REPO_NAME, "collaboration/Users/uncheckoutFolder/" + name, null, null);
    } finally {
      applySystemSession();
    }
  }

  private Node addVersionedFile(String name) throws Exception {
    Node file = folder.addNode(name, "nt:file");
    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/plain");
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    resource.setProperty("jcr:data", new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8)));
    file.addMixin("mix:versionable");
    session.save();
    return file;
  }

  private Node reload(String uuid) throws Exception {
    applySystemSession();
    session.refresh(false);
    return session.getNodeByUUID(uuid);
  }

  private Set<String> entriesOf(Node node) throws Exception {
    return ((ExtendedNode) node).getACL()
                                .getPermissionEntries()
                                .stream()
                                .map(AccessControlEntry::getAsString)
                                .collect(Collectors.toSet());
  }
}
