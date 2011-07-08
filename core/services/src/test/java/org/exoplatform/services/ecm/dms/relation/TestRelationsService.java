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
package org.exoplatform.services.ecm.dms.relation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by The eXo Platform SARL
 * June 09, 2009
 */
public class TestRelationsService extends BaseDMSTestCase {

  private RelationsService relationsService;

  private static final String RELATION_MIXIN = "exo:relationable";
  private static final String RELATION_PROP = "exo:relation";

  public void setUp() throws Exception {
    super.setUp();
    relationsService = (RelationsService)container.getComponentInstanceOfType(RelationsService.class);
  }

  /**
   * Test method: RelationsServiceImpl.init()
   * Input: repository        The name of repository
   * Expect: Initial the root of relation node and its sub node
   * @throws Exception
   */
  public void testInit() throws Exception {
  }

  /**
   * Test method: RelationsServiceImpl.addRelation()
   * Input: node              Specify the node wants to remove a relation
   *        relationPath      The path of relation
   *        repository        The name of repository
   * Expect: Removes the relation to the given node
   * @throws Exception
   */
  public void testAddRelation() throws Exception {
    Node root = session.getRootNode();
    Node aaa = root.addNode("AAA");
    Node bbb = root.addNode("BBB");
    session.save();

    relationsService.addRelation(aaa, bbb.getPath(), COLLABORATION_WS, REPO_NAME);
    assertTrue(aaa.isNodeType(RELATION_MIXIN));

    Value[] values = aaa.getProperty(RELATION_PROP).getValues();
    Node relatedNode = session.getNodeByUUID(values[0].getString());
    assertEquals(bbb.getPath(), relatedNode.getPath());
  }

  /**
   * Test method: RelationsServiceImpl.hasRelations()
   * Input: node              Specify the node wants to check relation
   * Expect: Returns true is the given node has relation
   * @throws Exception
   */
  public void testHasRelations() throws Exception {
    Node root = session.getRootNode();
    Node aaa = root.addNode("AAA");
    Node bbb = root.addNode("BBB");
    session.save();

    relationsService.addRelation(aaa, bbb.getPath(), COLLABORATION_WS, REPO_NAME);
    assertTrue(relationsService.hasRelations(aaa));
    assertFalse(relationsService.hasRelations(bbb));
  }

  /**
   * Test method: RelationsServiceImpl.getRelations()
   * Input: node              Specify the node wants to get all node relative to it
   *        repository        The name of repository
   *        provider          The SessionProvider object is used to managed Sessions
   * Expect: Return all node that has relation to the given node
   * @throws Exception
   */
  public void testGetRelations() throws Exception {
    Node root = session.getRootNode();
    Node aaa = root.addNode("AAA");
    Node bbb = root.addNode("BBB");
    Node ccc = root.addNode("CCC");
    Node ddd = root.addNode("DDD");
    session.save();

    relationsService.addRelation(aaa, bbb.getPath(), COLLABORATION_WS, REPO_NAME);
    relationsService.addRelation(aaa, ccc.getPath(), COLLABORATION_WS, REPO_NAME);

    List<Node> listRelation = relationsService.getRelations(aaa, REPO_NAME, sessionProviderService_.getSystemSessionProvider(null));
    List<String> relationPathList = new ArrayList<String>();
    for (Node relation : listRelation) {
      relationPathList.add(relation.getPath());
    }
    assertTrue(relationPathList.contains(bbb.getPath()));
    assertTrue(relationPathList.contains(ccc.getPath()));
    assertFalse(relationPathList.contains(ddd.getPath()));
  }

  /**
   * Test method: RelationsServiceImpl.removeRelation()
   * Input: node              Specify the node wants to remove a relation
   *        relationPath      The path of relation
   *        repository        The name of repository
   * Expect: Removes the relation to the given node
   * @throws Exception
   */
  public void testRemoveRelation() throws Exception {
    Node root = session.getRootNode();
    Node aaa = root.addNode("AAA");
    Node bbb = root.addNode("BBB");
    Node ccc = root.addNode("CCC");
    Node ddd = root.addNode("DDD");
    session.save();

    relationsService.addRelation(aaa, bbb.getPath(), COLLABORATION_WS, REPO_NAME);
    relationsService.addRelation(aaa, ccc.getPath(), COLLABORATION_WS, REPO_NAME);
    relationsService.addRelation(aaa, ddd.getPath(), COLLABORATION_WS, REPO_NAME);

    List<Node> listBeforeRemove = relationsService.getRelations(aaa, REPO_NAME, sessionProviderService_.getSystemSessionProvider(null));
    List<String> pathBeforeRemove = new ArrayList<String>();
    for (Node relation : listBeforeRemove) {
      pathBeforeRemove.add(relation.getPath());
    }
    assertTrue(pathBeforeRemove.contains(bbb.getPath()));
    assertTrue(pathBeforeRemove.contains(ccc.getPath()));
    assertTrue(pathBeforeRemove.contains(ddd.getPath()));

    relationsService.removeRelation(aaa, "/DDD", REPO_NAME);

    List<Node> listAfterRemove = relationsService.getRelations(aaa, REPO_NAME, sessionProviderService_.getSystemSessionProvider(null));
    List<String> pathAfterRemove = new ArrayList<String>();
    for (Node relation : listAfterRemove) {
      pathAfterRemove.add(relation.getPath());
    }
    assertTrue(pathAfterRemove.contains(bbb.getPath()));
    assertTrue(pathAfterRemove.contains(ccc.getPath()));
    assertFalse(pathAfterRemove.contains(ddd.getPath()));
  }

  /**
   * Clean all node for testing
   */
  public void tearDown() throws Exception {
    Node root = session.getRootNode();
    String[] paths = new String[] {"AAA", "BBB", "CCC", "DDD"};
    for (String path : paths) {
      if (root.hasNode(path)) {
        root.getNode(path).remove();
      }
    }
    session.save();
    super.tearDown();
  }
}
