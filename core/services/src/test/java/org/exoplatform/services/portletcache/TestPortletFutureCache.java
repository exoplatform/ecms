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

import java.util.HashMap;
import java.util.Locale;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.test.BasicTestCase;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * henri.gomez@exoplatform.com Jan 19, 2011
 */
public class TestPortletFutureCache extends BasicTestCase {

  private static final Log LOG = ExoLogger.getLogger(TestPortletFutureCache.class.getName());

  /** The Portlet Future Cache */
  private PortletFutureCache portletFutureCache;

  private WindowKey windowKey1;
  private MarkupFragment fragment1;

  private WindowKey windowKey2;
  private MarkupFragment fragment2;

  private WindowKey windowKey3;
  private MarkupFragment fragment3;

  /*
   * (non-Javadoc)
   *
   * @see org.exoplatform.services.wcm.core.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

  }

  private void injectContents(long  content1Duration, long content2Duration, long content3Duration) {
    windowKey1 = new WindowKey("wid1",
                               WindowState.NORMAL,
                               PortletMode.VIEW,
                               Locale.FRENCH,
                               new HashMap<String, String[]>(),
                               new HashMap<String, String[]>());
    fragment1 = new MarkupFragment(System.currentTimeMillis() + content1Duration, new byte[10]); // New
                                                                                                 // Fragment,
                                                                                                 // to
                                                                                                 // be
                                                                                                 // kept
                                                                                                 // FRAGMENT_DURATION

    portletFutureCache.put(windowKey1, fragment1);

    if (content2Duration != -1) {
      windowKey2 = new WindowKey("wid2",
                                 WindowState.NORMAL,
                                 PortletMode.VIEW,
                                 Locale.FRENCH,
                                 new HashMap<String, String[]>(),
                                 new HashMap<String, String[]>());
      fragment2 = new MarkupFragment(System.currentTimeMillis() + content2Duration, new byte[10]); // New
                                                                                                   // Fragment,
                                                                                                   // to
                                                                                                   // be
                                                                                                   // kept
                                                                                                   // FRAGMENT_DURATION

      portletFutureCache.put(windowKey2, fragment2);
    }

    if (content3Duration != -1) {
      windowKey3 = new WindowKey("wid3",
                                 WindowState.NORMAL,
                                 PortletMode.VIEW,
                                 Locale.FRENCH,
                                 new HashMap<String, String[]>(),
                                 new HashMap<String, String[]>());
      fragment3 = new MarkupFragment(System.currentTimeMillis() + content3Duration, new byte[10]); // New
                                                                                                   // Fragment,
                                                                                                   // to
                                                                                                   // be
                                                                                                   // kept
                                                                                                   // FRAGMENT_DURATION

      portletFutureCache.put(windowKey3, fragment3);
    }


  }

  /**
   * Test Cache with size limits
   *
   * @throws Exception
   *             the exception
   */
  public void testCacheFixedSize() throws Exception {

    MarkupFragment nfragment;

    portletFutureCache = new PortletFutureCache(LOG, 1, 2);
    portletFutureCache.start();

    // 900ms, 1100ms, 2100ms retentions for objects
    injectContents(900, 1100, 2100);

    // CacheSize should be 2
    assertEquals(2, portletFutureCache.getCacheSize());

    // Should get back fragment1
    nfragment = portletFutureCache.get(windowKey1);
    assertSame(fragment1, nfragment);

    // Should get back fragment2
    nfragment = portletFutureCache.get(windowKey2);
    assertSame(fragment2, nfragment);

    // Should get back fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertNull(nfragment);

    portletFutureCache.stop();
  }

