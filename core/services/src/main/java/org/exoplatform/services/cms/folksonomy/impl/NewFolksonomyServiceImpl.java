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
package org.exoplatform.services.cms.folksonomy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 16, 2009
 * 10:30:24 AM
 */
public class NewFolksonomyServiceImpl implements NewFolksonomyService, Startable {

  private static final String       USER_FOLKSONOMY_ALIAS  = "userPrivateFolksonomy";

  private static final String       GROUPS_ALIAS           = "groupsPath";

  private static final String       TAG_STYLE_ALIAS        = "exoTagStylePath";

  private static final String       PUBLIC_TAG_NODE_PATH   = "exoPublicTagNode";

  private static final String       EXO_TRASH_FOLDER       = "exo:trashFolder";

  private static final String       EXO_HIDDENABLE         = "exo:hiddenable";

  private static final String       TAG_PERMISSION_LIST    = "tagPermissionList";

  private static final Log          LOG                    = ExoLogger.getLogger(NewFolksonomyServiceImpl.class.getName());

  private NodeHierarchyCreator      nodeHierarchyCreator;

  private LinkManager               linkManager;

  private InitParams                initParams_;

  private List<TagStylePlugin>      plugin_                = new ArrayList<TagStylePlugin>();

  private List<TagPermissionPlugin> tagPermissionPlugin_   = new ArrayList<TagPermissionPlugin>();

  private ExoCache<String, List<String>> tagPermissionList;

  private Map<String, String>       sitesTagPath           = new HashMap<String, String>();

  private ListenerService           listenerService;

  private ActivityCommonService     activityService;

  //The DataDistributionType used to store tagNodes
  private DataDistributionType dataDistributionType;

