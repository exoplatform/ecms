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

import static org.testng.AssertJUnit.assertEquals;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.testng.annotations.Test;


/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 23 Aug 2012  
 */
public class TestThumbnailRESTService extends BaseConnectorTestCase{
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    ThumbnailRESTService restService = (ThumbnailRESTService) this.container.getComponentInstanceOfType(ThumbnailRESTService.class);
    this.binder.addResource(restService, null);
  }
  
  @Test
  public void testGetOriginImage() throws Exception{
    String restPath = "/thumbnailImage/origin/repository/collaboration/offices.jpg";
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetLargeImage() throws Exception{
    String restPath = "/thumbnailImage/large/repository/collaboration/offices.jpg";
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetBigImage() throws Exception{
    String restPath = "/thumbnailImage/big/repository/collaboration/offices.jpg";
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetMediumImage() throws Exception{
    String restPath = "/thumbnailImage/medium/repository/collaboration/offices.jpg";
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testGetSmallImage() throws Exception{
    String restPath = "/thumbnailImage/small/repository/collaboration/offices.jpg";
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    /* Prepare the favourite nodes */
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
}
