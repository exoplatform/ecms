/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.classify;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.exoplatform.services.jcr.ext.classify.impl.AlphabetClassifyPlugin;
import org.exoplatform.services.jcr.ext.classify.impl.TypeClassifyPlugin;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS
 * Author : Anh Do Ngoc
 * anh.do@exoplatform.com
 * Jun 2, 2008
 */
public class TestNodeClassifyService extends BaseWCMTestCase {

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Test classify plugin manager.
   *
   * @throws Exception the exception
   */
  public void testClassifyPluginManager() throws Exception {
    NodeClassifyService classifyService =
      (NodeClassifyService)container.getComponentInstanceOfType(NodeClassifyService.class);
    String strAlphabetClassify = "org.exoplatform.services.jcr.ext.classify.impl.AlphabetClassifyPlugin";
    NodeClassifyPlugin classifyPlugin = classifyService.getNodeClassifyPlugin(strAlphabetClassify);
    assertNotNull(classifyPlugin);
  }

  /**
   * Test alphabet classify.
   *
   * @throws Exception the exception
   */
  public void testAlphabetClassify() throws Exception {
    NodeClassifyService classifyService =
      (NodeClassifyService)container.getComponentInstanceOfType(NodeClassifyService.class);
    AlphabetClassifyPlugin alphabetClassifyPlugin =
      (AlphabetClassifyPlugin)classifyService.getNodeClassifyPlugin(AlphabetClassifyPlugin.class.getName());
    Node root = session.getRootNode();
    Node test = root.addNode("test", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("ebook", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("economy", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("emule", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("document", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("dot", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("temp", NodetypeConstant.NT_UNSTRUCTURED);
    session.save();

    assertEquals(6, test.getNodes().getSize());

    alphabetClassifyPlugin.classifyChildrenNode(test);

    try{
//    -> classified nodes: E_Node, D_Node and T_Node (sub nodes of test node)
      Node E_node = test.getNode("E_Node");
      Node D_node = test.getNode("D_Node");
      Node T_node = test.getNode("T_Node");

      assertEquals(3, test.getNodes().getSize());
      assertNotNull(E_node);
      assertNotNull(D_node);
      assertNotNull(T_node);

//    sub nodes of E_node: ebook, emule, economy
      assertEquals(3, E_node.getNodes().getSize());
      assertNotNull(E_node.getNode("ebook"));
      assertNotNull(E_node.getNode("emule"));
      assertNotNull(E_node.getNode("economy"));

//    sub nodes of D_node: document, dot
      assertEquals(2, D_node.getNodes().getSize());
      assertNotNull(D_node.getNode("document"));
      assertNotNull(D_node.getNode("dot"));

//    sub nodes of T_node: temp
      assertEquals(1, T_node.getNodes().getSize());
      assertNotNull(T_node.getNode("temp"));
    }catch(PathNotFoundException ex){}

    test.remove();
    session.save();
  }

  /**
   * Test date time classify.
   *
   * @throws Exception the exception
   */
//  public void testDateTimeClassify() throws Exception{
//    NodeClassifyService classifyService =
//      (NodeClassifyService)container.getComponentInstanceOfType(NodeClassifyService.class) ;
//    DateTimeClassifyPlugin dateClassifyPlugin =
//      (DateTimeClassifyPlugin)classifyService.getNodeClassifyPlugin(DateTimeClassifyPlugin.class.getName()) ;
//    Node root = session.getRootNode();
//    Node test = root.addNode("test", NodetypeConstant.NT_UNSTRUCTURED);
//
//    Calendar c1 = new GregorianCalendar();
//    c1.set(Calendar.YEAR, 2013);
//    c1.set(Calendar.MONTH, 10);
//
//    Calendar c2 = new GregorianCalendar();
//    c2.set(Calendar.YEAR, 2013);
//    c2.set(Calendar.MONTH, 4);
//
//    Calendar c3 = new GregorianCalendar();
//    c3.set(Calendar.YEAR, 2012);
//    c3.set(Calendar.MONTH, 8);
//
//    Calendar c4 = new GregorianCalendar();
//    c4.set(Calendar.YEAR, 2018);
//    c4.set(Calendar.MONTH, 2);
//
//    Calendar c5 = new GregorianCalendar();
//    c5.set(Calendar.YEAR, 2019);
//    c5.set(Calendar.MONTH, 2);
//
//    Node n1 = test.addNode("node1", NodetypeConstant.NT_UNSTRUCTURED);
//    n1.setProperty("exo:dateCreated", c1);
//    Node n2 = test.addNode("node2", NodetypeConstant.NT_UNSTRUCTURED);
//    n2.setProperty("exo:dateCreated", c2);
//    Node n3 = test.addNode("node3", NodetypeConstant.NT_UNSTRUCTURED);
//    n3.setProperty("exo:dateCreated", c3);
//    Node n4 = test.addNode("node4", NodetypeConstant.NT_UNSTRUCTURED);
//    n4.setProperty("exo:dateCreated", c4);
//    Node n5 = test.addNode("node5", NodetypeConstant.NT_UNSTRUCTURED);
//    n5.setProperty("exo:dateCreated", c5);
//
//    session.save();
//
//    NodeIterator nodes = test.getNodes();
//    assertEquals(5, nodes.getSize());
//
//    dateClassifyPlugin.classifyChildrenNode(test);
//
//    //test node has 2 child nodes: "2012-2016" and "2017-2020"
//    assertEquals(5, nodes.getSize());
//    Node n_2012 = test.getNode("2012-2016");
//    Node n_2017 = test.getNode("2017-2020");
//    assertNotNull(n_2012);
//    assertNotNull(n_2017);
//
//    //n_2012 node has 3 child nodes: "11" , "5" and "9"
//    assertEquals(3, n_2012.getNodes().getSize());
//    Node n_2012_11 = n_2012.getNode("11");
//    Node n_2012_9 = n_2012.getNode("9");
//    Node n_2012_5 = n_2012.getNode("5");
//    assertNotNull(n_2012_11);
//    assertNotNull(n_2012_9);
//    assertNotNull(n_2012_5);
//    /*
//    n_2012_11 has one child node is "node1" - and it is leaft node
//    n_2012_5 has one child node is "node2" - and it is leaft node
//    n_2012_9 has one child node is "node3" - and it is leaft node
//     */
//    assertEquals(1, n_2012_11.getNodes().getSize());
//    Node leaft1 = n_2012_11.getNode("node1");
//    assertNotNull(leaft1);
//    assertEquals(0, leaft1.getNodes().getSize());
//
//    assertEquals(1, n_2012_5.getNodes().getSize());
//    Node leaft2 = n_2012_5.getNode("node2");
//    assertNotNull(leaft2);
//    assertEquals(0, leaft2.getNodes().getSize());
//
//    assertEquals(1, n_2012_9.getNodes().getSize());
//    Node leaft3 = n_2012_9.getNode("node3");
//    assertNotNull(leaft3);
//    assertEquals(0, leaft3.getNodes().getSize());
//
//    //n_2017 has 1 child node: "3"
//    assertEquals(1, n_2017.getNodes().getSize());
//    Node n_2017_3 = n_2017.getNode("3");
//    assertNotNull(n_2017_3);
//
//    //n_2017_3 has 2 child nodes: "node4" and "node5" -> they are leaft nodes.
//    assertEquals(2, n_2017_3.getNodes().getSize());
//    Node leaft4 = n_2017_3.getNode("node4");
//    Node leaft5 = n_2017_3.getNode("node5");
//    assertNotNull(leaft4);
//    assertEquals(0, leaft4.getNodes().getSize());
//    assertNotNull(leaft5);
//    assertEquals(0, leaft5.getNodes().getSize());
//
//    test.remove();
//    session.save();
//  }

  /**
   * Test type classify.
   *
   * @throws Exception the exception
   */
  public void testTypeClassify() throws Exception{
    NodeClassifyService classifyService =
      (NodeClassifyService)container.getComponentInstanceOfType(NodeClassifyService.class) ;
    TypeClassifyPlugin typeClassifyPlugin =
      (TypeClassifyPlugin)classifyService.getNodeClassifyPlugin(TypeClassifyPlugin.class.getName()) ;
    Node root = session.getRootNode();
    Node test = root.addNode("test", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("chicken", NodetypeConstant.NT_FOLDER);
    test.addNode("dog", NodetypeConstant.NT_FOLDER);
    test.addNode("bird", NodetypeConstant.NT_FOLDER);
    test.addNode("ball", NodetypeConstant.NT_UNSTRUCTURED);
    test.addNode("hat", NodetypeConstant.NT_UNSTRUCTURED);
    session.save();

    assertEquals(5, test.getNodes().getSize());

    typeClassifyPlugin.classifyChildrenNode(test);

    try{
//    -> classified nodes: nt:folder_Nodes, nt:unstructured_Nodes (sub nodes of test node)
      Node folder_node = test.getNode("nt:folder_Nodes");
      Node unstructured_node = test.getNode("nt:unstructured_Nodes");

      assertEquals(2, test.getNodes().getSize());
      assertNotNull(folder_node);
      assertNotNull(unstructured_node );

//    sub nodes of nt:folder_Nodes : chicken, dog, bird
      assertEquals(3, folder_node.getNodes().getSize());
      assertNotNull(folder_node.getNode("chicken"));
      assertNotNull(folder_node.getNode("dog"));
      assertNotNull(folder_node.getNode("bird"));

//    sub nodes of nt:unstructured_Nodes: ball, hat
      assertEquals(2, unstructured_node.getNodes().getSize());
      assertNotNull(unstructured_node.getNode("ball"));
      assertNotNull(unstructured_node.getNode("hat"));

    }catch(PathNotFoundException ex){}
  }
}
