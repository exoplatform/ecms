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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.api.info.PreferencesInfo;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Oct 6, 2009 3:39:53 AM
 */
public class TrashServiceImpl implements TrashService {

  private static final String FILE_EXPLORER_PORTLET = "FileExplorerPortlet";
  final static public String EXO_TOTAL = "exo:total";
  final static public String MIX_REFERENCEABLE = "mix:referenceable";
  final static public String TAXONOMY_LINK   = "exo:taxonomyLink";
  final static public String UUID         = "exo:uuid";
  final static public String SYMLINK      = "exo:symlink";
  final static public String EXO_WORKSPACE = "exo:workspace";
  final static public String EXO_TARGETWS = "exo:targetWorkspace";
  final static public String EXO_TARGETPATH = "exo:targetPath";

  private RepositoryService repositoryService;
  private LinkManager linkManager;
  private TaxonomyService taxonomyService_;
  private String trashWorkspace_;
  private String trashHome_;
  private ExoCache<String, Object> cache;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(TrashServiceImpl.class.getName());

  public TrashServiceImpl(RepositoryService repositoryService,
                          LinkManager linkManager,
                          TaxonomyService taxonomyService,
                          InitParams initParams) throws Exception {
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
    this.taxonomyService_ = taxonomyService;
    this.trashWorkspace_ = initParams.getValueParam("trashWorkspace").getValue();
    this.trashHome_ = initParams.getValueParam("trashHomeNodePath").getValue();
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance("wcm.seo");
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
  public void moveToTrash(Node node, SessionProvider sessionProvider) throws Exception {
    moveToTrash(node, sessionProvider, 0);
  }


  /**
   *{@inheritDoc}
   */
  @Override
  public void moveToTrash(Node node,
                          SessionProvider sessionProvider,
                          int deep) throws Exception {
    ((SessionImpl)node.getSession()).getActionHandler().preRemoveItem((ItemImpl)node);
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
    String originalPath = node.getPath();
    String nodeWorkspaceName = nodeSession.getWorkspace().getName();
    //List<Node> categories = taxonomyService_.getAllCategories(node, true);
    String nodeUUID = node.isNodeType(MIX_REFERENCEABLE) ? node.getUUID() : null;
    if (node.isNodeType(SYMLINK)) nodeUUID = null;
    String taxonomyLinkUUID = node.isNodeType(TAXONOMY_LINK) ? node.getProperty(UUID).getString() : null;
    String taxonomyLinkWS = node.isNodeType(TAXONOMY_LINK) ? node.getProperty(EXO_WORKSPACE).getString() : null;
    if(nodeUUID != null) {
      SEOService seoService = WCMCoreUtils.getService(SEOService.class);
      cache.remove(seoService.getHash(nodeUUID));
    }
    if (!node.isNodeType(EXO_RESTORE_LOCATION)) {
      addRestorePathInfo(node);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session trashSession = WCMCoreUtils.getSystemSessionProvider().getSession(this.trashWorkspace_, manageableRepository);
      String actualTrashPath = this.trashHome_ + (this.trashHome_.endsWith("/") ? "" : "/")
          + fixRestorePath(nodeName);
      if (trashSession.getWorkspace().getName().equals(
          nodeSession.getWorkspace().getName())) {
        try {
	        trashSession.getWorkspace().move(node.getPath(),
		        actualTrashPath);
        } catch(PathNotFoundException pne) {
	        if (LOG.isWarnEnabled()) {
		        LOG.warn("Cannot move node " + node.getPath() + " to Trash due to: " + pne.getMessage());
	        }
	        return;
        }
      } else {
        //clone node in trash folder
        trashSession.getWorkspace().clone(nodeWorkspaceName,
            node.getPath(), actualTrashPath, true);
        if (node.isNodeType(MIX_REFERENCEABLE)) {
            Node clonedNode = trashSession.getNodeByUUID(node.getUUID());
            //remove link from tag to node

            NewFolksonomyService newFolksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);

            String tagWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
            List<Node> tags = newFolksonomyService.getLinkedTagsOfDocument(node, tagWorkspace);
            for (Node tag : tags) {
              newFolksonomyService.removeTagOfDocument(tag.getPath(), node, tagWorkspace);
              linkManager.createLink(tag, clonedNode);
              long total = tag.hasProperty(EXO_TOTAL) ?
                  tag.getProperty(EXO_TOTAL).getLong() : 0;
                  tag.setProperty(EXO_TOTAL, total - 1);
                  tag.getSession().save();
            }
        }
        node.remove();
      }

      trashSession.save();
      
      //check and delete target node when there is no its symlink
      if (deep == 0 && taxonomyLinkUUID != null && taxonomyLinkWS != null) {
        Session targetNodeSession = sessionProvider.getSession(taxonomyLinkWS, manageableRepository);
        Node targetNode = null;
        try {
          targetNode = targetNodeSession.getNodeByUUID(taxonomyLinkUUID);
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        if (targetNode != null && isInTaxonomyTree(originalPath, targetNode)) {
          List<Node> symlinks = linkManager.getAllLinks(targetNode, SYMLINK, sessionProvider);
          boolean found = false;
          for (Node symlink : symlinks)
            if (!symlink.isNodeType(EXO_RESTORE_LOCATION)) {
              found = true;
              break;
            }
          if (!found) {
            this.moveToTrash(targetNode, sessionProvider);
          }
        }
      }
      trashSession.save();
    }
  }
 
  /* Store original path of deleted node.
  * 
  * @param node
  * @throws RepositoryException 
  * @throws LockException 
  * @throws ConstraintViolationException 
  * @throws VersionException 
  * @throws NoSuchNodeTypeException 
  */
  private void addRestorePathInfo(Node node) throws Exception {
    String originWorkspace = node.getSession().getWorkspace().getName();
    Session sysSession = WCMCoreUtils.getSystemSessionProvider().getSession(originWorkspace, WCMCoreUtils.getRepository());
    Node sysSessionNode = (Node)sysSession.getItem(node.getPath());
    sysSessionNode.addMixin(EXO_RESTORE_LOCATION);
    sysSessionNode.setProperty(RESTORE_PATH, fixRestorePath(node.getPath()));
    sysSessionNode.setProperty(RESTORE_WORKSPACE, originWorkspace);
    sysSession.save();
  }
  
  /**
   *
   * @param path
   * @param targetNode
   * @return
   */
  private boolean isInTaxonomyTree(String path, Node targetNode) {
    try {
      List<Node> taxonomyTrees = taxonomyService_.getAllTaxonomyTrees(true);
      for (Node tree : taxonomyTrees)
        if (path.contains(tree.getPath())) {
          Node taxonomyActionNode = tree.getNode("exo:actions/taxonomyAction");
          String targetWorkspace = taxonomyActionNode.getProperty(EXO_TARGETWS).getString();
          String targetPath = taxonomyActionNode.getProperty(EXO_TARGETPATH).getString();
          if (targetNode.getSession().getWorkspace().getName().equals(targetWorkspace)
              && targetNode.getPath().contains(targetPath))
            return true;
          break;
        }
      return false;
    } catch (Exception e) {
      return false;
    }
  }


  /**
   * {@inheritDoc}
   */
  public void restoreFromTrash(String trashNodePath,
                               SessionProvider sessionProvider) throws Exception {
    restoreFromTrash(trashNodePath, sessionProvider, 0);
  }

  private void restoreFromTrash(String trashNodePath,
      SessionProvider sessionProvider, int deep) throws Exception {

    Node trashHomeNode = this.getTrashHomeNode();
    Session trashNodeSession = trashHomeNode.getSession();
    Node trashNode = (Node)trashNodeSession.getItem(trashNodePath);
    String trashWorkspace = trashNodeSession.getWorkspace().getName();
    String restoreWorkspace = trashNode.getProperty(RESTORE_WORKSPACE).getString();
    String restorePath = trashNode.getProperty(RESTORE_PATH).getString();
    String nodeUUID = trashNode.isNodeType(MIX_REFERENCEABLE) ? trashNode.getUUID() : null;
    if (trashNode.isNodeType(SYMLINK)) nodeUUID = null;
    String taxonomyLinkUUID = trashNode.isNodeType(TAXONOMY_LINK) ? trashNode.getProperty(UUID).getString() : null;
    String taxonomyLinkWS = trashNode.isNodeType(TAXONOMY_LINK) ? trashNode.getProperty(EXO_WORKSPACE).getString() : null;

    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session restoreSession = sessionProvider.getSession(restoreWorkspace,  manageableRepository);

    if (restoreWorkspace.equals(trashWorkspace)) {
      trashNodeSession.getWorkspace().move(trashNodePath, restorePath);
    } else {
      //clone node
      restoreSession.getWorkspace().clone(
          trashWorkspace, trashNodePath, restorePath, true);
      if (trashNode.isNodeType(MIX_REFERENCEABLE)) {
        Node restoredNode = restoreSession.getNodeByUUID(trashNode.getUUID());

        //remove link from tag to node in trash
        NewFolksonomyService newFolksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);

        String tagWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
        List<Node> tags = newFolksonomyService.getLinkedTagsOfDocument(trashNode, tagWorkspace);
        for (Node tag : tags) {
          newFolksonomyService.removeTagOfDocument(tag.getPath(), trashNode, tagWorkspace);
          linkManager.createLink(tag, restoredNode);
          long total = tag.hasProperty(EXO_TOTAL) ?
              tag.getProperty(EXO_TOTAL).getLong() : 0;
              tag.setProperty(EXO_TOTAL, total + 1);
              tag.getSession().save();
        }
      }

      trashNodeSession.getItem(trashNodePath).remove();
    }

    removeMixinEXO_RESTORE_LOCATION(restoreSession, restorePath);

    trashNodeSession.save();
    restoreSession.save();

    //also restore categories of node
    if (deep == 0 && nodeUUID != null) {
      while (true) {
        boolean found = false;
        NodeIterator iter = trashHomeNode.getNodes();
        while (iter.hasNext()) {
          Node trashChild = iter.nextNode();
          if (trashChild.isNodeType(TAXONOMY_LINK) && trashChild.hasProperty(UUID)
              && trashChild.hasProperty(EXO_WORKSPACE)
              && nodeUUID.equals(trashChild.getProperty(UUID).getString())
              && restoreWorkspace.equals(trashChild.getProperty(EXO_WORKSPACE))) {
            try {
                restoreFromTrash(trashChild.getPath(), sessionProvider, deep + 1);
                found = true;
                break;
            } catch (Exception e) {
              if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
              }
            }
          }
        }
        if (!found) break;
      }
    }

    trashNodeSession.save();
    restoreSession.save();
    //restore target node of the restored categories.
    if (deep == 0 && taxonomyLinkUUID != null && taxonomyLinkWS != null) {
      while (true) {
        boolean found = false;
        NodeIterator iter = trashHomeNode.getNodes();
        while (iter.hasNext()) {
          Node trashChild = iter.nextNode();
          if (trashChild.isNodeType(MIX_REFERENCEABLE)
              && taxonomyLinkUUID.equals(trashChild.getUUID())
              && taxonomyLinkWS.equals(trashChild.getProperty(RESTORE_WORKSPACE).getString())) {
            try {
              restoreFromTrash(trashChild.getPath(),
                               sessionProvider,
                               deep + 1);
              found = true;
              break;
            } catch (Exception e) {
              if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
              }
            }
          }
        }
        if (!found) break;
      }
    }

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
