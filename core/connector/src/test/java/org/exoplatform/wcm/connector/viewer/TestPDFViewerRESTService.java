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
package org.exoplatform.wcm.connector.viewer;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 22 Aug 2012  
 */
public class TestPDFViewerRESTService extends BaseConnectorTestCase {
  private static final String    restPath        = "/pdfviewer/repository/collaboration/1/0.0/1.0/";
  private static final String    WrongNumberPath = "/pdfviewer/repository/collaboration/1a/0.a/1.b/";
  private ManageableRepository   manageableRepository;
  
  public void setUp() throws Exception {
    super.setUp();
    PDFViewerRESTService pdfViewerRestService = (PDFViewerRESTService) this.container.getComponentInstanceOfType(PDFViewerRESTService.class);
    this.binder.addResource(pdfViewerRestService, null);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  //http://localhost:8080/ecmdemo/rest-ecmdemo4d725eb57f0001010030cc50eb140189?2012-08-22T15:28:05.522+07:00  
  public void testGetFavoriteByUser() throws Exception{
    /* Prepare the favourite nodes */
    manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, manageableRepository);
    //Test the main case
    Node node = (Node) session.getItem("/metro.pdf");
    String restUUIDPath = restPath + node.getUUID();
    ContainerResponse response = service(HTTPMethods.GET.toString(), restUUIDPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    response = service(HTTPMethods.GET.toString(), restUUIDPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    //Cover the case of wrong number format input
    restUUIDPath = WrongNumberPath + node.getUUID();
    response = service(HTTPMethods.GET.toString(), restUUIDPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    //Test pdfViewer with doc file
    node = (Node) session.getItem("/conditions.doc");
    restUUIDPath = restPath + node.getUUID();
    response = service(HTTPMethods.GET.toString(), restUUIDPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
}
