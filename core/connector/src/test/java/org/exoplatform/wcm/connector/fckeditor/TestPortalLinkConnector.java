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
package org.exoplatform.wcm.connector.fckeditor;

import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Aug 6, 2012  
 */
public class TestPortalLinkConnector extends BaseConnectorTestCase {
  
  public void setUp() throws Exception {
    super.setUp();
    // Bind PortalLinkConnector REST service
    PortalLinkConnector restService = (PortalLinkConnector) this.container.getComponentInstanceOfType(PortalLinkConnector.class);
    this.binder.addResource(restService, null);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  /**
   * Test method PortalLinkConnector.getPageURI()
   * Input 1: /portalLinks/getFoldersAndFiles/
   * Expect 1: Connector return data in XML format as: 
   * <Connector command="" resourceType="PortalPageURI">
   *   <CurrentFolder path="" url="">
   *   </CurrentFolder>
   *   <Folders>
   *     <Folder name="classic" url="" path="/">
   *     </Folder>
   *   </Folders>
   * </Connector> 
   * 
   * Input 2: /portalLinks/getFoldersAndFiles?currentFolder=/classic/
   * Expect 2: Connector return data in XML format OK
   * 
   * Input 3: /portalLinks/getFoldersAndFiles?currentFolder=/classic/home
   * Expect 3: Connector return data in XML format as:
   * <Connector command="" resourceType="PortalPageURI">
   *   <CurrentFolder path="/classic/home" url="">
   *   </CurrentFolder>
   *   <Folders>
   *     <Folder name="classic" url="" path="/">
   *     </Folder>
   *   </Folders>
   *   <Files>
   *   </Files>
   * </Connector>
   * @throws Exception
   */
  public void testGetPageURI() throws Exception{
    ConversationState.setCurrent(new ConversationState(new Identity("root")));
    String restPath = "/portalLinks/getFoldersAndFiles/";
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DOMSource object = (DOMSource) response.getEntity();
    Document node = (Document) object.getNode();
    Element connector = (Element) node.getChildNodes().item(0);
    NodeList ConnectorChildren = connector.getChildNodes();
    assertEquals("Connector", connector.getNodeName());
    assertEquals("", connector.getAttribute("command"));
    assertEquals("PortalPageURI", connector.getAttribute("resourceType"));
    assertEquals(2, ConnectorChildren.getLength());
    Element currentFolder = (Element) ConnectorChildren.item(0);
    assertEquals("CurrentFolder", currentFolder.getNodeName());
    assertEquals("/", currentFolder.getAttribute("path"));
    assertEquals("", currentFolder.getAttribute("url"));

    Node folders = ConnectorChildren.item(1);
    assertEquals("Folders", folders.getNodeName());
    Element firstFolder = (Element) folders.getChildNodes().item(0);
    assertNotNull(firstFolder);
    assertEquals("classic", firstFolder.getAttribute("name"));
    assertEquals("", firstFolder.getAttribute("url"));
    assertEquals("", firstFolder.getAttribute("folderType"));
    
    restPath = "/portalLinks/getFoldersAndFiles?currentFolder=/classic/";
    response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    
    restPath = "/portalLinks/getFoldersAndFiles?currentFolder=/classic/home";
    response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    object = (DOMSource) response.getEntity();
    node = (Document) object.getNode();
    connector = (Element) node.getChildNodes().item(0);
    ConnectorChildren = connector.getChildNodes();
    assertEquals("Connector", connector.getNodeName());
    assertEquals("", connector.getAttribute("command"));
    assertEquals("PortalPageURI", connector.getAttribute("resourceType"));
    assertEquals(3, ConnectorChildren.getLength());
    currentFolder = (Element) ConnectorChildren.item(0);
    assertEquals("CurrentFolder", currentFolder.getNodeName());
    assertEquals("/classic/home", currentFolder.getAttribute("path"));
    assertEquals("", currentFolder.getAttribute("url"));
    folders = ConnectorChildren.item(1);
    assertEquals("Folders", folders.getNodeName());    
    Node files = ConnectorChildren.item(2);
    assertEquals("Files", files.getNodeName());    
  }
}
