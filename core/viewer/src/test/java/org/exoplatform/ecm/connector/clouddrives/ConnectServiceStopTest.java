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
package org.exoplatform.ecm.connector.clouddrives;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

public class ConnectServiceStopTest {

  @Test
  public void testTheCleanerRunsOnADaemonThreadThatEndsWithTheContainer() throws Exception {
    ConnectService connectService = new ConnectService(mock(CloudDriveService.class),
                                                       mock(DriveServiceLocator.class),
                                                       mock(RepositoryService.class),
                                                       mock(SessionProviderService.class),
                                                       mock(NodeFinder.class));
    assertTrue(connectService.connectsCleaner.submit(() -> Thread.currentThread().isDaemon()).get());

    connectService.stop();

    assertTrue(connectService.connectsCleaner.isShutdown());
  }
}
