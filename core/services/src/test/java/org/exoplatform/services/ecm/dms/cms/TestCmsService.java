/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.cms;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.wcm.BaseWCMTestCase;

import org.apache.commons.lang3.StringUtils;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 12, 2009
 */
@FixMethodOrder(MethodSorters.JVM)
public class TestCmsService extends BaseWCMTestCase {

  private CmsService cmsService;

  private static final String ARTICLE = "exo:article";

  private static final String NTRESOURCE = "nt:resource";

  public void setUp() throws Exception {
    super.setUp();
    cmsService = (CmsService) container.getComponentInstanceOfType(CmsService.class);
    applySystemSession();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   *  Create data for String with String property
   */
  private Map<String, JcrInputProperty> createArticleMapInput() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + "exo:title";
    String summaryPath = CmsService.NODE + "/" + "exo:summary";
    String textPath = CmsService.NODE + "/" + "exo:text";
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(CmsService.NODE);

    inputProperty.setValue("document_1");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("this is summary");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(textPath);
    inputProperty.setValue("this is article content");
    map.put(textPath, inputProperty);
    return map;
  }

  /**
   * Add mixin for node
   */
  private Map<String, JcrInputProperty> createArticleEditMapInput() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + "exo:title";
    String categoryPath = CmsService.NODE + "/" + "exo:category";
    JcrInputProperty inputProperty = new JcrInputProperty();

