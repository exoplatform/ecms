/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * July 26, 2010
 */
public class IsNotEditingDocumentFilter extends UIExtensionAbstractFilter {

  public IsNotEditingDocumentFilter() {
    this(null);
  }

  public IsNotEditingDocumentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    UIJCRExplorer uiExplorer = (UIJCRExplorer)context.get(UIJCRExplorer.class.getName());
    if (currentNode == null)
      return !uiExplorer.isEditingDocument();
    return  !uiExplorer.isEditingDocument() ||
            !currentNode.getSession().getWorkspace().getName().equals(uiExplorer.getCurrentDriveWorkspace()) ||
            !currentNode.getPath().equals(uiExplorer.getCurrentNode().getPath());
  }

  public void onDeny(Map<String, Object> context) throws Exception {

  }
}
