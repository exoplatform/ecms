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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          toannh@exoplatform.com
 * Dec 04/01, 2015
 * Test all methods of OpenInOfficeConnector
 */
public class TestOpenInOfficeConnector extends BaseConnectorTestCase{
  private final String OPEN_DOCUMENT_ON_DESKTOP_RESOURCE_KEY     = "OpenInOfficeConnector.label.exo.remote-edit.desktop";
  private final String OPEN_DOCUMENT_ON_DESKTOP_CSS_CLASS        = "uiIconOpenOnDesktop";
  private final String OPEN_DOCUMENT_IN_WORD_CSS_CLASS           = "uiIcon16x16applicationmsword";
  private final String OPEN_DOCUMENT_IN_WORD_RESOURCE_KEY        = "OpenInOfficeConnector.label.exo.remote-edit.word";
  private final String OPEN_DOCUMENT_IN_EXCEL_CSS_CLASS          = "uiIcon16x16applicationxls";
  private final String OPEN_DOCUMENT_IN_EXCEL_RESOURCE_KEY       = "OpenInOfficeConnector.label.exo.remote-edit.excel";
  private final String OPEN_DOCUMENT_IN_PPT_CSS_CLASS            = "uiIcon16x16applicationvndopenxmlformats-officedocumentpresentationmlpresentation";
  private final String OPEN_DOCUMENT_IN_PPT_RESOURCE_KEY         = "OpenInOfficeConnector.label.exo.remote-edit.powerpoint";

  OpenInOfficeConnector openInOfficeConnector =null;
  private ManageableRepository manageableRepository;

  public void setUp() throws Exception {
    super.setUp();
    // Bind OpenInOfficeConnector REST service
    openInOfficeConnector = (OpenInOfficeConnector) this.container.getComponentInstanceOfType(OpenInOfficeConnector.class);
    this.binder.addResource(openInOfficeConnector, null);
  }

  public void testUpdateDocumentTitle() throws Exception{
    String restPath = "/office/updateDocumentTitle?objId=collaboration:/sites/test.doc&lang=en";
    applyUserSession("john", "gtn", "collaboration");
    manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, manageableRepository);
    Node rootNode = session.getRootNode();
    Node sites = rootNode.addNode("sites");
    sites.addNode("test.doc");
    rootNode.save();
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  public void testGetDocumentInfos() throws Exception{
    String word = "test.doc";
    String excel = "test.xls";
    String powerpoint = "test.ppt";
    String other = "test.txt";

    String[] resultWordActual  = openInOfficeConnector.getDocumentInfos(word);
    String[] resultExcelActual = openInOfficeConnector.getDocumentInfos(excel);
    String[] resultPptActual   = openInOfficeConnector.getDocumentInfos(powerpoint);
    String[] resultOtherActual = openInOfficeConnector.getDocumentInfos(other);

    String[] resultWordExpected  = {OPEN_DOCUMENT_IN_WORD_RESOURCE_KEY, OPEN_DOCUMENT_IN_WORD_CSS_CLASS};
    String[] resultExcelExpected = {OPEN_DOCUMENT_IN_EXCEL_RESOURCE_KEY, OPEN_DOCUMENT_IN_EXCEL_CSS_CLASS};
    String[] resultPptExpected   = {OPEN_DOCUMENT_IN_PPT_RESOURCE_KEY, OPEN_DOCUMENT_IN_PPT_CSS_CLASS};
    String[] resultOtherExpected = {OPEN_DOCUMENT_ON_DESKTOP_RESOURCE_KEY, OPEN_DOCUMENT_ON_DESKTOP_CSS_CLASS};

    assertTrue(Arrays.equals(resultWordActual, resultWordExpected));
    assertTrue(Arrays.equals(resultExcelActual, resultExcelExpected));
    assertTrue(Arrays.equals(resultPptActual, resultPptExpected));
    assertTrue(Arrays.equals(resultOtherActual, resultOtherExpected));
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
