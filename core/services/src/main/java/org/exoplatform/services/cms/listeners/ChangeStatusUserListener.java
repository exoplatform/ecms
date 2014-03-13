/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.services.cms.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 13, 2014  
 */
public class ChangeStatusUserListener extends UserEventListener {
  
  @Override
  public void postSetEnabled(User user) throws Exception
  {
    super.postSetEnabled(user);
    NodeHierarchyCreator nodeHierarchy = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    Node userNode = nodeHierarchy.getUserNode(WCMCoreUtils.getSystemSessionProvider(), user.getUserName());
    if (user.isEnabled()) {
      if (userNode.isNodeType(NodetypeConstant.EXO_PRIVILEGEABLE)) {
        Map<String, String[]> permissions = new HashMap<String, String[]>();
        List<AccessControlEntry> aces = ((ExtendedNode)userNode.getParent()).getACL().getPermissionEntries();
        for (AccessControlEntry entry : aces) {
          if (permissions.containsKey(entry.getIdentity())) {
            String[] perms = permissions.get(entry.getIdentity());
            String[] newPerms = new String[perms.length + 1];
            System.arraycopy(perms, 0, newPerms, 0, perms.length);
            newPerms[perms.length] = entry.getPermission();
            permissions.put(entry.getIdentity(), newPerms);
          } else {
            permissions.put(entry.getIdentity(), new String[]{entry.getPermission()});
          }
        }
        ((ExtendedNode)userNode).setPermissions(permissions);
        userNode.save();
        userNode.removeMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
        userNode.save();
      }
    } else {
      if (userNode.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
        userNode.addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
        Map<String, String[]> permissions = new HashMap<String, String[]>();
        permissions.put(IdentityConstants.SYSTEM, PermissionType.ALL);
        ((ExtendedNode)userNode).setPermissions(permissions);
        userNode.save();
      }
    }
  }

}
