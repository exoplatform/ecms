/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.cms.link;

import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * nicolas.filotto@exoplatform.com 31 mars 2009
 */
public class NodeLinkAware extends ItemLinkAware implements ExtendedNode {

  final static public String    EXO_RESTORE_LOCATION = "exo:restoreLocation";

  /**
   * Logger.
   */
  private static final Log LOG = ExoLogger.getLogger(NodeLinkAware.class.getName());

  private final NodeLocation    nodeLocation;

  private volatile NodeLocation targetNodeLocation;

  public NodeLinkAware(String originalWorkspace, String virtualPath, Node node) {
    super(originalWorkspace, virtualPath, node);
    this.nodeLocation = NodeLocation.getNodeLocationByNode(node);
  }

  public String getRealPath() {
    return nodeLocation.getPath();
  }

  public Node getRealNode() {
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  public NodeLinkAware getTargetNode() throws RepositoryException {
    return new NodeLinkAware(originalWorkspaceName, virtualPath, getTarget());
  }

  public Session getNodeSession() throws RepositoryException {
    return getRealNode().getSession();
  }

  Node getTarget() throws RepositoryException {
    Node targetNode = null;
    if (targetNodeLocation == null) {
      synchronized (this) {
        if (targetNodeLocation == null) {
          LinkManager linkManager = LinkUtils.getLinkManager();
          Node node = getRealNode();
          if (linkManager.isLink(node)) {
            targetNode = linkManager.getTarget(node);
          } else {
            targetNode = node;
          }
        }
      }
    }
    return targetNode;
  }

  private Node getTargetReachable() throws RepositoryException {
    try {
      return getTarget();
    } catch (AccessDeniedException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot access to the target of the node " + nodeLocation.getPath());
      }
    } catch (ItemNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("The target of the node " + nodeLocation.getPath() + " doesn't exist anymore");
      }
    }
    return null;
  }

  private ExtendedNode getExtendedTarget() throws RepositoryException {
    return (ExtendedNode) getTarget();
  }

  private ExtendedNode getExtendedRealNode() {
    return (ExtendedNode) getRealNode();
  }

  private String getVirtualPath(String relativePath) {
    return LinkUtils.createPath(virtualPath, relativePath);
  }

  /**
   * {@inheritDoc}
   */
  public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    getTarget().addMixin(mixinName);
  }

  /**
   * {@inheritDoc}
   */
  public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException {
    return new NodeLinkAware(originalWorkspaceName, getVirtualPath(relPath), getTarget().addNode(
        relPath));
  }

  /**
   * {@inheritDoc}
   */
  public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException,
      PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
      ConstraintViolationException, RepositoryException {
    return new NodeLinkAware(originalWorkspaceName, getVirtualPath(relPath), getTarget().addNode(
        relPath, primaryNodeTypeName));
  }

  /**
   * {@inheritDoc}
   */
  public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
    return getTarget().canAddMixin(mixinName);
  }

  /**
   * {@inheritDoc}
   */
  public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException {
    getTarget().cancelMerge(version);
  }

  /**
   * {@inheritDoc}
   */
  public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
      InvalidItemStateException, LockException, RepositoryException {
    return getTarget().checkin();
  }

  /**
   * {@inheritDoc}
   */
  public void checkout() throws UnsupportedRepositoryOperationException, LockException,
      RepositoryException {
    getTarget().checkout();
  }

  /**
   * {@inheritDoc}
   */
  public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException {
    getTarget().doneMerge(version);
  }

  /**
   * {@inheritDoc}
   */
  public Version getBaseVersion() throws UnsupportedRepositoryOperationException,
      RepositoryException {
    return getTarget().getBaseVersion();
  }

  /**
   * {@inheritDoc}
   */
  public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException,
      NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
    return getTarget().getCorrespondingNodePath(workspaceName);
  }

  /**
   * {@inheritDoc}
   */
  public NodeDefinition getDefinition() throws RepositoryException {
    return getTarget().getDefinition();
  }

  /**
   * {@inheritDoc}
   */
  public int getIndex() throws RepositoryException {
    return getRealNode().getIndex();
  }

  /**
   * {@inheritDoc}
   */
  public Lock getLock() throws UnsupportedRepositoryOperationException, LockException,
      AccessDeniedException, RepositoryException {
    return getTarget().getLock();
  }

  /**
   * {@inheritDoc}
   */
  public NodeType[] getMixinNodeTypes() throws RepositoryException {
    return getTarget().getMixinNodeTypes();
  }

  /**
   * {@inheritDoc}
   */
  public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
    return new NodeLinkAware(originalWorkspaceName, getVirtualPath(relPath), (Node) LinkUtils
        .getNodeFinder().getItem(getNodeSession(), getVirtualPath(relPath)));
  }

  /**
   * {@inheritDoc}
   */
  public NodeIterator getNodes() throws RepositoryException {
    return new NodeIteratorLinkAware(originalWorkspaceName, virtualPath, getTarget().getNodes());
  }

  /**
   * {@inheritDoc}
   */
  public NodeIterator getNodes(String namePattern) throws RepositoryException {
    return new NodeIteratorLinkAware(originalWorkspaceName, virtualPath, getTarget().getNodes(
        namePattern));
  }

  /**
   * {@inheritDoc}
   */
  public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
    return ItemLinkAware.newInstance(originalWorkspaceName, getVirtualPath(super.getName()),
        getTarget().getPrimaryItem());
  }

  /**
   * {@inheritDoc}
   */
  public NodeType getPrimaryNodeType() throws RepositoryException {
    return new NodeTypeLinkAware(this);
  }

  /**
   * {@inheritDoc}
   */
  public PropertyIterator getProperties() throws RepositoryException {
    return new PropertyIteratorLinkAware(originalWorkspaceName, virtualPath, getTarget()
        .getProperties());
  }

  /**
   * {@inheritDoc}
   */
  public PropertyIterator getProperties(String namePattern) throws RepositoryException {
    return new PropertyIteratorLinkAware(originalWorkspaceName, virtualPath, getTarget()
        .getProperties(namePattern));
  }

  /**
   * {@inheritDoc}
   */
  public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
    String path = getVirtualPath(relPath);
    return new PropertyLinkAware(originalWorkspaceName, path, (Property) LinkUtils.getNodeFinder()
        .getItem(getOriginalSession(), path));
  }

  /**
   * {@inheritDoc}
   */
  public PropertyIterator getReferences() throws RepositoryException {
    return getTarget().getReferences();
  }

  /**
   * {@inheritDoc}
   */
  public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
    return getTarget().getUUID();
  }

  /**
   * {@inheritDoc}
   */
  public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException,
      RepositoryException {
    return getTarget().getVersionHistory();
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasNode(String relPath) throws RepositoryException {
    try {
      return LinkUtils.getNodeFinder().getItem(getOriginalSession(), getVirtualPath(relPath)) instanceof Node;
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasNodes() throws RepositoryException {
    return getTarget().hasNodes();
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasProperties() throws RepositoryException {
    return getTarget().hasProperties();
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasProperty(String relPath) throws RepositoryException {
    try {
      return LinkUtils.getNodeFinder().getItem(getOriginalSession(), getVirtualPath(relPath)) instanceof Property;
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean holdsLock() throws RepositoryException {
    Node node = getTargetReachable();
    return node == null ? false : node.holdsLock();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isCheckedOut() throws RepositoryException {
    Node node = getTargetReachable();
    return node == null ? false : node.isCheckedOut();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isLocked() throws RepositoryException {
    Node node = getTargetReachable();
    return node == null ? false : node.isLocked();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isNodeType(String nodeTypeName) throws RepositoryException {
    if (EXO_RESTORE_LOCATION.equals(nodeTypeName))
      return this.getRealNode().isNodeType(nodeTypeName);
    Node node = getTargetReachable();
    return node == null ? false : node.isNodeType(nodeTypeName);
  }

  /**
   * {@inheritDoc}
   */
  public Lock lock(boolean isDeep, boolean isSessionScoped)
      throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
      InvalidItemStateException, RepositoryException {
    return getTarget().lock(isDeep, isSessionScoped);
  }

  /**
   * {@inheritDoc}
   */
  public NodeIterator merge(String srcWorkspace, boolean bestEffort)
      throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException,
      InvalidItemStateException, RepositoryException {
    return getTarget().merge(srcWorkspace, bestEffort);
  }

  /**
   * {@inheritDoc}
   */
  public void orderBefore(String srcChildRelPath, String destChildRelPath)
      throws UnsupportedRepositoryOperationException, VersionException,
      ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
    getTarget().orderBefore(srcChildRelPath, destChildRelPath);
  }

  /**
   * {@inheritDoc}
   */
  public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    getTarget().removeMixin(mixinName);
  }

  /**
   * {@inheritDoc}
   */
  public void restore(String versionName, boolean removeExisting) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException,
      InvalidItemStateException, RepositoryException {
    getTarget().restore(versionName, removeExisting);
  }

  /**
   * {@inheritDoc}
   */
  public void restore(Version version, boolean removeExisting) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException,
      RepositoryException {
    getTarget().restore(version, removeExisting);
  }

  /**
   * {@inheritDoc}
   */
  public void restore(Version version, String relPath, boolean removeExisting)
      throws PathNotFoundException, ItemExistsException, VersionException,
      ConstraintViolationException, UnsupportedRepositoryOperationException, LockException,
      InvalidItemStateException, RepositoryException {
    getTarget().restore(version, relPath, removeExisting);
  }

  /**
   * {@inheritDoc}
   */
  public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException,
      InvalidItemStateException, RepositoryException {
    getTarget().restoreByLabel(versionLabel, removeExisting);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Value value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Value[] values) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, values);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, String[] values) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, values);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, String value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, InputStream value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, boolean value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, double value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, long value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Calendar value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Node value) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Value value, int type) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value, type);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, Value[] values, int type) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, values, type);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, String[] values, int type) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, values, type);
  }

  /**
   * {@inheritDoc}
   */
  public Property setProperty(String name, String value, int type) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    return getTarget().setProperty(name, value, type);
  }

  /**
   * {@inheritDoc}
   */
  public void unlock() throws UnsupportedRepositoryOperationException, LockException,
      AccessDeniedException, InvalidItemStateException, RepositoryException {
    getTarget().unlock();
  }

  /**
   * {@inheritDoc}
   */
  public void update(String srcWorkspaceName) throws NoSuchWorkspaceException,
      AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
    getTarget().update(srcWorkspaceName);
  }

  /**
   * {@inheritDoc}
   */
  public void save() throws AccessDeniedException, ItemExistsException,
      ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException,
      VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    getTarget().save();
  }

  /**
   * {@inheritDoc}
   */
  public void checkPermission(String actions) throws AccessControlException, RepositoryException {
    getExtendedRealNode().checkPermission(actions);
  }

  /**
   * {@inheritDoc}
   */
  public void clearACL() throws RepositoryException, AccessControlException {
    getExtendedRealNode().clearACL();
  }

  /**
   * {@inheritDoc}
   */
  public AccessControlList getACL() throws RepositoryException {
    return getExtendedRealNode().getACL();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isNodeType(InternalQName name) throws RepositoryException {
    ExtendedNode node = (ExtendedNode) getTargetReachable();
    return node == null ? false : node.isNodeType(name);
  }

  /**
   * {@inheritDoc}
   */
  public Lock lock(boolean isDeep, long timeOut) throws UnsupportedRepositoryOperationException,
      LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
    return getExtendedTarget().lock(isDeep, timeOut);
  }

  /**
   * {@inheritDoc}
   */
  public void removePermission(String identity) throws RepositoryException, AccessControlException {
    getExtendedRealNode().removePermission(identity);
  }

  /**
   * {@inheritDoc}
   */
  public void removePermission(String identity, String permission) throws RepositoryException,
      AccessControlException {
    getExtendedRealNode().removePermission(identity, permission);
  }

  /**
   * {@inheritDoc}
   */
  public void setPermission(String identity, String[] permission) throws RepositoryException,
      AccessControlException {
    getExtendedRealNode().setPermission(identity, permission);
  }

  /**
   * {@inheritDoc}
   */
  public void setPermissions(Map<String, String[]> permissions) throws RepositoryException,
      AccessControlException {
    getExtendedRealNode().setPermissions(permissions);
  }

  /**
   * {@inheritDoc}
   */
  public String getIdentifier() throws RepositoryException {
    ExtendedNode node = (ExtendedNode) getTarget();
    return node.getIdentifier();
  }

  /**
   * {@inheritDoc}
   */
  public NodeIterator getNodesLazily() throws RepositoryException {
    ExtendedNode node = (ExtendedNode) getTarget();
    return node.getNodesLazily();
  }

  @Override
  public NodeIterator getNodesLazily(int pageSize) throws RepositoryException {
    ExtendedNode node = (ExtendedNode) getTarget();
    return node.getNodesLazily(pageSize);
  }

  @Override
  public long getNodesCount() throws RepositoryException {
    ExtendedNode node = (ExtendedNode) getTarget();
    return node.getNodesCount();
  }
}
