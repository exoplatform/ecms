
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.webui.filters;

import java.util.List;

/**
 * Filter files with size larger of configured.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileSizeSmallerOfFilter.java 00000 Jul 27, 2015 pnedonosko $
 */
public class CloudFileLargerFilter extends CloudFileFilter {

  /**
   * Instantiates a new cloud file larger filter.
   */
  public CloudFileLargerFilter() {
    super();
  }

  /**
   * Instantiates a new cloud file larger filter.
   *
   * @param providers the providers
   * @param minSize the min size
   * @param maxSize the max size
   */
  public CloudFileLargerFilter(List<String> providers, long minSize, long maxSize) {
    super(providers, minSize, maxSize);
    // TODO Auto-generated constructor stub
  }

  /**
   * Instantiates a new cloud file larger filter.
   *
   * @param providers the providers
   */
  public CloudFileLargerFilter(List<String> providers) {
    super(providers);
    // TODO Auto-generated constructor stub
  }

  /**
   * Instantiates a new cloud file larger filter.
   *
   * @param minSize the min size
   * @param maxSize the max size
   */
  public CloudFileLargerFilter(long minSize, long maxSize) {
    super(minSize, maxSize);
    // TODO Auto-generated constructor stub
  }

  /**
   * Instantiates a new cloud file larger filter.
   *
   * @param providers the providers
   * @param minSize the min size
   */
  public CloudFileLargerFilter(List<String> providers, long minSize) {
    super(providers, minSize, Long.MAX_VALUE);
  }

  /**
   * Instantiates a new cloud file larger filter.
   *
   * @param minSize the min size
   */
  public CloudFileLargerFilter(long minSize) {
    super(minSize, Long.MAX_VALUE);
  }
}
