/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.collaboration;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 24 Aug 2012  
 */
public class TestDownloadConnector extends BaseConnectorTestCase{
  private static final String    restPath        = "/contents/download/collaboration/offices.jpg";
  
  public void setUp() throws Exception {
    super.setUp();
    
    // Bind DownloadConnector REST service
    DownloadConnector restService = (DownloadConnector) this.container.getComponentInstanceOfType(DownloadConnector.class);
    this.binder.addResource(restService, null);
  }
  
  public void testDownload() throws Exception{
//    ConversationState.setCurrent(new ConversationState(new Identity("root")));
    applyUserSession("john", "gtn", "collaboration");
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
