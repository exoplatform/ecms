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
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.wadl.research.HTTPMethods;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Aug 3, 2012  
 */
public class TestLifecycleConnector extends BaseConnectorTestCase {

  JSONParser parser = new JSONParser();
  
  public void setUp() throws Exception {
    super.setUp();
    
    // Bind LifecycleConnector REST service
    LifecycleConnector restService = (LifecycleConnector) this.container.getComponentInstanceOfType(LifecycleConnector.class);
    this.binder.addResource(restService, null);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  /**
   * Test method LifecycleConnector.byState()
   * Input: /authoring/bystate?fromstate=draft&user=root&lang=en&workspace=collaboration&json=true
   * Expect:a collection of nodes in JSON type that contains 2 nodes:
   *         Node 1: name is Mock node1
   *                 path is /node1
   *         Node 2: name is Mock node2
   *                 path is /node2
   *                 title is Mock node 2
   *                 publication:startPublishedDate is 03/18/2012
   * @throws Exception
   */
  public void testByState() throws Exception{
    String restPath = "/authoring/bystate?fromstate=draft&user=root&lang=en&workspace=collaboration&json=true";
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());    
    JSONArray object = (JSONArray) parser.parse(response.getEntity().toString());
    assertEquals(2, object.size());
    JSONObject firstObject = (JSONObject) object.get(0);
    assertEquals("Mock node1", firstObject.get("name"));
    assertEquals("/node1", firstObject.get("path"));
    JSONObject secondObject = (JSONObject) object.get(1);
    assertEquals("Mock node2", secondObject.get("name"));
    assertEquals("/node2", secondObject.get("path"));
    assertEquals("Mock node2", secondObject.get("title"));
    assertEquals("03/18/2012", secondObject.get("publishedDate"));
  }
  
  /**
   * Test method LifecycleConnector.toState()
   * Input: /authoring/tostate?fromstate=draft&tostate=pending&user=root&lang=en&workspace=collaboration&json=true
   * Expect:a collection of nodes in JSON type that contains 2 nodes:
   *         Node 1: name is Mock node1
   *                 path is /node1
   *         Node 2: name is Mock node2
   *                 path is /node2
   *                 title is Mock node 2
   *                 publication:startPublishedDate is 03/18/2012
   * @throws Exception
   */
  public void testToState() throws Exception{
    String restPath = "/authoring/tostate?fromstate=draft&tostate=pending&user=root&lang=en&workspace=collaboration&json=true";
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    JSONArray object = (JSONArray) parser.parse(response.getEntity().toString());
    assertEquals(2, object.size());
    JSONObject firstObject = (JSONObject) object.get(0);
    assertEquals("Mock node1", firstObject.get("name"));
    assertEquals("/node1", firstObject.get("path"));
    JSONObject secondObject = (JSONObject) object.get(1);
    assertEquals("Mock node2", secondObject.get("name"));
    assertEquals("/node2", secondObject.get("path"));
    assertEquals("Mock node2", secondObject.get("title"));
    assertEquals("03/18/2012", secondObject.get("publishedDate"));
  }
  
  /**
   * Test method LifecycleConnector.byDate()
   * Input: /authoring/bydate?fromstate=staged&date=2&lang=en&workspace=collaboration
   * Expect:a collection of nodes in XML type that contains 2 nodes:
   *         Node 1: name is Mock node1
   *                 path is /node1
   *         Node 2: name is Mock node2
   *                 path is /node2
   *                 title is Mock node 2
   *                 publication:startPublishedDate is 03/18/2012
   * @throws Exception
   */
  public void testByDate() throws Exception {
    String restPath = "/authoring/bydate?fromstate=staged&date=2&lang=en&workspace=collaboration";
    ContainerResponse response = service(HTTPMethods.GET.toString(), restPath, StringUtils.EMPTY, null, null);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    DOMSource object = (DOMSource) response.getEntity();
    Document document = (Document) object.getNode();
    Element element = (Element) document.getChildNodes().item(0);
    assertEquals("contents", element.getNodeName());
    NodeList nodes = element.getChildNodes();
    Node firstNode = nodes.item(0);
    Node secondNode = nodes.item(1);
    assertEquals("content", firstNode.getNodeName());
    assertEquals("Mock node1", firstNode.getAttributes().getNamedItem("name").getNodeValue());
    assertEquals("/node1", firstNode.getAttributes().getNamedItem("path").getNodeValue());
    assertEquals("content", secondNode.getNodeName());
    assertEquals("Mock node2", secondNode.getAttributes().getNamedItem("name").getNodeValue());
    assertEquals("/node2", secondNode.getAttributes().getNamedItem("path").getNodeValue());
    assertEquals("Mock node2", secondNode.getAttributes().getNamedItem("title").getNodeValue());
    assertEquals("03/18/2012", secondNode.getAttributes().getNamedItem("publishedDate").getNodeValue());
  }
}
