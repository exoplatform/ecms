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
package org.exoplatform.services.wcm.core;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *              anhvurz90@gmail.com
 * 5 Jul. 2011
 */
public class ItemLocation {
  
  /** The repository. */
  protected String repository;

  /** The workspace. */
  protected String workspace;

  /** The path. */
  protected String path;
  
  /** The UUID. */
  protected String uuid;
  
  /** If session is system */
  protected boolean isSystemSession;
  
  /**
   * Instantiates a new item location.
   */
  public ItemLocation() {}
  
  /**
   * Instantiates a new item location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   * @param isSystem if the node session is system 
   */
  public ItemLocation(final String repository, final String workspace, final String path, final String uuid, 
      final boolean isSystem) {
    this.repository = repository;
    this.workspace = workspace;
    this.path = path;
    this.uuid = uuid;
    this.isSystemSession = isSystem;
  }
  
  /**
   * Instantiates a new item location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   */
  public ItemLocation(final String repository, final String workspace, final String path, final String uuid ) {
    this(repository, workspace, path, uuid, false);
  }
  
  /**
   * Instantiates a new item location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   */
  public ItemLocation(final String repository, final String workspace, final String path) {
    this(repository, workspace, path, null, false);
  }

  /**
   * Instantiates a new item location.
   */
  public ItemLocation(ItemLocation itemLocation) {
    this(itemLocation.repository, itemLocation.workspace, itemLocation.path, itemLocation.uuid, itemLocation.isSystemSession);
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
   * Get an ItemLocation object by an item
   *
   * @param item the item
   *
   * @return a ItemLocation object
   */
  public static final ItemLocation getItemLocationByItem(final Item item) {
    Session session = null;
    try {
      session = item.getSession();
      String repository = ((ManageableRepository)session.getRepository()).getConfiguration().getName();
      String workspace = session.getWorkspace().getName();
      String path = item.getPath();
      String uuid = null;
      try {
        if (item instanceof Node)
          uuid = ((Node)item).getUUID();
      } catch (RepositoryException e) {
        uuid = null;
      }
      boolean isSystemSession = IdentityConstants.SYSTEM.equals(session.getUserID());
      return new ItemLocation(repository, workspace, path, uuid, isSystemSession);
    } catch (RepositoryException e) {
      return null;
    }
  }

  /**
   * Get an item by a ItemLocation object.
   *
   * @param itemLocation the ItemLocation object
   *
   * @return an item
   */
  public static final Item getItemByLocation(final ItemLocation itemLocation) {
    Session session = null;
    try {
      ManageableRepository repository = WCMCoreUtils.getRepository();
      session = (itemLocation.isSystemSession ? 
                        WCMCoreUtils.getSystemSessionProvider() : WCMCoreUtils.getUserSessionProvider())
                                    .getSession(itemLocation.getWorkspace(), repository);
      if (itemLocation.getUUID() != null)
        return session.getNodeByUUID(itemLocation.getUUID());
      else {
        return WCMCoreUtils.getService(NodeFinder.class).getItem(session, itemLocation.getPath());
      }
    } catch(PathNotFoundException pne) {
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  
  
}