    inputProperty.setJcrPath(CmsService.NODE);
    inputProperty.setMixintype("exo:categorized");
    inputProperty.setValue("document_1");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title edit");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(categoryPath);
    inputProperty.setValue(new String[] {COLLABORATION_WS + ":/referencedNodeArticle"});
    map.put(categoryPath, inputProperty);
    return map;
  }

  /**
   * Create data for Node with String and date time property
   */
  private Map<String, JcrInputProperty> createSampleMapInput() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + "exo:title";
    String descriptPath = CmsService.NODE + "/" + "exo:description";
    String datePath = CmsService.NODE + "/" + "exo:date";
    String datetimePath = CmsService.NODE + "/" + "exo:datetime";
    String summaryPath = CmsService.NODE + "/" + "exo:summary";
    String contentPath = CmsService.NODE + "/" + "exo:content";
    String totalScorePath = CmsService.NODE + "/" + "exo:totalScore";
    String averageScorePath = CmsService.NODE + "/" + "exo:averageScore";
    String moveablePath = CmsService.NODE + "/" + "exo:moveable";

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setValue("document_2");
    inputProperty.setJcrPath(CmsService.NODE);
    //inputProperty.setMixintype("mix:cms");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(descriptPath);
    inputProperty.setValue("this is description");
    map.put(descriptPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(datePath);
    inputProperty.setValue(ISO8601.parse("06/12/2009"));
    map.put(datePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(datetimePath);
    inputProperty.setValue(ISO8601.parse("06/12/2009 14:58:49"));
    map.put(datetimePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("this is summary");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(contentPath);
    inputProperty.setValue("this is sample's content");
    map.put(contentPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(totalScorePath);
    inputProperty.setValue(new Long(15));
    map.put(totalScorePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(averageScorePath);
    inputProperty.setValue(new Double(1.23));
    map.put(averageScorePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(moveablePath);
    inputProperty.setValue("true");
    map.put(moveablePath, inputProperty);
    return map;
  }

  /**
   * Create data for Node with String and date time property
   */
  private Map<String, JcrInputProperty> createSampleMapInputEdit() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + "exo:title";
    String descriptPath = CmsService.NODE + "/" + "exo:description";
    String datePath = CmsService.NODE + "/" + "exo:date";
    String datetimePath = CmsService.NODE + "/" + "exo:datetime";
    String summaryPath = CmsService.NODE + "/" + "exo:summary";
    String contentPath = CmsService.NODE + "/" + "exo:content";
    String totalScorePath = CmsService.NODE + "/" + "exo:totalScore";
    String averageScorePath = CmsService.NODE + "/" + "exo:averageScore";
    String moveablePath = CmsService.NODE + "/" + "exo:moveable";

    String linkPath = CmsService.NODE + "/" + "exo:linkdata";

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setValue("document_2");
    inputProperty.setJcrPath(CmsService.NODE);
    //inputProperty.setMixintype("mix:cms");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title edit");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(descriptPath);
    inputProperty.setValue("this is description");
    map.put(descriptPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(datePath);
    inputProperty.setValue(ISO8601.parse("06/12/2009"));
    map.put(datePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(datetimePath);
    inputProperty.setValue(ISO8601.parse("06/12/2009 14:58:49"));
    map.put(datetimePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("this is summary");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(contentPath);
    inputProperty.setValue("this is sample's content");
    map.put(contentPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(totalScorePath);
    inputProperty.setValue(new Long(16));
    map.put(totalScorePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(averageScorePath);
    inputProperty.setValue(new Double(2.34));
    map.put(averageScorePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(moveablePath);
    inputProperty.setValue("false");
    map.put(moveablePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(linkPath);
    inputProperty.setValue(COLLABORATION_WS + ":/referencedNode");
    map.put(linkPath, inputProperty);
    return map;
  }

  /**
   * Create data for Node with String and date time property
   */
  private Map<String, JcrInputProperty> createReferenceMapInput() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String categoryPath = CmsService.NODE + "/" + "exo:category";

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setValue("document_3");
    inputProperty.setJcrPath(CmsService.NODE);
    inputProperty.setMixintype("exo:categorized");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(categoryPath);
    inputProperty.setValue(new String[] {COLLABORATION_WS + ":/referencedNode"});
    map.put(categoryPath, inputProperty);
    return map;
  }

  /**
   * Test property value is string with
   * CmsService.storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew,String repository)
   * Input: property for node: exo:title = "this is title";exo:summary="this is summary";exo:text="this is article content"
   *        name = "document_1";
   * Expect: node: name = "document_1";property for node: exo:title = "this is title";
   * exo:summary="this is summary";exo:text="this is article content"
   */
  public void testStoreNodeArticle() throws RepositoryException, Exception {
    Node storeNode = session.getRootNode();
    Map<String, JcrInputProperty> map = createArticleMapInput();
    String path = cmsService.storeNode(ARTICLE, storeNode, map, true);
    assertTrue(session.itemExists(path));
    Node articleNode = (Node)session.getItem(path);
    assertEquals("document_1", articleNode.getName());
    assertEquals("this is title", articleNode.getProperty("exo:title").getString());
    assertEquals("this is summary", articleNode.getProperty("exo:summary").getString());
    assertEquals("this is article content", articleNode.getProperty("exo:text").getString());
  }

  /**
   * Test add mixin
   * CmsService.storeNode(String nodetypeName, Node storeNode, Map inputProperties, boolean isAddNew,String repository)}
   * Input: property for node: exo:title = "this is title";exo:summary="this is summary";exo:text="this is article content"
   *        exo:category = uuid of one reference node; name = "document_1";
   * Expect: node: name = "document_1";property for node: exo:title = "this is title";
   * exo:summary="this is summary";exo:text="this is article content"; exo:category = uuid of one reference node above;
   */
  public void testStoreNodeArticleEdit() throws RepositoryException, Exception {
    Node storeNode = session.getRootNode();
    Node referencedNode = storeNode.addNode("referencedNodeArticle");
    if (referencedNode.canAddMixin("mix:referenceable")) {
      referencedNode.addMixin("mix:referenceable");
    }
    session.save();
    Map<String, JcrInputProperty> map = createArticleMapInput();
    String path1 = cmsService.storeNode(ARTICLE, storeNode, map, true);
    assertTrue(session.itemExists(path1));
    Node articleNode = (Node)session.getItem(path1);
    assertEquals("document_1", articleNode.getName());
    assertEquals("this is title", articleNode.getProperty("exo:title").getValue().getString());
    map = createArticleEditMapInput();
    String path2 = cmsService.storeNode(ARTICLE, storeNode, map, false);
    assertTrue(session.itemExists(path2));

    session.getItem(path1).remove();
    if(!StringUtils.equals(path1, path2)) {
      session.getItem(path2).remove();
    }
    referencedNode.remove();
    session.save();
  }

  /**
   * Test property value is string with
   * CmsService.storeNode(String workspace, String nodetypeName, String storePath, Map inputProperties,String repository)
   * Input: property for node: exo:title = "this is title";exo:summary="this is summary";exo:text="this is article content"
   *        name = "document_1";
   * Expect: node: name = "document_1";property for node: exo:title = "this is title";
   *  exo:summary="this is summary";exo:text="this is article content"
   */
//  public void testStoreNodeArticleByPath1() throws RepositoryException, Exception {
//    Node storeNode = session.getRootNode().addNode("storeNode");
//    session.save();
//    Map<String, JcrInputProperty> map = createArticleMapInput();
//    String path = cmsService.storeNode(COLLABORATION_WS, ARTICLE, storeNode.getPath(), map, REPO_NAME);
//    assertTrue(session.itemExists(path));
//    Node articleNode = (Node)session.getItem(path);
//    assertEquals("document_1", articleNode.getName());
//    assertEquals("this is title", articleNode.getProperty("exo:title").getString());
//    assertEquals("this is summary", articleNode.getProperty("exo:summary").getString());
//    assertEquals("this is article content", articleNode.getProperty("exo:text").getString());
//
//    articleNode.remove();
//    storeNode.remove();
//    session.save();
//  }

  /**
   *  Create binary data
   * @throws IOException
   */
  private Map<String, JcrInputProperty> createBinaryMapInput() throws IOException {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String data = CmsService.NODE + "/" + "jcr:data";

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(CmsService.NODE);
    inputProperty.setValue("BinaryData");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue("test");
    map.put(data, inputProperty);
    return map;
  }

  /**
   * Test property value is binary
   * input: data of file /conf/standalone/system-configuration.xml
   * output: value in property jcr:data of one nt:resource node = value in file /conf/standalone/system-configuration.xml
   * @throws RepositoryException
   * @throws Exception
   */
  public void testStoreNodeBinaryProperty() throws RepositoryException, Exception {
    Node rootNode = session.getRootNode();
    Map<String, JcrInputProperty> map = createBinaryMapInput();
    if ((rootNode != null) && (map != null)) {
      try {
        System.out.println("NTRESOURCE: " + NTRESOURCE);
        System.out.println("rootNode: " + rootNode.getPath());
        System.out.println("map: " + map);
        System.out.println("REPO_NAME: " + REPO_NAME);

        String path1 = cmsService.storeNode(NTRESOURCE, rootNode, map, true);
        System.out.println("path1: " + path1);
        assertTrue(session.itemExists(path1));
        Node binaryNode = (Node)session.getItem(path1);
        assertEquals("BinaryData", binaryNode.getName());
        InputStream is = getClass().getResource("/conf/standalone/system-configuration.xml").openStream();
        assertEquals("test", binaryNode.getProperty("jcr:data").getString());
        binaryNode.remove();
        session.save();
      } catch (NullPointerException e) {
        // TODO: handle exception
      }
    }
  }

  /**
   * Test with path does not existed,
   * Input: /temp: path does not exist
   * Expect: PathNotFoundException
   * @throws RepositoryException
   * @throws Exception
   */
  public void testStoreNodeArticleByPath2() throws RepositoryException, Exception {
    Map<String, JcrInputProperty> map = createArticleMapInput();
    try {
      MockCmsServiceImpl mock = (MockCmsServiceImpl) container.getComponentInstanceOfType(MockCmsServiceImpl.class);
      String path = mock.storeNode(COLLABORATION_WS, ARTICLE, "/temp", map);
      session.getItem(path).remove();
      session.save();
    } catch (PathNotFoundException ex) {
    }
  }

  /**
   * Test create property with reference type
   * Input: property for node: exo:category = uuid of one reference node; name = "document_3";
   * Expect: node: name = "document_3";property for node:  exo:category = uuid of one reference node above;
   * @throws RepositoryException
   * @throws Exception
   */
  public void testStoreNodeWithReference() throws RepositoryException, Exception {
    Node storeNode = session.getRootNode();
    Node referencedNode = storeNode.addNode("referencedNode");
    session.save();
    if (referencedNode.canAddMixin("mix:referenceable")) {
      referencedNode.addMixin("mix:referenceable");
    }
    session.save();
    String uuid = referencedNode.getUUID();
    Map<String, JcrInputProperty> map = createReferenceMapInput();
    String path1 = cmsService.storeNode(ARTICLE, storeNode, map, true);
    assertTrue(session.itemExists(path1));
    Node sampleNode = (Node)session.getItem(path1);
    assertEquals("document_3", sampleNode.getName());
    assertEquals(uuid, sampleNode.getProperty("exo:category").getValues()[0].getString());

    sampleNode.remove();
    referencedNode.remove();
    session.save();
  }

 /**
  * Test method CmsService.moveNode()
  * Move node test1 to test2 node, clean test1 node
  * Expect: node test1 does not exist, node test2/test1 exits
  * @throws Exception
  */
  public void testMoveNode() throws Exception {
    Node test1 = session.getRootNode().addNode("source");
    session.save();
    Session session2 = sessionProviderService_.getSystemSessionProvider(null).getSession(SYSTEM_WS, repository);
    Node test2 = session.getRootNode().addNode("target");
    session2.save();
    String destPath = test2.getPath() + test1.getPath();
    cmsService.moveNode(test1.getPath(), COLLABORATION_WS, SYSTEM_WS, destPath);
    assertTrue(session2.itemExists(destPath));
    assertTrue(!session.itemExists("/source"));

    test2.remove();
    session.save();
    session2.save();
  }

  /**
   *  Compare two input stream, return true if bytes of is1 equal bytes of is2
   * @param is1
   * @param is2
   * @return
   * @throws IOException
   */
  private boolean compareInputStream(InputStream is1, InputStream is2) throws IOException {
    int b1, b2;
    do {
      b1 = is1.read();
      b2 = is2.read();
      if (b1 != b2) return false;
    } while ((b1 !=-1) && (b2!=-1));
    return true;
  }
}
