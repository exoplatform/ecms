/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.portletcache;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.test.BasicTestCase;

public class TestFragmentCacheService extends BasicTestCase
{

  /*
   * (non-Javadoc)
   *
   * @see org.exoplatform.services.wcm.core.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

  }

  /**
   * Test Cache when cleaned
   *
   * @throws Exception
   *             the exception
   */
  public void testInvalidArgumentException() throws Exception {

    InitParams params = new InitParams();
    FragmentCacheService service = new FragmentCacheService(params);

    try {
      service.setCacheSize(1000);
    } catch (IllegalArgumentException e) {
      fail("Shouldn't raise an IllegalArgumentException");
    }

    try {
      service.setCacheSize(0);
      fail("Should raise an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }



  }

  /*
   * (non-Javadoc)
   *
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    super.tearDown();
  }

}