  /**
   * Test Cache when cleaned
   *
   * @throws Exception
   *             the exception
   */
  public void testCacheCleared() throws Exception {

    MarkupFragment nfragment;

    portletFutureCache = new PortletFutureCache(LOG, 1);
    portletFutureCache.start();

    // 900ms, 1100ms, 2100ms retentions for objects
    injectContents(900, 1100, 2100);

    // CacheSize should be 3
    assertEquals(3, portletFutureCache.getCacheSize());

    // Should get back fragment1
    nfragment = portletFutureCache.get(windowKey1);
    assertSame(fragment1, nfragment);

    // Should get back fragment2
    nfragment = portletFutureCache.get(windowKey2);
    assertSame(fragment2, nfragment);

    // Should get back fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertSame(fragment3, nfragment);

    portletFutureCache.clearCache();

    // CacheSize should be 0
    assertEquals(0, portletFutureCache.getCacheSize());

    // Should not get back  fragment1
    nfragment = portletFutureCache.get(windowKey1);
    assertNull( nfragment);

    // Should not get back  fragment2
    nfragment = portletFutureCache.get(windowKey2);
    assertNull(nfragment);

    // Should not get back  fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertNull(nfragment);

    portletFutureCache.stop();
  }

  /**
   * Test Cache Evictions during time
   *
   * @throws Exception
   *             the exception
   */
  public void testCacheEvictions() throws Exception {

    MarkupFragment nfragment;
    int cacheSize;

    portletFutureCache = new PortletFutureCache(LOG, 2);
    portletFutureCache.start();

    // 1000ms, 3500ms, 5100ms retentions for objects
    injectContents(1000, 3500, 5100);

    // CacheSize should be 3
    assertEquals(3, portletFutureCache.getCacheSize());

    // Should get back fragment1
    nfragment = portletFutureCache.get(windowKey1);
    assertSame(fragment1, nfragment);

    // Should get back fragment2
    nfragment = portletFutureCache.get(windowKey2);
    assertSame(fragment2, nfragment);

    // Should get back fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertSame(fragment3, nfragment);

    // CacheSize should be 3
    assertEquals(3, portletFutureCache.getCacheSize());

    Thread.sleep(3000); // Sleep 3000 -> time for first cleanup passes

    // CacheSize should be 2 (2100 + 1100)
    cacheSize = portletFutureCache.getCacheSize();
    System.out.println("After first eviction pass, cacheSize=" + cacheSize);
    assertEquals(2, cacheSize);

    // fragment1 shouldn't exist anymore
    nfragment = portletFutureCache.get(windowKey1);
    assertNull( nfragment);

    // Should get back fragment2
    nfragment = portletFutureCache.get(windowKey2);
    assertSame(fragment2, nfragment);

    // Should get back fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertSame(fragment3, nfragment);

    // CacheSize should be 2 (2100 + 1100)
    cacheSize = portletFutureCache.getCacheSize();
    System.out.println("After first eviction pass and gets, cacheSize=" + cacheSize);
    assertEquals(2, cacheSize);

    Thread.sleep(2000); // Sleep 2000 -> time for second cleanup passes

    // CacheSize should be 1 (2100)
    cacheSize = portletFutureCache.getCacheSize();
    System.out.println("After second eviction pass, cacheSize=" + cacheSize);
    assertEquals(1, cacheSize);

    // fragment1 shouldn't exist anymore
    nfragment = portletFutureCache.get(windowKey1);
    assertNull( nfragment);

    // fragment2 shouldn't exist anymore
    nfragment = portletFutureCache.get(windowKey2);
    assertNull(nfragment);

    // Should get back fragment3
    nfragment = portletFutureCache.get(windowKey3);
    assertSame(fragment3, nfragment);

    // CacheSize should be 1 (2100)
    cacheSize = portletFutureCache.getCacheSize();
    System.out.println("After second eviction pass and gets, cacheSize=" + cacheSize);
    assertEquals(1, cacheSize);

    Thread.sleep(2000); // Sleep 2000 -> time for third  cleanup pass

    // CacheSize should be 0, no more entries in cache
    cacheSize = portletFutureCache.getCacheSize();
    System.out.println("After third eviction pass, cacheSize=" + cacheSize);
    assertEquals(0, cacheSize);

    portletFutureCache.stop();
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
