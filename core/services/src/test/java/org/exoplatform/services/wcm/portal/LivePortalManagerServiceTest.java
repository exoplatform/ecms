/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.portal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import javax.jcr.Node;

import org.junit.Test;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.impl.LivePortalManagerServiceImpl;

public class LivePortalManagerServiceTest {

  @Test
  public void testNotCreatingGlobalSiteFolder() {
    ListenerService listenerService = mock(ListenerService.class);
    WebSchemaConfigService webSchemaConfigService = mock(WebSchemaConfigService.class);
    WCMConfigurationService wcmConfigurationService = mock(WCMConfigurationService.class);
    RepositoryService repositoryService = mock(RepositoryService.class);
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);

    LivePortalManagerServiceImpl livePortalManagerService = new LivePortalManagerServiceImpl(listenerService,
                                                                                             webSchemaConfigService,
                                                                                             wcmConfigurationService,
                                                                                             portalConfigService,
                                                                                             repositoryService);

    String globalPortalName = "global";
    when(portalConfigService.getGlobalPortal()).thenReturn(globalPortalName);

    try {
      Node livePortal = livePortalManagerService.getLivePortal(sessionProvider, "repository", globalPortalName);
      assertNull(livePortal);

      verifyZeroInteractions(wcmConfigurationService, sessionProvider);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
