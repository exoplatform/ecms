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
import javax.jcr.RepositoryException;

import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */
public class IsNotRestrictedNodeFilter extends UIExtensionAbstractFilter {

  public IsNotRestrictedNodeFilter() {
    this("UIPopupMenu.msg.is-restricted-node");
  }

  public IsNotRestrictedNodeFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context==null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    String username = ConversationState.getCurrent().getIdentity().getUserId();
    
    String ownerPrivateFolder = "/Users/" + username + "/Private";
    String ownerPublicFolder = "/Users/" + username + "/Public";
    
    if (currentNode != null
        && !currentNode.getPath().startsWith(ownerPrivateFolder) && !currentNode.getPath().startsWith(ownerPublicFolder)){
      return true;
    }    
    return false;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
