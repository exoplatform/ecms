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
package org.exoplatform.wcm.webui;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.phamvu@exoplatform.com
 * Oct 23, 2008
 */
public class Utils {

  /**
   * Check if the node is viewable for the current user or not viewable. <br>
   * return True if the node is viewable, otherwise will return False
   *
   * @param node: The node to check
   */
  public static boolean isViewable(Node node) {
    try {
      node.refresh(true);
      ((ExtendedNode) node).checkPermission(PermissionType.READ);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Get the real node from frozen node, symlink node return True if the node is
   * viewable, otherwise will return False
   *
   * @param node: The node to check
   */
  public static Node getRealNode(Node node) throws Exception {
    // TODO: Need to add to check symlink node
    if (node.isNodeType("nt:frozenNode")) {
      String uuid = node.getProperty("jcr:frozenUuid").getString();
      return node.getSession().getNodeByUUID(uuid);
    }
    return node;
  }

  public static String getRealNodePath(Node node) throws Exception {
    if (node.isNodeType("nt:frozenNode")) {
      Node realNode = getRealNode(node);
      return Text.escape(realNode.getPath(), '%', true) + "?version=" + node.getParent().getName();
    }
    return Text.escape(node.getPath(), '%', true);
  }

  public static String getWebdavURL(Node node) throws Exception {
    return getWebdavURL(node, true);
  }

  public static String getWebdavURL(Node node, boolean withTimeParam) throws Exception {
    return getWebdavURL(node, withTimeParam, true);
  }

  public static String getWebdavURL(Node node, boolean withTimeParam, boolean isGetRealNodePath) throws Exception {
    NodeLocation location = NodeLocation.getNodeLocationByNode(getRealNode(node));
    String repository = location.getRepository();
    String workspace = location.getWorkspace();
    String currentProtal = PortalContainer.getCurrentRestContextName();
    String portalName = PortalContainer.getCurrentPortalContainerName();

    String originalNodePath = isGetRealNodePath ? getRealNodePath(node) : Text.escape(node.getPath(), '%', true);
    StringBuilder imagePath = new StringBuilder();
    imagePath.append("/")
             .append(portalName)
             .append("/")
             .append(currentProtal)
             .append("/jcr/")
             .append(repository)
             .append("/")
             .append(workspace)
             .append(originalNodePath);
    if (withTimeParam) {
      if (imagePath.indexOf("?") > 0) {
        imagePath.append("&time=");
      } else {
        imagePath.append("?time=");
      }
      imagePath.append(System.currentTimeMillis());
    }
    return imagePath.toString();
  }

  /**
   * Get node nt:file if node support multi-language
   *
   * @param currentNode Current Node
   * @return Node which has type nt:file
   * @throws Exception
   */
  public static Node getFileLangNode(Node currentNode) throws Exception {
    if (currentNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)
        && currentNode.getNodes().getSize() > 0) {
      NodeIterator nodeIter = currentNode.getNodes();
      while (nodeIter.hasNext()) {
        Node ntFile = nodeIter.nextNode();
        if (ntFile.isNodeType(NodetypeConstant.NT_FILE)) {
          return ntFile;
        }
      }
      return currentNode;
    }
    return currentNode;
  }

}
