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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 1 avr. 2009
 */
public class NodeIteratorLinkAware extends RangeIteratorLinkAware implements NodeIterator {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("cms.NodeIteratorLinkAware");

  public NodeIteratorLinkAware(String originalWorkspace, String virtualPath, NodeIterator nodeIterator) {
    super(originalWorkspace, virtualPath, nodeIterator);
  }

  /**
   * {@inheritDoc}
   */
  public Node nextNode() {
    Node node = (Node) iterator.next();
    try {
      return new NodeLinkAware(originalWorkspace, LinkUtils.createPath(virtualPath, node.getName()
          + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : "")), node);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot create an instance of NodeLinkAware", e);
      }
    }
    return node;
  }

  /**
   * {@inheritDoc}
   */
  public Object next() {
    return nextNode();
  }
}
