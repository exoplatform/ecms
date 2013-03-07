/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 7 Mar 2013  
 */
public class IsLockForSymlink extends UIExtensionAbstractFilter{
  public IsLockForSymlink() {
    this(null);
  }
  public IsLockForSymlink(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    return (!currentNode.isLocked()) ;
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
    if (context == null) return;
    Node currentNode = (Node) context.get(Node.class.getName());
    Object[] arg = { currentNode.getPath() };
    createUIPopupMessages(context, "UIPopupMenu.msg.node-locked", arg);    
  }
}