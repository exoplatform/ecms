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

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import lombok.SneakyThrows;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.phamvu@exoplatform.com
 * Oct 23, 2008
 */
public class Utils {

  private static final String JCR_CONTENT = "jcr:content";

  private static final String JCR_DATA = "jcr:data";

  private static final String JCR_MIMETYPE = "jcr:mimeType";  

  private static final String NT_FILE = "nt:file";

  private static final String NT_UNSTRUCTURED = "nt:unstructured";

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
      return Text.escape(realNode.getPath(),'%',true) + "?version=" + node.getParent().getName();
    }
    return Text.escape(node.getPath(),'%',true);
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

    String originalNodePath = isGetRealNodePath ? getRealNodePath(node) : Text.escape(node.getPath(),'%',true);
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
   * GetRealNode
   *
   * @param strRepository
   * @param strWorkspace
   * @param strIdentifier
   * @return the required node/ the target of a symlink node / null if node was
   *         in trash.
   * @throws RepositoryException
   */
  public static Node getRealNode(String strRepository,
                                 String strWorkspace,
                                 String strIdentifier,
                                 boolean isWCMBase) throws RepositoryException {
    return getRealNode(strRepository, strWorkspace, strIdentifier, isWCMBase, null);
  }

  /**
   * GetRealNode
   *
   * @param strRepository
   * @param strWorkspace
   * @param strIdentifier
   * @param cacheVisibility the visibility of cache
   *
   * @return the required node/ the target of a symlink node / null if node was
   *         in trash.
   * @throws RepositoryException
   */
  @SneakyThrows
  public static Node getRealNode(String strRepository,
                                 String strWorkspace,
                                 String strIdentifier,
                                 boolean isWCMBase,
                                 String cacheVisibility) throws RepositoryException {
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(strWorkspace, WCMCoreUtils.getService(RepositoryService.class).getRepository(strRepository));
    Node selectedNode = session.getNodeByUUID(strIdentifier);
    if (selectedNode != null) {
      if (!org.exoplatform.ecm.webui.utils.Utils.isInTrash(selectedNode)) {
        if (linkManager.isLink(selectedNode)) {
          if (linkManager.isTargetReachable(selectedNode)) {
            selectedNode = linkManager.getTarget(selectedNode);
            if (!org.exoplatform.ecm.webui.utils.Utils.isInTrash(selectedNode)) {
              return selectedNode;
            }
          }
        } else {
          return selectedNode;
        }
      }
    }
    return null;
  }

  /**
   * Get download link of a node which stored binary data
   * @param node Node
   * @return download link
   * @throws Exception
   */
  public static String getDownloadLink(Node node) throws Exception {

    if (!Utils.getRealNode(node).isNodeType(NT_FILE)) return null;

    // Get binary data from node
    DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);
    Node jcrContentNode = node.getNode(JCR_CONTENT);
    InputStream input = jcrContentNode.getProperty(JCR_DATA).getStream();

    // Get mimeType of binary data
    String mimeType = jcrContentNode.getProperty(JCR_MIMETYPE).getString() ;

    // Make download stream
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, mimeType);

    // Make extension part for file if it have not yet
    DMSMimeTypeResolver mimeTypeSolver = DMSMimeTypeResolver.getInstance();
    String ext = "." + mimeTypeSolver.getExtension(mimeType) ;
    String fileName = Utils.getRealNode(node).getName();
    if (fileName.lastIndexOf(ext) < 0 && !mimeTypeSolver.getMimeType(fileName).equals(mimeType)) {
      dresource.setDownloadName(fileName + ext);
    } else {
      dresource.setDownloadName(fileName);
    }

    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  /**
   * Get node nt:file if node support multi-language
   *
   * @param currentNode Current Node
   * @return Node which has type nt:file
   * @throws Exception
   */
  public static Node getFileLangNode(Node currentNode) throws Exception {
    if(currentNode.isNodeType(NT_UNSTRUCTURED)) {
      if(currentNode.getNodes().getSize() > 0) {
        NodeIterator nodeIter = currentNode.getNodes() ;
        while(nodeIter.hasNext()) {
          Node ntFile = nodeIter.nextNode() ;
          if(ntFile.isNodeType(NT_FILE)) {
            return ntFile ;
          }
        }
        return currentNode ;
      }
    }
    return currentNode ;
  }

}
