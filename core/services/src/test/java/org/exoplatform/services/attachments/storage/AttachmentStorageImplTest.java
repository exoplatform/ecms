/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.attachments.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.services.attachments.dao.AttachmentDAO;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.model.AttachmentContextEntity;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class AttachmentStorageImplTest {

  private static final long                 ENTITY_ID              = 5L;

  private static final String               ENTITY_TYPE            = "EVENT";

  private static final String               WORKSPACE              = "collaboration";

  private final AttachmentDAO               attachmentDAO          = mock(AttachmentDAO.class);

  private final RepositoryService           repositoryService      = mock(RepositoryService.class);

  private final SessionProviderService      sessionProviderService = mock(SessionProviderService.class);

  private final DocumentService             documentService        = mock(DocumentService.class);

  private final LinkManager                 linkManager            = mock(LinkManager.class);

  private final ExtendedSession             systemSession          = mock(ExtendedSession.class);

  private final ExtendedSession             userSession            = mock(ExtendedSession.class);

  private MockedStatic<DMSMimeTypeResolver> mimeTypeResolver;

  private MockedStatic<WCMCoreUtils>        wcmCoreUtils;

  private AttachmentStorage                 attachmentStorage;

  @Before
  public void setUp() throws Exception {
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn(WORKSPACE);
    when(repositoryEntry.getName()).thenReturn("repository");
    SessionProvider systemSessionProvider = mock(SessionProvider.class);
    SessionProvider userSessionProvider = mock(SessionProvider.class);
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(systemSessionProvider);
    when(sessionProviderService.getSessionProvider(any())).thenReturn(userSessionProvider);
    when(systemSessionProvider.getSession(WORKSPACE, repository)).thenReturn(systemSession);
    when(userSessionProvider.getSession(WORKSPACE, repository)).thenReturn(userSession);
    DMSMimeTypeResolver resolver = mock(DMSMimeTypeResolver.class);
    mimeTypeResolver = mockStatic(DMSMimeTypeResolver.class);
    mimeTypeResolver.when(DMSMimeTypeResolver::getInstance).thenReturn(resolver);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    attachmentStorage = new AttachmentStorageImpl(attachmentDAO,
                                                  repositoryService,
                                                  sessionProviderService,
                                                  documentService,
                                                  linkManager);
  }

  @After
  public void tearDown() {
    mimeTypeResolver.close();
    wcmCoreUtils.close();
  }

  /**
   * An attachment that can't be built, a shortcut whose target is gone, a node
   * without content or one gone from the user's session, is left out, and the
   * entity's other attachments are still listed: a readable one in full, an
   * unreadable one as its private entry. A quarantined one stays left out.
   * Mutation: let one of these errors escape, stop the listing at it, list a
   * quarantined node, or turn an unreadable node into an error, and this test
   * fails.
   */
  @Test
  public void getAttachmentsByEntityLeavesOutWhatCantBeBuilt() throws Exception {
    // The newest first: the attachments that can't be built come before the others
    linkedNodes(ENTITY_ID, "dangling", "noContent", "gone", "readable", "unreadable", "quarantined");
    NodeImpl danglingLink = mock(NodeImpl.class);
    when(userSession.getNodeByIdentifier("dangling")).thenReturn(danglingLink);
    when(linkManager.isLink(danglingLink)).thenReturn(true);
    when(linkManager.getTarget(danglingLink)).thenThrow(new ItemNotFoundException("target"));
    NodeImpl noContent = titledNode("noContent");
    when(noContent.getNode("jcr:content")).thenThrow(new PathNotFoundException("jcr:content"));
    when(userSession.getNodeByIdentifier("noContent")).thenReturn(noContent);
    when(userSession.getNodeByIdentifier("gone")).thenThrow(new ItemNotFoundException("gone"));
    NodeImpl readable = buildableNode("readable");
    when(userSession.getNodeByIdentifier("readable")).thenReturn(readable);
    when(userSession.getNodeByIdentifier("unreadable")).thenThrow(new AccessDeniedException("read"));
    NodeImpl quarantined = buildableNode("quarantined");
    when(userSession.getNodeByIdentifier("quarantined")).thenReturn(quarantined);

    List<Attachment> attachments = attachmentStorage.getAttachmentsByEntity(ENTITY_ID, ENTITY_TYPE);

    Map<String, Attachment> attachmentsById = attachments.stream()
                                                         .collect(Collectors.toMap(Attachment::getId, Function.identity()));
    assertEquals(new HashSet<>(Arrays.asList("readable", "unreadable")), attachmentsById.keySet());
    assertTrue(attachmentsById.get("readable").getAcl().isCanAccess());
    assertEquals("readable.pdf", attachmentsById.get("readable").getTitle());
    assertFalse(attachmentsById.get("unreadable").getAcl().isCanAccess());
  }

  /**
   * Any other error while building an attachment still fails the list, a
   * repository error as a runtime one: only what is gone or has no content is
   * left out. Mutation: catch every exception and this test fails.
   */
  @Test
  public void getAttachmentsByEntityFailsOnAnotherError() throws Exception {
    linkedNodes(6L, "unreadableData");
    NodeImpl unreadableData = titledNode("unreadableData");
    Node content = mock(Node.class);
    Property data = mock(Property.class);
    when(data.getLength()).thenThrow(new RepositoryException("value storage"));
    when(content.getProperty("jcr:data")).thenReturn(data);
    when(unreadableData.getNode("jcr:content")).thenReturn(content);
    when(userSession.getNodeByIdentifier("unreadableData")).thenReturn(unreadableData);
    assertThrows(RepositoryException.class, () -> attachmentStorage.getAttachmentsByEntity(6L, ENTITY_TYPE));

    linkedNodes(7L, "failing");
    NodeImpl failing = buildableNode("failing");
    when(documentService.isDocumentTrashed(failing)).thenThrow(new IllegalStateException("trash"));
    when(userSession.getNodeByIdentifier("failing")).thenReturn(failing);
    assertThrows(IllegalStateException.class, () -> attachmentStorage.getAttachmentsByEntity(7L, ENTITY_TYPE));
  }

  /**
   * Links the nodes to the entity, the first one the newest, each found by
   * the system session in a user's drive, or in the quarantine for the one
   * named "quarantined"
   */
  private void linkedNodes(long entityId, String... ids) throws Exception {
    List<AttachmentContextEntity> contexts = new ArrayList<>();
    for (int i = 0; i < ids.length; i++) {
      AttachmentContextEntity context = new AttachmentContextEntity();
      context.setAttachmentId(ids[i]);
      context.setEntityId(entityId);
      context.setEntityType(ENTITY_TYPE);
      context.setAttachedDate((long) (ids.length - i));
      contexts.add(context);
      Node systemNode = mock(Node.class);
      when(systemNode.getPath()).thenReturn(("quarantined".equals(ids[i]) ? "/Quarantine/" : "/Users/u/Documents/") + ids[i]);
      when(systemSession.getNodeByIdentifier(ids[i])).thenReturn(systemNode);
    }
    when(attachmentDAO.getAttachmentContextByEntity(entityId, ENTITY_TYPE)).thenReturn(contexts);
  }

  private NodeImpl titledNode(String id) throws Exception {
    NodeImpl node = mock(NodeImpl.class);
    when(node.getIdentifier()).thenReturn(id);
    when(node.getPath()).thenReturn("/Users/u/Documents/" + id);
    Property title = mock(Property.class);
    when(title.getString()).thenReturn(id + ".pdf");
    when(node.hasProperty("exo:title")).thenReturn(true);
    when(node.getProperty("exo:title")).thenReturn(title);
    return node;
  }

  private NodeImpl buildableNode(String id) throws Exception {
    NodeImpl node = titledNode(id);
    Node content = mock(Node.class);
    Property data = mock(Property.class);
    when(data.getLength()).thenReturn(10L);
    when(content.getProperty("jcr:data")).thenReturn(data);
    when(node.getNode("jcr:content")).thenReturn(content);
    return node;
  }
}
