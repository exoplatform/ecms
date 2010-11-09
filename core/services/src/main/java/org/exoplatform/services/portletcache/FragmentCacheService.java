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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class FragmentCacheService implements Startable
{

   /** . */
   private static final Log log = ExoLogger.getLogger(FragmentCacheService.class);

   /** . */
   final PortletFutureCache cache;

   public FragmentCacheService(InitParams params)
   {
      int cleanupCache = -1;
      if (params.getValueParam("cleanup-cache") != null)
      {
         String cleanupCacheConfig = params.getValueParam("cleanup-cache").getValue();
         try
         {
            cleanupCache = Integer.parseInt(cleanupCacheConfig);
         }
         catch (NumberFormatException e)
         {
            log.warn("Invalid cleanup-cache setting " + cleanupCacheConfig);
         }
      }

      //
      this.cache = new PortletFutureCache(log, cleanupCache);
   }

   public int getCleanupCache()
   {
      return cache.getCleanupCache();
   }

   public void setCleanupCache(int cleanupCache)
   {
      this.cache.updateCleanupCache(cleanupCache);
   }

   public void start()
   {
      cache.start();
   }

   public void stop()
   {
      cache.stop();
   }
}
