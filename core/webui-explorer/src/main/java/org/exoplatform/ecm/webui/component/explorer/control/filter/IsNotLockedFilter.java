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

import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 5 mai 2009
 */
public class IsNotLockedFilter extends UIExtensionAbstractFilter {

  private boolean checkGroup = false;
  private boolean checkOwner = false;

  public IsNotLockedFilter() {
    this(null);
  }

  public IsNotLockedFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public IsNotLockedFilter(boolean checkGroup) {
    this(null);
    this.checkGroup = checkGroup;
  }

  public IsNotLockedFilter(boolean checkGroup, boolean checkOwner) {
    this(null);
    this.checkGroup = checkGroup;
    this.checkOwner = checkOwner;
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    String remoteUser = currentNode.getSession().getUserID();
    String superUser = WCMCoreUtils.getService(UserACL.class).getSuperUser();
    if (remoteUser.equalsIgnoreCase(superUser)) {
      return true;
    }
    if(!currentNode.isLocked()) return true;
    if (checkOwner && currentNode.isLocked()) {
      String lockOwner = currentNode.getLock().getLockOwner();
      if (lockOwner.equals(remoteUser)) return true;
    }
    String lockToken = checkGroup ? LockUtil.getLockToken(currentNode): LockUtil.getLockTokenOfUser(currentNode);
    if(lockToken != null) {
      currentNode.getSession().addLockToken(LockUtil.getLockToken(currentNode));
      return true;
    }
    return false;

  }

  public void onDeny(Map<String, Object> context) throws Exception {
    if (context == null) return;
    Node currentNode = (Node) context.get(Node.class.getName());
    Object[] arg = { currentNode.getPath() };
    createUIPopupMessages(context, "UIPopupMenu.msg.node-locked", arg);
  }
}
