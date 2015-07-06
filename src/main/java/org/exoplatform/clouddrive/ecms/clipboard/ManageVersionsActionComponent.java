/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.clipboard;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.List;

/**
 * Overrides original ECMS component to replace {@link IsNotIgnoreVersionNodeFilter} with safe version that
 * can work with symlinks to private user Cloud Drive files.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ManageVersionsActionComponent.java 00000 Jul 6, 2015 pnedonosko $
 * 
 */
@ComponentConfig(events = {
    @EventConfig(listeners = ManageVersionsActionComponent.ManageVersionsActionListener.class) })
public class ManageVersionsActionComponent extends
                                           org.exoplatform.ecm.webui.component.explorer.control.action.ManageVersionsActionComponent {

  protected static IsNotIgnoreVersionNodeFilter cdFilter = new IsNotIgnoreVersionNodeFilter();

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    List<UIExtensionFilter> filters = super.getFilters();

    // replace ECMS's IsNotIgnoreVersionNodeFilter with ones from Cloud Drive
    for (int i = 0; i < filters.size(); i++) {
      UIExtensionFilter of = filters.get(i);
      if (of instanceof org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotIgnoreVersionNodeFilter) {
        filters.set(i, cdFilter);
      }
    }

    return filters;
  }
}
