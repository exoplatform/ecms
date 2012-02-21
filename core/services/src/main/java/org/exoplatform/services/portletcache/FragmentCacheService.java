/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.portletcache;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */

@Managed
@NameTemplate( { @Property(key = "view", value = "portal"),
    @Property(key = "service", value = "fragmentcache"), @Property(key = "type", value = "content") })
@ManagedDescription("FragmentCache Service")
@RESTEndpoint(path = "fragmentcacheservice")
public class FragmentCacheService implements Startable {

  private static final int DEFAULT_CACHE_SIZE    = 10000;                                          // default
                                                                                                    // to
                                                                                                    // 10000
                                                                                                    // entries
                                                                                                    // in
                                                                                                    // FutureCache

  private static final int DEFAULT_CACHE_CLEANUP = 15;                                             // default
                                                                                                    // 15s
                                                                                                    // interval
                                                                                                    // for
                                                                                                    // cleanup
                                                                                                    // thread

  /** . */
  private static final Log log                   = ExoLogger.getLogger(FragmentCacheService.class);

  /** . */
  final PortletFutureCache cache;

  public FragmentCacheService(InitParams params) {
    int cleanupCache = DEFAULT_CACHE_CLEANUP;
    int cacheSize = DEFAULT_CACHE_SIZE;

    if (params.getValueParam("cleanup-cache") != null) {
      String cleanupCacheConfig = params.getValueParam("cleanup-cache").getValue();
      try {
        cleanupCache = Integer.parseInt(cleanupCacheConfig);
      } catch (NumberFormatException e) {
        if (log.isWarnEnabled()) {
          log.warn("Invalid cleanup-cache setting " + cleanupCacheConfig);
        }
      }
    }

    if (params.getValueParam("cache-size") != null) {
      String cacheSizeConfig = params.getValueParam("cache-size").getValue();
      try {
        cacheSize = Integer.parseInt(cacheSizeConfig);
      } catch (NumberFormatException e) {
        if (log.isWarnEnabled()) {
          log.warn("Invalid cache-size setting " + cacheSizeConfig);
        }
      }
    }

    this.cache = new PortletFutureCache(log, cleanupCache, cacheSize);

  }

  @Managed
  @ManagedDescription("What is the Cleanup Cache period (in seconds) ?")
  public int getCleanupCache() {
    return cache.getCleanupCache();
  }

  @Managed
  @ManagedDescription("How many Entries in Cache  ?")
  public int getCacheSize() {
    return cache.getCacheSize();
  }

  @Managed
  @ManagedDescription("Get Maximum Entries in Cache")
  public int getCacheMaxSize() {
    return cache.getCacheMaxSize();
  }

  @Managed
  @ManagedDescription("Set Maximum Entries in Cache")
  /***
   * Set Max Cache size, ie, max entries allowed in FutureCache.
   * An IllegalArgumentException is thrown if cacheMaxSize is less than 1.
   */
  public void setCacheSize(int cacheMaxSize) {
    if (cacheMaxSize < 1)
      throw new IllegalArgumentException("invalid value for max cache size");

    cache.setCacheMaxSize(cacheMaxSize);
  }

  @Managed
  @ManagedDescription("Sets Cleanup Cache period (in seconds)")
  public void setCleanupCache(int cleanupCache) {
    this.cache.updateCleanupCache(cleanupCache);
  }

  public void start() {
    cache.start();
  }

  public void stop() {
    cache.stop();
  }

  @Managed
  @ManagedDescription("Clear the Fragment Cache")
  public void clearCache() {
    cache.clearCache();
  }
}
