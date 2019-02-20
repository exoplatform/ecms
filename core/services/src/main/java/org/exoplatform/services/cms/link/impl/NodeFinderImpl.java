/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.cms.link.impl;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Mar 14, 2009
 */
public class NodeFinderImpl implements NodeFinder {

  private final RepositoryService repositoryService_;

  private final LinkManager linkManager_;

  public NodeFinderImpl(RepositoryService repositoryService, LinkManager linkManager){
    this.repositoryService_ = repositoryService;
    this.linkManager_ = linkManager;
  }

  /**
   * {@inheritDoc}
   */
  public Item getItem(String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                           RepositoryException {
    return getItemGiveTargetSys(workspace, absPath, giveTarget, false);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItemGiveTargetSys(String workspace,
                                   String absPath,
                                   boolean giveTarget,
                                   boolean system) throws PathNotFoundException,
                                                  RepositoryException {
    if (!absPath.startsWith("/"))
      throw new IllegalArgumentException(absPath + " isn't absolute path");
    Session session = getSession(repositoryService_.getCurrentRepository(), workspace);
    return getItemTarget(session, absPath, giveTarget, system);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItem(String workspace, String absPath) throws PathNotFoundException,
                                                       RepositoryException {
    return getItem(workspace, absPath, false);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItemSys(String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                                             RepositoryException {
    return getItemGiveTargetSys(workspace, absPath, false, system);
  }

  /**
   * {@inheritDoc}
   */
  public Node getNode(Node ancestorNode, String relativePath, boolean giveTarget) throws PathNotFoundException,
                                                             RepositoryException {
    if (relativePath.startsWith("/"))
      throw new IllegalArgumentException("Invalid relative path: " + relativePath);
    String absPath = "";
    if (ancestorNode.getPath().equals("/"))
      absPath = "/" + relativePath;
    else
      absPath = ancestorNode.getPath() + "/" + relativePath;
    Session session = ancestorNode.getSession();
    return (Node) getItem(session, absPath, giveTarget);
  }

  /**
   * {@inheritDoc}
   */
  public Node getNode(Node ancestorNode, String relativePath) throws PathNotFoundException,
                                                             RepositoryException {
    return getNode(ancestorNode, relativePath, false);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItem(Session session, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                          RepositoryException {
    return getItem(session, absPath, giveTarget, 0, false);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItemTarget(Session session, String absPath, boolean giveTarget, boolean system) throws PathNotFoundException,
                                                                                                RepositoryException {
    return getItem(session, absPath, giveTarget, 0, system);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItem(Session session, String absPath) throws PathNotFoundException, RepositoryException {
    return getItem(session, absPath, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean itemExists(Session session, String absPath) throws RepositoryException {
    try {
      return getItem(session, absPath) != null;
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  /**
   * Get item by absolute path
   * @param session    The user session
   * @param absPath    The absolute path to node
   * @param fromIdx    The start index used to find the link
   * @param giveTarget Indicates if the target must be returned in case the item is a link
   * @return the item corresponding to the path
   */
  public Item getItem(Session session,
                       String absPath,
                       boolean giveTarget,
                       int fromIdx,
                       boolean system) throws PathNotFoundException, RepositoryException {
    if(absPath.contains("\\'")) {
      absPath = absPath.replaceAll("\\\\'", "'");
    }

    boolean itemExists = session.itemExists(absPath);
    if (!itemExists && absPath.contains("%") && session.itemExists(Text.unescapeIllegalJcrChars(absPath))) {
      absPath = Text.unescapeIllegalJcrChars(absPath);
      itemExists = session.itemExists(absPath);
    }

    if (itemExists) {
      // The item corresponding to absPath can be found
      Item item = session.getItem(absPath);
      if (giveTarget && linkManager_.isLink(item)) {
        return linkManager_.getTarget((Node) item);
      }
      return item;
    }
    // The item corresponding to absPath can not be found so we split absPath
    // and check
    String[] splitPath = absPath.substring(1).split("/");
    int low = fromIdx;
    int high = splitPath.length - 1;
    int mid = high;
    while (low <= mid) {
      mid--;
      String partPath = makePath(splitPath, mid);

      if (session.itemExists(partPath)) {
        // The item can be found
        Item item = session.getItem(partPath);
        if (linkManager_.isLink(item)) {
          // The item is a link
          Node link = (Node) item;
          if (linkManager_.isTargetReachable(link, system)) {
            // The target can be reached
            Node target = linkManager_.getTarget(link, system);
            String targetPath = target.getPath();
            return getItem(target.getSession(),
                           targetPath + absPath.substring(partPath.length()),
                           giveTarget,
                           targetPath.substring(1).split("/").length, system);
          }
          // The target cannot be found
          throw new PathNotFoundException("Can't reach the target of the link: " + link.getPath());
        }
      }
    }
    throw new PathNotFoundException("Can't find path: " + absPath);
  }

  /**
   * Get session of user in given workspace and repository
   * @param manageableRepository
   * @param workspace
   * @throws RepositoryException
   */
  public Session getSession(ManageableRepository manageableRepository, String workspace) throws RepositoryException {
    SessionProviderService service = WCMCoreUtils.getService(SessionProviderService.class);
    return service.getSessionProvider(null).getSession(workspace, manageableRepository);
  }

  /**
   * Make sub path of absolute path from 0 to toIdx index
   *
   * @param splitString
   * @param toIdx
   */
  public String makePath(String[] splitString, int toIdx) {
    StringBuilder buffer = new StringBuilder(1024);
    for(int i = 0; i <= toIdx; i++) {
      buffer.append('/').append(splitString[i]);
    }
    return buffer.toString();
  }
}
