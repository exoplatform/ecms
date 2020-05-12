
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
package org.exoplatform.services.cms.clouddrives.webui.action;

import java.util.Map;

import org.exoplatform.services.cms.clouddrives.webui.filters.NotCloudDriveOrFileFilter;

/**
 * Overrides original ECMS's filter to do not accept Cloud Drive and its
 * files.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IsVersionableOrAncestorFilter.java 00000 Jul 8, 2015 pnedonosko
 *          $
 */
public class IsVersionableOrAncestorFilter extends
                                           org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableOrAncestorFilter {

  /** The not cloud drive filter. */
  private NotCloudDriveOrFileFilter notCloudDriveFilter = new NotCloudDriveOrFileFilter();

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (notCloudDriveFilter.accept(context)) {
      return super.accept(context);
    } else {
      return false;
    }
  }
}
