/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.clouddrive.jcr;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRNodeFinder.java 00000 Feb 26, 2013 pnedonosko $
 * 
 */
public class JCRNodeFinder implements NodeFinder {

  protected final RepositoryService      jcrService;

  protected final SessionProviderService sessionProviderService;

  protected final NodeHierarchyCreator   hierarchyCreator;

  public JCRNodeFinder(RepositoryService jcrService,
                       SessionProviderService sessionProviderService,
                       NodeHierarchyCreator hierarchyCreator) {
    this.jcrService = jcrService;
    this.sessionProviderService = sessionProviderService;
    this.hierarchyCreator = hierarchyCreator;
  }

  /**
   * @inherritDoc
   */
  @Override
  public Item getItem(Session userSession, String path, boolean symlinkTarget) throws PathNotFoundException,
                                                                               RepositoryException {
    return userSession.getItem(path);
  }

  /**
   * @inherritDoc
   */
  @Override
  public Item findItem(Session userSession, String path) throws PathNotFoundException, RepositoryException {
    return userSession.getItem(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Node> findLinked(Session session, String uuid) throws PathNotFoundException, RepositoryException {
    Set<Node> res = new LinkedHashSet<Node>();
    try {
      Node target = session.getNodeByUUID(uuid);
      for (PropertyIterator piter = target.getReferences(); piter.hasNext();) {
        res.add(piter.nextProperty().getParent());
      }
    } catch (ItemNotFoundException e) {
      // nothing
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String cleanName(String name) {
    return name; // no conversion required
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getUserNode(String userName) throws Exception {
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    Node userNode = hierarchyCreator.getUserNode(sessionProvider, userName);
    return userNode;
  }
}
