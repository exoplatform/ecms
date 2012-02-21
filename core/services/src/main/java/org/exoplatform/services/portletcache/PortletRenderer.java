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

import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class PortletRenderer implements Loader<WindowKey, MarkupFragment, PortletRenderContext>
{

  /** . */
  private final Log log;

  PortletRenderer(Log log)
  {
    this.log = log;
  }

  public MarkupFragment retrieve(PortletRenderContext context, WindowKey key) throws Exception
  {
    BufferedRenderResponse bufferedResp = new BufferedRenderResponse(context.resp);
    context.chain.doFilter(context.req, bufferedResp);

    //
    long now = System.currentTimeMillis();

    //
    String expirationCache = bufferedResp.getExpirationCache();
    long expirationCacheMillis = now;
    if (expirationCache != null)
    {
      try
      {
        int expirationCacheSec = Integer.parseInt(expirationCache);
        if (expirationCacheSec == -1)
        {
          expirationCacheMillis = Long.MAX_VALUE;
        }
        else if (expirationCacheSec > 0)
        {
          expirationCacheMillis += 1000 * expirationCacheSec;
        }
      }
      catch (NumberFormatException e)
      {
        if (log.isWarnEnabled()) {
          log.warn("Incorrect expiration cache value " + expirationCache);
        }
      }
    }

    //
    return new MarkupFragment(expirationCacheMillis, bufferedResp.getBytes());
  }
}
