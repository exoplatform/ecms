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
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
public class IsReferenceableNodeFilter extends UIExtensionAbstractFilter {

  public boolean accept(Map<String, Object> context) throws Exception {
    Node currentNode = (Node) context.get(Node.class.getName());
    UIJCRExplorer uiExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
    return uiExplorer.isReferenceableNode(currentNode);
  }

  public void onDeny(Map<String, Object> context) throws Exception {
    Node currentNode = (Node) context.get(Node.class.getName());
    Object[] args = { currentNode.getPath() };
    createUIPopupMessages(context, "UIActionBar.msg.is-not-referenable", args);
  }
}
