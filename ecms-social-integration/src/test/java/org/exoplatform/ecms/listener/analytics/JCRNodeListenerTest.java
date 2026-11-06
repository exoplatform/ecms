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
package org.exoplatform.ecms.listener.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.observation.Event;

import org.apache.commons.chain.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wcm.ext.component.activity.listener.FileUpdateActivityListener;

/**
 * Only a change to the content or to an edited property of an existing file
 * is recorded as an update of the file.
 */
public class JCRNodeListenerTest {

  private static final String FILE_PATH = "/Groups/spaces/x/Documents/file.docx";

  private static final String QUEUE_KEY = "john@@" + FILE_PATH;

  private MockedStatic<PortalContainer> portalContainer;

  private TrashService                  trashService;

  @Before
  public void setUp() {
    JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.clear();
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    PortalContainer container = mock(PortalContainer.class);
    trashService = mock(TrashService.class);
    when(container.getComponentInstanceOfType(TrashService.class)).thenReturn(trashService);
    portalContainer = mockStatic(PortalContainer.class);
    portalContainer.when(PortalContainer::getInstance).thenReturn(container);
  }

  @After
  public void tearDown() {
    portalContainer.close();
    ConversationState.setCurrent(null);
    JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.clear();
  }

  @Test
  public void contentAndEditedPropertiesAreUpdates() {
    for (String name : new String[] { "jcr:data", "dc:description", "exo:title", "exo:summary",
        "exo:language", "dc:title", "dc:creator", "dc:source" }) {
      assertTrue(name, JCRNodeListener.isEditedFileProperty(name));
    }
  }

  @Test
  public void listenerListMatchesTheActivityStreamList() throws Exception {
    Field field = FileUpdateActivityListener.class.getDeclaredField("editedField");
    field.setAccessible(true);
    String[] streamList = (String[]) field.get(new FileUpdateActivityListener());

    assertEquals(Arrays.asList(streamList), FileUpdateActivityListener.EDITED_FILE_PROPERTIES);
  }

  @Test
  public void otherPropertiesAreNotUpdates() {
    for (String name : new String[] { "exo:permissions", "exo:dateModified", "exo:lastModifiedDate",
        "exo:lastModifier", "exo:name", "exo:hiddenable", "jcr:mimeType", null }) {
      assertFalse(String.valueOf(name), JCRNodeListener.isEditedFileProperty(name));
    }
  }

  @Test
  public void editorPropertyRemovalOnExistingFileIsNotQueued() throws Exception {
    Context context = context(property("exo:currentProvider", file(false)), Event.PROPERTY_REMOVED);

    assertTrue(new JCRNodeListener().execute(context));

    assertFalse(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void modificationDateChangeOnExistingFileIsNotQueued() throws Exception {
    Context context = context(property("exo:lastModifiedDate", file(false)), Event.PROPERTY_CHANGED);

    assertTrue(new JCRNodeListener().execute(context));

    assertFalse(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void titleChangeOnExistingFileIsQueued() throws Exception {
    Context context = context(property("exo:title", file(false)), Event.PROPERTY_CHANGED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void sourceAddedOnExistingFileIsQueued() throws Exception {
    Context context = context(property("dc:source", file(false)), Event.PROPERTY_ADDED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void checkinOnExistingFileIsQueued() throws Exception {
    Context context = context(file(false), ExtendedEvent.CHECKIN);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void checkinAfterContentWriteIsQueuedOnce() throws Exception {
    Node file = file(false);
    Node content = mock(Node.class);
    when(content.isNew()).thenReturn(false);
    when(content.isNodeType("nt:resource")).thenReturn(true);
    when(content.getPath()).thenReturn(FILE_PATH + "/jcr:content");
    when(content.getParent()).thenReturn(file);
    JCRNodeListener listener = new JCRNodeListener();

    assertTrue(listener.execute(context(property("jcr:data", content), Event.PROPERTY_CHANGED)));
    assertEquals(1, JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.size());
    assertTrue(listener.execute(context(file, ExtendedEvent.CHECKIN)));

    assertEquals(1, JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.size());
    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void checkinOnFileInTrashIsNotQueued() throws Exception {
    Node file = file(false);
    when(trashService.isInTrash(file)).thenReturn(true);

    assertTrue(new JCRNodeListener().execute(context(file, ExtendedEvent.CHECKIN)));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.isEmpty());
  }

  @Test
  public void checkinOnNodeThatIsNotAFileIsNotQueued() throws Exception {
    // The parent climb would find the file above and queue it: only the guard
    // on the checked-in node stops the record
    Node parentFile = file(false);
    Node child = mock(Node.class);
    when(child.isNew()).thenReturn(false);
    when(child.isNodeType("nt:file")).thenReturn(false);
    when(child.isNodeType("nt:resource")).thenReturn(false);
    when(child.getPath()).thenReturn(FILE_PATH + "/child");
    when(child.getParent()).thenReturn(parentFile);

    assertTrue(new JCRNodeListener().execute(context(child, ExtendedEvent.CHECKIN)));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.isEmpty());
  }

  @Test
  public void actionIsRegisteredForCheckin() throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("conf/portal/configuration.xml")) {
      String configuration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      int action = configuration.indexOf(JCRNodeListener.class.getName());
      int eventTypes = configuration.lastIndexOf("eventTypes", action);
      String registered = configuration.substring(eventTypes, action);

      assertTrue(registered, registered.contains("checkin"));
    }
  }

  @Test
  public void propertyWrittenOnFileInTrashIsQueued() throws Exception {
    Node file = file(false);
    when(trashService.isInTrash(file)).thenReturn(true);
    Context context = context(property("exo:restorePath", file), Event.PROPERTY_ADDED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void propertyWrittenOnNewNodeIsQueued() throws Exception {
    Context context = context(property("exo:title", file(true)), Event.PROPERTY_ADDED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  /** Stops at the queue, before the delayed executor: the repository lookup is not mocked. */
  @Test
  public void contentChangeOnExistingFileIsQueued() throws Exception {
    Node file = file(false);
    Node content = mock(Node.class);
    when(content.isNew()).thenReturn(false);
    when(content.isNodeType("nt:resource")).thenReturn(true);
    when(content.getPath()).thenReturn(FILE_PATH + "/jcr:content");
    when(content.getParent()).thenReturn(file);
    Context context = context(property("jcr:data", content), Event.PROPERTY_CHANGED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  private Node file(boolean isNew) throws Exception {
    Node file = mock(Node.class);
    when(file.isNew()).thenReturn(isNew);
    when(file.isNodeType("nt:file")).thenReturn(true);
    when(file.isNodeType("nt:resource")).thenReturn(false);
    when(file.getPath()).thenReturn(FILE_PATH);
    Node root = mock(Node.class);
    when(root.getPath()).thenReturn("/");
    when(file.getParent()).thenReturn(root);
    return file;
  }

  private Property property(String name, Node parent) throws Exception {
    Property property = mock(Property.class);
    when(property.getName()).thenReturn(name);
    when(property.getParent()).thenReturn(parent);
    return property;
  }

  private Context context(Item item, int eventType) {
    Context context = mock(Context.class);
    when(context.get(InvocationContext.CURRENT_ITEM)).thenReturn(item);
    when(context.get(InvocationContext.EVENT)).thenReturn(eventType);
    return context;
  }
}
