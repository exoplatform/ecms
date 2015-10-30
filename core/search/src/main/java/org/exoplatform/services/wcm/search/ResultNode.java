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
package org.exoplatform.services.wcm.search;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
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
import javax.jcr.query.Row;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.services.wcm.core.NodeLocation;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Feb 2, 2009
 */
public class ResultNode implements Node{

  /** The node. */
  private NodeLocation nodeLocation;

  /** The score. */
  private float score;

  /** The excerpt. */
  private String excerpt;

  /** user navigation node's uri **/
  private String userNavigationURI;

  /**
   * Instantiates a new result node.
   *
   * @param node the node
   * @param row the row
   *
   * @throws RepositoryException the repository exception
   */
  public ResultNode(Node node, Row row) throws RepositoryException{
    this.nodeLocation = NodeLocation.getNodeLocationByNode(node);
    Value excerpt = row.getValue("rep:excerpt(.)");
    this.excerpt = excerpt == null ? "" : excerpt.getString();
    this.score = row.getValue("jcr:score").getLong();
  }

  public ResultNode(Node node, float score, String excerpt) {
    this.nodeLocation = NodeLocation.getNodeLocationByNode(node);
    this.excerpt = excerpt;
    this.score = score;
  }

  public ResultNode(Node node, Row row, String userNavURI) throws RepositoryException {
    this.nodeLocation = NodeLocation.getNodeLocationByNode(node);
    this.userNavigationURI = userNavURI;
    Value excerpt = row.getValue("rep:excerpt(.)");
    this.excerpt = excerpt == null ? "" : excerpt.getString();
    this.score = row.getValue("jcr:score").getLong();
  }

  /**
   * Gets the node.
   *
   * @return the node
   */
  public Node getNode() { return NodeLocation.getNodeByLocation(nodeLocation); }

  /**
   * Sets the node.
   *
   * @param node the new node
   */
  public void setNode(Node node) { this.nodeLocation = NodeLocation.getNodeLocationByNode(node); }

  /**
   * @param userNavigationURI the userNavigationURI to set
   */
  public void setUserNavigationURI(String userNavigationURI) {
    this.userNavigationURI = userNavigationURI;
  }

  /**
   * @return the userNavigationURI
   */
  public String getUserNavigationURI() {
    return userNavigationURI;
  }

  /**
   * Gets the score.
   *
   * @return the score
   */
  public float getScore() { return score; }

  /**
   * Sets the score.
   *
   * @param score the new score
   */
  public void setScore(float score) { this.score = score; }

  /**
   * Gets the excerpt.
   *
   * @return the excerpt
   */
  public String getExcerpt() {
	excerpt = StringEscapeUtils.unescapeHtml(excerpt);
    return excerpt;
  }

  public String getEditor() {
    String editor = null;
    try {
      if (getNode().hasProperty("exo:owner")) {
        editor = getNode().getProperty("exo:owner").getString();
      }
    } catch (Exception ex) {
      editor = null;
    }
    return editor;
  }

  /**
   * Sets the excerpt.
   *
   * @param excerpt the new excerpt
   */
  public void setExcerpt(String excerpt) {
    this.excerpt = excerpt;
  }

  /**
   * Gets the title.
   *
   * @return the title
   *
   * @throws Exception the exception
   */
  public String getTitle() throws Exception {
    if(getNode().hasProperty("exo:title")) {
      return getNode().getProperty("exo:title").getString();
    }
    return getNode().getName();
  }

  /**
   * Gets the summary.
   *
   * @return the summary
   *
   * @throws Exception the exception
   */
  public String getSummary() throws Exception {
    if(getNode().hasProperty("exo:summary")) {
      return getNode().getProperty("exo:summary").getString();
    }
    return null;
  }

