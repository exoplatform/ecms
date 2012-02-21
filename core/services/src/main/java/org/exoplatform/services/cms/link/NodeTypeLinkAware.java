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
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 8 avr. 2009
 */
public class NodeTypeLinkAware implements NodeType {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("services.cms.link.NodeTypeLinkAware");

  private final String name;

  private NodeLinkAware node;

  private volatile NodeType targetNodeType;

  NodeTypeLinkAware(NodeLinkAware node) throws RepositoryException {
    this.node = node;
    Node realNode = node.getRealNode();
    LinkManager manager = LinkUtils.getLinkManager();
    this.name = manager.isLink(realNode) ? manager.getTargetPrimaryNodeType(realNode) : realNode.getPrimaryNodeType().getName();
  }

  private NodeType getTargetNodeType() throws RepositoryException {
    if (targetNodeType == null) {
      synchronized (this) {
        if (targetNodeType == null) {
          targetNodeType = node.getTarget().getPrimaryNodeType();
          node = null;
        }
      }
    }
    return targetNodeType;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canAddChildNode(String childNodeName) {
    try {
      return getTargetNodeType().canAddChildNode(childNodeName);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
    try {
      return getTargetNodeType().canAddChildNode(childNodeName, nodeTypeName);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canRemoveItem(String itemName) {
    try {
      return getTargetNodeType().canRemoveItem(itemName);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canSetProperty(String propertyName, Value value) {
    try {
      return getTargetNodeType().canSetProperty(propertyName, value);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canSetProperty(String propertyName, Value[] values) {
    try {
      return getTargetNodeType().canSetProperty(propertyName, values);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public NodeDefinition[] getChildNodeDefinitions() {
    try {
      return getTargetNodeType().getChildNodeDefinitions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public NodeDefinition[] getDeclaredChildNodeDefinitions() {
    try {
      return getTargetNodeType().getDeclaredChildNodeDefinitions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public PropertyDefinition[] getDeclaredPropertyDefinitions() {
    try {
      return getTargetNodeType().getDeclaredPropertyDefinitions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public NodeType[] getDeclaredSupertypes() {
    try {
      return getTargetNodeType().getDeclaredSupertypes();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  public String getPrimaryItemName() {
    try {
      return getTargetNodeType().getPrimaryItemName();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public PropertyDefinition[] getPropertyDefinitions() {
    try {
      return getTargetNodeType().getPropertyDefinitions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public NodeType[] getSupertypes() {
    try {
      return getTargetNodeType().getSupertypes();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasOrderableChildNodes() {
    try {
      return getTargetNodeType().hasOrderableChildNodes();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isMixin() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isNodeType(String nodeTypeName) {
    try {
      return getTargetNodeType().isNodeType(nodeTypeName);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return false;
  }
}
