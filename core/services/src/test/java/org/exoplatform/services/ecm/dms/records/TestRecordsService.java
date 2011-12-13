/*
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
 */
package org.exoplatform.services.ecm.dms.records;

import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@gmail.com Jun 16, 2009
 */
public class TestRecordsService extends BaseDMSTestCase {
  private RecordsService       recordsService;

  private Node                 rootNode;

  public void setUp() throws Exception {
    super.setUp();
    recordsService = (RecordsService) container.getComponentInstanceOfType(RecordsService.class);
    createTree();
  }

  /**
   * Create tree for testing
   * Input: A1: nt:file (name = "A1")
   *        A2: nt:file (name = "A2")
   *        A3: filePlan (name = "A3" and some property for nodetype rma:filePlan)
   *        A4: filePlan (name = "A4" and some property for nodetype rma:filePlan)
   *        A5: Node test
   * @throws Exception
   */
  public void createTree() throws Exception {
    rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("TestTreeNode");

    Node nodeA1 = testNode.addNode("A1", "nt:file");
    Node contentA1 = nodeA1.addNode("jcr:content", "nt:resource");
    contentA1.setProperty("jcr:lastModified", Calendar.getInstance());
    contentA1.setProperty("jcr:mimeType", "text/xml");
    contentA1.setProperty("jcr:data", "");

    Node nodeA2 = testNode.addNode("A2", "nt:file");
    Node contentA2 = nodeA2.addNode("jcr:content", "nt:resource");
    contentA2.setProperty("jcr:lastModified", Calendar.getInstance());
    contentA2.setProperty("jcr:mimeType", "text/xml");
    contentA2.setProperty("jcr:data", "");

    addNodeFilePlan("A3", testNode, "cateIdentify1", "disposition1", true, true, "mediaType1",
        "markingList1", "original1", true, false, "trigger1", false, false, false, false, "hourly");

    Node nodeA4 = addNodeFilePlan("A4", testNode, "cateIdentify2", "disposition2", true, true, "mediaType2",
        "markingList2", "original2", true, true, "trigger2", true, true, false, false,
        "quarterly");
    nodeA4.setProperty("rma:cutoffPeriod", "hourly");
    nodeA4.setProperty("rma:cutoffOnObsolete", true);
    nodeA4.setProperty("rma:cutoffOnSuperseded", false);

    testNode.addNode("A5");
    session.save();
  }

  private Node addNodeFilePlan(String nodeName, Node parent, String cateIdentify,
      String disposition, boolean permanentRecord, boolean recordFolder, String mediaType,
      String markingList, String original, boolean recordIndicator, boolean cutoff,
      String eventTrigger, boolean processHold, boolean processTransfer, boolean processAccession,
      boolean processDestruction, String vitalRecordReview) throws Exception {
    Node filePlan = parent.addNode(nodeName, "rma:filePlan");
    filePlan.setProperty("rma:recordCategoryIdentifier", cateIdentify);
    filePlan.setProperty("rma:dispositionAuthority", disposition);
    filePlan.setProperty("rma:permanentRecordIndicator", permanentRecord);
    filePlan.setProperty("rma:containsRecordFolders", recordFolder);
    filePlan.setProperty("rma:defaultMediaType", mediaType);
    filePlan.setProperty("rma:defaultMarkingList", markingList);
    filePlan.setProperty("rma:defaultOriginatingOrganization", original);
    filePlan.setProperty("rma:vitalRecordIndicator", recordIndicator);
    filePlan.setProperty("rma:processCutoff", cutoff);
    filePlan.setProperty("rma:eventTrigger", eventTrigger);
    filePlan.setProperty("rma:processHold", processHold);
    filePlan.setProperty("rma:processTransfer", processTransfer);
    filePlan.setProperty("rma:processAccession", processAccession);
    filePlan.setProperty("rma:processDestruction", processDestruction);
    filePlan.setProperty("rma:vitalRecordReviewPeriod", vitalRecordReview);
    return filePlan;
  }

