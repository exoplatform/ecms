/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;
import javax.jcr.Node;

import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * An extension filter that do not accept extensions on user private nodes which are:
 * Documents, Favorites, Pictures, Music, videos, Public, Folksonomy and Searches.
 *
 */
public class IsNotSpecificFolderNodeFilter extends UIExtensionAbstractFilter {

  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) {
      return true;
    }
    Node currentNode = (Node) context.get(Node.class.getName());
    return !Utils.isPersonalDefaultFolder(currentNode);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {}

  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }
}