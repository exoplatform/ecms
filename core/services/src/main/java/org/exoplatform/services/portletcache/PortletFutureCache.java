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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.exoplatform.commons.cache.future.FutureCache;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Deprecated
class PortletFutureCache extends FutureCache<WindowKey, MarkupFragment, PortletRenderContext>
{

  /**
   * default to 5000 entries
   */
  private static final int                                   DEFAULT_CACHE_SIZE = 5000;

  /** . */
  private final ConcurrentHashMap<WindowKey, MarkupFragment> entries;

  /** . */
  private final Log                                          log;

  /** . */
  private ScheduledExecutorService                           scheduler          = Executors.newScheduledThreadPool(1);

  /** . */
  private int                                                cleanupCache;

  /** . */
  private int                                                cacheMaxSize       = DEFAULT_CACHE_SIZE;

  /** . */
  private ScheduledFuture<?>                                 scheduled;

  PortletFutureCache(Log log, int cleanupCache)
  {
    super(new PortletRenderer(log));

    //
    this.log = log;
    this.entries = new ConcurrentHashMap<WindowKey, MarkupFragment>();
    this.cleanupCache = preventWrongCleanupCacheValue(cleanupCache);
    this.scheduled = null;
  }

  PortletFutureCache(Log log, int cleanupCache, int cacheSize)
  {
    this(log, cleanupCache);
    this.cacheMaxSize = cacheSize;
  }

  private static int preventWrongCleanupCacheValue(int value)
  {
    // 10 mns by default
    return value < 0 ? 5 * 60 : value;
  }

  public int getCleanupCache()
  {
    return cleanupCache;
  }

  public void updateCleanupCache(int cleanupCache)
  {
    this.cleanupCache = cleanupCache;

    //
    if (scheduled != null)
    {
      stop();
      start();
    }
  }

  /*
   * Returns the number of entries in this cache.
   */
  protected int getCacheSize()
  {
    return (entries.size());
  }

  /*
   * Returns the defined Max Cache Size.
   */
  protected int getCacheMaxSize()
  {
    return (cacheMaxSize);
  }

  /*
   * Returns the defined Max Cache Size.
   */
  protected void setCacheMaxSize(int cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize;
  }

  /*
   * Clear Cache (should be called by JMX in urgently mode.
   */
  protected void clearCache()
  {
    if (scheduled != null)
    {
      stop();
      entries.clear();
      start();
    }
    else
      entries.clear();
  }

  /*
   * Did Cache contains the following key (do not remove old values).
   */
  protected boolean containsKey(WindowKey key)
  {
    return (entries.containsKey(key));
  }


  @Override
  protected MarkupFragment get(WindowKey key)
  {
    // System.out.println("get asked for key " + key);

    MarkupFragment value = entries.get(key);
    if (value != null)
    {
      if (value.expirationTimeMillis > System.currentTimeMillis())
      {
        if (log.isTraceEnabled())
          log.trace("Using cached markup for portlet " + key);
        return value;
      }
      if (log.isTraceEnabled())
        log.trace("Expired markup for portlet " + key);
      entries.remove(key);
      return null;
    }
    return null;
  }

  @Override
  protected void put(WindowKey key, MarkupFragment value)
  {
    boolean canInsert = false;

    // System.out.println("put asked for key " + key + " duration " + (value.expirationTimeMillis - System.currentTimeMillis()));

    if (value.expirationTimeMillis > System.currentTimeMillis()) {
      if ((entries.size() < cacheMaxSize))
        canInsert = true;

      if (canInsert) {
        entries.put(key, value);
        if (log.isTraceEnabled())
          log.trace("Cached markup for portlet " + key);

        // System.out.println("Cached markup for portlet " + key);
      }
    }
  }

  @Override
  protected void putOnly(WindowKey key, MarkupFragment value) {
    put(key, value);
  }

  public void start()
  {
    if (scheduled == null)
    {
      if (log.isDebugEnabled()) {
        log.debug("Starting cache cleaner with a period of " + cleanupCache + " seconds");
      }
      if(scheduler.isShutdown()) scheduler = Executors.newScheduledThreadPool(1);
      scheduled = scheduler.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          long now = System.currentTimeMillis();
          for (Iterator<Map.Entry<WindowKey, MarkupFragment>> i = entries.entrySet().iterator(); i.hasNext();)
          {
            Map.Entry<WindowKey, MarkupFragment> entry = i.next();
            if (entry.getValue().expirationTimeMillis < now)
            {
              if (log.isTraceEnabled())
                log.trace("Removing expired entry " + entry.getKey().getWindowId());

              // System.out.println("Removing expired entry " + entry.getKey().getWindowId());

              i.remove();
            }
          }
        }
      }, cleanupCache, cleanupCache, TimeUnit.SECONDS);
    }
  }

  public void stop()
  {
    if (scheduled != null)
    {
      if (log.isDebugEnabled()) {
        log.debug("Stopping cache cleaner");
      }
      scheduled.cancel(false);
      scheduled = null;
    }
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }
}
