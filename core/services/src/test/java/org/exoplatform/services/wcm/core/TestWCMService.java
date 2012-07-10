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
package org.exoplatform.services.wcm.core;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 14, 2009
 */
public class TestWCMService extends BaseWCMTestCase {

  /** The WCM Core Service. */
  private WCMService wcmService;

  /** The jcr node. */
  private Node node;
  
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    wcmService = (WCMService) container.getComponentInstanceOfType(WCMService.class);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWCMTestCase#setUp()
   */
  @BeforeMethod
  public void setUp() throws Exception {
    applySystemSession();
    node = session.getRootNode().addNode("parentNode").addNode("childNode");
    node.addMixin("mix:referenceable");
    session.save();
    
  }

  /**
   * Test get referenced jcr node by path.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetReferencedContent1() throws Exception {
    String nodePath = "/parentNode/childNode";
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node resultNode = wcmService.getReferencedContent(sessionProvider, REPO_NAME, COLLABORATION_WS, nodePath);
    assertEquals(resultNode.getPath(), nodePath);
  }

  /**
   * Test get referenced jcr node by UUID.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetReferencedContent2() throws Exception {
    String nodeUUID = node.getUUID();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node resultNode = wcmService.getReferencedContent(sessionProvider, REPO_NAME, COLLABORATION_WS, nodeUUID);
    assertEquals(resultNode.getUUID(), nodeUUID);
  }

  /**
   * Test get null if input is wrong identifier.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetReferencedContent3() throws Exception {
    String nodeIdentifier = "WrongIdentifier";
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node resultNode = wcmService.getReferencedContent(sessionProvider, REPO_NAME, COLLABORATION_WS, nodeIdentifier);
    assertNull(resultNode);
  }

  /**
   * Test a portal is shared portal.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIsSharedPortal1() throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    boolean isSharedPortal = wcmService.isSharedPortal(sessionProvider, "shared");
    assertTrue(isSharedPortal);
  }

  /**
   * Test a portal is not shared portal.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIsSharedPortal2() throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    boolean isSharedPortal = wcmService.isSharedPortal(sessionProvider, "classic");
    assertFalse(isSharedPortal);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterMethod
  protected void tearDown() throws Exception {
    session.getRootNode().getNode("parentNode").remove();
    session.save();
  }
}
