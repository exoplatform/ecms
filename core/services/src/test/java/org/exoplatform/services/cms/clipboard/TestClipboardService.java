/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.services.cms.clipboard;

import java.util.Set;

import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 28, 2014  
 */
public class TestClipboardService extends BaseWCMTestCase {
  
  ClipboardService clipboardService_ = null;
  /**
   * add some nodes for testing because getClipboardList check the existing of node
   * some nodes will not be found if workspace is not defined
   */
  public void setUp() throws Exception {
    super.setUp();
    session.getRootNode().addNode("documents");
    session.getRootNode().addNode("music");
    session.getRootNode().addNode("favorites");
    session.getRootNode().addNode("pictures");
    session.save();
    clipboardService_ = WCMCoreUtils.getService(ClipboardService.class);
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/documents", "collaboration"), false);
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/music", "collaboration"), false);
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/favorites", "collaboration"), true);
    //duplication
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/documents", "collaboration"), false);
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/documents", "collaboration"), true);
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.COPY, "/documents", "dms-system"), false);
    clipboardService_.addClipboardCommand("mary", 
      new ClipboardCommand(ClipboardCommand.CUT, "/documents", "collaboration"), true);
    clipboardService_.addClipboardCommand("mary", 
      new ClipboardCommand(ClipboardCommand.CUT, "/publics", "collaboration"), true);
    
  }
  
  public void tearDown() throws Exception {
    clipboardService_.clearClipboardList("john", true);
    clipboardService_.clearClipboardList("john", false);
    clipboardService_.clearClipboardList("mary", true);
    super.tearDown();
  }
  
  public void testAddClipboardCommand() throws Exception {
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/pictures", "collaboration"), false);
    // expect 3 because workspace "dms-system" doesn't exist in test platform
    assertEquals(3, clipboardService_.getClipboardList("john", false).size());
    clipboardService_.addClipboardCommand("john", 
      new ClipboardCommand(ClipboardCommand.CUT, "/public", "collaboration"), true);
    // expect 2 because public node is not added
    assertEquals(2, clipboardService_.getClipboardList("john", true).size());
    // expect 1 because publics node is not added
    assertEquals(1, clipboardService_.getClipboardList("mary", true).size());
    assertEquals(0, clipboardService_.getClipboardList("james", false).size());
  }
  
  public void testGetLastClipboard() throws Exception {
    ClipboardCommand command = clipboardService_.getLastClipboard("john");
    assertEquals("dms-system", command.getWorkspace());
    assertEquals(ClipboardCommand.COPY, command.getType());
    
    command = clipboardService_.getLastClipboard("mary");
    assertNull(command);
    
    clipboardService_.addClipboardCommand("mary", 
      new ClipboardCommand(ClipboardCommand.CUT, "/documents", "calendar"), false);
    command = clipboardService_.getLastClipboard("mary");
    assertEquals("calendar", command.getWorkspace());
    assertEquals(ClipboardCommand.CUT, command.getType());
  }

  public void testGetClipboardList() {
    Set<ClipboardCommand> commands = clipboardService_.getClipboardList("john", false);
    assertEquals(2, commands.size());
    commands = clipboardService_.getClipboardList("john", true);
    assertEquals(2, commands.size());
    commands = clipboardService_.getClipboardList("mary", true);
    assertEquals(1, commands.size());
    commands = clipboardService_.getClipboardList("mary", false);
    assertEquals(0, commands.size());
  }
  
  public void testClearClipboardList() {
    clipboardService_.clearClipboardList("john", false);
    assertEquals(0, clipboardService_.getClipboardList("john", false).size());
    
    clipboardService_.clearClipboardList("john", true);
    assertEquals(0, clipboardService_.getClipboardList("john", true).size());

    clipboardService_.clearClipboardList("mary", true);
    assertEquals(0, clipboardService_.getClipboardList("mary", true).size());
  }
  
  public void testRemoveClipboardCommand() {
    clipboardService_.removeClipboardCommand("john", new ClipboardCommand(ClipboardCommand.CUT, "/documents", "collaboration"));
    Set<ClipboardCommand> commands = clipboardService_.getClipboardList("john", false);
    assertEquals(1, commands.size());
    clipboardService_.removeClipboardCommand("john", new ClipboardCommand(ClipboardCommand.COPY, "/documents", "dms-system"));
    commands = clipboardService_.getClipboardList("john", false);
    assertEquals(1, commands.size());
    ClipboardCommand command = clipboardService_.getLastClipboard("john");
    assertEquals("collaboration", command.getWorkspace());
    assertEquals(ClipboardCommand.CUT, command.getType());
  }
}
