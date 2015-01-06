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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;

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
   * Checks if is any role.
   *
   * @param node the node
   * @return true, if is any role
   * @throws RepositoryException the repository exception
   */
  public static boolean isAnyRole(Node node)throws RepositoryException {
    return checkPermission(node,IdentityConstants.ANY);
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

  private static boolean checkPermission(Node node,String permissionType) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(permissionType);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }

}
