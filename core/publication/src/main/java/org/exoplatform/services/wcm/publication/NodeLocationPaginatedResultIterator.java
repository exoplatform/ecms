/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication;

import java.util.ArrayList;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 */
@SuppressWarnings({ "deprecation", "unchecked" })
public class NodeLocationPaginatedResultIterator extends PaginatedResultIterator {

  public NodeLocationPaginatedResultIterator(int pageSize) {
    super(pageSize);
  }

  public NodeLocationPaginatedResultIterator(Result result, int pageSize) {
    super(result, pageSize);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.commons.utils.PageList#populateCurrentPage(int)
   */
  protected void populateCurrentPage(int page) throws Exception {
    if(page == currentPage_) {
      if(currentListPage_ != null)
        return;
    }
    currentListPage_ = new ArrayList();

    WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);
    result.getFiltersDescriber().put(WCMComposer.FILTER_LIMIT, ""+this.getPageSize());
    result.getFiltersDescriber().put(WCMComposer.FILTER_OFFSET, ""+(this.getPageSize()*(page-1)));
    result.getFiltersDescriber().put(WCMComposer.FILTER_TOTAL, ""+this.result.getNumTotal());
    result = composer.getPaginatedContents(result.getNodeLocationDescriber(),
                                           result.getFiltersDescriber(),
                                           WCMCoreUtils.getUserSessionProvider());

    currentListPage_ = NodeLocation.getLocationsByNodeList(result.getNodes());

    currentPage_ = page;
  }

}
