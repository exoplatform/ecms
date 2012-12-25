/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import java.util.List;
import java.util.Map;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          dongpd@exoplatform.com
 * Jan 07, 2013  
 */
public class HasAllowedFolderTypeFilter extends UIExtensionAbstractFilter {
  public HasAllowedFolderTypeFilter() {
    this("UIActionBar.msg.unsupported-action");
  }

  public HasAllowedFolderTypeFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) {
      return true;
    }
    
    UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
    List<String> allowedFolderTypes =
        Utils.getAllowedFolderTypesInCurrentPath(uiExplorer.getCurrentNode(),
                                                 uiExplorer.getDriveData());

    return (allowedFolderTypes.size() != 0);
  }
}
