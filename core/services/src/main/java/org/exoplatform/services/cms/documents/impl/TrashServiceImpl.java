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
package org.exoplatform.services.cms.documents.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.api.info.PreferencesInfo;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Oct 6, 2009 3:39:53 AM
 */
public class TrashServiceImpl implements TrashService {

  private static final String FILE_EXPLORER_PORTLET = "FileExplorerPortlet";
  private final static String CACHE_NAME = "ecms.seo";
  final static public String EXO_TOTAL = "exo:total";
  final static public String MIX_REFERENCEABLE = "mix:referenceable";
  final static public String UUID         = "exo:uuid";
  final static public String SYMLINK      = "exo:symlink";
  final static public String EXO_WORKSPACE = "exo:workspace";
  final static public String EXO_TARGETWS = "exo:targetWorkspace";
  final static public String EXO_TARGETPATH = "exo:targetPath";

  private RepositoryService repositoryService;
  private LinkManager linkManager;
  private String trashWorkspace_;
  private String trashHome_;
  private ExoCache<String, Object> cache;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(TrashServiceImpl.class.getName());

  public TrashServiceImpl(RepositoryService repositoryService,
                          LinkManager linkManager,
                          InitParams initParams) throws Exception {
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
    this.trashWorkspace_ = initParams.getValueParam("trashWorkspace").getValue();
    this.trashHome_ = initParams.getValueParam("trashHomeNodePath").getValue();
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(CACHE_NAME);
    ExoContainer manager = ExoContainerContext.getCurrentContainer();
    PortletInvoker portletInvoker = (PortletInvoker)manager.getComponentInstance(PortletInvoker.class);
    if (portletInvoker != null) {
      Set<org.gatein.pc.api.Portlet> portlets = portletInvoker.getPortlets();
      for (org.gatein.pc.api.Portlet portlet : portlets) {
        PortletInfo info = portlet.getInfo();
        String portletName = info.getName();
        if (FILE_EXPLORER_PORTLET.equalsIgnoreCase(portletName)) {
          PreferencesInfo prefs = info.getPreferences();
          String trashWorkspace = prefs.getPreference("trashWorkspace").getDefaultValue().get(0);
          String trashHome = prefs.getPreference("trashHomeNodePath").getDefaultValue().get(0);
          if (trashWorkspace != null && !trashWorkspace.equals(this.trashWorkspace_)) {
            this.trashWorkspace_ = trashWorkspace;
          }

          if (trashHome != null && !trashHome.equals(this.trashHome_)) {
            this.trashHome_ = trashHome;
          }
          break;
        }
      }
    }
  }


  /**
   * {@inheritDoc}
   */
  public String moveToTrash(Node node, SessionProvider sessionProvider) throws Exception {
    return moveToTrash(node, sessionProvider, 0);
  }


  /**
   *{@inheritDoc}
   */
  @Override
  public String moveToTrash(Node node,
                          SessionProvider sessionProvider,
                            int deep) throws Exception {
    ((SessionImpl) node.getSession()).getActionHandler().preRemoveItem((ItemImpl) node);
    String trashId = "-1";
    String nodeName = node.getName();
    Session nodeSession = node.getSession();
    nodeSession.checkPermission(node.getPath(), PermissionType.REMOVE);
    if (deep == 0 && !node.isNodeType(SYMLINK)) {
      try {
        Utils.removeDeadSymlinks(node);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
    if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) || node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
      ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
      if (activityService.isBroadcastNTFileEvents(node)) {
        listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, null, node);
      }
    } else {
      listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, null, node);
    }
    String nodeWorkspaceName = nodeSession.getWorkspace().getName();
    if (!node.isNodeType(EXO_RESTORE_LOCATION)) {
      String restorePath = fixRestorePath(node.getPath());
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session trashSession = WCMCoreUtils.getSystemSessionProvider().getSession(this.trashWorkspace_, manageableRepository);
      String actualTrashPath = this.trashHome_ + (this.trashHome_.endsWith("/") ? "" : "/") + fixRestorePath(nodeName);
      if (trashSession.getWorkspace().getName().equals(nodeSession.getWorkspace().getName())) {
        trashSession.getWorkspace().move(node.getPath(), actualTrashPath);
      } else {
        // clone node in trash folder
        trashSession.getWorkspace().clone(nodeWorkspaceName, node.getPath(), actualTrashPath, true);
        node.remove();
      }
      trashId = addRestorePathInfo(nodeName, restorePath, nodeWorkspaceName);
      trashSession.save();
    }
    return trashId;
  }
 
