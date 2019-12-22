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
package org.exoplatform.services.cms.taxonomy.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@gmail.com Mar 31, 2009
 */
public class TaxonomyServiceImpl implements TaxonomyService, Startable {
  private SessionProviderService providerService_;

  private NodeHierarchyCreator   nodeHierarchyCreator_;

  private RepositoryService      repositoryService_;

  private static final String    TAXONOMY_LINK   = "exo:taxonomyLink";

  private static final String    EXOSYMLINK_LINK = "exo:symlink";

  private static final String    EXO_WORKSPACE   = "exo:workspace";

  private static final String    EXO_UUID        = "exo:uuid";

  private LinkManager            linkManager_;
  private ListenerService        listenerService;
  private ActivityCommonService  activityService;

  private final String           SQL_QUERY       = "Select * from exo:taxonomyLink where jcr:path like '$0/%' "
      + "and exo:uuid = '$1' "
      + "and exo:workspace = '$2' "
      + "order by exo:dateCreated DESC";

  private final String SQL_QUERY_EXACT_PATH = "Select * from exo:taxonomyLink where jcr:path like '$0/%' "
      + "and not jcr:path like '$0/%/%' "
      + "and exo:uuid = '$1' "
      + "and exo:workspace = '$2' "
      + "order by exo:dateCreated DESC";


  List<TaxonomyPlugin>           plugins_        = new ArrayList<>();

  private DMSConfiguration       dmsConfiguration_;

  private Map<String, String[]>  taxonomyTreeDefaultUserPermissions_;

  private static final Log       LOG             = ExoLogger.getLogger(TaxonomyServiceImpl.class.getName());
  private String categoryNameLength;

  /**
   * Constructor method
   * @param providerService         create session
   * @param nodeHierarchyCreator    get path by alias name
   * @param repoService             manage repository
   * @param linkManager             create and reach link
   * @param dmsConfiguration        get dms-system workspace
   * @throws Exception
   */
  public TaxonomyServiceImpl(InitParams initParams, SessionProviderService providerService,
                             LivePortalManagerService livePortalManagerService,
                             NodeHierarchyCreator nodeHierarchyCreator, RepositoryService repoService,
                             LinkManager linkManager, DMSConfiguration dmsConfiguration) throws Exception {
    providerService_ = providerService;
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositoryService_ = repoService;
    linkManager_ = linkManager;
    dmsConfiguration_ = dmsConfiguration;
    ValueParam valueParam = initParams.getValueParam("categoryNameLength");
    if(valueParam!=null) {
      categoryNameLength = valueParam.getValue();
    } else {
      categoryNameLength = "150";
    }
    ObjectParameter objectParam = initParams.getObjectParam("defaultPermission.configuration");
    if (objectParam != null)
      taxonomyTreeDefaultUserPermissions_
      = getPermissions(((TaxonomyTreeDefaultUserPermission)objectParam.getObject()).getPermissions());
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }

  public String getCategoryNameLength() {
    return categoryNameLength;
  }

