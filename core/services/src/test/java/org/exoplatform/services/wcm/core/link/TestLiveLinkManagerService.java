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
package org.exoplatform.services.wcm.core.link;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.link.LiveLinkManagerService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 22, 2009
 */
public class TestLiveLinkManagerService extends BaseWCMTestCase {

  /** The live link manager service. */
  private LiveLinkManagerService liveLinkManagerService;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    liveLinkManagerService = getService(LiveLinkManagerService.class);
    Node folder = (Node) session.getItem("/sites content/live/classic/web contents");
    createWebcontentNode(folder, "webcontent", "This is the live link: <a href='http://www.google.com'>Goolge</a> and this is the broken link: <a href='http://www.thiscannotbeanactivelink.com'>Broken</a>", null, null);
  }

  /**
   * Test extract links.
   *
   * @throws Exception the exception
   */
  public void testExtractLinks() throws Exception {
    Node result = (Node) session.getItem("/sites content/live/classic/web contents/webcontent");
    Node htmlFile = result.getNode("default.html");
    List<String> links = liveLinkManagerService.extractLinks(htmlFile);
    assertEquals(2, links.size());
    assertEquals("http://www.google.com", links.get(0));
    assertEquals("http://www.thiscannotbeanactivelink.com", links.get(1));
  }

  /**
   * Test update link data for node.
   *
   * @throws Exception the exception
   */
  public void testUpdateLinkDataForNode() throws Exception {
    liveLinkManagerService.updateLinks("classic");
    Node result = (Node) session.getItem("/sites content/live/classic/web contents/webcontent");
    List<String> links = new ArrayList<String>();
    links.add("http://www.mozilla.com");
    liveLinkManagerService.updateLinkDataForNode(result, links);

    Value[] values = result.getProperty(NodetypeConstant.EXO_LINKS).getValues();
    assertEquals(1, values.length);
    assertEquals("status=unchecked@url=http://www.mozilla.com", values[0].getString());
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    session.getItem("/sites content/live/classic/web contents/webcontent").remove();
    session.save();
  }
}
