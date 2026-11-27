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
package org.exoplatform.services.attachments.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.storage.AttachmentStorage;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.social.core.space.model.Space;

/**
 * A change of an article's permissions gives its attachments the read right of
 * its spaces and of all users without taking away any other right those
 * identities hold on them, on real JCR nodes.
 */
public class TestUpdateContentPermissionsListenerAccessList extends BaseWCMTestCase {

  private static final String   SPACE_GROUP = "/spaces/test";

  private static final String   SPACE_ENTRY = "*:" + SPACE_GROUP;

  private static final String   ENTITY_TYPE = "WIKI_DRAFT_PAGES";

  private static final String[] EDIT        = { PermissionType.READ, PermissionType.ADD_NODE,
      PermissionType.SET_PROPERTY, PermissionType.REMOVE };

  private static final Set<String> EDIT_SET = Set.of(EDIT);

  private Node                  folder;

  private Node                  inherited;

  private Node                  own;

  private Node                  partial;

  private AttachmentStorage     attachmentStorage;

  private UpdateContentPermissionsListener listener;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    applySystemSession();
    folder = session.getRootNode().addNode("permissionsListenerFolder", "nt:unstructured");
    folder.addMixin("exo:privilegeable");
    ((ExtendedNode) folder).setPermissions(Map.of(SPACE_ENTRY, EDIT));
    // a file that reads its folder's access list, and one that holds its own
    inherited = addFile(folder, "inherited.txt");
    own = addFile(folder, "own.txt");
    own.addMixin("exo:privilegeable");
    ((ExtendedNode) own).setPermissions(Map.of(SPACE_ENTRY, EDIT));
    // a file whose own list gives the space every right but the read one
    partial = addFile(folder, "partial.txt");
    partial.addMixin("exo:privilegeable");
    ((ExtendedNode) partial).setPermissions(Map.of(SPACE_ENTRY,
                                                   new String[] { PermissionType.ADD_NODE, PermissionType.SET_PROPERTY,
                                                       PermissionType.REMOVE }));
    session.save();
    attachmentStorage = mock(AttachmentStorage.class);
    when(attachmentStorage.getAttachmentsByEntity(1L, ENTITY_TYPE)).thenReturn(List.of(attachment(inherited.getPath()),
                                                                                       attachment(own.getPath()),
                                                                                       attachment(partial.getPath())));
    listener = new UpdateContentPermissionsListener(repositoryService, attachmentStorage);
  }

  @Override
  public void tearDown() throws Exception {
    session.refresh(false);
    session.getRootNode().getNode("permissionsListenerFolder").remove();
    session.save();
    super.tearDown();
  }

  public void testAnEventWithoutAudienceKeepsTheSpaceMembersEditRight() throws Exception {
    listener.onEvent(event(null));

    session.refresh(false);
    Node inheritedNode = session.getRootNode().getNode("permissionsListenerFolder/inherited.txt");
    Node ownNode = session.getRootNode().getNode("permissionsListenerFolder/own.txt");
    // the space already reads both files: nothing is written, and the first one keeps reading its folder's list
    assertFalse(inheritedNode.isNodeType("exo:privilegeable"));
    assertEquals(EDIT_SET, permissionsOf(inheritedNode, SPACE_ENTRY));
    assertEquals(EDIT_SET, permissionsOf(ownNode, SPACE_ENTRY));
    assertTrue(permissionsOf(ownNode, "any").isEmpty());
    // the read right is added to the rights the space holds, not put in their place
    Node partialNode = session.getRootNode().getNode("permissionsListenerFolder/partial.txt");
    assertEquals(EDIT_SET, permissionsOf(partialNode, SPACE_ENTRY));
  }

  public void testAnAllUsersAudienceAddsTheirReadRightAndKeepsTheSpaceMembersEditRight() throws Exception {
    listener.onEvent(event("all"));

    session.refresh(false);
    for (String name : List.of("inherited.txt", "own.txt", "partial.txt")) {
      Node node = session.getRootNode().getNode("permissionsListenerFolder/" + name);
      assertEquals(name, EDIT_SET, permissionsOf(node, SPACE_ENTRY));
      assertEquals(name, Set.of(PermissionType.READ), permissionsOf(node, "any"));
    }
  }

  public void testASpaceAudienceTakesAwayTheAllUsersReadRightAndKeepsTheSpaceMembersEditRight() throws Exception {
    // a file that reads its folder's list, which gives all users the read right
    Node publicFolder = folder.addNode("public", "nt:unstructured");
    publicFolder.addMixin("exo:privilegeable");
    ((ExtendedNode) publicFolder).setPermissions(Map.of(SPACE_ENTRY, EDIT, "any", new String[] { PermissionType.READ }));
    Node publicFile = addFile(publicFolder, "public.txt");
    session.save();
    when(attachmentStorage.getAttachmentsByEntity(1L, ENTITY_TYPE)).thenReturn(List.of(attachment(publicFile.getPath()),
                                                                                       attachment(inherited.getPath())));

    listener.onEvent(event("space"));

    session.refresh(false);
    Node publicNode = session.getRootNode().getNode("permissionsListenerFolder/public/public.txt");
    assertTrue(publicNode.isNodeType("exo:privilegeable"));
    assertTrue(permissionsOf(publicNode, "any").isEmpty());
    assertEquals(EDIT_SET, permissionsOf(publicNode, SPACE_ENTRY));
    // a file that gives all users nothing is left reading its folder's list
    Node inheritedNode = session.getRootNode().getNode("permissionsListenerFolder/inherited.txt");
    assertFalse(inheritedNode.isNodeType("exo:privilegeable"));
    assertEquals(EDIT_SET, permissionsOf(inheritedNode, SPACE_ENTRY));
  }

  private Event<Object, Map<String, Object>> event(String audience) {
    Space space = new Space();
    space.setGroupId(SPACE_GROUP);
    Map<String, Object> data = new HashMap<>();
    data.put("entityId", "1");
    data.put("entityType", ENTITY_TYPE);
    data.put("spaces", List.of(space));
    if (audience != null) {
      data.put("audience", audience);
    }
    return new Event<>("content.update.permissions", null, data);
  }

  private Set<String> permissionsOf(Node node, String identity) throws Exception {
    return Set.copyOf(((ExtendedNode) node).getACL().getPermissions(identity));
  }

  private Node addFile(Node parent, String name) throws Exception {
    Node file = parent.addNode(name, "nt:file");
    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/plain");
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    resource.setProperty("jcr:data", new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8)));
    return file;
  }

  private Attachment attachment(String id) {
    Attachment attachment = new Attachment();
    attachment.setId(id);
    return attachment;
  }
}