  public void init() throws Exception {
    for (TaxonomyPlugin plugin : plugins_) {
      plugin.init();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addTaxonomyPlugin(ComponentPlugin plugin) {
    if (plugin instanceof TaxonomyPlugin) {
      plugins_.add((TaxonomyPlugin) plugin);
    }
  }

  public List<Node> getAllTaxonomyTrees() throws RepositoryException {
    return getAllTaxonomyTrees(false);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllTaxonomyTrees(boolean system)
      throws RepositoryException {
    List<Node> listNode = new ArrayList<>();
    try {
      Node taxonomyDef = getRootTaxonomyDef();
      NodeIterator nodeIter = taxonomyDef.getNodes();
      while (nodeIter.hasNext()) {
        Node node = (Node) nodeIter.next();
        if (node.isNodeType(EXOSYMLINK_LINK)) {
          try {
            Node target = linkManager_.getTarget(node, system);
            if (target != null)
              listNode.add(target);
          } catch (ItemNotFoundException ex) {
            continue;
          }
          catch (AccessDeniedException adex) {
            continue;
          }
        }
      }
    } catch (RepositoryConfigurationException e) {
      throw new RepositoryException(e);
    }
    return listNode;
  }

  /**
   * {@inheritDoc}
   */
  public Node getTaxonomyTree(String taxonomyName) throws RepositoryException {
    return getTaxonomyTree(taxonomyName, false);
  }

  /**
   * {@inheritDoc}
   */
  public Node getTaxonomyTree(String taxonomyName, boolean system)
      throws RepositoryException {
    try {
      Node taxonomyDef = getRootTaxonomyDef();
      try {
        Node taxonomyTree = taxonomyDef.getNode(taxonomyName);
        if (taxonomyTree.isNodeType(EXOSYMLINK_LINK))
          return linkManager_.getTarget(taxonomyTree, system);
      }catch (PathNotFoundException pne) {
        throw new RepositoryException(pne);
      }
    } catch (RepositoryConfigurationException e1) {
      throw new RepositoryException(e1);
    } catch (PathNotFoundException e2) {
      throw new RepositoryException(e2);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasTaxonomyTree(String taxonomyName) throws RepositoryException {
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      Node taxonomyTree = getRootTaxonomyDef(systemProvider).getNode(taxonomyName);
      return taxonomyTree.isNodeType(EXOSYMLINK_LINK);
    } catch (RepositoryConfigurationException e1) {
      throw new RepositoryException(e1);
    } catch (PathNotFoundException e2) {
      //ignore this exception
    } finally {
      systemProvider.close();
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void addTaxonomyTree(Node taxonomyTree) throws RepositoryException,
  TaxonomyAlreadyExistsException {
    if (hasTaxonomyTree(taxonomyTree.getName())) {
      throw new TaxonomyAlreadyExistsException();
    }
    SessionProvider systemProvider = SessionProvider.createSystemProvider();
    try {
      Node taxonomyDef = getRootTaxonomyDef(systemProvider);
      linkManager_.createLink(taxonomyDef, EXOSYMLINK_LINK, taxonomyTree, taxonomyTree.getName());
    } catch (RepositoryConfigurationException e) {
      throw new RepositoryException(e);
    } finally {
      systemProvider.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void updateTaxonomyTree(String taxonomyName, Node taxonomyTree) throws RepositoryException {
    try {
      if (hasTaxonomyTree(taxonomyName)) {
        Node taxonomyTreeLink = getRootTaxonomyDef().getNode(taxonomyName);
        linkManager_.updateLink(taxonomyTreeLink, taxonomyTree);
      }
    } catch (RepositoryConfigurationException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeTaxonomyTree(String taxonomyName) throws RepositoryException {
    Session session = null;
    try {
      if (hasTaxonomyTree(taxonomyName)) {
        Node targetNode = getTaxonomyTree(taxonomyName, true);
        session = targetNode.getSession();
        targetNode.remove();
        session.save();
        Node taxonomyDef = getRootTaxonomyDef();
        if (taxonomyDef.hasNode(taxonomyName)) {
          Node taxonomyTree = taxonomyDef.getNode(taxonomyName);
          taxonomyTree.remove();
          taxonomyDef.getSession().save();
        }
      }
    } catch (RepositoryConfigurationException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addTaxonomyNode(String workspace, String parentPath, String taxoNodeName,
                              String creatorUser) throws RepositoryException, TaxonomyNodeAlreadyExistsException {
    Session systemSession = null;
    try {
      ManageableRepository manaRepo = repositoryService_.getCurrentRepository();
      systemSession = getSession(manaRepo, workspace, true);
      Node parentNode = (Node) systemSession.getItem(parentPath);
      if (parentNode.hasNode(taxoNodeName))
        throw new TaxonomyNodeAlreadyExistsException();
      ExtendedNode node = (ExtendedNode) parentNode.addNode(taxoNodeName, "exo:taxonomy");
      if (node.canAddMixin("exo:privilegeable")) {
        if(node.hasProperty("exo:owner")) {
          String owner = node.getProperty("exo:owner").getString();
          node.addMixin("exo:privilegeable");
          node.setPermission(owner, PermissionType.ALL);
          if (creatorUser != null)
            node.setPermission(creatorUser, PermissionType.ALL);
          for(Map.Entry<String, String[]> entry : taxonomyTreeDefaultUserPermissions_.entrySet()) {
            node.setPermission(entry.getKey(), entry.getValue());
          }
        }
        if (!node.isNodeType("exo:privilegeable"))
          node.addMixin("exo:privilegeable");
        String systemUser = IdentityConstants.SYSTEM;
        if (!containsUser(node.getACL().getPermissionEntries(), systemUser))
          node.setPermission(systemUser, PermissionType.ALL);
      }
      systemSession.save();
    } catch (PathNotFoundException e2) {
      throw new RepositoryException(e2);
    }
  }

  private boolean containsUser(List<AccessControlEntry> entries, String userName) {
    if (userName == null) return false;
    for (AccessControlEntry entry : entries)
      if (userName.equals(entry.getIdentity()))
        return true;
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void removeTaxonomyNode(String workspace, String absPath) throws RepositoryException {
    Session systemSession = null;
    try {
      ManageableRepository manaRepo = repositoryService_.getCurrentRepository();
      systemSession = getSession(manaRepo, workspace, true);
      Node taxonomyNode = (Node) systemSession.getItem(absPath);
      taxonomyNode.remove();
      systemSession.save();
    } catch (PathNotFoundException e2) {
      throw new RepositoryException(e2);
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getCategories(Node node, String taxonomyName) throws RepositoryException {
    return getCategories(node, taxonomyName, false);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    List<Node> listCate = new ArrayList<>();
    Session session = null;
    try {
      if (node.isNodeType("mix:referenceable")) {
        Node rootNodeTaxonomy = getTaxonomyTree(taxonomyName, system);
        if (rootNodeTaxonomy != null) {
          String sql = null;
          sql = StringUtils.replace(SQL_QUERY, "$0", rootNodeTaxonomy.getPath());
          sql = StringUtils.replace(sql, "$1", node.getUUID());
          sql = StringUtils.replace(sql, "$2", node.getSession().getWorkspace().getName());
          session =
              repositoryService_.getCurrentRepository().login(rootNodeTaxonomy.getSession().getWorkspace().getName());
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          Query query = queryManager.createQuery(sql, Query.SQL);
          QueryResult result = query.execute();
          NodeIterator iterate = result.getNodes();
          Set<String> addedNode = new HashSet<>();
          while (iterate.hasNext()) {
            Node parentCate = iterate.nextNode().getParent();
            // We need filtering duplicated result to fix the problem of ECMS-3282.
            if (!addedNode.contains(parentCate.getSession().getWorkspace().getName() + ":/" + parentCate.getPath())) {
              listCate.add(parentCate);
              addedNode.add(parentCate.getSession().getWorkspace().getName() + ":/" + parentCate.getPath());
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(e);
    } finally {
      if(session != null) session.logout();
    }
    return listCate;
  }
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllCategories(Node node) throws RepositoryException {
    return getAllCategories(node, false);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllCategories(Node node, boolean system) throws RepositoryException {
    List<Node> listCategories = new ArrayList<>();
    List<Node> allTrees = getAllTaxonomyTrees(system);
    for (Node tree : allTrees) {
      List<Node> categories = getCategories(node, tree.getName(), system);
      for (Node category : categories) listCategories.add(category);
    }
    return listCategories;
  }

  /**
   * {@inheritDoc}
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException {
    addCategories(node, taxonomyName, new String[] { categoryPath });
  }

  /**
   * {@inheritDoc}
   */
  public void addCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException {
    addCategories(node, taxonomyName, new String[] { categoryPath }, system);
  }

  /**
   * {@inheritDoc}
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths)
      throws RepositoryException {
    addCategories(node, taxonomyName, categoryPaths, false);
  }

  /**
   * {@inheritDoc}
   */
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths, boolean system)
      throws RepositoryException {
    if (listenerService ==null) {
      listenerService = WCMCoreUtils.getService(ListenerService.class);
    }
    String category = "";
    try {
      Node rootNodeTaxonomy = getTaxonomyTree(taxonomyName, system);
      for (String categoryPath : categoryPaths) {
        //get category path
        if (rootNodeTaxonomy.getPath().equals("/")) {
          category = categoryPath;
        } else if (categoryPath.length() != 0) {
          if (!categoryPath.startsWith("/"))
            category = rootNodeTaxonomy.getPath() + "/" + categoryPath;
          else
            category = rootNodeTaxonomy.getPath() + categoryPath;
        } else {
          category = rootNodeTaxonomy.getPath();
        }
        //get category node
        Node categoryNode;
        if (categoryPath.startsWith(rootNodeTaxonomy.getPath())) {
          categoryNode = (Node) rootNodeTaxonomy.getSession().getItem(categoryPath);
        } else if (categoryPath.equals("")) {
          categoryNode = rootNodeTaxonomy;
        } else {
          categoryNode = (Node) rootNodeTaxonomy.getSession().getItem(category);
        }
        String categoryName = categoryNode.getName();
        if (categoryNode.hasProperty("exo:title")) {
          categoryName = categoryNode.getProperty("exo:title").getString();
        }
        //add mix referenceable for node
        if (node.canAddMixin("mix:referenceable")) {
          node.addMixin("mix:referenceable");
          node.getSession().save();
        }
        //generate unique linkName
        String nodeUUID = node.getUUID();
        String nodeWS = node.getSession().getWorkspace().getName();
        String linkName = node.getName();
        int index = 1;
        while (categoryNode.hasNode(linkName)) {
          Node taxonomyNode = categoryNode.getNode(linkName);
          if (nodeUUID.equals(taxonomyNode.getProperty(EXO_UUID).getString()) &&
              nodeWS.equals(taxonomyNode.getProperty(EXO_WORKSPACE).getString())) {
            throw new ItemExistsException();
          }
          linkName = node.getName() + index++;
        }

        //create link
        linkManager_.createLink(categoryNode, TAXONOMY_LINK, node, linkName);
        if (listenerService!=null) {
          try {
            if (activityService.isAcceptedNode(node) || (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) &&
                activityService.isBroadcastNTFileEvents(node))) {
              listenerService.broadcast(ActivityCommonService.CATEGORY_ADDED_ACTIVITY, node, categoryName);
            }
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("Can not notify CategoryAddedActivity because of: " + e.getMessage());
            }
          }
        }
      }
    } catch (PathNotFoundException e) {
      throw new RepositoryException(e);
    }

  }

  /**
   * {@inheritDoc}
   */
  public boolean hasCategories(Node node, String taxonomyName) throws RepositoryException {
    return hasCategories(node, taxonomyName, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    List<Node> listCate = getCategories(node, taxonomyName, system);
    if (listCate != null && listCate.size() > 0)
      return true;
    return false;
  }

  public void moveTaxonomyNode(String workspace, String srcPath, String destPath, String type) throws RepositoryException {
    Session systemSession = null;
    try {
      ManageableRepository manaRepo = repositoryService_.getCurrentRepository();
      systemSession = getSession(manaRepo, workspace, true);
      if ("cut".equals(type)) {
        systemSession.move(srcPath, destPath);
        systemSession.save();
      } else if ("copy".equals(type)) {
        Workspace wspace = systemSession.getWorkspace();
        wspace.copy(srcPath, destPath);
        systemSession.save();
      } else
        throw new UnsupportedRepositoryOperationException();
    } finally {
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath)
      throws RepositoryException {
    removeCategory(node, taxonomyName, categoryPath, false);
  }

  /**
   * {@inheritDoc}
   */
  public void removeCategory(Node node, String taxonomyName, String categoryPath, boolean system)
      throws RepositoryException {
    try {
      //get category node
      String category = "";
      Node rootNodeTaxonomy = getTaxonomyTree(taxonomyName, system);
      if (rootNodeTaxonomy.getPath().equals("/")) {
        category = categoryPath;
      } else if (!categoryPath.startsWith("/")) {
        category = rootNodeTaxonomy.getPath() + "/" + categoryPath;
      } else {
        category = rootNodeTaxonomy.getPath() + categoryPath;
      }
      Node categoryNode = ((Node) rootNodeTaxonomy.getSession().getItem(category));
      String categoryName = categoryNode.getName();
      //get taxonomyLink node
      String sql = StringUtils.replace(SQL_QUERY_EXACT_PATH, "$0", categoryNode.getPath());
      sql = StringUtils.replace(sql, "$1", node.getUUID());
      sql = StringUtils.replace(sql, "$2", node.getSession().getWorkspace().getName());

      QueryManager queryManager = categoryNode.getSession().getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(sql, Query.SQL);
      QueryResult result = query.execute();
      NodeIterator iterate = result.getNodes();

      Node nodeTaxonomyLink = null;
      if (iterate != null && iterate.hasNext()) {
        nodeTaxonomyLink = iterate.nextNode();
      }

      //remove taxonomyLink node
      if (nodeTaxonomyLink == null) {
        throw new RepositoryException("canot found taxonomy link node");
      }
      nodeTaxonomyLink.remove();
      categoryNode.save();
      node.getSession().save();
      if (listenerService!=null) {
        try {
          if (activityService.isAcceptedNode(node) || (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) &&
              activityService.isBroadcastNTFileEvents(node))) {
            listenerService.broadcast(ActivityCommonService.CATEGORY_REMOVED_ACTIVITY, node, categoryName);
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not notify Activity because of: " + e.getMessage());
          }
        }
      }
    } catch (PathNotFoundException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Map<String, String[]> getTaxonomyTreeDefaultUserPermission() {
    return taxonomyTreeDefaultUserPermissions_;
  }

  public Map<String, String[]> getPermissions(List<TaxonomyTreeDefaultUserPermission.Permission> permissions) {
    Map<String, String[]> permissionsMap = new HashMap<>();
    for (TaxonomyTreeDefaultUserPermission.Permission permission : permissions) {
      StringBuilder strPer = new StringBuilder();
      if ("true".equals(permission.getRead()))
        strPer.append(PermissionType.READ);
      if ("true".equals(permission.getAddNode()))
        strPer.append(",").append(PermissionType.ADD_NODE);
      if ("true".equals(permission.getSetProperty()))
        strPer.append(",").append(PermissionType.SET_PROPERTY);
      if ("true".equals(permission.getRemove()))
        strPer.append(",").append(PermissionType.REMOVE);
      permissionsMap.put(permission.getIdentity(), strPer.toString().split(","));
    }
    return permissionsMap;
  }

  /**
   * Get node as root of all taxonomy in the repository that is in TAXONOMIES_TREE_DEFINITION_PATH
   * @param systemProvider System Provider
   * @return
   * @throws RepositoryException
   * @throws RepositoryConfigurationException
   */
  private Node getRootTaxonomyDef(SessionProvider systemProvider) throws RepositoryException,
      RepositoryConfigurationException {
    ManageableRepository manaRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    Session systemSession = systemProvider.getSession(dmsRepoConfig.getSystemWorkspace(), manaRepository);
    String taxonomiesTreeDef = nodeHierarchyCreator_
        .getJcrPath(BasePath.TAXONOMIES_TREE_DEFINITION_PATH);
    Node taxonomyRootDef = (Node) systemSession.getItem(taxonomiesTreeDef);
    return taxonomyRootDef;
  }
  
  /**
   * Get node as root of all taxonomy in the repository that is in TAXONOMIES_TREE_DEFINITION_PATH
   * @return
   * @throws RepositoryException
   * @throws RepositoryConfigurationException
   */
  private Node getRootTaxonomyDef() throws RepositoryException,
  RepositoryConfigurationException {
    ManageableRepository manaRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    Session systemSession = getSession(manaRepository, dmsRepoConfig.getSystemWorkspace(), true);
    String taxonomiesTreeDef = nodeHierarchyCreator_
        .getJcrPath(BasePath.TAXONOMIES_TREE_DEFINITION_PATH);
    Node taxonomyRootDef = (Node) systemSession.getItem(taxonomiesTreeDef);
    return taxonomyRootDef;
  }

  /**
   * Get session by workspace and ManageableRepository
   * @param manageRepository
   * @param workspaceName
   * @param system
   * @return          System session if system = true, else return session of current user
   * @throws RepositoryException
   */
  private Session getSession(ManageableRepository manageRepository, String workspaceName,
                             boolean system) throws RepositoryException {
    if (system)
      return providerService_.getSystemSessionProvider(null).getSession(workspaceName,
                                                                        manageRepository);
    return providerService_.getSessionProvider(null).getSession(workspaceName, manageRepository);
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    try {
      for (TaxonomyPlugin plugin : plugins_) {
        plugin.init() ;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }
}
