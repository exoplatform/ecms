/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.link.impl;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeLinkAware;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 * Mar 13, 2009
 */
public class LinkManagerImpl implements LinkManager {

  private final static String    SYMLINK       = "exo:symlink";

  private final static String    WORKSPACE     = "exo:workspace";

  private final static String    UUID          = "exo:uuid";

  private final static String    PRIMARY_TYPE  = "exo:primaryType";

  private final static String    SYMLINK_NAME  = "exo:name";

  private final static String    SYMLINK_TITLE = "exo:title";

  private final static Log       LOG  = ExoLogger.getLogger(LinkManagerImpl.class.getName());

  private final SessionProviderService providerService_;

  public LinkManagerImpl(SessionProviderService providerService) throws Exception {
    providerService_ = providerService;
  }

  /**
   * {@inheritDoc}
   */
  public Node createLink(Node parent, String linkType, Node target) throws RepositoryException {
    return createLink(parent, linkType, target, null);
  }

  /**
   * {@inheritDoc}
   */
  public Node createLink(Node parent, Node target) throws RepositoryException {
    return createLink(parent, null, target, null);
  }

  /**
   * {@inheritDoc}
   */  
  public Node createLink(Node parent, String linkType, Node target, String linkName) throws RepositoryException {
    return createLink(parent, linkType, target, linkName, linkName);
  }