  /**
   * Set property for filePlan node which is get from record node
   * Input:
   *    1. Add record for file plan node A3 from node record A1
   *    2. Add record for file plan node A4 from node record A2
   * Expect:
   *    Some property in file plan node has been changed
   * @throws Exception
   */
  public void testAddRecord() throws Exception {
    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");

    // record o day la nodeA1, node A2
    // fileplan o day la nodeA3, nodeA4
//    recordsService.addRecord(nodeA3, nodeA1);
//    assertEquals(nodeA3.getProperty("rma:recordCounter").getLong(), 1);
//    assertEquals(nodeA3.getProperty("rma:vitalRecordIndicator").getBoolean(), true);
//    assertEquals(nodeA3.getProperty("rma:vitalRecordReviewPeriod").getString(), "hourly");
//    assertEquals(nodeA3.getProperty("rma:processCutoff").getBoolean(), false);
//    assertEquals(nodeA1.getProperty("rma:originator").getString(), "__system");
//    assertEquals(nodeA1.getProperty("rma:recordIdentifier").getString(), "cateIdentify1-1 A1");
//    assertEquals(nodeA1.getProperty("rma:originatingOrganization").getString(), "original1");
//
//    recordsService.addRecord(nodeA4, nodeA2);
//    assertEquals(nodeA4.getProperty("rma:recordCounter").getLong(), 1);
//    assertEquals(nodeA4.getProperty("rma:vitalRecordIndicator").getBoolean(), true);
//    assertEquals(nodeA4.getProperty("rma:vitalRecordReviewPeriod").getString(), "quarterly");
//    assertEquals(nodeA4.getProperty("rma:processCutoff").getBoolean(), true);
//    assertEquals(nodeA4.getProperty("rma:cutoffPeriod").getString(), "hourly");
//    assertEquals(nodeA4.getProperty("rma:cutoffOnObsolete").getBoolean(), true);
//    assertEquals(nodeA4.getProperty("rma:cutoffOnSuperseded").getBoolean(), false);
//    assertEquals(nodeA4.getProperty("rma:eventTrigger").getString(), "trigger2");
//
//    assertEquals(nodeA2.getProperty("rma:originator").getString(), "__system");
//    assertEquals(nodeA2.getProperty("rma:recordIdentifier").getString(), "cateIdentify2-1 A2");
//    assertEquals(nodeA2.getProperty("rma:originatingOrganization").getString(), "original2");
//    assertEquals(nodeA2.getProperty("rma:cutoffObsolete").getBoolean(), true);
//    assertEquals(nodeA2.getProperty("rma:cutoffEvent").getString(), "trigger2");
  }
//
//  /**
//   * Get list of node by query statement
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:vitalRecord
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetVitalRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//
//    session.save();
//
//    List<Node> listRecord = recordsService.getVitalRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:isObsolete
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Set property rma:isObsolete is true for node A1
//   * Expect:
//   *    Result: Size of result = 1; item node first = A1
//   * @throws Exception
//   */
//  public void testGetObsoleteRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getObsoleteRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA1.setProperty("rma:isObsolete", true);
//    session.save();
//    listRecord = recordsService.getObsoleteRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 1);
//    assertEquals(listRecord.get(0).getName(), "A1");
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:superseded
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Set property rma:superseded is true for node A1 and node A2
//   *    4. Set property rma:dateReceived for node A2 and node A1
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetSupersededRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getSupersededRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA1.setProperty("rma:superseded", true);
//    nodeA2.setProperty("rma:superseded", true);
//
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//    nodeA1.setProperty("rma:dateReceived", new GregorianCalendar());
//
//    session.save();
//    listRecord = recordsService.getSupersededRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:cutoffExecuted
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 1; item node first = A2
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Add mixin rma:cutoffable for node A1; set property rma:cutoffExecuted is false and
//   *    rma:cutoffDateTime
//   *    4. Set property rma:dateReceived for node A2 and node A1
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetCutoffRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//
//    List<Node> listRecord = recordsService.getCutoffRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 1);
//    assertEquals(listRecord.get(0).getName(), "A2");
//
//    nodeA1.addMixin("rma:cutoffable");
//    nodeA1.setProperty("rma:cutoffExecuted", false);
//    nodeA1.setProperty("rma:cutoffDateTime", new GregorianCalendar());
//
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//    nodeA1.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    listRecord = recordsService.getCutoffRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:holdExecuted
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Add mixin rma:holdable and set property rma:holdExecuted is true for node A1
//   *    4. Add mixin rma:holdable and set property rma:holdExecuted is false for node A2
//   * Expect:
//   *    Result: Size of result = 1; item node first = A2
//   * @throws Exception
//   */
//  public void testGetHolableRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getHolableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA1.addMixin("rma:holdable");
//    nodeA1.setProperty("rma:holdExecuted", true);
//    nodeA2.addMixin("rma:holdable");
//    nodeA2.setProperty("rma:holdExecuted", false);
//    session.save();
//
//    listRecord = recordsService.getHolableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 1);
//    assertEquals(listRecord.get(0).getName(), "A2");
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:transferExecuted
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Add mixin rma:transferable and set property (rma:transferDate, rma:transferLocation =
//   *    "location2", rma:transferExecuted = false, rma:dateReceived) for node A2
//   *    4. Add mixin rma:transferable and set property (rma:transferDate, rma:transferLocation =
//   *    "location1", rma:transferExecuted = false, rma:dateReceived) for node A1
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetTransferableRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getTransferableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA2.addMixin("rma:transferable");
//    nodeA2.setProperty("rma:transferDate", new GregorianCalendar());
//    nodeA2.setProperty("rma:transferLocation", "location2");
//    nodeA2.setProperty("rma:transferExecuted", false);
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//
//    nodeA1.addMixin("rma:transferable");
//    nodeA1.setProperty("rma:transferDate", new GregorianCalendar());
//    nodeA1.setProperty("rma:transferLocation", "location1");
//    nodeA1.setProperty("rma:transferExecuted", false);
//    nodeA1.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    listRecord = recordsService.getTransferableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of node by query statement with constraint concerning @rma:accessionExecuted
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Add mixin rma:accessionable and set property (rma:accessionExecuted is false,
//   *    rma:accessionDate, rma:dateReceived for node A2
//   *    4. Add mixin rma:accessionable and set property (rma:accessionExecuted is false,
//   *    rma:accessionDate, rma:dateReceived for node A1
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetAccessionableRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getAccessionableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA2.addMixin("rma:accessionable");
//    nodeA2.setProperty("rma:accessionExecuted", false);
//    nodeA2.setProperty("rma:accessionDate", new GregorianCalendar());
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//
//    nodeA1.addMixin("rma:accessionable");
//    nodeA1.setProperty("rma:accessionExecuted", false);
//    nodeA1.setProperty("rma:accessionDate", new GregorianCalendar());
//    nodeA1.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    listRecord = recordsService.getAccessionableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Get list of rma:destroyable node by query statement @param filePlan filePlan node
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   * Expect:
//   *    Result: Size of result = 0
//   *
//   * Input:
//   *    1. Add record for file plan node A3 from node record A1
//   *    2. Add record for file plan node A4 from node record A2
//   *    3. Add mixin rma:destroyable and set property (rma:destructionDate, rma:dateReceived for node A2
//   *    4. Add mixin rma:destroyable and set property (rma:destructionDate, rma:dateReceived for node A1
//   * Expect:
//   *    Result: Size of result = 2; contains node A1 and A2
//   * @throws Exception
//   */
//  public void testGetDestroyableRecords() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA3, nodeA1);
//    recordsService.addRecord(nodeA4, nodeA2);
//    session.save();
//
//    List<Node> listRecord = recordsService.getDestroyableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 0);
//
//    nodeA2.addMixin("rma:destroyable");
//    nodeA2.setProperty("rma:destructionDate", new GregorianCalendar());
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//
//    nodeA1.addMixin("rma:destroyable");
//    nodeA1.setProperty("rma:destructionDate", new GregorianCalendar());
//    nodeA1.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    listRecord = recordsService.getDestroyableRecords(rootNode.getNode("TestTreeNode"));
//    assertEquals(listRecord.size(), 2);
//    List<String> listNodeName = getListName(listRecord);
//    assertTrue(listNodeName.contains("A1"));
//    assertTrue(listNodeName.contains("A2"));
//  }
//
//  /**
//   * Copy record node in filePlan node to path which value is
//   * rma:transferLocation property of filePlan Node
//   * Input:
//   *    1. Add record for file plan node A4 from node record A2
//   *    2. Add mixin rma:transferable and set property (rma:transferDate, rma:dateReceived,
//   *    rma:transferLocation = "/TestTreeNode/A5", rma:transferExecuted = false) for node A2
//   * Expect:
//   *    1. Property rma:transferExecuted of node A2 is set to true
//   *    2. node A2 is copied to path "/TestTreeNode/A5"
//   * @throws Exception
//   */
//  public void testComputeTransfers() throws Exception {
//    Node nodeA2 = rootNode.getNode("TestTreeNode/A2");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//
//    recordsService.addRecord(nodeA4, nodeA2);
//
//    nodeA2.addMixin("rma:transferable");
//    nodeA2.setProperty("rma:transferDate", new GregorianCalendar());
//    nodeA2.setProperty("rma:transferLocation", "/TestTreeNode/A5");
//    nodeA2.setProperty("rma:transferExecuted", false);
//    nodeA2.setProperty("rma:dateReceived", new GregorianCalendar());
//
//    session.save();
//
//    recordsService.computeTransfers(rootNode.getNode("TestTreeNode"));
//
//    assertTrue(nodeA2.getProperty("rma:transferExecuted").getBoolean());
//    Node node = (Node)session.getItem("/TestTreeNode/A5/A2");
//    assertNotNull(node);
//  }
//
//  /**
//   * Copy record node in filePlan node to path which value is rma:accessionLocation property of
//   * filePlan Node
//   * Input:
//   *    1. Create a new node A1 (nodeA1Copy) under node A4 (copy)
//   *    2. Set property rma:accessionLocation = "/TestTreeNode/A5" for node A4
//   *    3. Add record for file plan node A3 from node record nodeA1Copy
//   *    4. Add mixin rma:accessionable and set property (rma:accessionDate, rma:dateReceived,
//   *    rma:accessionExecuted = false) for node nodeA1Copy
//   * Expect:
//   *    1. Property rma:accessionExecuted of node nodeA1Copy is set to true
//   *    2. nodeA1Copy is copied to path "/TestTreeNode/A5"
//   * @throws Exception
//   */
//  public void testComputeAccessions() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//    nodeA4.setProperty("rma:accessionLocation", "/TestTreeNode/A5");
//
//    session.getWorkspace().copy(nodeA1.getPath(), "/TestTreeNode/A4/A1");
//    Node nodeA1Copy = (Node)session.getItem("/TestTreeNode/A4/A1");
//    session.save();
//
//    recordsService.addRecord(nodeA3, nodeA1Copy);
//    session.save();
//
//    nodeA1Copy.addMixin("rma:accessionable");
//    nodeA1Copy.setProperty("rma:accessionExecuted", false);
//    nodeA1Copy.setProperty("rma:accessionDate", new GregorianCalendar());
//    nodeA1Copy.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    recordsService.computeAccessions(rootNode.getNode("TestTreeNode/A4"));
//    assertTrue(nodeA1Copy.getProperty("rma:accessionExecuted").getBoolean());
//    Node node = (Node)session.getItem("/TestTreeNode/A5/A1");
//    assertNotNull(node);
//  }
//
//  /**
//   * Remove record node in filePlan node
//   * Input:
//   *    1. Create a new node A1 (nodeA1Copy) under node A4 (copy)
//   *    2. Set property rma:accessionLocation = "/TestTreeNode/A5" for node A4
//   *    3. Add record for file plan node A3 from node record nodeA1Copy
//   *    4. Add mixin rma:accessionable and set property (rma:accessionDate, rma:dateReceived,
//   *    rma:accessionExecuted = false) for nodeA1Copy
//   * Expect:
//   *    1. Property rma:accessionExecuted of node nodeA1Copy is set to true
//   *    2. nodeA1Copy is copied to path "/TestTreeNode/A5"
//   * @throws Exception
//   */
//  public void testComputeDestructions() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//    nodeA4.setProperty("rma:accessionLocation", "/TestTreeNode/A5");
//
//    session.getWorkspace().copy(nodeA1.getPath(), "/TestTreeNode/A4/A1");
//    Node nodeA1Copy = (Node)session.getItem("/TestTreeNode/A4/A1");
//    session.save();
//
//    recordsService.addRecord(nodeA3, nodeA1Copy);
//    session.save();
//
//    nodeA1Copy.addMixin("rma:destroyable");
//    Calendar calendar = Calendar.getInstance();
//    calendar.add(Calendar.YEAR, 2);
//    nodeA1Copy.setProperty("rma:destructionDate", calendar);
//    nodeA1Copy.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    recordsService.computeDestructions(nodeA4);
//
//    try {
//      session.getItem("/TestTreeNode/A4/A1");
//      fail("Node A1 has been removed!");
//    } catch (PathNotFoundException e) {
//    }
//  }
//
//  /**
//   * Determine if the next phase is a hold, transfer or destruction
//   * Input:
//   *    1. Create a new node A1 (nodeA1Copy) under node A4 (copy)
//   *    2. Set property rma:discretionaryHold = true for node A4
//   *    3. Add record for file plan node A3 from node record nodeA1Copy
//   *    4. Add mixin rma:cutoffable and set property (rma:cutoffDateTime, rma:dateReceived,
//   *    rma:cutoffExecuted = false, rma:isObsolete = true) for nodeA1Copy
//   * Expect:
//   *    nodeA1Copy: rma:holdsDiscretionary = true, rma:holdUntilEvent = "EventToWaitFor",
//   *    rma:cutoffExecuted = true
//   * @throws Exception
//   */
//  public void testComputeCutoffs() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//    session.getWorkspace().copy(nodeA1.getPath(), "/TestTreeNode/A4/A1");
//    nodeA4.setProperty("rma:discretionaryHold", true);
//    Node nodeA1Copy = (Node)session.getItem("/TestTreeNode/A4/A1");
//    session.save();
//
//    recordsService.addRecord(nodeA3, nodeA1Copy);
//
//    nodeA1Copy.addMixin("rma:cutoffable");
//    nodeA1Copy.setProperty("rma:cutoffDateTime", new GregorianCalendar());
//    nodeA1Copy.setProperty("rma:cutoffExecuted", false);
//    nodeA1Copy.setProperty("rma:isObsolete", true);
//    nodeA1Copy.setProperty("rma:dateReceived", new GregorianCalendar());
//    session.save();
//
//    recordsService.computeCutoffs(nodeA4);
//    assertTrue(nodeA1Copy.getProperty("rma:holdsDiscretionary").getBoolean());
//    assertEquals(nodeA1Copy.getProperty("rma:holdUntilEvent").getString(), "EventToWaitFor");
//    assertTrue(nodeA1Copy.getProperty("rma:cutoffExecuted").getBoolean());
//  }
//
//  /**
//   * Process transfer or destruction
//   * Input:
//   *    1. Create a new node A1 (nodeA1Copy) under node A4 (copy)
//   *    2. Set property rma:defaultTransferLocation = "location" for node A4
//   *    3. Add record for file plan node A3 from node record nodeA1Copy
//   *    4. Add mixin rma:holdable and set property (rma:holdUntil,
//   *    rma:holdExecuted = false for nodeA1Copy
//   * Expect:
//   *    nodeA1Copy: rma:holdExecuted = true, rma:transferLocation = "location",
//   * @throws Exception
//   */
//  public void testComputeHolds() throws Exception {
//    Node nodeA1 = rootNode.getNode("TestTreeNode/A1");
//    Node nodeA3 = rootNode.getNode("TestTreeNode/A3");
//    Node nodeA4 = rootNode.getNode("TestTreeNode/A4");
//    nodeA4.setProperty("rma:defaultTransferLocation", "location");
//    session.getWorkspace().copy(nodeA1.getPath(), "/TestTreeNode/A4/A1");
//    Node nodeA1Copy = (Node)session.getItem("/TestTreeNode/A4/A1");
//    session.save();
//
//    recordsService.addRecord(nodeA3, nodeA1Copy);
//    nodeA1Copy.addMixin("rma:holdable");
//    nodeA1Copy.setProperty("rma:holdExecuted", false);
//    nodeA1Copy.setProperty("rma:holdUntil", new GregorianCalendar());
//    session.save();
//
//    recordsService.computeHolds(nodeA4);
//    assertTrue(nodeA1Copy.getProperty("rma:holdExecuted").getBoolean());
//    assertEquals(nodeA1Copy.getProperty("rma:transferLocation").getString(), "location");
//  }
//
//  /**
//   * Add action for filePlan node in repository
//   * Input:
//   *    Node A3 - filePlan
//   * Expect:
//   *    actionNode: not null, type = "exo:processRecordAction";
//   * @throws Exception
//   */
//  public void testBindFilePlanAction() throws Exception {
//    Node nodeA3 = (Node)session.getItem("/TestTreeNode/A3");
//    recordsService.bindFilePlanAction(nodeA3, REPO_NAME);
//
//    Node actionNameNode = nodeA3.getNode("exo:actions/processRecords");
//    assertNotNull(actionNameNode);
//    assertEquals(actionNameNode.getPrimaryNodeType().getName(), "exo:processRecordAction");
//  }
//
//  private void deleteNode(NodeIterator iter) throws Exception {
//    while (iter.hasNext()) {
//      Node node = iter.nextNode();
//      if (node.getNodes().getSize() > 0) deleteNode(node.getNodes());
//      NodeType[] mixins = node.getMixinNodeTypes();
//      for (NodeType mixinNode : mixins) {
//        node.removeMixin(mixinNode.getName());
//      }
//    }
//  }
//
//  private List<String> getListName(List<Node> listNode) throws Exception {
//    List<String> listNodeName = new ArrayList<String>();
//    for (Node node : listNode) {
//      listNodeName.add(node.getName());
//    }
//    return listNodeName;
//  }

  public void tearDown() throws Exception {
    try {
      Node testNode = rootNode.getNode("TestTreeNode");
//      deleteNode(testNode.getNodes());
      testNode.remove();
      session.save();
    } catch (Exception e) {
    }
    super.tearDown();
  }
}
