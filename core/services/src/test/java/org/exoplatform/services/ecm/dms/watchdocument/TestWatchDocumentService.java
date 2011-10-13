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
package org.exoplatform.services.ecm.dms.watchdocument;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by eXo Platform
 * Author : Nguyen Manh Cuong
 *          manhcuongpt@gmail.com
 * Jun 16, 2009
 */

/**
 * Unit test for WatchDocumentService
 * Methods need to test:
 * 1. getNotificationType() method
 * 2. watthDocument() method
 * 3. unWatchDocument() method
 */
public class TestWatchDocumentService extends BaseDMSTestCase{

  private final static String  EXO_WATCHABLE_MIXIN  = "exo:watchable";

  private final static String  EMAIL_WATCHERS_PROP  = "exo:emailWatcher";

  private final static String  RSS_WATCHERS_PROP    = "exo:rssWatcher";

  private WatchDocumentService watchDocumentService = null;

  private Node test;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    watchDocumentService = (WatchDocumentService) container.getComponentInstanceOfType(WatchDocumentService.class);
    test = session.getRootNode().addNode("Test");
    session.save();
  }

  /**
   * Test Method: getNotification()
   * Input: test node is not exo:watchable document
   * Expected:
   *        Value of notification is -1.
   */
  public void testGetNotificationType() throws Exception{
    int notification = watchDocumentService.getNotificationType(test, "root");
    assertEquals(-1, notification);
  }

  /**
   * Test Method: getNotification()
   * Input: type of notification for test node is email and rss.
   * Expected:
   *       Value of notification is 0.
   */
  public void testGetNotificationType1() throws Exception{
    test.addMixin(EXO_WATCHABLE_MIXIN);
    session.save();
    createMultipleProperty(test, "root", "root");
    int notification = watchDocumentService.getNotificationType(test, "root");
    assertEquals(0, notification);
  }

  /**
   * Test Method: getNotification()
   * Input: type of notification for test node is email
   * Expected:
   *       Value of notification is 1.
   */
  public void testGetNotificationType2() throws Exception{
    test.addMixin(EXO_WATCHABLE_MIXIN);
    session.save();
    createMultipleProperty(test, "root", "marry");
    int notification = watchDocumentService.getNotificationType(test, "root");
    assertEquals(1, notification);
  }

  /**
   * Test Method: getNotification()
   * Input: type of notification for test node is rss
   * Expected:
   *       Value of notification is 2.
   */
  public void testGetNotificationType3() throws Exception{
    test.addMixin(EXO_WATCHABLE_MIXIN);
    session.save();
    createMultipleProperty(test, "marry", "root");
    int notification = watchDocumentService.getNotificationType(test, "root");
    assertEquals(2, notification);
  }

  /**
   * Test Method: watchDocument()
   * Input: document node is not added exo:watchable mixinType, user: root will watch this document
   * Expected:
   *        document is added exo:watchable mixinType
   *        exo:emailWatch property of document node has value: root.
   */
  public void testWatchDocument() throws Exception{
    watchDocumentService.watchDocument(test, "root", 1);
    assertTrue(test.isNodeType(EXO_WATCHABLE_MIXIN));
    Property pro = test.getProperty(EMAIL_WATCHERS_PROP);
    Value[] value = pro.getValues();
    for(Value val : value){
      assertEquals("root", val.getString());
    }
  }

  /**
   * Test Method: watchDocument()
   * Input: document node is not added exo:watchable mixinType,
   *        user: root will watch this document
   *        Notification has value equal 2
   * Expected:
   *        document is added exo:watchable mixinType
   *        document doesn't has exo:emailWatch property.
   */
  public void testWatchDocument1() throws Exception{
    watchDocumentService.watchDocument(test, "root", 2);
    assertTrue(test.isNodeType(EXO_WATCHABLE_MIXIN));
    assertFalse(test.hasProperty(EMAIL_WATCHERS_PROP));
  }

  /**
   * Test Method: watchDocument()
   * Input: document node follows exo:watchable mixinType,
   *        user: root and marry will watch this document
   * Expected:
   *        document is added exo:watchable mixinType
   *        exo:emailWatch property of document node has value: root and marry.
   */
  @SuppressWarnings("unchecked")
  public void testWatchDocument2() throws Exception{
    watchDocumentService.watchDocument(test, "root", 1);
    watchDocumentService.watchDocument(test, "marry", 1);
    List watcher  = Arrays.asList(new String[]{"root", "marry"});
    Property pro = test.getProperty(EMAIL_WATCHERS_PROP);
    Value[] value = pro.getValues();
    for(Value val : value){
    assertTrue(watcher.contains(val.getString()));
    }
  }

  /**
   * Test Method: unwatchDocument()
   * Input: Document is watched by two user(root, marry)
   *        Root user will be unwatched
   * Expected:
   *        exo:emailWatch property of document node only has value: marry.
   */
  @SuppressWarnings("unchecked")
  public void testUnWatchDocument() throws Exception {
    watchDocumentService.watchDocument(test, "root", 1);
    watchDocumentService.watchDocument(test, "marry", 1);
    watchDocumentService.unwatchDocument(test, "root", 1);
    Property pro = test.getProperty(EMAIL_WATCHERS_PROP);
    List watcher = Arrays.asList(new String[] { "marry" });
    Value[] value = pro.getValues();
    assertEquals(1, value.length);
    assertTrue(watcher.contains(value[0].getString()));
    assertFalse(value[0].equals("root"));
  }

  /**
   * Test Method: unwatchDocument()
   * Input: Document is watch by two user(root, marry)
   *        Root user will be unwatched
   *        Notification has value: 2
   * Expected:
   *        exo:emailWatch property of a document node has value: both root and marry.
   */
  @SuppressWarnings("unchecked")
  public void testUnWatchDocument2() throws Exception{
    watchDocumentService.watchDocument(test, "root", 1);
    watchDocumentService.watchDocument(test, "marry", 1);
    watchDocumentService.unwatchDocument(test, "root", 2);
    List watcher = Arrays.asList(new String[] {"root", "marry"});
    Property pro = test.getProperty(EMAIL_WATCHERS_PROP);
    Value[] value = pro.getValues();
    for (Value val : value) {
      assertTrue(watcher.contains(val.getString()));
    }
    assertEquals(2, value.length);
  }

  /**
   * Create multiple value property for node.
   * @param node
   * @param emailWatcherValue
   * @param rssWatcherValue
   * @throws Exception
   */
  private void createMultipleProperty(Node node, String emailWatcherValue, String rssWatcherValue) throws Exception{
    NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
    NodeType nodeType = nodeTypeManager.getNodeType(EXO_WATCHABLE_MIXIN);
    for(PropertyDefinition def : nodeType.getDeclaredPropertyDefinitions()){
      String proName = def.getName();
      if(def.isMultiple() & proName.equals(EMAIL_WATCHERS_PROP)){
        Value val = session.getValueFactory().createValue(emailWatcherValue);
        node.setProperty(EMAIL_WATCHERS_PROP, new Value[] {val});
      }
      if(def.isMultiple() & proName.equals(RSS_WATCHERS_PROP)){
        Value val = session.getValueFactory().createValue(rssWatcherValue);
        node.setProperty(RSS_WATCHERS_PROP, new Value[] {val});
      }
    }
    session.save();
  }

  /**
   * Clean data test
   */
  public void tearDown() throws Exception {
    if (session.itemExists("/Test")) {
      test = session.getRootNode().getNode("Test");
      test.remove();
      session.save();
    }
    super.tearDown();
  }
}
