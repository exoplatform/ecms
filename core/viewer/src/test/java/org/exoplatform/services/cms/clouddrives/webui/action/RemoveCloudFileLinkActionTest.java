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
package org.exoplatform.services.cms.clouddrives.webui.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.observation.ExtendedEvent;

public class RemoveCloudFileLinkActionTest {

  private Node                   trashHome;

  private CloudFileActionService cloudFileActions;

  private Context                context;

  private Node                   link;

  private Node                   linkParent;

  @Before
  public void setUp() {
    trashHome = mock(Node.class);
    TrashService trashService = mock(TrashService.class);
    when(trashService.getTrashHomeNode()).thenReturn(trashHome);
    cloudFileActions = mock(CloudFileActionService.class);
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(TrashService.class)).thenReturn(trashService);
    when(container.getComponentInstanceOfType(CloudFileActionService.class)).thenReturn(cloudFileActions);
    link = mock(Node.class);
    linkParent = mock(Node.class);
    context = new ContextBase();
    context.put(InvocationContext.EXO_CONTAINER, container);
    context.put(InvocationContext.CURRENT_ITEM, link);
    context.put(InvocationContext.EVENT, ExtendedEvent.NODE_REMOVED);
  }

  @Test
  public void testALinkRemovedFromADriveIsMarkedForRemoval() throws Exception {
    when(link.getParent()).thenReturn(linkParent);

    new RemoveCloudFileLinkAction().execute(context);

    verify(cloudFileActions).markRemoveLink(link);
  }

  @Test
  public void testALinkRemovedFromTheTrashHomeIsLeftAlone() throws Exception {
    when(link.getParent()).thenReturn(trashHome);
    when(trashHome.isSame(trashHome)).thenReturn(true);

    new RemoveCloudFileLinkAction().execute(context);

    verify(cloudFileActions, never()).markRemoveLink(any());
  }

  @Test
  public void testALinkDeletedForeverFromARecycleBinFolderIsMarkedForRemoval() throws Exception {
    // A shortcut deleted by a user stays in its Recycle Bin folder: removing
    // it from there is its removal for good, which withdraws its share
    when(link.getParent()).thenReturn(linkParent);
    when(linkParent.isNodeType("exo:recycleBinFolder")).thenReturn(true);

    new RemoveCloudFileLinkAction().execute(context);

    verify(cloudFileActions).markRemoveLink(link);
  }

}
