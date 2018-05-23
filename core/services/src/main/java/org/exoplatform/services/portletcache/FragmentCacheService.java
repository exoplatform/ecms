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

import org.exoplatform.commons.cache.future.FutureCache;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
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

  private static final int DEFAULT_CACHE_SIZE    = 10000;  // default to 10000 entries in FutureCache

  private static final int DEFAULT_CACHE_CLEANUP = 30;     // default 30s interval for cleanup thread
  
  /** . */
  private static final Log LOG                   = ExoLogger.getLogger(FragmentCacheService.class.getName());

  private final static String CACHE_NAME = "ecms.FragmentCacheService";

  /** . */
  private ExoCache<WindowKey, MarkupFragment> markupCache_;
  private FutureCache<WindowKey, MarkupFragment, PortletRenderContext> futureCache;
  
  public FragmentCacheService(CacheService cacheService, InitParams params) {
    markupCache_ = cacheService.getCacheInstance(CACHE_NAME);
    futureCache = new FutureExoCache<WindowKey, MarkupFragment, PortletRenderContext>(new PortletRenderer(LOG), markupCache_);
  }
  
  public MarkupFragment getMarkupFragment(PortletRenderContext context, WindowKey key) {
    return futureCache.get(context, key);
  }
  
  public void setCacheSize(int size) {
    markupCache_.setMaxSize(size);
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

}
