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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

/**
 * Only a change to the content or the description of an existing file is
 * recorded as an update of the file.
 */
public class JCRNodeListenerTest {

  private static final String FILE_PATH = "/Groups/spaces/x/Documents/file.docx";

  private static final String QUEUE_KEY = "john@@" + FILE_PATH;

  private MockedStatic<PortalContainer> portalContainer;

  @Before
  public void setUp() {
    JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.clear();
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    PortalContainer container = mock(PortalContainer.class);
    TrashService trashService = mock(TrashService.class);
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
  public void contentAndDescriptionAreUpdates() {
    assertTrue(JCRNodeListener.isContentOrDescription("jcr:data"));
    assertTrue(JCRNodeListener.isContentOrDescription("dc:description"));
  }

  @Test
  public void otherPropertiesAreNotUpdates() {
    for (String name : new String[] { "exo:permissions", "exo:dateModified", "exo:lastModifiedDate",
        "exo:lastModifier", "exo:title", "exo:name", "exo:hiddenable", "exo:editorsId", "jcr:mimeType", null }) {
      assertFalse(String.valueOf(name), JCRNodeListener.isContentOrDescription(name));
    }
  }

  @Test
  public void permissionsChangeOnExistingFileIsNotScheduled() throws Exception {
    Node file = file();
    Context context = context(property("exo:permissions", file), Event.PROPERTY_CHANGED);

    assertTrue(new JCRNodeListener().execute(context));

    assertFalse(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  @Test
  public void contentChangeOnExistingFileIsScheduled() throws Exception {
    Node file = file();
    Node content = mock(Node.class);
    when(content.isNew()).thenReturn(false);
    when(content.isNodeType("nt:resource")).thenReturn(true);
    when(content.getPath()).thenReturn(FILE_PATH + "/jcr:content");
    when(content.getParent()).thenReturn(file);
    Context context = context(property("jcr:data", content), Event.PROPERTY_CHANGED);

    assertTrue(new JCRNodeListener().execute(context));

    assertTrue(JCRNodeListener.CURRENTLY_PROCESSING_NODE_PATH_QUEUE.contains(QUEUE_KEY));
  }

  private Node file() throws Exception {
    Node file = mock(Node.class);
    when(file.isNew()).thenReturn(false);
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

  private Context context(Property property, int eventType) {
    Context context = mock(Context.class);
    when(context.get(InvocationContext.CURRENT_ITEM)).thenReturn(property);
    when(context.get(InvocationContext.EVENT)).thenReturn(eventType);
    return context;
  }
}