  /**
   * {@inheritDoc}
   */
  public Node createLink(Node parent, String linkType, Node target, String linkName, String linkTitle)
      throws RepositoryException {
    if (!target.isNodeType(SYMLINK)) {
      boolean targetEdited = false;
      if (target.canAddMixin("mix:referenceable")) {
        target.addMixin("mix:referenceable");
        target.getSession().save();
        targetEdited = true;
      }
      if (linkType == null || linkType.trim().length() == 0)
        linkType = SYMLINK;
      if (linkName == null || linkName.trim().length() == 0)
        linkName = target.getName();
      Node linkNode = parent.addNode(linkName, linkType);
      try {
        updateAccessPermissionToLink(linkNode, target);
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("CAN NOT UPDATE ACCESS PERMISSIONS FROM TARGET NODE TO LINK NODE", e);
        }
      }
      linkNode.setProperty(WORKSPACE, target.getSession().getWorkspace().getName());
      linkNode.setProperty(PRIMARY_TYPE, target.getPrimaryNodeType().getName());
      linkNode.setProperty(UUID, target.getUUID());
      if(linkNode.canAddMixin("exo:sortable")) {
        linkNode.addMixin("exo:sortable");
      }
      linkNode.setProperty(SYMLINK_TITLE, linkTitle);
      linkNode.setProperty(SYMLINK_NAME, linkName);
      linkNode.getSession().save();
      ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
      try {
        String remoteUser = WCMCoreUtils.getRemoteUser();
        if (remoteUser != null) {
          if (Utils.isDocument(target) && targetEdited) {
            listenerService.broadcast(CmsService.POST_EDIT_CONTENT_EVENT, null, target);
          }
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error while broadcasting event: " + e.getMessage());
        }
      }
      return linkNode;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Node getTarget(Node link, boolean system) throws ItemNotFoundException,
  RepositoryException {
    String uuid = link.getProperty(UUID).getString();
    Node targetNode = getSession(link, system).getNodeByUUID(uuid);
    return targetNode;
  }

  private Session getSession(Node link, boolean system) throws RepositoryException {
    String workspaceTarget = link.getProperty(WORKSPACE).getString();
    return getSession((ManageableRepository) link.getSession().getRepository(), workspaceTarget,
                      system);
  }

  private Session getSession(ManageableRepository manageRepository, String workspaceName,
                             boolean system) throws RepositoryException {
    if (system)
      return providerService_.getSystemSessionProvider(null).getSession(workspaceName, manageRepository);
    return providerService_.getSessionProvider(null).getSession(workspaceName, manageRepository);
  }

  /**
   * {@inheritDoc}
   */
  public Node getTarget(Node link) throws ItemNotFoundException, RepositoryException {
    return getTarget(link, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isTargetReachable(Node link) throws RepositoryException {
    return isTargetReachable(link, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isTargetReachable(Node link, boolean system) throws RepositoryException {
    Session session = null;
    try {
      session = getSession(link, system);
      session.getNodeByUUID(link.getProperty(UUID).getString());
    } catch (ItemNotFoundException e) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public Node updateLink(Node linkNode, Node targetNode) throws RepositoryException {
    if (targetNode.canAddMixin("mix:referenceable")) {
      targetNode.addMixin("mix:referenceable");
      targetNode.getSession().save();
    }
    try {
      updateAccessPermissionToLink(linkNode, targetNode);
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("CAN NOT UPDATE ACCESS PERMISSIONS FROM TARGET NODE TO LINK NODE", e);
      }
    }
    linkNode.setProperty(UUID, targetNode.getUUID());
    linkNode.setProperty(PRIMARY_TYPE, targetNode.getPrimaryNodeType().getName());
    linkNode.setProperty(WORKSPACE, targetNode.getSession().getWorkspace().getName());
    linkNode.getSession().save();
    return linkNode;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isLink(Item item) throws RepositoryException {
    if (item instanceof Node) {
      Node node = (Node) item;
      if (node instanceof NodeLinkAware) {
        node = ((NodeLinkAware) node).getRealNode();
      }
      return node.isNodeType(SYMLINK);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getTargetPrimaryNodeType(Node link) throws RepositoryException {
    return link.getProperty(PRIMARY_TYPE).getString();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isFileOrParentALink(Session session, String path) {
    LinkManager linkManager = CommonsUtils.getService(LinkManager.class);
    StringBuilder itemPath = new StringBuilder();
    boolean isLink;
    try {
      for (String pathParts : path.split("/")) {
        if (!pathParts.isEmpty()) {
          itemPath.append("/").append(pathParts);
          isLink = linkManager.isLink(session.getItem(itemPath.toString()));
          if (isLink) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  /**
   * Update the permission between two given node
   * @param linkNode    The node to update permission
   * @param targetNode  The target node to get permission
   * @throws Exception
   */
  private void updateAccessPermissionToLink(Node linkNode, Node targetNode) throws Exception {
    if(canChangePermission(linkNode)) {
      if(linkNode.canAddMixin("exo:privilegeable")) {
        linkNode.addMixin("exo:privilegeable");
        ((ExtendedNode)linkNode).setPermission(getNodeOwner(linkNode),PermissionType.ALL);
      }
      removeCurrentIdentites(linkNode);
      Map<String, String[]> perMap = new HashMap<String, String[]>();
      List<String> permsList = new ArrayList<String>();
      List<String> idList = new ArrayList<String>();
      for(AccessControlEntry accessEntry : ((ExtendedNode)targetNode).getACL().getPermissionEntries()) {
        if(!idList.contains(accessEntry.getIdentity())) {
          idList.add(accessEntry.getIdentity());
          permsList = ((ExtendedNode)targetNode).getACL().getPermissions(accessEntry.getIdentity());
          perMap.put(accessEntry.getIdentity(), permsList.toArray(new String[permsList.size()]));
        }
      }
      ((ExtendedNode)linkNode).setPermissions(perMap);
    }
  }

  /**
   * Remove all identity of the given node
   * @param linkNode  The node to remove all identity
   * @throws AccessControlException
   * @throws RepositoryException
   */
  private void removeCurrentIdentites(Node linkNode) throws AccessControlException, RepositoryException {
    String currentUser = linkNode.getSession().getUserID();
    if (currentUser != null)
      ((ExtendedNode)linkNode).setPermission(currentUser, PermissionType.ALL);
    for(AccessControlEntry accessEntry : ((ExtendedNode)linkNode).getACL().getPermissionEntries()) {
      if(canRemovePermission(linkNode, accessEntry.getIdentity())
          && ((ExtendedNode)linkNode).getACL().getPermissions(accessEntry.getIdentity()).size() > 0
          && !accessEntry.getIdentity().equals(currentUser)) {
        ((ExtendedNode) linkNode).removePermission(accessEntry.getIdentity());
      }
    }
  }

  /**
   * Remove the permission from the given node
   * @param node      The node to remove permission
   * @param identity  The identity of the permission
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private boolean canRemovePermission(Node node, String identity) throws ValueFormatException,
  PathNotFoundException, RepositoryException {
    String owner = getNodeOwner(node);
    if(identity.equals(IdentityConstants.SYSTEM)) return false;
    if(owner != null && owner.equals(identity)) return false;
    return true;
  }

  /**
   * Get the owner of the given node
   * @param node      The node to get owner
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private String getNodeOwner(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
    if(node.hasProperty("exo:owner")) {
      return node.getProperty("exo:owner").getString();
    }
    return IdentityConstants.SYSTEM;
  }

  /**
   * Check permission of the given node
   * @param node      The Node to check permission
   * @return
   * @throws RepositoryException
   */
  private boolean canChangePermission(Node node) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(PermissionType.CHANGE_PERMISSION);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllLinks(Node targetNode, String linkType, SessionProvider sessionProvider) {
    try {
      List<Node> result = new ArrayList<Node>();
      ManageableRepository repository  = WCMCoreUtils.getRepository();
      String[] workspaces = repository.getWorkspaceNames();
      String queryString = new StringBuilder().append("SELECT * FROM ").
          append(linkType).
          append(" WHERE exo:uuid='").
          append(targetNode.getUUID()).append("'").
          append(" AND exo:workspace='").
          append(targetNode.getSession().getWorkspace().getName()).
          append("'").toString();

      for (String workspace : workspaces) {
        Session session = sessionProvider.getSession(workspace, repository);
        //Continue In the case cannot access to a workspace
        if(session == null) continue;
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.SQL);
        QueryResult queryResult = query.execute();
        NodeIterator iter = queryResult.getNodes();
        while (iter.hasNext()) {
          result.add(iter.nextNode());
        }
      }

      return result;
    } catch (RepositoryException e) {
      // return empty node list if there are errors in execution or user has no right to access nodes
      return new ArrayList<Node>();
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllLinks(Node targetNode, String linkType) {
    return getAllLinks(targetNode, linkType, WCMCoreUtils.getUserSessionProvider());
  }

  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  public void updateSymlink(Node node) throws RepositoryException {
    if (node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
      try {
        ((ExtendedNode)node).checkPermission(PermissionType.SET_PROPERTY);
      } catch(AccessControlException e) {
        SessionProvider provider = WCMCoreUtils.getSystemSessionProvider();
        node = (Node)provider.getSession(node.getSession().getWorkspace().getName(), 
                                         WCMCoreUtils.getRepository()).getItem(node.getPath());
      }
      if (node.canAddMixin(NodetypeConstant.EXO_TARGET_DATA)) {
        node.addMixin(NodetypeConstant.EXO_TARGET_DATA);
      }
      Node target = this.getTarget(node, true);
      if (!node.hasProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE) || 
          node.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate().compareTo( 
          target.getProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE).getDate()) < 0) {
        String[] propList = {NodetypeConstant.EXO_DATE_CREATED,
            NodetypeConstant.EXO_LAST_MODIFIED_DATE, NodetypeConstant.PUBLICATION_LIVE_DATE,
            NodetypeConstant.EXO_START_EVENT, NodetypeConstant.EXO_INDEX};
        for (String p : propList) {
          try {
            if (target.hasProperty(p)) {
              node.setProperty(p, target.getProperty(p).getValue());
              node.save();
            }
          } catch (RepositoryException e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("Can not update property: " + p + " for node: " + node.getPath(), e);
            }
          }
        }
    }
    }
  }

}