  public NewFolksonomyServiceImpl(InitParams initParams,
                                  NodeHierarchyCreator nodeHierarchyCreator,
                                  LinkManager linkManager,
                                  DataDistributionManager dataDistributionManager,
                                  SessionProviderService sessionProviderService,
                                  CacheService cacheService) throws Exception {
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.linkManager = linkManager;
    this.initParams_ = initParams;
    listenerService = WCMCoreUtils.getService(ListenerService.class);
    this.activityService = WCMCoreUtils.getService(ActivityCommonService.class);
    this.tagPermissionList = cacheService.getCacheInstance(NewFolksonomyServiceImpl.class.getName());
    //get the DataDistributionType object;
    if (initParams != null && initParams.getValueParam("tagDistributionMode") != null) {
      String strTagDistributionMode = initParams.getValueParam("tagDistributionMode").getValue();
      if ("none".equals(strTagDistributionMode)) {
        dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
      } else if ("readable".equals(strTagDistributionMode)) {
        dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.READABLE);
      } else if ("optimized".equals(strTagDistributionMode)) {
        dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.OPTIMIZED);
      }
    } else {
      dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.READABLE);
    }    
  }

  /**
   * Implement method in Startable Call init() method
   *
   * @see #init()
   */
  public void start() {
    try {
      init();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("===>>>>Exception when init FolksonomySerice", e);
      }
    }
  }

  /**
   * Implement method in Startable
   */
  public void stop() {
    tagPermissionList.clearCache();
  }



  /**
   * {@inheritDoc}
   */
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String workspace,
                            String userName) throws Exception {
    Node userFolksonomyNode = getUserFolksonomyFolder(userName);
    userFolksonomyNode.getSession().save();
    Node targetNode = getTargetNode(documentNode);
    boolean firstTagFlag = true;
    StringBuffer tagValue = new StringBuffer();
    for (String tag : tagsName) {
      try {
        // Find tag node
        Node tagNode = getTagNode(userFolksonomyNode, tag);
        // Add symlink and total
        addTag(tagNode, targetNode);
        userFolksonomyNode.getSession().save();
        if (firstTagFlag) {
          firstTagFlag = false;
          tagValue.append(tag);
        }else {
          tagValue.append(",").append(tag);
        }

      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can't add tag '" + tag + "' to node: " + targetNode.getPath() + " for user: "
              + userName);
        }
      }
    }//
    broadcastActivityTag(documentNode, tagValue.toString());
  }
  private void broadcastActivityTag(Node documentNode, String tagValue ) {
    if (listenerService!=null && activityService !=null) {
      try {
        if (activityService.isAcceptedNode(documentNode) || 
            documentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
          listenerService.broadcast(ActivityCommonService.TAG_ADDED_ACTIVITY, documentNode, tagValue);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not notify Tag Added Activity because of: " + e.getMessage());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String workspace,
                           String[] roles) throws Exception {
    Node targetNode = getTargetNode(documentNode);
    for (String group : roles) {
      Node groupFolksonomyNode = getGroupFolksonomyFolder(group, workspace);
      for (String tag : tagsName) {
        try {
          // Find tag node
          Node tagNode = getTagNode(groupFolksonomyNode, tag);

          // Add symlink and total
          addTag(tagNode, targetNode);

          groupFolksonomyNode.getSession().save();
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("can't add tag '" + tag + "' to node: " + targetNode.getPath() + " for group: "
                + group);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String workspace) throws Exception {
    Node publicFolksonomyTreeNode = getNode(workspace, treePath);
    Node targetNode = getTargetNode(documentNode);
    boolean firstTagFlag = true;
    StringBuffer tagValue = new StringBuffer();

    for (String tag : tagsName) {
      try {
        // Find tag node
        Node tagNode = getTagNode(publicFolksonomyTreeNode, tag);
        // Add symlink and total
        addTag(tagNode, targetNode);
        publicFolksonomyTreeNode.getSession().save();
        if (firstTagFlag) {
          firstTagFlag = false;
          tagValue.append(tag);
        }else {
          tagValue.append(",").append(tag);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can't add tag '" + tag + "' to node: " + targetNode.getPath()
                    + " in public folksonomy tree!");
        }
      }
    }//off for
    broadcastActivityTag(documentNode, tagValue.toString());
  }

  private Node getTargetNode(Node showingNode) throws Exception {
    Node targetNode = null;
    if (linkManager.isLink(showingNode)) {
      try {
        targetNode = linkManager.getTarget(showingNode);
      } catch (ItemNotFoundException e) {
        targetNode = showingNode;
      }
    } else {
      targetNode = showingNode;
    }
    return targetNode;
  }

  /**
   * {@inheritDoc}
   */
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String workspace) throws Exception {
    if (sitesTagPath.get(getRepoName()) == null) {
      createSiteTagPath();
    }
    addPublicTag(sitesTagPath.get(getRepoName()) + "/" + siteName,
                 tagsName,
                 node,
                 workspace);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getAllDocumentsByTagsAndPath(String selectedPath,
                                                  Set<String> tagPaths,
                                                  String workspace,
                                                 SessionProvider sessionProvider) throws Exception {
    if (StringUtils.isBlank(selectedPath)) {
      throw new IllegalArgumentException("Parent path is empty");
    }
    if (tagPaths == null || tagPaths.isEmpty()) {
      throw new IllegalArgumentException("Tags is empty");
    }
    // The Parent Node must ends with '/' and the original selected node path
    // without '/'
    String selectedParentPath = selectedPath;
    if (selectedPath.endsWith("/")) {
      selectedPath = selectedPath.substring(0, selectedPath.length() - 1);
    } else {
      selectedParentPath += "/";
    }

    List<Node> ret = new ArrayList<Node>();
    Set<String> nodesPaths = new HashSet<>();
    boolean firstSearch = true;
    Iterator<String> tagPathsIterator = tagPaths.iterator();
    while (tagPathsIterator.hasNext()) {
      String tagPath = (String) tagPathsIterator.next();
      Node tagNode = getNode(workspace, tagPath, sessionProvider);
      if (tagNode == null) {
        tagPathsIterator.remove();
        continue;
      }
      NodeIterator nodeIter = tagNode.getNodes();

      Set<String> singleTagNodesPaths = new HashSet<>();
      while (nodeIter.hasNext()) {
        Node node = nodeIter.nextNode();
        if (linkManager.isLink(node)) {
          Node targetNode = null;
          try {
            targetNode = linkManager.getTarget(node);
          } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(e.getMessage());
            }
          }
          if (targetNode != null && !((Node) targetNode.getAncestor(1)).isNodeType(EXO_TRASH_FOLDER)
              && targetNode.getSession().getWorkspace().getName().equals(workspace)
              && (targetNode.getPath().equals(selectedPath) || targetNode.getPath().startsWith(selectedParentPath))) {
            if (firstSearch) {
              ret.add(targetNode);
            }
            singleTagNodesPaths.add(targetNode.getPath());
          }
        }
      }
      if (firstSearch) {
        nodesPaths.addAll(singleTagNodesPaths);
      } else {
        nodesPaths.retainAll(singleTagNodesPaths);
      }
      firstSearch = false;
    }
    Iterator<Node> nodesIterator = ret.iterator();
    while (nodesIterator.hasNext()) {
      Node node = nodesIterator.next();
      if (!nodesPaths.contains(node.getPath())) {
        nodesIterator.remove();
      }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    Node tagNode = getNode(workspace, tagPath, sessionProvider);
    if (tagNode == null) {
      return Collections.emptyList();
    }
    NodeIterator nodeIter = tagNode.getNodes();

    while (nodeIter.hasNext()) {
      Node node = nodeIter.nextNode();
      if (linkManager.isLink(node)) {
        Node targetNode = null;
        try {
          targetNode = linkManager.getTarget(node);
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        if (targetNode != null && !((Node) targetNode.getAncestor(1)).isNodeType(EXO_TRASH_FOLDER)) {
          ret.add(targetNode);
        }
      }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllGroupTags(String[] roles, String workspace) throws Exception {
    Set<Node> tagSet = new TreeSet<Node>(new NodeComparator());
    for (String group : roles) {
      Node groupFolksonomyNode = getGroupFolksonomyFolder(group, workspace);
      List<Node> tagNodes = queryTagNodes(groupFolksonomyNode);
      for(Node tag : tagNodes) {
        if (!((Node) tag.getAncestor(1)).isNodeType(EXO_TRASH_FOLDER)) {
          tagSet.add(tag);
        }
      }
    }
    return new ArrayList<Node>(tagSet);
  }

  private List<Node> queryTagNodes(Node rootNode) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    StringBuilder queryStr = new StringBuilder().append("select * from ").append(EXO_TAGGED).append(" where jcr:path like '").
        append(rootNode.getPath()).append("/%'");
    Query query = rootNode.getSession().getWorkspace().getQueryManager().createQuery(queryStr.toString(), Query.SQL); 
    for (NodeIterator iter = query.execute().getNodes(); iter.hasNext();) {
      ret.add(iter.nextNode());
    }
    Collections.sort(ret, new NodeComparator());
    return ret;
  }  

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllGroupTags(String role, String workspace) throws Exception {
    List<Node> tagSet = new ArrayList<Node>();
    Node groupFolksonomyNode = getGroupFolksonomyFolder(role, workspace);
    for (Node tagNode : queryTagNodes(groupFolksonomyNode)) {
      tagSet.add(tagNode);
    }
    return tagSet;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllPrivateTags(String userName) throws Exception {
    Node userFolksonomyNode = getUserFolksonomyFolder(userName);
    return queryTagNodes(userFolksonomyNode);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllPublicTags(String treePath, String workspace) throws Exception {
    Node publicFolksonomyTreeNode = getNode(workspace, treePath);
    return queryTagNodes(publicFolksonomyTreeNode);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllSiteTags(String siteName, String workspace) throws Exception {
    if (sitesTagPath.get(getRepoName()) == null) {
      createSiteTagPath();
    }
    return getAllPublicTags(sitesTagPath.get(getRepoName()) + "/" + siteName, workspace);
  }

  private String getRepoName() {
    try {
      String repositoryName = WCMCoreUtils.getRepository().getConfiguration().getName();
      if (LOG.isDebugEnabled()) {
        LOG.debug("The repository name is: " + repositoryName);
      }
      return repositoryName;
    } catch (NullPointerException e) {
      String repositoryName = System.getProperty("gatein.tenant.repository.name");
      if (repositoryName != null) {
        return repositoryName;
      }
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository exception occurs:", e);
      }
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllTagStyle(String workspace) throws Exception {
    String tagStylesPath = nodeHierarchyCreator.getJcrPath(TAG_STYLE_ALIAS);
    Node tagStylesNode = getNode(workspace, tagStylesPath);
    return getChildNodes(tagStylesNode);
  }

  /**
   * {@inheritDoc}
   */
  public String getTagStyle(String tagStylePath, String workspace) throws Exception {
    Node tagStyleNode = getNode(workspace, tagStylePath);
    return tagStyleNode.getProperty(HTML_STYLE_PROP).getString();
  }

  /**
   * Add new TagStylePlugin in plugin_
   *
   * @param plugin
   */
  public void addTagStylePlugin(ComponentPlugin plugin) {
    if (plugin instanceof TagStylePlugin) {
      plugin_.add((TagStylePlugin) plugin);
    }
  }

  /**
   * init all available TagStylePlugin
   *
   * @throws Exception
   */
  public void init() throws Exception {
    for (TagStylePlugin plugin : plugin_) {
      try {
        plugin.init();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can not init tag style: ", e);
        }
      }
    }
    initTagPermissionListCache();
  }

  /**
   * init the cache tagPermissionList
   * @throws Exception
   */
  public void initTagPermissionListCache() throws Exception {
    List<String> _tagPermissionList = new ArrayList<String>();
    for (TagPermissionPlugin plugin : tagPermissionPlugin_) {
      try {
        _tagPermissionList.addAll(plugin.initPermission());
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can not init tag permission: ", e);
        }
      }
    }
    tagPermissionList.clearCache();
    tagPermissionList.put(TAG_PERMISSION_LIST, _tagPermissionList);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Node modifyTagName(String tagPath, String newTagName, String workspace) throws Exception {
    Node oldTagNode = getNode(workspace, tagPath);
    if (oldTagNode.getParent().hasNode(newTagName))
      throw new ItemExistsException("node " + newTagName + " has already existed!");

    StringBuilder newPath = new StringBuilder(oldTagNode.getParent().getPath()).append('/')
        .append(newTagName);

    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    session.move(tagPath, newPath.toString());
    session.save();
    return getNode(workspace, newPath.toString());
  }

  /**
   * {@inheritDoc}
   */
  public Node modifyPublicTagName(String tagPath, String newTagName, String workspace, String treePath) throws Exception {
    Node oldTagNode = getNode(workspace, tagPath);
    Node publicFolksonomyTreeNode = getNode(workspace, treePath);
    Node newTagNode = null;
    try {
      newTagNode = dataDistributionType.getDataNode(publicFolksonomyTreeNode, newTagName);
      throw new ItemExistsException("node " + newTagName + " has already existed!");
    } catch (PathNotFoundException e) {
      //Path not found means newTagName does not exists, it is expected behavior.
      newTagNode = dataDistributionType.getOrCreateDataNode(publicFolksonomyTreeNode, newTagName);
    }
    newTagNode.addMixin(EXO_TAGGED);
    if (oldTagNode.hasProperty(EXO_TOTAL)) {
      newTagNode.setProperty(EXO_TOTAL, oldTagNode.getProperty(EXO_TOTAL).getValue());
    }

    Map<String, String> pathMap = new HashMap<String, String>();
    for (NodeIterator iter = oldTagNode.getNodes(); iter.hasNext();) {
      Node node = iter.nextNode();
      pathMap.put(node.getPath(), newTagNode.getPath() + "/" + node.getName());
    }

    Session session = newTagNode.getSession();
    for (Entry<String, String> entry : pathMap.entrySet()) {
      session.move(entry.getKey(), entry.getValue());
    }
    oldTagNode.remove();
    session.save();
    return newTagNode;
  }      


  /**
   * {@inheritDoc}
   */
  public void removeTag(String tagPath, String workspace) throws Exception {
    Node tagNode = getNode(workspace, tagPath);
    NodeIterator nodeIterator = tagNode.getNodes();
    Exception e = null;
    while (nodeIterator.hasNext()) {
      try {
        Node document = linkManager.getTarget(nodeIterator.nextNode());
        removeTagOfDocument(tagPath, document, workspace);
      }catch(Exception exception){
        if(e!=null) {
          e.addSuppressed(exception);
        }else{
          e = exception;
        }
      }
    }
    if(e!=null) throw e;
  }

  /**
   * {@inheritDoc}
   */
  public void removeTagOfDocument(String tagPath, Node document, String workspace) throws Exception {
    Node tagNode = getNode(workspace, tagPath);
    NodeIterator nodeIter = tagNode.getNodes();
    StringBuffer removedTags = new StringBuffer();
    String tagName ;
    boolean isFirstFlag =  true;
    while (nodeIter.hasNext()) {
      Node link = nodeIter.nextNode();
      if (linkManager.isLink(link)) {
        Node targetNode = null;
        try {
          targetNode = linkManager.getTarget(link);
        } catch (RepositoryException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        if (document.isSame(targetNode)) {
          tagName = tagNode.getName();
          if (isFirstFlag) {
            removedTags.append(tagName);
            isFirstFlag = false;
          }else {
            removedTags.append(ActivityCommonService.VALUE_SEPERATOR).append(tagName);
          }
          link.remove();

          long total = tagNode.getProperty(EXO_TOTAL).getLong();
          tagNode.setProperty(EXO_TOTAL, total - 1);
          Node parentNode = tagNode.getParent();
          if (tagNode.getProperty(EXO_TOTAL).getLong() == 0L)
            tagNode.remove();
          parentNode.getSession().save();
          break;
        }
      }
    }
    if (listenerService!=null && activityService!=null) {
      try {
        if (activityService.isAcceptedNode(document) || (document.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)
            && activityService.isBroadcastNTFileEvents(document))) {
          listenerService.broadcast(ActivityCommonService.TAG_REMOVED_ACTIVITY, document, removedTags.toString());
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not notify RemoveTag Activity because of: " + e.getMessage());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void updateTagStyle(String styleName, String tagRange, String htmlStyle, String workspace) throws Exception {
    String tagStylesPath = nodeHierarchyCreator.getJcrPath(TAG_STYLE_ALIAS);
    Node tagStylesNode = getNode(workspace, tagStylesPath);
    Node styleNode = tagStylesNode.getNode(styleName);
    styleNode.setProperty(TAG_RATE_PROP, tagRange);
    styleNode.setProperty(HTML_STYLE_PROP, htmlStyle);
    tagStylesNode.getSession().save();
  }

  /**
   * {@inheritDoc}
   */
  public void addTagStyle(String styleName, String tagRange, String htmlStyle, String workspace) throws Exception {
    String tagStylesPath = nodeHierarchyCreator.getJcrPath(TAG_STYLE_ALIAS);
    Node tagStylesNode = getNode(workspace, tagStylesPath);
    Node styleNode = tagStylesNode.addNode(styleName, EXO_TAGSTYLE);
    styleNode.addMixin("exo:privilegeable");
    styleNode.setProperty(TAG_RATE_PROP, tagRange);
    styleNode.setProperty(HTML_STYLE_PROP, htmlStyle);
    tagStylesNode.getSession().save();
  }

  /**
   * {@inheritDoc}
   */
  public void removeTagsOfNodeRecursively(Node node,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception {
    int[] scopes = new int[] { PRIVATE, PUBLIC, GROUP, SITE };
    Map<Integer, String> map = new HashMap<Integer, String>();
    map.put(PUBLIC, "");
    map.put(PRIVATE, username);
    map.put(GROUP, groups);
    map.put(SITE, "");
    for (int scope : scopes) {
      for (Node child : getAllNodes(node)) {
        List<Node> tags = getLinkedTagsOfDocumentByScope(scope, map.get(scope), child, workspace);
        for (Node tag : tags)
          removeTagOfDocument(tag.getPath(), child, workspace);
      }
    }
  }

  private List<Node> getAllNodes(Node node) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    getAllNodes(node, ret);
    return ret;
  }

  private void getAllNodes(Node node, List<Node> list) throws Exception {
    list.add(node);
    for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
      getAllNodes(iter.nextNode(), list);
    }
  }

  private List<Node> getChildNodes(Node node) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    NodeIterator nodeIter = node.getNodes();
    while (nodeIter.hasNext()) {
      ret.add(nodeIter.nextNode());
    } 
    Collections.sort(ret, new NodeComparator());
    return ret;
  }

  private Node getGroupFolksonomyFolder(String group, String workspace) throws Exception {
    String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_ALIAS);
    String folksonomyPath = "ApplicationData/Tags";
    Node groupsNode = getNode(workspace, groupsPath);
    return groupsNode.getNode(group.substring(1)).getNode(folksonomyPath);
  }

  private Node getUserFolksonomyFolder(String userName) throws Exception {
    // code for running
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userName);
    String folksonomyPath = nodeHierarchyCreator.getJcrPath(USER_FOLKSONOMY_ALIAS);
    return userNode.getNode(folksonomyPath);
  }

  private Node getNode(String workspace, String path) throws Exception {
    return getNode(workspace, path, WCMCoreUtils.getSystemSessionProvider());
  }

  private Node getNode(String workspace, String path, SessionProvider sessionProvider) throws Exception {
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);

    return session.itemExists(path) ? ((Node) session.getItem(path)) : null;
  }

  private boolean existSymlink(Node parentNode, Node targetNode) throws Exception {
    NodeIterator nodeIter = parentNode.getNodes();
    while (nodeIter.hasNext()) {
      Node link = nodeIter.nextNode();
      Node pointTo = null;
      try {
        if (linkManager.isLink(link))
          pointTo = linkManager.getTarget(link, true);
      } catch (ItemNotFoundException e) {
        continue;// target of symlink does not exist -> no exist symlink
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      if (targetNode != null && targetNode.isSame(pointTo))
        return true;
    }
    return false;
  }

  private static class NodeComparator implements Comparator<Node> {
    public int compare(Node o1, Node o2) {
      try {
        if (o1.isSame(o2))
          return 0;
        int pathComparison = o1.getPath().compareTo(o2.getPath());
        return (pathComparison == 0) ? 1 : pathComparison;
      } catch (RepositoryException e) {
        return 1;
      }
    }
  }

  public List<Node> getLinkedTagsOfDocument(Node documentNode, String workspace) throws Exception {

    Set<Node> ret = new HashSet<Node>();
    // prepare query
    StringBuilder queryStr = new StringBuilder("SELECT * FROM ").append(EXO_TAGGED);
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();

    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    QueryManager queryManager = sessionProvider.getSession(workspace, manageableRepository)
        .getWorkspace()
        .getQueryManager();
    Query query = queryManager.createQuery(queryStr.toString(), Query.SQL);
    QueryResult queryResult = query.execute();
    NodeIterator nodeIter = queryResult.getNodes();
    while (nodeIter.hasNext()) {
      Node tagNode = nodeIter.nextNode();
      if (existSymlink(tagNode, documentNode))
        ret.add(tagNode);
    }
    return new ArrayList<Node>(ret);
  }

  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String workspace) throws Exception {

    List<Node> ret = new ArrayList<Node>();
    if (scope == PRIVATE) {
      Node userFolksonomyNode = getUserFolksonomyFolder(value);
      for (Node tagNode : queryTagNodes(userFolksonomyNode)) {
        if (existSymlink(tagNode, documentNode))
          ret.add(tagNode);
      }
    }

    else if (scope == PUBLIC) {
      String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
      Node publicFolksonomyTreeNode = getNode(workspace, publicTagNodePath);
      for (Node tagNode : queryTagNodes(publicFolksonomyTreeNode)) {
        if (existSymlink(tagNode, documentNode))
          ret.add(tagNode);
      }
    }

    else if (scope == GROUP) {
      String[] roles = value.split(";");
      for (String group : roles) {
        if (group.length() < 1)
          continue;
        Node groupFolksonomyNode = getGroupFolksonomyFolder(group, workspace);
        for (Node tagNode : queryTagNodes(groupFolksonomyNode)) {
          if (existSymlink(tagNode, documentNode))
            ret.add(tagNode);
        }
      }
    } 
    Collections.sort(ret, new NodeComparator());
    return ret;
  }

  /**
   * Add new users or groups into tagPermissionPlugin_
   *
   * @param plugin
   */
  public void addTagPermissionPlugin(ComponentPlugin plugin) {
    if (plugin instanceof TagPermissionPlugin)
      tagPermissionPlugin_.add((TagPermissionPlugin) plugin);
  }

  /**
   * {@inheritDoc}
   */
  public void addTagPermission(String usersOrGroups) {
    List<String> _tagPermissionList = tagPermissionList.get(TAG_PERMISSION_LIST);
    if (_tagPermissionList!=null && !_tagPermissionList.contains(usersOrGroups)) {
      _tagPermissionList.add(usersOrGroups);
      tagPermissionList.put(TAG_PERMISSION_LIST, _tagPermissionList);
    } else if (_tagPermissionList == null) {
      _tagPermissionList = new ArrayList<String>();
      _tagPermissionList.add(usersOrGroups);
      tagPermissionList.put(TAG_PERMISSION_LIST, _tagPermissionList);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeTagPermission(String usersOrGroups) {
    List<String> _tagPermissionList = tagPermissionList.get(TAG_PERMISSION_LIST);
    if(_tagPermissionList!=null && _tagPermissionList.contains(usersOrGroups)) {
      _tagPermissionList.remove(usersOrGroups);
      tagPermissionList.put(TAG_PERMISSION_LIST, _tagPermissionList);
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getTagPermissionList() {
    return tagPermissionList.get(TAG_PERMISSION_LIST);
  }

  /**
   * {@inheritDoc}
   */
  public boolean canEditTag(int scope, List<String> memberships) {
    if (scope == PUBLIC) {
      List<String> _tagPermissionList = tagPermissionList.get(TAG_PERMISSION_LIST);
      if (_tagPermissionList == null || _tagPermissionList.size() == 0) {
        return false;
      }
      for (String membership : memberships) {
        if (_tagPermissionList.contains(membership))
          return true;
        if (membership.contains(":")) {
          if (_tagPermissionList.contains("*" + membership.substring(membership.indexOf(":"))))
            return true;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getAllTagNames(String workspace, int scope, String value) throws Exception {
    List<String> ret = new ArrayList<String>();
    List<Node> tags = new ArrayList<Node>();
    switch (scope) {
    case PUBLIC:
      tags = getAllPublicTags(value, workspace);
      break;
    case PRIVATE:
      tags = getAllPrivateTags(value);
      break;
    case GROUP:
      tags = value.indexOf(";") >= 0 ? getAllGroupTags(value.split(";"), workspace)
                                     : getAllGroupTags(value, workspace);
      break;
    case SITE:
      tags = getAllSiteTags(value, workspace);
    }
    for (Node tag : tags)
      ret.add(tag.getName());
    Collections.sort(ret);
    return ret;
  }

  private void createSiteTagPath() throws Exception {
    if (sitesTagPath.get(getRepoName()) == null) {
      // init path to site tags
      ManageableRepository manageableRepository = WCMCoreUtils.getRepository();

      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession(initParams_.getValueParam("workspace")
                                                   .getValue(), manageableRepository);

      String[] paths = initParams_.getValueParam("path").getValue().split("/");
      Node rootNode = session.getRootNode();
      Node currentNode = rootNode;
      int depth = 0;
      for (String path : paths) {
        if (path.length() > 0) {
          Node cnode = currentNode.hasNode(path) ? currentNode.getNode(path)
                                                 : currentNode.addNode(path);
          currentNode = cnode;
          if (depth++ == 0)
            if (!currentNode.isNodeType(EXO_HIDDENABLE))
              currentNode.addMixin(EXO_HIDDENABLE);
        }
      }
      session.save();
      sitesTagPath.put(getRepoName(), currentNode.getPath());
    }
  }

  private Node getTagNode(Node folksonomyHome, String tagName) throws Exception {
    Node tagNode = dataDistributionType.getOrCreateDataNode(folksonomyHome, tagName);
    if (!tagNode.isNodeType(EXO_TAGGED)) {
      tagNode.addMixin(EXO_TAGGED);
      tagNode.setProperty(EXO_TOTAL, 0);
    }
    return tagNode;
  }

  private void addTag(Node tagNode, Node targetNode) throws Exception {
    if (!existSymlink(tagNode, targetNode)) {
      linkManager.createLink(tagNode, targetNode);
      long total = tagNode.getProperty(EXO_TOTAL).getLong();
      tagNode.setProperty(EXO_TOTAL, total + 1);
    }
  }

  /**
   * {@inheritDoc}
   */
  public DataDistributionType getDataDistributionType() {
    return this.dataDistributionType;
  }

}
