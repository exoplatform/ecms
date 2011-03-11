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
class PortletFutureCache extends FutureCache<WindowKey, MarkupFragment, PortletRenderContext>
{

   /** . */
   private final ConcurrentHashMap<WindowKey, MarkupFragment> entries;

   /** . */
   private final Log log;

   /** . */
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   /** . */
   private int cleanupCache;

   /** . */
   private ScheduledFuture<?> scheduled;

   PortletFutureCache(Log log, int cleanupCache)
   {
      super(new PortletRenderer(log));

      //
      this.log = log;
      this.entries = new ConcurrentHashMap<WindowKey, MarkupFragment>();
      this.cleanupCache = preventWrongCleanupCacheValue(cleanupCache);
      this.scheduled = null;
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

   @Override
   protected MarkupFragment get(WindowKey key)
   {
      MarkupFragment value = entries.get(key);
      if (value != null)
      {
         if (value.expirationTimeMillis > System.currentTimeMillis())
         {
/*
            if (log.isTraceEnabled())
            {
               log.trace("Using cached markup for portlet " + key);
            }
*/
            //System.out.println("Using cached markup for portlet " + key);
            return value;
         }
         else
         {
            //System.out.println("Expired markup for portlet " + key);
            entries.remove(key);
            return null;
         }
      }
      else
      {
         return null;
      }
   }

   @Override
   protected void put(WindowKey key, MarkupFragment value)
   {
      if (value.expirationTimeMillis > System.currentTimeMillis())
      {
         entries.put(key, value);
         //System.out.println("Cached markup for portlet " + key);
/*
         if (log.isTraceEnabled())
         {
            log.trace("Cached markup for portlet " + key);
         }
*/
      }
   }

   public void start()
   {
      if (scheduled == null)
      {
         log.debug("Starting cache cleaner with a period of " + cleanupCache + " seconds");
         scheduler.scheduleWithFixedDelay(new Runnable()
         {
            public void run()
            {
               long now = System.currentTimeMillis();
               for (Iterator<Map.Entry<WindowKey, MarkupFragment>> i = entries.entrySet().iterator(); i.hasNext();)
               {
                  Map.Entry<WindowKey, MarkupFragment> entry = i.next();
                  if (entry.getValue().expirationTimeMillis > now)
                  {
                     log.trace("Removing expired entry " + entry.getKey().getWindowId());
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
         log.debug("Stopping cache cleaner");
         scheduled.cancel(false);
         scheduled = null;
      }
   }
}
