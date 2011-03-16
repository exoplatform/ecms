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
package org.exoplatform.services.ecm.dms.thumbnail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.exoplatform.services.cms.impl.ImageUtils;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 20, 2009
 */
public class TestThumbnailService extends BaseDMSTestCase {

  private ThumbnailService thumbnailService;

  public void setUp() throws Exception {
    super.setUp();
    thumbnailService = (ThumbnailService)container.getComponentInstanceOfType(ThumbnailService.class);
  }

  /**
   * Test method ThumbnailService.addThumbnailNode()
   * Input: Node test with no thumbnail node folder
   * Expect: Node with name = identifier of test node in ThumbnailService.EXO_THUMBNAILS_FOLDER node exists
   * @throws Exception
   */
  public void testAddThumbnailNode1() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    assertFalse(test.getParent().hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER));
    thumbnailService.addThumbnailNode(test);
    assertTrue(test.getParent().hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER));
    Node thumbnailFoder = test.getParent().getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
    String identifier = ((NodeImpl)test).getInternalIdentifier();
    assertTrue(thumbnailFoder.hasNode(identifier));
  }

  /**
   * Test method ThumbnailService.addThumbnailNode()
   * Input: Node test with thumbnail node folder
   * Expect: Node with name = identifier of test node in ThumbnailService.EXO_THUMBNAILS_FOLDER node exists
   * @throws Exception
   */
  public void testAddThumbnailNode2() throws Exception {
    Node test = session.getRootNode().addNode("test");
    test.getParent().addNode(ThumbnailService.EXO_THUMBNAILS_FOLDER, ThumbnailService.EXO_THUMBNAILS);
    session.save();
    assertTrue(test.getParent().hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER));
    thumbnailService.addThumbnailNode(test);
    assertTrue(test.getParent().hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER));
    Node thumbnailFoder = test.getParent().getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
    String identifier = ((NodeImpl)test).getInternalIdentifier();
    assertTrue(thumbnailFoder.hasNode(identifier));
  }

  /**
   * Test method ThumbnailService.getFlowImages()
   * Input: Add node test
   * Expect: return empty list images of node test
   * @throws Exception
   */
  public void testGetFlowImages1() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    List<Node> lstFlowImages = thumbnailService.getFlowImages(test);
    assertEquals(0, lstFlowImages.size());
  }

  /**
   * Test method ThumbnailService.getFlowImages()
   * Input: Add node test, add node childTest. getFlowImages of test
   * Expect: return list images of childTest node with one node name = identifier of childTest node
   * @throws Exception
   */
  public void testGetFlowImages2() throws Exception {
    Node test = session.getRootNode().addNode("test");
    Node childTest = test.addNode("childTest");
    session.save();
    Node thumbnail = thumbnailService.addThumbnailNode(childTest);
    thumbnail.hasProperty(ThumbnailService.BIG_SIZE);
    InputStream is = getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg");
    Value contentValue = session.getValueFactory().createValue(is);
    thumbnail.setProperty(ThumbnailService.BIG_SIZE, contentValue);
    thumbnail.save();
    session.save();
    thumbnail.getPrimaryNodeType().getName();
    thumbnail.hasProperty(ThumbnailService.BIG_SIZE);
    List<Node> lstFlowImages = thumbnailService.getFlowImages(test);
    String identifier = ((NodeImpl) childTest).getInternalIdentifier();
    assertTrue(test.hasNode(ThumbnailService.EXO_THUMBNAILS_FOLDER));
    Node thumbnailFolder = test.getNode(ThumbnailService.EXO_THUMBNAILS_FOLDER);
    assertTrue(thumbnailFolder.hasNode(identifier));
    assertTrue(lstFlowImages.contains(childTest));
  }

  /**
   * Test method ThumbnailService.getAllFileInNode()
   * Input: Add 2 node (file1 and file2) with node type = nt:file
   * Expect: List of 2 node file1 and file2
   * @throws Exception
   */
  public void testGetAllFileInNode() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    Node file1 = test.addNode("file1", "nt:file");
    file1.addNode("jcr:content", "nt:resource");
    InputStream is = getClass().getResource("/conf/standalone/system-configuration.xml").openStream();
    Value contentValue = session.getValueFactory().createValue(is);
    file1.getNode("jcr:content").setProperty("jcr:data", contentValue);
    file1.getNode("jcr:content").setProperty("jcr:mimeType", "text/xml");
    file1.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    Node file2 = test.addNode("file2", "nt:file");
    file2.addNode("jcr:content", "nt:resource");
    file2.getNode("jcr:content").setProperty("jcr:data", contentValue);
    file2.getNode("jcr:content").setProperty("jcr:mimeType", "text/xml");
    file2.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    test.save();
    session.save();
    List<Node> lstNode = thumbnailService.getAllFileInNode(test);
    assertEquals(2, lstNode.size());
    assertTrue(lstNode.contains(file1));
    assertTrue(lstNode.contains(file2));
  }

  /**
   * Test method ThumbnailService.getFileNodesByType()
   * Input: Add 2 node (file1 and file2) with node type = nt:file
   *        file1 has child node jcr:content with mimeType = text/xml
   *        file1 has child node jcr:content with mimeType = text/html
   * Expect: with type = text/xml return list of 1 node (file1)
   *         with type = text/xml return list of 1 node (file2)
   * @throws Exception
   */
  public void testGetFileNodesByType() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    Node file1 = test.addNode("file1", "nt:file");
    file1.addNode("jcr:content", "nt:resource");
    InputStream is = getClass().getResource("/conf/standalone/system-configuration.xml").openStream();
    Value contentValue = session.getValueFactory().createValue(is);
    file1.getNode("jcr:content").setProperty("jcr:data", contentValue);
    file1.getNode("jcr:content").setProperty("jcr:mimeType", "text/xml");
    file1.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    Node file2 = test.addNode("file2", "nt:file");
    file2.addNode("jcr:content", "nt:resource");
    file2.getNode("jcr:content").setProperty("jcr:data", contentValue);
    file2.getNode("jcr:content").setProperty("jcr:mimeType", "text/html");
    file2.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    test.save();
    session.save();
    List<Node> lstNode1 = thumbnailService.getFileNodesByType(test, "text/xml");
    assertEquals(1, lstNode1.size());
    assertTrue(lstNode1.contains(file1));
    assertEquals(lstNode1.get(0), file1);
    List<Node> lstNode2 = thumbnailService.getFileNodesByType(test, "text/html");
    assertEquals(1, lstNode2.size());
    assertTrue(lstNode2.contains(file2));
    assertEquals(lstNode2.get(0), file2);
  }

  /**
   * Test method ThumbnailService.addThumbnailImage()
   * Input: Node: thumbnail, resource /conf/dms/artifacts/images/ThumnailView.jpg, size = ThumbnailService.SMALL_SIZE
   * Expect: data in property exo:smallSize of thumbmail node is resource with height*width = 32x32
   * @throws Exception
   */
  public void testAddThumbnailImage() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    Node childTest = thumbnailService.addThumbnailNode(test);
    Value value = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 32, 32));
    thumbnailService.addThumbnailImage(childTest, ImageIO.read(getClass().getResource("/conf/dms/artifacts/images/ThumnailView.jpg").openStream()),  ThumbnailService.SMALL_SIZE);
    assertNotNull(value);
  }

  /**
   * Test method ThumbnailService.getThumbnailImage()
   * Input: add thumbnail image to childTest node with resource = /conf/dms/artifacts/images/ThumnailView.jpg, size = ThumbnailService.SMALL_SIZE
   * Expect: property exo:smallSize contains data of resource = /conf/dms/artifacts/images/ThumnailView.jpg
   * @throws Exception
   */
  public void testGetThumbnailImage() throws Exception {
    Node test = session.getRootNode().addNode("test");
    assertNull(thumbnailService.getThumbnailImage(test, "exo:smallSize"));
    Node childTest = thumbnailService.addThumbnailNode(test);
//    Value value = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 32, 32));
    thumbnailService.addThumbnailImage(childTest, ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")),  ThumbnailService.SMALL_SIZE);
    assertNotNull(childTest.getProperty(ThumbnailService.SMALL_SIZE).getValue());
  }

  /**
   * Test method ThumbnailService.createSpecifiedThumbnail()
   * Input: resource = /conf/dms/artifacts/images/ThumnailView.jpg, mimeType = image/jpeg, node = test
   * Output: thumbnail node has property exo:smallSize contains data of resource
   *           /conf/dms/artifacts/images/ThumnailView.jpg which is scale by size = 32*32
   * @throws Exception
   */
  public void testCreateSpecifiedThumbnail() throws Exception {
    Node test = session.getRootNode().addNode("test");
//    Value value = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 32, 32));
    thumbnailService.createSpecifiedThumbnail(test, ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), ThumbnailService.SMALL_SIZE);
    Node thumbnail = thumbnailService.getThumbnailNode(test);
    assertNotNull(thumbnail.getProperty(ThumbnailService.SMALL_SIZE).getValue());
  }

  /**
   * Test method ThumbnailService.createThumbnailImage()
   * Input: resource = /conf/dms/artifacts/images/ThumnailView.jpg, mimeType = image/jpeg, node = test
   * Output: thumbnail node has 3 property exo:smallSize, exo:midiumSize, exo:bigSize contain data of resource
   *           /conf/dms/artifacts/images/ThumnailView.jpg which is scale by size = 32*32, 64*64, 300*300 respectively
   * @throws Exception
   */
  public void testCreateThumbnailImage() throws Exception {
    Node test = session.getRootNode().addNode("test");
//    Value value1 = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 32, 32));
//    Value value2 = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 64, 64));
//    Value value3 = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 300, 300));
    InputStream is = getClass().getResource("/conf/dms/artifacts/images/ThumnailView.jpg").openStream();
    thumbnailService.createThumbnailImage(test, ImageIO.read(is), "image/jpeg");
    Node thumbnail = thumbnailService.getThumbnailNode(test);
    assertNotNull(thumbnail.getProperty(ThumbnailService.SMALL_SIZE).getValue().getStream());
    assertNotNull(thumbnail.getProperty(ThumbnailService.MEDIUM_SIZE).getValue());
    assertNotNull(thumbnail.getProperty(ThumbnailService.BIG_SIZE).getValue());
  }

  /**
   * Test method ThumbnailService.processThumbnailList()
   * Input: List child node, 3 in 4 node are nt:file node type, data in nt:file has mimeType = image/jpeg
   *          binary data = /conf/dms/artifacts/images/ThumnailView.jpg, propertyName = SMALL_SIZE
   * Output: Create thumbnail node for each node in list, each node has property = SMALL_SIZE
   *         with binary data = /conf/dms/artifacts/images/ThumnailView.jpg
   * @throws Exception
   */
  public void testProcessThumbnailList() throws Exception {
    Node test = session.getRootNode().addNode("test2");
    Node child1 = test.addNode("child1", "nt:file");
    child1.addNode("jcr:content", "nt:resource");
    ValueFactory valueFactory = session.getValueFactory();
    child1.getNode("jcr:content").setProperty("jcr:data", valueFactory.createValue(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")));
    child1.getNode("jcr:content").setProperty("jcr:mimeType", "image/jpeg");
    child1.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    Node child2 = test.addNode("child2", "nt:file");
    child2.addNode("jcr:content", "nt:resource");
    valueFactory = session.getValueFactory();
    child2.getNode("jcr:content").setProperty("jcr:data", valueFactory.createValue(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")));
    child2.getNode("jcr:content").setProperty("jcr:mimeType", "image/jpeg");
    child2.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    Node child3 = test.addNode("child3", "nt:file");
    child3.addNode("jcr:content", "nt:resource");
    child3.getNode("jcr:content").setProperty("jcr:data", valueFactory.createValue(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")));
    child3.getNode("jcr:content").setProperty("jcr:mimeType", "image/jpeg");
    child3.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
    Node child4 = test.addNode("child4");
    List<Node> lstNode = new ArrayList<Node>();
    lstNode.add(child1);
    lstNode.add(child2);
    lstNode.add(child3);
    lstNode.add(child4);
    session.save();
    thumbnailService.processThumbnailList(lstNode, ThumbnailService.SMALL_SIZE);
    assertTrue(session.itemExists("/test2/" + ThumbnailService.EXO_THUMBNAILS_FOLDER));
    Node thumbnailsFolder = (Node)session.getItem("/test2/" + ThumbnailService.EXO_THUMBNAILS_FOLDER);
    assertTrue(thumbnailsFolder.hasNode(((NodeImpl)child1).getInternalIdentifier()));
    assertTrue(thumbnailsFolder.hasNode(((NodeImpl)child2).getInternalIdentifier()));
    assertTrue(thumbnailsFolder.hasNode(((NodeImpl)child3).getInternalIdentifier()));
    assertTrue(thumbnailsFolder.hasNode(((NodeImpl)child4).getInternalIdentifier()));
    Node thumbnailImage1 = thumbnailsFolder.getNode(((NodeImpl)child1).getInternalIdentifier());
    Node thumbnailImage2 = thumbnailsFolder.getNode(((NodeImpl)child2).getInternalIdentifier());
    Node thumbnailImage3 = thumbnailsFolder.getNode(((NodeImpl)child3).getInternalIdentifier());
    //Value value = session.getValueFactory().createValue(ImageUtils.scaleImage(ImageIO.read(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg")), 32, 32));
    assertNotNull(thumbnailImage1.getProperty(ThumbnailService.SMALL_SIZE).getValue());
    assertNotNull(thumbnailImage2.getProperty(ThumbnailService.SMALL_SIZE).getValue());
    assertNotNull(thumbnailImage3.getProperty(ThumbnailService.SMALL_SIZE).getValue());

  }

  /**
   *  Test method ThumbnailService.getThumbnailNode()
   *  Input: 1.test node
   *         2. add thumbnail node for test node
   *  Output: 1. thumbnail of test node = null
   *          2. return thumbnail node of test node
   * @throws Exception
   */
  public void testGetThumbnailNode() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    Node thumbnail = thumbnailService.getThumbnailNode(test);
    assertNull(thumbnail);
    thumbnailService.addThumbnailNode(test);
    thumbnail = thumbnailService.getThumbnailNode(test);
    assertNotNull(thumbnail);
    assertEquals(((NodeImpl)test).getInternalIdentifier(), thumbnail.getName());
  }

  /**
   * Test method ThumbnailService.processRemoveThumbnail()
   * Input: test node whether or not thumbnail node exists
   * Output: Delete thumbnail node of test node if it exists
   * @throws Exception
   */
  public void testProcessRemoveThumbnail() throws Exception {
    Node test = session.getRootNode().addNode("test");
    session.save();
    String identifier = ((NodeImpl)test).getInternalIdentifier();
    thumbnailService.processRemoveThumbnail(test);
    assertFalse(session.itemExists("/" + ThumbnailService.EXO_THUMBNAILS_FOLDER + "/" + identifier));
    thumbnailService.addThumbnailNode(test);
    assertTrue(session.itemExists("/" + ThumbnailService.EXO_THUMBNAILS_FOLDER + "/" + identifier));
    thumbnailService.processRemoveThumbnail(test);
    assertFalse(session.itemExists("/" + ThumbnailService.EXO_THUMBNAILS_FOLDER + "/" + identifier));
  }

  /**
   * Clean data
   */
  public void tearDown() throws Exception {
    String[] paths = {"test2", "test", ThumbnailService.EXO_THUMBNAILS_FOLDER};
    for (String path : paths) {
      if (session.getRootNode().hasNode(path)) {
        session.getRootNode().getNode(path).remove();
        session.save();
      }
    }
    session.logout();
    super.tearDown();
  }
}