/** Store original path of deleted node.
  * Return restore_id of deleted node. Use when find node in trash to undo
  * @param nodeName name of removed node
  * @param restorePath path of node before removing
  * @param nodeWs node workspace before removing
  * @throws RepositoryException 
  * @throws LockException 
  * @throws ConstraintViolationException 
  * @throws VersionException 
  * @throws NoSuchNodeTypeException 
  */
  private String addRestorePathInfo(String nodeName, String restorePath, String nodeWs) throws Exception {
    String restoreId = java.util.UUID.randomUUID().toString();
    NodeIterator nodes = this.getTrashHomeNode().getNodes(nodeName);
    Node node = null;
    while (nodes.hasNext()) {
      Node currentNode = nodes.nextNode();
      if (node == null) {
        node = currentNode;
      } else {
        if (node.getIndex() < currentNode.getIndex()) {
          node = currentNode;
        }
      }
    }
    if (node != null) {
      node.addMixin(EXO_RESTORE_LOCATION);
      node.setProperty(RESTORE_PATH, restorePath);
      node.setProperty(RESTORE_WORKSPACE, nodeWs);
      node.setProperty(TRASH_ID, restoreId);
      node.save();
    }
    return restoreId;
  }

  /**
   * {@inheritDoc}
   */
  public void restoreFromTrash(String trashNodePath,
                               SessionProvider sessionProvider) throws Exception {
    Node trashHomeNode = this.getTrashHomeNode();
    Session trashNodeSession = trashHomeNode.getSession();
    Node trashNode = (Node)trashNodeSession.getItem(trashNodePath);
    String trashWorkspace = trashNodeSession.getWorkspace().getName();
    String restoreWorkspace = trashNode.getProperty(RESTORE_WORKSPACE).getString();
    String restorePath = trashNode.getProperty(RESTORE_PATH).getString();

    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session restoreSession = sessionProvider.getSession(restoreWorkspace,  manageableRepository);

    if (restoreWorkspace.equals(trashWorkspace)) {
      trashNodeSession.getWorkspace().move(trashNodePath, restorePath);
    } else {
      //clone node
      restoreSession.getWorkspace().clone(
          trashWorkspace, trashNodePath, restorePath, true);
      trashNodeSession.getItem(trashNodePath).remove();
    }

    removeMixinEXO_RESTORE_LOCATION(restoreSession, restorePath);

    trashNodeSession.save();
    restoreSession.save();
  }


  /**
   * {@inheritDoc}
   */
  public List<Node> getAllNodeInTrash(SessionProvider sessionProvider) throws Exception {

    StringBuilder query = new StringBuilder("SELECT * FROM nt:base WHERE exo:restorePath IS NOT NULL");

    return selectNodesByQuery(sessionProvider, query.toString(), Query.SQL);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllNodeInTrashByUser(SessionProvider sessionProvider,
                                            String userName) throws Exception {
    StringBuilder query = new StringBuilder(
        "SELECT * FROM nt:base WHERE exo:restorePath IS NOT NULL AND exo:lastModifier='").append(userName).append("'");
    return selectNodesByQuery(sessionProvider, query.toString(), Query.SQL);
  }


  public void removeRelations(Node node, SessionProvider sessionProvider) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    String[] workspaces = manageableRepository.getWorkspaceNames();

    String queryString = "SELECT * FROM exo:relationable WHERE exo:relation IS NOT NULL";
    boolean error = false;

    for (String ws : workspaces) {
      Session session = sessionProvider.getSession(ws, manageableRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.SQL);
      QueryResult queryResult = query.execute();

      NodeIterator iter = queryResult.getNodes();
      while (iter.hasNext()) {
        try {
          iter.nextNode().removeMixin("exo:relationable");
          session.save();
        } catch (Exception e) {
          error = true;
        }
      }
    }
    if (error) throw new Exception("Can't remove exo:relationable of all related nodes");
  }

  /**
   * {@inheritDoc}
   */
  public boolean isInTrash(Node node) throws RepositoryException {
    return node.getPath().startsWith(this.trashHome_) && !node.getPath().equals(this.trashHome_);
  }

  /**
   * {@inheritDoc}
   */
  public Node getTrashHomeNode() {
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider()
                                    .getSession(trashWorkspace_,
                                                repositoryService.getCurrentRepository());
      return (Node) session.getItem(trashHome_);
    } catch (Exception e) {
      return null;
    }

  }
  
  public Node getNodeByTrashId(String trashId) throws RepositoryException{
    QueryResult queryResult;
    NodeIterator iter;
    Session session = WCMCoreUtils.getSystemSessionProvider()
        .getSession(trashWorkspace_,
                    repositoryService.getCurrentRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT * from exo:restoreLocation WHERE exo:trashId = '").append(trashId).append("'");
    QueryImpl query = (QueryImpl) queryManager.createQuery(sb.toString(), Query.SQL);
    query.setLimit(1);
    queryResult = query.execute();
    iter = queryResult.getNodes();
    if(iter.hasNext()) return iter.nextNode();
    else return null;
  }


  private List<Node> selectNodesByQuery(SessionProvider sessionProvider,
                                        String queryString,
                                        String language) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(this.trashWorkspace_, manageableRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryString, language);
    QueryResult queryResult = query.execute();

    NodeIterator iter = queryResult.getNodes();
    while (iter.hasNext()) {
      ret.add(iter.nextNode());
    }

    return ret;
  }

  private String fixRestorePath(String path) {
    int leftBracket = path.lastIndexOf('[');
    int rightBracket = path.lastIndexOf(']');
    if (leftBracket == -1 || rightBracket == -1 ||
        (leftBracket >= rightBracket)) return path;

    try {
      Integer.parseInt(path.substring(leftBracket+1, rightBracket));
    } catch (Exception ex) {
      return path;
    }
    return path.substring(0, leftBracket);
  }

  private void removeMixinEXO_RESTORE_LOCATION(Session session, String restorePath) throws Exception {
    Node sameNameNode = ((Node) session.getItem(restorePath));
    Node parent = sameNameNode.getParent();
    String name = sameNameNode.getName();
    NodeIterator nodeIter = parent.getNodes(name);
    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (node.isNodeType(EXO_RESTORE_LOCATION))
        node.removeMixin(EXO_RESTORE_LOCATION);
    }
  }

}
