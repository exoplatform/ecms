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
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 13, 2009
 * 4:42:38 PM
 */
public class IsNotTrashHomeNodeFilter extends UIExtensionAbstractFilter {

  public IsNotTrashHomeNodeFilter() {
    this(null);
  }

  public IsNotTrashHomeNodeFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    LinkManager linkManager = (LinkManager)
      container.getComponentInstanceOfType(LinkManager.class);
    Node currentNode = (Node) context.get(Node.class.getName());
    return linkManager.isLink(currentNode) || !Utils.isTrashHomeNode(currentNode);
  }

  public void onDeny(Map<String, Object> context) throws Exception {

  }
}
