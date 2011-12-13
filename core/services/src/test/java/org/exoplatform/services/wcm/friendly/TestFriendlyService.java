package org.exoplatform.services.wcm.friendly;
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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.wcm.friendly.impl.FriendlyServiceImpl;

import junit.framework.TestCase;

public class TestFriendlyService extends TestCase {

  private FriendlyServiceImpl fserv;

  public void setUp() throws Exception {
    InitParams initParams = new InitParams();
    fserv = new FriendlyServiceImpl(initParams);
  }

  public void testNotActiveByDefault() {
    assertFalse(fserv.isEnabled());
  }

  public void testUnfriendlyUriIfNotActive() {
    fserv.setEnabled(false);
    String friendlyUri = "news";
    String unfriendlyUri = "/public/acme/news?path=/acme";
    fserv.addFriendly(friendlyUri, unfriendlyUri);

    String target = fserv.getUnfriendlyUri(friendlyUri);
    assertEquals(friendlyUri, target);
  }

  public void testFriendlyUriIfNotActive() {
    fserv.setEnabled(false);
    String friendlyUri = "news";
    String unfriendlyUri = "/public/acme/news?path=/acme";
    fserv.addFriendly(friendlyUri, unfriendlyUri);

    String target = fserv.getFriendlyUri(unfriendlyUri);
    assertEquals(unfriendlyUri, target);
  }

  public void testNoDuplicates() {
    fserv.setEnabled(true);
    fserv.addFriendly("acme", "/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents");
    fserv.addFriendly("news", "/public/acme/news?path=/acme");
    fserv.addFriendly("news-detail", "/public/acme/detail?path=/acme");
    fserv.addFriendly("events", "/public/acme/events?path=/events");
    fserv.addFriendly("files", "/rest-ecmdemo/jcr/repository/collaboration/sites content/live/acme/documents");

    assertEquals(5, fserv.getFriendlies().size());
  }

  public void testFriendly() {
    fserv.setEnabled(true);
    fserv.setServletName("content");
    fserv.addFriendly("acme", "/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents");
    fserv.addFriendly("news", "/public/acme/news?path=/acme");
    fserv.addFriendly("news-detail", "/public/acme/detail?path=/acme");
    fserv.addFriendly("events", "/public/acme/events?path=/events");
    fserv.addFriendly("files", "/rest-ecmdemo/jcr/repository/collaboration/sites content/live/acme/documents");

    // returns friendly
    assertEquals("http://monsite/ecmdemo/content/acme/News/News1", fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents"+"/News/News1"));
    assertEquals("http://monsite/ecmdemo/content/news/MyCategory/myContent", fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/news?path=/acme/MyCategory/myContent"));
    assertEquals("http://monsite/ecmdemo/content/files/doc1", fserv.getFriendlyUri("http://monsite/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/sites content/live/acme/documents/doc1"));
    // no friendly
    assertEquals("http://monsite/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/doc1", fserv.getFriendlyUri("http://monsite/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/doc1"));

  }

  public void testUnfriendly() {
    fserv.setEnabled(true);
    fserv.setServletName("content");
    fserv.addFriendly("acme", "/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents");
    fserv.addFriendly("news", "/public/acme/news?path=/acme");
    fserv.addFriendly("news-detail", "/public/acme/detail?path=/acme");
    fserv.addFriendly("events", "/public/acme/events?path=/events");
    fserv.addFriendly("files", "/rest-ecmdemo/jcr/repository/collaboration/sites content/live/acme/documents");

    // returns unfriendly
    assertEquals("/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents/News/News1", fserv.getUnfriendlyUri("/ecmdemo/content/acme/News/News1"));
    // no unfriendly
    assertEquals("/ecmdemo/content/xxxx/News/News1", fserv.getUnfriendlyUri("/ecmdemo/content/xxxx/News/News1"));

  }

  public void testRemoveFriendly() {
    fserv.setEnabled(true);
    fserv.setServletName("content");
    fserv.addFriendly("acme", "/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents");
    fserv.addFriendly("news", "/public/acme/news?path=/acme");
    fserv.addFriendly("news-detail", "/public/acme/detail?path=/acme");
    fserv.addFriendly("events", "/public/acme/events?path=/events");
    fserv.addFriendly("files", "/rest-ecmdemo/jcr/repository/collaboration/sites content/live/acme/documents");

    // returns friendly
    assertEquals(5, fserv.getFriendlies().size());
    assertEquals("http://monsite/ecmdemo/content/acme/News/News1", fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents"+"/News/News1"));
    fserv.removeFriendly("acme");
    assertEquals(4, fserv.getFriendlies().size());
    assertEquals("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents"+"/News/News1", fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents"+"/News/News1"));
  }

  public void testPriority() {
    InitParams initParams = new InitParams();
    fserv = new FriendlyServiceImpl(initParams);
    fserv.setEnabled(true);
    fserv.setServletName("content");
    fserv.addFriendly("acme", "/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents");
    fserv.addFriendly("collab", "/public/acme/detail?path=/repository/collaboration");

    assertEquals("http://monsite/ecmdemo/content/acme/News/News1",
                 fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents/News/News1"));
    assertEquals("http://monsite/ecmdemo/content/collab/Doc/Doc1",
                 fserv.getFriendlyUri("http://monsite/ecmdemo/public/acme/detail?path=/repository/collaboration/Doc/Doc1"));


  }

}
