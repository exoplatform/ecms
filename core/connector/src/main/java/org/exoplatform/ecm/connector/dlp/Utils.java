package org.exoplatform.ecm.connector.dlp;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Utils {

  private static final Log LOG = ExoLogger.getLogger(Utils.class.getName());

  /**
   * @param node nt:file node with have the data stream
   * @return Link to download the jcr:data of the given node
   * @throws Exception
   */
  public static String getDownloadRestServiceLink(Node node) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    String portalName = containerInfo.getContainerName();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    String restContextName = portalContainerConfig.getRestContextName(portalName);
    StringBuilder sb = new StringBuilder();
    Node currentNode = getRealNode(node);
    String ndPath = currentNode.getPath();
    if (ndPath.startsWith("/")) {
      ndPath = ndPath.substring(1);
    }
    String encodedPath = encodePath(ndPath,"UTF-8");
    sb.append("/").append(restContextName).append("/contents/download/");
    sb.append(currentNode.getSession().getWorkspace().getName()).append("/").append(encodedPath);
    if (node.isNodeType("nt:frozenNode")) {
      sb.append("?version=" + node.getParent().getName());
    }
    return sb.toString();
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

  public static String encodePath(String path, String encoding) {
    try {
      String encodedPath = URLEncoder.encode(path, encoding);
      encodedPath = encodedPath.replaceAll("%2F","/");
      return encodedPath;
    } catch (UnsupportedEncodingException e){
      LOG.error("Failed to encode path '" + path + "' with encoding '" + encoding + "'",e);
    }
    return null;
  }
}
