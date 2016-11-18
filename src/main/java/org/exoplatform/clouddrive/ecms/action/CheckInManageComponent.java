
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
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.List;

/**
 * Overrides original ECMS component to replace {@link IsVersionableOrAncestorFilter} with safe version that
 * can work with symlinks to private user Cloud Drive files.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CheckInManageComponent.java 00000 Jul 8, 2015 pnedonosko $
 * 
 */
@ComponentConfig(events = { @EventConfig(listeners = CheckInManageComponent.CheckInActionListener.class) })
public class CheckInManageComponent extends
                                    org.exoplatform.ecm.webui.component.explorer.rightclick.manager.CheckInManageComponent {

  protected static IsVersionableOrAncestorFilter cdFilter = new IsVersionableOrAncestorFilter();

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    List<UIExtensionFilter> filters = super.getFilters();

    // replace ECMS's IsVersionableOrAncestorFilter with ones from Cloud Drive
    for (int i = 0; i < filters.size(); i++) {
      UIExtensionFilter of = filters.get(i);
      if (of instanceof org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableOrAncestorFilter) {
        filters.set(i, cdFilter);
      }
    }

    return filters;
  }

}
