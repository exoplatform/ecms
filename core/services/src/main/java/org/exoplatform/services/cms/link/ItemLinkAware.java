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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.ItemLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 1 avr. 2009
 */
public abstract class ItemLinkAware implements Item {

  protected final ItemLocation itemLocation;
  protected final String originalWorkspaceName;
  protected final String virtualPath;

  protected ItemLinkAware(String originalWorkspaceName, String virtualPath, Item item) {
    this.originalWorkspaceName = originalWorkspaceName;
    this.itemLocation = ItemLocation.getItemLocationByItem(item);
    if (!virtualPath.startsWith("/")) {
      throw new IllegalArgumentException("The path '" + virtualPath +  "' must be an absolute path");
    }
    this.virtualPath = virtualPath;
  }

  public static ItemLinkAware newInstance(String originalWorkspaceName, String originalAbsPath, Item item) {
    if (item instanceof Node) {
      return new NodeLinkAware(originalWorkspaceName, originalAbsPath, (Node) item);
    } 
    return new PropertyLinkAware(originalWorkspaceName, originalAbsPath, (Property) item);
  }
  
  public Item getItem() {
    return ItemLocation.getItemByLocation(itemLocation);
  }
  
  public Session getItemSession() throws RepositoryException {
    return getItem().getSession();
  }
  
  public Session getOriginalSession() throws RepositoryException {
    SessionProvider sessionProvider;
    if (itemLocation.isSystemSession()) {
      sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    } else {
      sessionProvider = WCMCoreUtils.getUserSessionProvider();
    }
    return sessionProvider.getSession(originalWorkspaceName, WCMCoreUtils.getRepository());
  }
  
  /**
   * {@inheritDoc}
   */
  public void accept(ItemVisitor visitor) throws RepositoryException {
    getItem().accept(visitor);
  }

  /**
   * {@inheritDoc}
   */
  public Item getAncestor(int depth) throws ItemNotFoundException,
                                   AccessDeniedException,
                                   RepositoryException {
    return LinkUtils.getNodeFinder().getItem(getOriginalSession(), LinkUtils.getAncestorPath(virtualPath, depth));
  }

  /**
   * {@inheritDoc}
   */
  public int getDepth() throws RepositoryException {
    return LinkUtils.getDepth(virtualPath);
  }

  /**
   * {@inheritDoc}
   */
  public String getName() throws RepositoryException {
    return getItem().getName();
  }

  /**
   * {@inheritDoc}
   */
  public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    return (Node) LinkUtils.getNodeFinder().getItem(getOriginalSession(), LinkUtils.getParentPath(virtualPath));
  }

  /**
   * {@inheritDoc}
   */
  public String getPath() throws RepositoryException {
    return virtualPath;
  }

  /**
   * {@inheritDoc}
   */
  public Session getSession() throws RepositoryException {
    return new SessionLinkAware(this);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isModified() {
    return getItem().isModified();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isNew() {
    return getItem().isNew();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isNode() {
    return getItem().isNode();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isSame(Item otherItem) throws RepositoryException {
    return getItem().isSame(otherItem);
  }

  /**
   * {@inheritDoc}
   */
  public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
    getItem().refresh(keepChanges);
  }

  /**
   * {@inheritDoc}
   */
  public void remove() throws VersionException,
                      LockException,
                      ConstraintViolationException,
                      RepositoryException {
    getItem().remove();
  }

  /**
   * {@inheritDoc}
   */
  public void save() throws AccessDeniedException,
                    ItemExistsException,
                    ConstraintViolationException,
                    InvalidItemStateException,
                    ReferentialIntegrityException,
                    VersionException,
                    LockException,
                    NoSuchNodeTypeException,
                    RepositoryException {
    getItem().save();
  }
}
