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

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          dongpd@exoplatform.com
 *          phamdong@gmail.com
 * Nov 24, 2011  
 */
public class IsNotNtFileFilter extends UIExtensionAbstractFilter{

  public IsNotNtFileFilter() {
    this("UIActionBar.msg.unsupported-action");
  }

  public IsNotNtFileFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#accept(java.util.Map)
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    return !currentNode.isNodeType(Utils.NT_FILE);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.ext.filter.UIExtensionFilter#onDeny(java.util.Map)
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
