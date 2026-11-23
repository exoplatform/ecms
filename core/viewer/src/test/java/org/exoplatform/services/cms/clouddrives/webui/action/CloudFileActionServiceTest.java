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

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;

import org.junit.Test;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;

public class CloudFileActionServiceTest {

  @Test
  public void testALinkMovedToARecycleBinFolderIsLeftThere() throws Exception {
    // Only a link moved to the Trash is removed from it: a shortcut deleted to
    // a Recycle Bin folder stays there, restorable
    Session session = mock(Session.class);
    Workspace workspace = mock(Workspace.class);
    ObservationManager observation = mock(ObservationManager.class);
    when(session.getWorkspace()).thenReturn(workspace);
    // The repository knows the Recycle Bin folder type
    when(workspace.getNodeTypeManager()).thenReturn(mock(NodeTypeManager.class));
    when(workspace.getObservationManager()).thenReturn(observation);
    CloudFileActionService service = new CloudFileActionService(mock(CloudDriveService.class),
                                                                mock(RepositoryService.class),
                                                                mock(SessionProviderService.class),
                                                                mock(OrganizationService.class),
                                                                mock(NodeFinder.class),
                                                                mock(NodeHierarchyCreator.class),
                                                                mock(LinkManager.class),
                                                                mock(ManageDriveService.class),
                                                                mock(TrashService.class),
                                                                mock(ListenerService.class),
                                                                mock(CmsService.class)) {
      @Override
      protected Session systemSession() {
        return session;
      }
    };

    service.listenFileLinks();

    verify(observation).addEventListener(any(CloudFileActionService.LinkTrashListener.class),
                                         eq(Event.NODE_ADDED),
                                         isNull(),
                                         eq(false),
                                         isNull(),
                                         aryEq(new String[] { "exo:trashFolder" }),
                                         eq(false));
  }

}
