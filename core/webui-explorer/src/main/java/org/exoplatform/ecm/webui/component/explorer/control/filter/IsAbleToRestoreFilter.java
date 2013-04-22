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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * This filter will be used to check the current user has permission to restore selected document/folder or not.
 */
public class IsAbleToRestoreFilter extends UIExtensionAbstractFilter {

  public IsAbleToRestoreFilter() {
    this(null);
  }
  
  public IsAbleToRestoreFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }
  
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    String restorePath = null, restoreWorkspace = null;
    Node currentNode = (Node) context.get(Node.class.getName());
    Node restoreLocationNode;
    if (ConversationState.getCurrent().getIdentity().getUserId().equalsIgnoreCase(WCMCoreUtils.getSuperUser())) return true;
    
    if ( currentNode.isNodeType(TrashService.EXO_RESTORE_LOCATION)) {
      restorePath = currentNode.getProperty(TrashService.RESTORE_PATH).getString();
      restoreWorkspace = currentNode.getProperty(TrashService.RESTORE_WORKSPACE).getString();
      restorePath = restorePath.substring(0, restorePath.lastIndexOf("/"));
    } else {
      //Is not a deleted node, may be groovy action, hidden node,...
      return false;
    }
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(restoreWorkspace, WCMCoreUtils.getRepository());
    try {
      restoreLocationNode = (Node) session.getItem(restorePath);
    } catch(Exception e) {
      return false;
    }
    return PermissionUtil.canAddNode(restoreLocationNode);
  }

  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
    createUIPopupMessages(context, "UIDocumentInfo.msg.access-denied");    
  }

}
