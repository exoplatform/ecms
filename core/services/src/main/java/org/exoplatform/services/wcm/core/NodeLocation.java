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
package org.exoplatform.services.wcm.core;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.services.cms.link.ItemLinkAware;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Jun 20, 2008
 */
public class NodeLocation extends ItemLocation {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(NodeLocation.class.getName());
  
  private List<NodeLocation> children_;

  public List<NodeLocation> getChildren() {
    return children_;
  }

  public void setChildren(List<NodeLocation> children) {
    children_ = children;
  }

  public boolean hasChildren() {
    return children_ != null && children_.size() > 0;
  }
  /**
   * Instantiates a new node location.
   */
  public NodeLocation() {
    super();
  }

  /**
   * Instantiates a new node location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   * @param isSystem if the node session is system
   */
  public NodeLocation(final String repository, final String workspace, final String path, final String uuid,
      final boolean isSystem) {
    super(repository, workspace, path, uuid, isSystem);
  }

  /**
   * Instantiates a new node location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   */
  public NodeLocation(final String repository, final String workspace, final String path, final String uuid ) {
    super(repository, workspace, path, uuid, false);
  }

  /**
   * Instantiates a new node location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   */
  public NodeLocation(final String repository, final String workspace, final String path) {
    super(repository, workspace, path, null, false);
  }

  /**
   * Instantiates a new node location.
   *
   */
  public NodeLocation(ItemLocation location) {
    super(location);
  }

  /**
   * Get an NodeLocation object by an expression.
   *
   * @param exp the expression with pattern repository:workspace:path
   *
   * @return a NodeLocation object
   */
  public static final NodeLocation getNodeLocationByExpression(final String exp) {
    String[] temp = split(exp, ":");
    if (temp.length >= 3 && temp[2].indexOf("/") == 0) {
      String repository = temp[0];
      String workspace = temp[1];
      String nodepath = exp.substring(repository.length() + workspace.length() + 2);
      return new NodeLocation(repository, workspace, nodepath);
    }
    throw new IllegalArgumentException("Invalid expression: " + exp
        + ". An valid expression has pattern repository:workspace:path");
  }

  /**
   * Get an NodeLocation object by a node.
   *
   * @param node the node
   *
   * @return a NodeLocation object
   */
  public static final NodeLocation getNodeLocationByNode(final Node node) {
    try {
      ItemLocation itemLocation = ItemLocation.getItemLocationByItem(node);
      return new NodeLocation(itemLocation);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Get a node by a NodeLocation object.
   *
   * @param nodeLocation the NodeLocation object
   *
   * @return a node
   */
  public static final Node getNodeByLocation(final NodeLocation nodeLocation) {
    try {
      Item item = ItemLocation.getItemByLocation(nodeLocation);
      return (Node)item;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * gets a Node list from a NodeLocation list
   * @param locationList NodeLocation list
   * @return the Node list
   */
  @SuppressWarnings("unchecked")
  public static final List getNodeListByLocationList(final List locationList) {
    List ret = new ArrayList();
    try {
      ManageableRepository repository = WCMCoreUtils.getRepository();
      SessionProvider systemSessionProvider = WCMCoreUtils.getSystemSessionProvider();
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      String systemWorkspace = repository.getConfiguration().getSystemWorkspaceName();
      Session session = null;
      Node node;
      for (Object obj : locationList)
        if (obj instanceof NodeLocation) {
          node = null;
          try {
           NodeLocation location = (NodeLocation)obj;
           session = (systemWorkspace.equals(location.getWorkspace()) || location.isSystemSession)?
                      systemSessionProvider.getSession(location.getWorkspace(), repository) :
                      sessionProvider.getSession(location.getWorkspace(), repository);

           node = location.getUUID() != null ? session.getNodeByUUID(location.getUUID()) :
                                          (Node)session.getItem(location.getPath());
           ret.add(node);
          } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(e.getMessage(), e);
            }
          }
        } else {
          ret.add(obj);
        }
    } catch (Exception e) {
      return ret;
    }
    return ret;
  }

  /**
   * returns the list of node location from the node list
   * @param nodeList the node list
   * @return node location list
   */
  @SuppressWarnings("unchecked")
  public static final List getLocationsByNodeList(final List nodeList) {
    List ret = new ArrayList();
    for(Object obj : nodeList)
      if (obj instanceof ItemLinkAware) {
        ret.add(obj);
      } else if (obj instanceof Node) {
        NodeLocation location = getNodeLocationByNode((Node)obj);
        if (location != null)
          ret.add(location);
      } else {
        ret.add(obj);
      }
    return ret;
  }

  /**
   * returns the list of node location from the node iterator
   * @param nodeIterator the Node iterator
   * @return node location list
   */
  public static final List<NodeLocation> getLocationsByIterator(final NodeIterator nodeIterator) {
    List<NodeLocation> ret = new ArrayList<NodeLocation>();
    while (nodeIterator.hasNext()) {
      NodeLocation location = getNodeLocationByNode(nodeIterator.nextNode());
      if (location != null)
        ret.add(location);
    }
    return ret;
  }


  /**
   * Get a node by an expression.
   *
   * @param expression the expression
   * @return a node
   */
  public static final Node getNodeByExpression(final String expression) {
    return getNodeByLocation(getNodeLocationByExpression(expression));
  }

  /**
   * Get node's expression by a node.
   *
   * @param node the node to get expression
   * @return The node's expression
   */
  public static final String getExpressionByNode(final Node node) {
    NodeLocation location = NodeLocation.getNodeLocationByNode(node);
    return mergeString(location.getRepository(), location.getWorkspace(), location.getPath());
  }

  /**
   * Get node's expression by a NodeLocation.
   *
   * @param location location
   * @return The node's expression
   */
  public static final String getExpressionByNodeLocation(final NodeLocation location) {
    return mergeString(location.getRepository(), location.getWorkspace(), location.getPath());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return mergeString(repository, workspace, path);
  }

  /**
   * Get the merged string
   *
   * @param repository: The node's repository
   * @param workspace: The node's workspace
   * @param path: The node's path
   * @return A merged string of the parameters
   */
  private static String mergeString(String repository, String workspace, String path) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(repository)
          .append(":")
          .append(workspace)
          .append(":")
          .append(path);
    return buffer.toString();
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeLocation)) return false;
    NodeLocation location2 = (NodeLocation)obj;
    return equalsString(this.repository, location2.getRepository()) &&
                equalsString(this.getWorkspace(), location2.getWorkspace()) &&
                (equalsString(this.getPath(), location2.getPath()) ||
                    equalsString(this.getUUID(), location2.getUUID()));
  }

  public boolean equalsString(String st1, String st2) {
    if (st1 == null && st2 == null) return true;
    if (st1 == null || st2 == null) return false;
    return st1.equals(st2);
  }

  public int hashCode() {
    return (repository == null ? 0 : repository.hashCode()) +
           (workspace == null ? 0 : workspace.hashCode()) +
           (uuid == null ? 0 : uuid.hashCode()) +
           (path == null ? 0 : path.hashCode());
  }

  private static final String[] split(String s, String ch) {
    int maxLength = 3;
    String[] ss = new String[maxLength];
    int prev = 0;
    int i=0;
    while(true) {
      int next = s.indexOf(ch, prev);
      if (next == -1 || i == maxLength - 1) {
        ss[i] = s.substring(prev);
        break;
      }
      ss[i++] = s.substring(prev, next);
      prev = next+1;
    }

    return ss;
  }
}
