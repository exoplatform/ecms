/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.permission;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.*;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class AdjustDrivePermissionNewUserListener extends UserEventListener {
  private NodeHierarchyCreator  nodeHierarchyCreator_ ;
  /**
   *
   * @param nodeHierarchyCreator
   * @param params
   * @throws Exception
   */
  public AdjustDrivePermissionNewUserListener(NodeHierarchyCreator nodeHierarchyCreator,
                                              InitParams params) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreator;
  }

  /**
   * This method used to remove "Remove" right permission from user folder's nodes
   * User user Current user
   * Boolean isNew Is adding new user or updating
   */
  public void preSave(User user, boolean isNew) throws Exception {
    String userName = user.getUserName();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator_.getUserNode(sessionProvider, userName);
    NodeIterator nodeIter = userNode.getNodes();
    while (nodeIter.hasNext()) {
      NodeImpl nodeImpl = (NodeImpl) nodeIter.next();
      if (nodeImpl.canAddMixin(NodetypeConstant.EXO_PRIVILEGEABLE)) {
        nodeImpl.addMixin(NodetypeConstant.EXO_PRIVILEGEABLE);
        Map<String, String[]> permissions = buildPermissions(nodeImpl);
        nodeImpl.setPermissions(permissions);
        nodeImpl.save();
      }
      nodeImpl.removePermission(userName, PermissionType.REMOVE);
    }
  }

  public void preDelete(User user) throws Exception {
  }

  public Map<String, String[]> buildPermissions(NodeImpl node) throws RepositoryException{
    Map<String, String[]> permissions = new HashMap<String, String[]>();
    List<String> permsList = new ArrayList<String>();
    String key = null;
    for (AccessControlEntry entry : node.getACL().getPermissionEntries()) {
      key = entry.getIdentity();
      if(!permissions.containsKey(key)) {
        permsList = node.getACL().getPermissions(key);
        permissions.put(key, permsList.toArray(new String[permsList.size()]));
      }
    }
    return permissions;
  }
}