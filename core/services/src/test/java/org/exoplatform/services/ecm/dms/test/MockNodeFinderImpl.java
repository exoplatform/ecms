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
package org.exoplatform.services.ecm.dms.test;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.link.impl.NodeFinderImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 10, 2010
 */
public class MockNodeFinderImpl extends NodeFinderImpl {
private final RepositoryService repositoryService_;

  private final LinkManager linkManager_;

  public MockNodeFinderImpl(RepositoryService repositoryService, LinkManager linkManager){
    super(repositoryService, linkManager);
    this.repositoryService_ = repositoryService;
    this.linkManager_ = linkManager;
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Item getItem(String repository, String workspace, String absPath, boolean giveTarget) throws PathNotFoundException,
                                                                          RepositoryException {
    return getItemGiveTargetSys(repository, workspace, absPath, giveTarget, false);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Item getItemGiveTargetSys(String repository, String workspace, String absPath,
      boolean giveTarget, boolean system) throws PathNotFoundException, RepositoryException {
    if (!absPath.startsWith("/"))
      throw new IllegalArgumentException(absPath + " isn't absolute path");
    Session session = getSession(repositoryService_.getCurrentRepository(), workspace);
    return getItemTarget(session, absPath, giveTarget, system);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Item getItem(String repository, String workspace, String absPath) throws PathNotFoundException,
                                                                          RepositoryException {
    return getItem(repository, workspace, absPath, false);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Item getItemSys(String repository, String workspace, String absPath, boolean system) throws PathNotFoundException,
                                                                          RepositoryException {
    return getItemGiveTargetSys(repository, workspace, absPath, false, system);
  }
}
