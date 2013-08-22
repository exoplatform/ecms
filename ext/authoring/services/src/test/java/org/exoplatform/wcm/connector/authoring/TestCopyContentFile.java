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
package org.exoplatform.wcm.connector.authoring;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Aug 6, 2012  
 */
public class TestCopyContentFile extends BaseConnectorTestCase {

  public void setUp() throws Exception {
    super.setUp();
    // Bind CopyContentFile REST service
    CopyContentFile restService = (CopyContentFile) this.container.getComponentInstanceOfType(CopyContentFile.class);
    this.binder.addResource(restService, null);
  }
  
  /**
   * Test method TestCopyContentFile.copyFile()
   * Input: /copyfile/copy/
   * Expect: connector return data ok
   * @throws Exception
   */
  public void testCopyFile() throws Exception{
    String restPath = "/copyfile/copy/";
    ContainerResponse response = service(HTTPMethods.POST.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}