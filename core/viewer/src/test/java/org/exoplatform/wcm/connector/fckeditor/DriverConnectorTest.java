/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.wcm.connector.fckeditor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

@RunWith(MockitoJUnitRunner.class)
public class DriverConnectorTest {

  @Mock
  private DocumentService       documentService;

  @Mock
  private CloudDriveService     cloudDrives;

  @Mock
  private InitParams            initParams;

  @Mock
  private LinkManager           linkManager;

  @Mock
  private Session               session;

  private MockedStatic<WCMCoreUtils> wcmCoreUtils;

  private MockedStatic<FCKUtils>     fckUtils;

  private MockedStatic<Utils>        utils;

  private DriverConnector       driverConnector;

  @Before
  public void setUp() throws Exception {
    ValueParam valueParam = mock(ValueParam.class);
    when(valueParam.getValue()).thenReturn("20");
    when(initParams.getValueParam("upload.limit.size")).thenReturn(valueParam);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(LinkManager.class)).thenReturn(linkManager);
    fckUtils = mockStatic(FCKUtils.class);
    fckUtils.when(() -> FCKUtils.createRootElement(any(), any(Node.class), any())).thenAnswer(invocation -> {
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = xml.createElement("Connector");
      xml.appendChild(root);
      root.appendChild(xml.createElement("CurrentFolder"));
      return root;
    });
    fckUtils.when(() -> FCKUtils.createWebdavURL(any(Node.class))).thenReturn("/webdav");
    utils = mockStatic(Utils.class);
    utils.when(() -> Utils.getTitle(any(Node.class))).thenReturn("title");
    utils.when(() -> Utils.getNodeTypeIcon(any(Node.class), anyString())).thenReturn("icon");
    driverConnector = new DriverConnector(initParams, documentService, cloudDrives);
  }

  @After
  public void tearDown() {
    utils.close();
    fckUtils.close();
    wcmCoreUtils.close();
  }

  @Test
  public void buildXMLResponseForChildrenSkipsShortcutWithMissingTarget() throws Exception {
    assertDeadShortcutIsSkipped(new ItemNotFoundException("target is gone"));
  }

  @Test
  public void buildXMLResponseForChildrenSkipsShortcutWithUnreadableTarget() throws Exception {
    assertDeadShortcutIsSkipped(new AccessDeniedException("target is not readable"));
  }

  private void assertDeadShortcutIsSkipped(Exception targetFailure) throws Exception {
    Node root = mockFolder("/root", "root");
    Node deadShortcut = mockFolder("/root/dead", "dead");
    when(deadShortcut.isNodeType(NodetypeConstant.EXO_SYMLINK)).thenReturn(true);
    when(deadShortcut.hasProperty("exo:uuid")).thenReturn(true);
    when(linkManager.getTarget(deadShortcut)).thenThrow(targetFailure);
    Node folder = mockFolder("/root/folder", "folder");
    when(root.getNodes()).thenAnswer(invocation -> iteratorOf(deadShortcut, folder));

    Response response = invokeBuildXMLResponseForChildren(root);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Document xml = (Document) ((DOMSource) response.getEntity()).getNode();
    NodeList folders = xml.getElementsByTagName("Folders").item(0).getChildNodes();
    assertEquals(1, folders.getLength());
    assertEquals("folder", ((Element) folders.item(0)).getAttribute("name"));
  }

  private NodeIterator iteratorOf(Node... nodes) {
    Iterator<Node> it = Arrays.asList(nodes).iterator();
    NodeIterator nodeIterator = mock(NodeIterator.class);
    lenient().when(nodeIterator.hasNext()).thenAnswer(invocation -> it.hasNext());
    lenient().when(nodeIterator.nextNode()).thenAnswer(invocation -> it.next());
    return nodeIterator;
  }

  private Node mockFolder(String path, String name) throws Exception {
    Node node = mock(Node.class);
    NodeType nodeType = mock(NodeType.class);
    lenient().when(nodeType.getName()).thenReturn(NodetypeConstant.NT_UNSTRUCTURED);
    lenient().when(node.getPrimaryNodeType()).thenReturn(nodeType);
    lenient().when(node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)).thenReturn(true);
    lenient().when(node.getPath()).thenReturn(path);
    lenient().when(node.getName()).thenReturn(name);
    NodeIterator noChildren = mock(NodeIterator.class);
    lenient().when(noChildren.hasNext()).thenReturn(false);
    lenient().when(node.getNodes()).thenReturn(noChildren);
    return node;
  }

  private Response invokeBuildXMLResponseForChildren(Node node) throws Exception {
    Method method = DriverConnector.class.getDeclaredMethod("buildXMLResponseForChildren",
                                                            Node.class,
                                                            String.class,
                                                            String.class,
                                                            Session.class,
                                                            String.class,
                                                            String.class,
                                                            String.class,
                                                            String.class);
    method.setAccessible(true);
    try {
      return (Response) method.invoke(driverConnector, node, "getFoldersAndFiles", "All", session, "portal", "", "drive", "folder");
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }
}
