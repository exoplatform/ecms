/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.utils;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.security.IdentityConstants;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityHelper.java 00000 May 15, 2014 pnedonosko $
 * 
 */
public class IdentityHelper {

  public static final String ROOT_USER_ID   = "root";

  public static final String SYSTEM_USER_ID = IdentityConstants.SYSTEM;

  public static final String EXO_OWNEABLE   = "exo:owneable";

  public static final String EXO_USER       = "exo:owner";

  /**
   * 
   */
  private IdentityHelper() {
  }

  /**
   * Compare user names and return <code>true</code> if they match. Also will be <code>true</code> if one of
   * user names is system account id or root.
   * 
   * @param user1 {@link String}
   * @param user2 {@link String}
   * @return boolean
   */
  public static boolean isUserMatch(String user1, String user2) {
    return user1.equals(user2) || (ROOT_USER_ID.equals(user1) || SYSTEM_USER_ID.equals(user1) || ROOT_USER_ID.equals(user2)
        || SYSTEM_USER_ID.equals(user2));
  }

  /**
   * Make the node owned by the current user (user of the node session) if current owner is a system account.
   * Does nothing otherwise.
   * 
   * @param node {@link Node} target node, its session will be used to set a new (current) owner
   * @param systemSession {@link Session} system session, it will be used to reset the current owner
   * @return {@link Node} fixed node (same as in the given in the parameter)
   * @throws RepositoryException
   * @throws PathNotFoundException if exo:owner property cannot be found on exo:owneable node, or if the node
   *           cannot be found by path via system session.
   */
  public static Node ensureOwned(Node node, Session systemSession) throws PathNotFoundException, RepositoryException {
    String currentUser = node.getSession().getUserID();
    // don't allow system account be an owner
    if (!currentUser.equals(IdentityHelper.SYSTEM_USER_ID)) {
      if (node.isNodeType(EXO_OWNEABLE) && node.getProperty(EXO_USER).getString().equals(IdentityHelper.SYSTEM_USER_ID)) {
        // owned not by the drive user
        // we need this as it may happen with ECMS that the node will be owned by system account
        // remove in system session
        Node snode = (Node) systemSession.getItem(node.getPath());
        snode.removeMixin(EXO_OWNEABLE);
        snode.save();
        // FIXME this will remove pending changes related to the node and childs (e.g. added mixin)
        node.refresh(true); 
        // add in current user session
        node.addMixin(EXO_OWNEABLE);
        node.save();
      }
    }
    return node;
  }

  public static boolean hasPermission(AccessControlList acl, String identity, String type) {
    if (acl.hasPermissions()) {
      for (String idp : acl.getPermissions(identity)) {
        if (idp.equals(type)) {
          return true;
        }
      }
    } else {
      return true;
    }
    return false;
  }

}
