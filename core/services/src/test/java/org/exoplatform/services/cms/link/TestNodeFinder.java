/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.services.cms.link;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class TestNodeFinder extends BaseWCMTestCase {

  private static final String TEST_NODE1_NAME = "nodefinder_a'%éé";
  private static final String TEST_NODE2_NAME = "nodefinder_test1";
  NodeFinder nodeFinder = null;

  public void setUp() throws Exception {
    super.setUp();
    session.getRootNode().addNode(TEST_NODE1_NAME);
    session.getRootNode().addNode(TEST_NODE2_NAME);
    session.save();
    nodeFinder = WCMCoreUtils.getService(NodeFinder.class);
  }
  
  public void tearDown() throws Exception {
    Node rootNode = session.getRootNode();
    NodeIterator nodes = rootNode.getNodes();
    while (nodes.hasNext()) {
      Node node = nodes.nextNode();
      if(node.getName().startsWith("nodefinder_")) {
        node.remove();
      }
    }
    session.save();
    super.tearDown();
  }

  public void testGetItem() throws Exception {
    assertNotNull(nodeFinder.getItem(session, "/" + TEST_NODE2_NAME));
  }

  public void testGetItemWithSpecialCharacters() throws Exception {
    assertNotNull(nodeFinder.getItem(session, "/" + TEST_NODE1_NAME));
    assertNotNull(nodeFinder.getItem(session, "/" + Text.escapeIllegalJcrChars(TEST_NODE1_NAME)));
    assertNotNull(nodeFinder.getItem(session, "/" + TEST_NODE1_NAME.replaceAll("'", "\\'")));
  }
}