  /**
   * Get the meta tag SEO's description of page node
   *
   * @return
   * @throws Exception
   */
  public String getDescription() throws Exception {
    if (getNode().hasProperty("exo:metaDescription")) {
      return getNode().getProperty("exo:metaDescription").getString();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#addMixin(java.lang.String)
   */
  public void addMixin(String name) throws NoSuchNodeTypeException, VersionException,
  ConstraintViolationException, LockException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#addNode(java.lang.String)
   */
  public Node addNode(String name) throws ItemExistsException, PathNotFoundException,
  VersionException, ConstraintViolationException, LockException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
   */
  public Node addNode(String name, String type) throws ItemExistsException,
  PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
  ConstraintViolationException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#canAddMixin(java.lang.String)
   */
  public boolean canAddMixin(String name) throws NoSuchNodeTypeException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
   */
  public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
  UnsupportedRepositoryOperationException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#checkin()
   */
  public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
  InvalidItemStateException, LockException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#checkout()
   */
  public void checkout() throws UnsupportedRepositoryOperationException, LockException,
  RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
   */
  public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
  UnsupportedRepositoryOperationException, RepositoryException {
    throw new  ConstraintViolationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getBaseVersion()
   */
  public Version getBaseVersion() throws UnsupportedRepositoryOperationException,
  RepositoryException {
    return getNode().getBaseVersion();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
   */
  public String getCorrespondingNodePath(String nodePath) throws ItemNotFoundException,
  NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
    return getNode().getCorrespondingNodePath(nodePath);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getDefinition()
   */
  public NodeDefinition getDefinition() throws RepositoryException {
    return getNode().getDefinition();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getIndex()
   */
  public int getIndex() throws RepositoryException {
    return getNode().getIndex();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getLock()
   */
  public Lock getLock() throws UnsupportedRepositoryOperationException, LockException,
  AccessDeniedException, RepositoryException {
    return getNode().getLock();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getMixinNodeTypes()
   */
  public NodeType[] getMixinNodeTypes() throws RepositoryException {
    return getNode().getMixinNodeTypes();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getNode(java.lang.String)
   */
  public Node getNode(String name) throws PathNotFoundException, RepositoryException {
    return getNode().getNode(name);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getNodes()
   */
  public NodeIterator getNodes() throws RepositoryException {
    return getNode().getNodes();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getNodes(java.lang.String)
   */
  public NodeIterator getNodes(String name) throws RepositoryException {
    return getNode().getNodes(name);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getPrimaryItem()
   */
  public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
    return getNode().getPrimaryItem();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getPrimaryNodeType()
   */
  public NodeType getPrimaryNodeType() throws RepositoryException {
    return getNode().getPrimaryNodeType();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getProperties()
   */
  public PropertyIterator getProperties() throws RepositoryException {
    return getNode().getProperties();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getProperties(java.lang.String)
   */
  public PropertyIterator getProperties(String name) throws RepositoryException {
    return getNode().getProperties(name);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getReferences()
   */
  public PropertyIterator getReferences() throws RepositoryException {
    return getNode().getReferences();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getUUID()
   */
  public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
    return getNode().getUUID();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getVersionHistory()
   */
  public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException,
  RepositoryException {
    return getNode().getVersionHistory();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#hasNode(java.lang.String)
   */
  public boolean hasNode(String name) throws RepositoryException {
    return getNode().hasNode(name);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#hasNodes()
   */
  public boolean hasNodes() throws RepositoryException {
    return getNode().hasNodes();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#hasProperties()
   */
  public boolean hasProperties() throws RepositoryException {
    return getNode().hasProperties();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#hasProperty(java.lang.String)
   */
  public boolean hasProperty(String name) throws RepositoryException {
    return getNode().hasProperty(name);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#holdsLock()
   */
  public boolean holdsLock() throws RepositoryException {
    return getNode().holdsLock();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#isCheckedOut()
   */
  public boolean isCheckedOut() throws RepositoryException {
    return getNode().isCheckedOut();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#isLocked()
   */
  public boolean isLocked() throws RepositoryException {
    return getNode().isLocked();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#isNodeType(java.lang.String)
   */
  public boolean isNodeType(String type) throws RepositoryException {
    return getNode().isNodeType(type);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#lock(boolean, boolean)
   */
  public Lock lock(boolean arg0, boolean arg1) throws UnsupportedRepositoryOperationException,
  LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
    throw new UnsupportedRepositoryOperationException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#merge(java.lang.String, boolean)
   */
  public NodeIterator merge(String arg0, boolean arg1) throws NoSuchWorkspaceException,
  AccessDeniedException, MergeException, LockException, InvalidItemStateException,
  RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
   */
  public void orderBefore(String arg0, String arg1)
  throws UnsupportedRepositoryOperationException, VersionException,
  ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
    getNode().orderBefore(arg0, arg1);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#removeMixin(java.lang.String)
   */
  public void removeMixin(String arg0) throws NoSuchNodeTypeException, VersionException,
  ConstraintViolationException, LockException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#restore(java.lang.String, boolean)
   */
  public void restore(String arg0, boolean arg1) throws VersionException, ItemExistsException,
  UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
  RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
   */
  public void restore(Version arg0, boolean arg1) throws VersionException, ItemExistsException,
  UnsupportedRepositoryOperationException, LockException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#restore(javax.jcr.version.Version, java.lang.String, boolean)
   */
  public void restore(Version arg0, String arg1, boolean arg2) throws PathNotFoundException,
  ItemExistsException, VersionException, ConstraintViolationException,
  UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
  RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#restoreByLabel(java.lang.String, boolean)
   */
  public void restoreByLabel(String arg0, boolean arg1) throws VersionException,
  ItemExistsException, UnsupportedRepositoryOperationException, LockException,
  InvalidItemStateException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value)
   */
  public Property setProperty(String arg0, Value arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[])
   */
  public Property setProperty(String arg0, Value[] arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[])
   */
  public Property setProperty(String arg0, String[] arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String)
   */
  public Property setProperty(String arg0, String arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.io.InputStream)
   */
  public Property setProperty(String arg0, InputStream arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
   */
  public Property setProperty(String arg0, boolean arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, double)
   */
  public Property setProperty(String arg0, double arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, long)
   */
  public Property setProperty(String arg0, long arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.util.Calendar)
   */
  public Property setProperty(String arg0, Calendar arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Node)
   */
  public Property setProperty(String arg0, Node arg1) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value, int)
   */
  public Property setProperty(String arg0, Value arg1, int arg2) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[], int)
   */
  public Property setProperty(String arg0, Value[] arg1, int arg2) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[], int)
   */
  public Property setProperty(String arg0, String[] arg1, int arg2) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String, int)
   */
  public Property setProperty(String arg0, String arg1, int arg2) throws ValueFormatException,
  VersionException, LockException, ConstraintViolationException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#unlock()
   */
  public void unlock() throws UnsupportedRepositoryOperationException, LockException,
  AccessDeniedException, InvalidItemStateException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#update(java.lang.String)
   */
  public void update(String arg0) throws NoSuchWorkspaceException, AccessDeniedException,
  LockException, InvalidItemStateException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
   */
  public void accept(ItemVisitor arg0) throws RepositoryException {
    getNode().accept(arg0);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getAncestor(int)
   */
  public Item getAncestor(int arg0) throws ItemNotFoundException, AccessDeniedException,
  RepositoryException {
    return getNode().getAncestor(arg0);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getDepth()
   */
  public int getDepth() throws RepositoryException {
    return getNode().getDepth();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getName()
   */
  public String getName() throws RepositoryException {
    return getNode().getName();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#isModified()
   */
  public boolean isModified() {
    return getNode().isModified();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#isNew()
   */
  public boolean isNew() {
    return getNode().isNew();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#isNode()
   */
  public boolean isNode() {
    return getNode().isNode();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#isSame(javax.jcr.Item)
   */
  public boolean isSame(Item arg0) throws RepositoryException {
    return getNode().isSame(arg0);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#refresh(boolean)
   */
  public void refresh(boolean arg0) throws InvalidItemStateException, RepositoryException {
    getNode().refresh(arg0);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#remove()
   */
  public void remove() throws VersionException, LockException, ConstraintViolationException,
  RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#save()
   */
  public void save() throws AccessDeniedException, ItemExistsException,
  ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException,
  VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    throw new RepositoryException("Unsupported this method");
  }

  /* (non-Javadoc)
   * @see javax.jcr.Node#getProperty(java.lang.String)
   */
  public Property getProperty(String arg0) throws PathNotFoundException, RepositoryException {
    return getNode().getProperty(arg0);
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getParent()
   */
  public Node getParent() throws ItemNotFoundException, AccessDeniedException,
  RepositoryException {
    return getNode().getParent();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getPath()
   */
  public String getPath() throws RepositoryException {
    return getNode().getPath();
  }

  /* (non-Javadoc)
   * @see javax.jcr.Item#getSession()
   */
  public Session getSession() throws RepositoryException {
    return getNode().getSession();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    try {
      ResultNode resNode = (ResultNode)obj;
      if(getNode().getPath().equals(resNode.getNode().getPath())) return true;
    } catch(Exception e) {
      return false;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return nodeLocation.hashCode();
  }
}
