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
package org.exoplatform.services.ecm.dms.test;

import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.test.BasicTestCase;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 avr. 2009
 */
public class LinkUtilsTest extends BasicTestCase {


  public void testEvaluatePath() {
    assertEquals(LinkUtils.evaluatePath("//"), "/");
    assertEquals(LinkUtils.evaluatePath("///"), "/");
    assertEquals(LinkUtils.evaluatePath("/a/"), "/a");
    assertEquals(LinkUtils.evaluatePath("/////a////"), "/a");
    assertEquals(LinkUtils.evaluatePath("/.."), "/");
    assertEquals(LinkUtils.evaluatePath("/."), "/.");
    assertEquals(LinkUtils.evaluatePath("/.a"), "/.a");
    assertEquals(LinkUtils.evaluatePath("/../.."), "/");
    assertEquals(LinkUtils.evaluatePath("/.././.."), "/");
    assertEquals(LinkUtils.evaluatePath("/./../.."), "/");
    assertEquals(LinkUtils.evaluatePath("/a/.."), "/");
    assertEquals(LinkUtils.evaluatePath("/./a/.."), "/");
    assertEquals(LinkUtils.evaluatePath("/a/."), "/a/.");
    assertEquals(LinkUtils.evaluatePath("/a/../a"), "/a");
    assertEquals(LinkUtils.evaluatePath("/a/./b"), "/a/b");
    assertEquals(LinkUtils.evaluatePath("/a//.//b//"), "/a/b");
    assertEquals(LinkUtils.evaluatePath("/a/b/../c/.."), "/a");
    assertEquals(LinkUtils.evaluatePath("/a/b/../c/../.."), "/");
    assertEquals(LinkUtils.evaluatePath("/a/b/../c/../../.."), "/");

  }
}
