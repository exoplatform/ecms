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
package org.exoplatform.ecm.utils.permission;

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

/**
 * The Class PermissionUtil use to check permission for a node
 */
public class PermissionUtil {

  /**
   * Can read node
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canRead(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.READ);
  }

  /**
   * Can add node.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canAddNode(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.ADD_NODE);
  }

  /**
   * Can change permission.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canChangePermission(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.CHANGE_PERMISSION);
  }

  /**
   * Checks if is any identity can access the node with {@link PermissionType#DEFAULT_AC} permissions.
   *
   * @param node the node
   * @return true, if any identity can access the node
   * @throws RepositoryException the repository exception
   */
  public static boolean isAnyRole(Node node)throws RepositoryException {
    // TODO this check is not correct: permissionType cannot be compared with identity {@link IdentityConstants#ANY}.
    // If use it, it leads to: javax.jcr.RepositoryException: Unknown permission entry any and 503 error in REST.
    //return checkPermission(node,IdentityConstants.ANY);
    //
    // Check if given node has default (read) permissions for any identity
    return hasPermissions(node, IdentityConstants.ANY, PermissionType.DEFAULT_AC);
  }

  /**
   * Can set property.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canSetProperty(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.SET_PROPERTY);
  }

  /**
   * Can remove node.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canRemoveNode(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.REMOVE);
  }
  
  /**
   * Checks for node permissions granted to given identity. This method checks actually assigned permissions to the identity. It
   * will not resolve an user group membership to check the groups permissions - user groups resolution should be done externally
   * and then checked with this method for each group.<br>
   * Method will return <code>true</code> only if all asked permissions match the granted on the node.
   *
   * @param node the target node
   * @param identity the organizational identity, can be user (e.g. "john") or group with membership (e.g. *:/some/group,
   *          manager:/platform/administrator)
   * @param permissions the permissions to check if granted (e.g. "read" or ["read", "write"]), see {@link PermissionType} for all
   *          variants
   * @return true, if all asked permissions are granted to the identity, false otherwise
   * @throws RepositoryException the repository exception if error reading the node permission (ACL)
   */
  public static boolean hasPermissions(Node node, String identity, String[] permissions) throws RepositoryException {
    if (ExtendedNode.class.isAssignableFrom(node.getClass())) {
      ExtendedNode extNode = ExtendedNode.class.cast(node); 
      MembershipEntry identityMembership = MembershipEntry.parse(identity);
      Set<String> identityPermissions = new HashSet<>();
      for (AccessControlEntry ace : extNode.getACL().getPermissionEntries()) {
        if (identityMembership != null && ace.getMembershipEntry() != null) {
          // Group permissions
          MembershipEntry me = ace.getMembershipEntry();
          // Check any (*) as well as exact match of membership
          if (identityMembership.getGroup().equals(me.getGroup())) {
            if (me.getMembershipType().equals(identityMembership.getMembershipType()) || me.getMembershipType()
                                                                                           .equals(MembershipEntry.ANY_TYPE)) {
              identityPermissions.add(ace.getPermission());
            }
          }
        } else if (ace.getIdentity().equals(identity)) {
          // It is user permissions
          identityPermissions.add(ace.getPermission());
        } else if (ace.getIdentity().equals(IdentityConstants.ANY)) {
          // ACL for any can be applied to any of given identity  
          identityPermissions.add(ace.getPermission());
        }
      }
      if (!identityPermissions.isEmpty()) {
        // Check that all asked present in the identity permissions of the node
        int checks = permissions.length;
        for (String p : permissions) {
          if (identityPermissions.contains(p)) {
            checks--;
          }
        }
        identityPermissions.clear(); // help GC
        return checks == 0;
      }
    }
    return false;
  }

  private static boolean checkPermission(Node node,String permissionType) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(permissionType);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }

}
