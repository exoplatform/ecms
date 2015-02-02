/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          toannh@exoplatform.com
 * Dec 10, 2014
 * Check can display of Remote-Edit-Open Document In Office
 */
public class CanDisplayInViewFilter extends UIExtensionAbstractFilter{

  public CanDisplayInViewFilter() {
    this("UIActionBar.msg.unsupported-action");
  }

  public CanDisplayInViewFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if(context==null) return true;
    UIJCRExplorer uijcrExplorer = (UIJCRExplorer)context.get(UIJCRExplorer.class.getName());
    String currentView = uijcrExplorer.findFirstComponentOfType(UIAddressBar.class).getSelectedViewName();
    Object lstViewAllow = context.get("views");
    if (lstViewAllow == null) return false;
    return lstViewAllow.toString().contains(currentView);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
