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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
public class NodeLocation {

  /** The log. */
  private static Log log = ExoLogger.getLogger("wcm:NodeLocation");

  /** The repository. */
  private String repository;

  /** The workspace. */
  private String workspace;

  /** The path. */
  private String path;
  
  /** The UUID. */
  private String uuid;
  
  /** If session is system */
  private boolean isSystemSession;

  /**
   * Instantiates a new node location.
   */
  public NodeLocation() { }

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
    this.repository = repository;
    this.workspace = workspace;
    this.path = path;
    this.uuid = uuid;
    this.isSystemSession = isSystem;
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
    this(repository, workspace, path, uuid, false);
  }
  
  /**
   * Instantiates a new node location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   */
  public NodeLocation(final String repository, final String workspace, final String path) {
	  this(repository, workspace, path, null, false);
  }


  /**
   * Gets the repository.
   *
   * @return the repository
   */
  public String getRepository() {
    return repository;
  }

  /**
   * Sets the repository.
   *
   * @param repository the new repository
   */
  public void setRepository(String repository) {
    this.repository = repository;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Sets the workspace.
   *
   * @param workspace the new workspace
   */
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }


  /**
   * Sets the path.
   *
   * @param path the new path
   */
  public void setPath(String path) {
    this.path = path;
  }
  
  /**
   * Sets the uuid.
   *
   * @param uuid the new uuid
   */
  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets the uuid.
   *
   * @return the uuid
   */
  public String getUUID() {
    return uuid;
  }
  
  /**
   * Sets the isSystemSession.
   *
   * @param value isSysstemSession
   */
  public void setSystemSession(boolean value) {
    this.isSystemSession = value;
  }

  /**
   * Gets the isSystemSession.
   *
   * @return true if node session is system, false if not
   */
  public boolean isSystemSession() {
    return this.isSystemSession;
  }
  

  /**
   * Parses the.
   *
   * @param exp the exp
   * @return the node location
   */
  @Deprecated
  /**
   * Get an NodeLocation object by an expression.
   *
   * @param exp the expression with pattern repository:workspace:path
   *
   * @return a NodeLocation object
   */
  public static final NodeLocation parse(final String exp) {
    String[] temp = exp.split(":");
    if (temp.length == 3 && temp[2].indexOf("/") == 0) {
      return new NodeLocation(temp[0], temp[1], temp[2]);
    } 
    throw new IllegalArgumentException("Invalid expression: " + exp
        + ". An valid expression has pattern repository:workspace:path");
  }

  /**
   * Make.
   *
   * @param node the node
   * @return the node location
   */
  @Deprecated
  /**
   * Get an NodeLocation object by a node. Try to use toNodeLocation() instead.
   *
   * @param node the node
   *
   * @return a NodeLocation object
   */
  public static final NodeLocation make(final Node node) {
    try {
      Session session = node.getSession();
      String repository = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
      String workspace = session.getWorkspace().getName();
      String path = node.getPath();
      return new NodeLocation(repository, workspace, path);
    } catch (RepositoryException e) {
      log.error("make() failed because of ", e);
    }
    return null;
  }

  /**
   * Get an NodeLocation object by an expression.
   *
   * @param exp the expression with pattern repository:workspace:path
   *
   * @return a NodeLocation object
   */
  public static final NodeLocation getNodeLocationByExpression(final String exp) {
    String[] temp = exp.split(":");
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
    Session session = null;
    try {
      session = node.getSession();
      String repository = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
      String workspace = session.getWorkspace().getName();
      String path = node.getPath();
      String uuid = null;
      try {
    	  uuid = node.getUUID();
      } catch (Exception e) {}
      return new NodeLocation(repository, workspace, path, uuid);
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
    Session session = null;
    try {
      ManageableRepository repository = WCMCoreUtils.getRepository();
      session = WCMCoreUtils.getSystemSessionProvider().getSession(nodeLocation.getWorkspace(), repository);
      if (nodeLocation.getUUID() != null)
    	  return session.getNodeByUUID(nodeLocation.getUUID());
      else {
    	  Node node = (Node)session.getItem(nodeLocation.getPath());
    	  return node;
      }
    } catch(PathNotFoundException pne) {
      return null;
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
		  ManageableRepository repository = WCMCoreUtils.getRepository(null);
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
					 session = (systemWorkspace.equals(location.getWorkspace()))? 
							  			systemSessionProvider.getSession(location.getWorkspace(), repository) : 
							  			sessionProvider.getSession(location.getWorkspace(), repository);
							  			
					 node = location.getUUID() != null ? session.getNodeByUUID(location.getUUID()) :
					  															(Node)session.getItem(location.getPath());
					 ret.add(node);
				  } catch (Exception e) {}
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
		  if (obj instanceof Node) {
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
    return getNodeByLocation(parse(expression));
  }

  /**
   * Get node's expression by a node.
   *
   * @param Node the node to get expression
   * @return The node's expression
   */
  public static final String getExpressionByNode(final Node node) {
    NodeLocation location = NodeLocation.getNodeLocationByNode(node);
    return mergeString(location.getRepository(), location.getWorkspace(), location.getPath());
  }

  /**
   * Get node's expression by a NodeLocation.
   *
   * @param NodeLocation location
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
    buffer.append(repository);
    buffer.append(":");
    buffer.append(workspace);
    buffer.append(":");
    buffer.append(path);
    return buffer.toString();
  }
  
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NodeLocation)) return false;
    NodeLocation location2 = (NodeLocation)obj;
    return this.repository.equals(location2.getRepository()) &&
                this.getWorkspace().equals(location2.getWorkspace()) &&
                (this.getPath().equals(location2.getPath()) || this.getUUID().equals(location2.getUUID()));
  }
}
