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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

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

  private static final Log          LOG                    = ExoLogger.getLogger(NewFolksonomyService.class);

  private NodeHierarchyCreator      nodeHierarchyCreator;

  private LinkManager               linkManager;

  private InitParams                initParams_;

  private SessionProviderService    sessionProviderService;

  private List<TagStylePlugin>      plugin_                = new ArrayList<TagStylePlugin>();

  private List<TagPermissionPlugin> tagPermissionPlugin_   = new ArrayList<TagPermissionPlugin>();

  private Set<String>               tagPermissionList      = new HashSet<String>();

  private Map<String, String>       sitesTagPath           = new HashMap<String, String>();

  public NewFolksonomyServiceImpl(InitParams initParams,
                                  NodeHierarchyCreator nodeHierarchyCreator,
                                  LinkManager linkManager) throws Exception {
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.linkManager = linkManager;
    this.initParams_ = initParams;

    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    sessionProviderService = (SessionProviderService) myContainer.getComponentInstanceOfType(SessionProviderService.class);
  }

  /**
   * Implement method in Startable Call init() method
   *
   * @see {@link #init()}
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
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String repository,
                            String workspace,
                            String userName) throws Exception {
    addPrivateTag(tagsName, documentNode, workspace, userName);
  }
  
  /**
   * {@inheritDoc}
   */
  public void addPrivateTag(String[] tagsName,
                            Node documentNode,
                            String workspace,
                            String userName) throws Exception {
    Node userFolksonomyNode = getUserFolksonomyFolder(userName);
    Node targetNode = getTargetNode(documentNode);
    for (String tag : tagsName) {
      try {
        // find tag node
        Node tagNode = userFolksonomyNode.hasNode(tag) ? userFolksonomyNode.getNode(tag)
                                                      : userFolksonomyNode.addNode(tag);
        // add symlink and total
        if (targetNode != null && !existSymlink(tagNode, targetNode)) {
          linkManager.createLink(tagNode, targetNode);
          long total = tagNode.hasProperty(EXO_TOTAL) ? tagNode.getProperty(EXO_TOTAL).getLong()
                                                     : 0;
          tagNode.setProperty(EXO_TOTAL, total + 1);
          if (!tagNode.isNodeType(EXO_TAGGED))
            tagNode.addMixin(EXO_TAGGED);
        } else {
          if (!tagNode.hasProperty(EXO_TOTAL))
            tagNode.setProperty(EXO_TOTAL, 0);
        }
        userFolksonomyNode.getSession().save();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can't add tag '" + tag + "' to node: " + targetNode.getPath() + " for user: "
            + userName);
        }
      }
    }
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void addGroupsTag(String[] tagsName,
                           Node documentNode,
                           String repository,
                           String workspace,
                           String[] roles) throws Exception {
    addGroupsTag(tagsName, documentNode, workspace, roles);
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
          // find tag node
          Node tagNode = groupFolksonomyNode.hasNode(tag) ? groupFolksonomyNode.getNode(tag)
                                                         : groupFolksonomyNode.addNode(tag);
          // add symlink and total
          if (targetNode != null && !existSymlink(tagNode, targetNode)) {
            linkManager.createLink(tagNode, targetNode);
            long total = tagNode.hasProperty(EXO_TOTAL) ? tagNode.getProperty(EXO_TOTAL).getLong()
                                                       : 0;
            tagNode.setProperty(EXO_TOTAL, total + 1);
            if (!tagNode.isNodeType(EXO_TAGGED))
              tagNode.addMixin(EXO_TAGGED);
          } else {
            if (!tagNode.hasProperty(EXO_TOTAL))
              tagNode.setProperty(EXO_TOTAL, 0);
          }
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
  @Deprecated
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String repository,
                           String workspace) throws Exception {
    addPublicTag(treePath, tagsName, documentNode, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public void addPublicTag(String treePath,
                           String[] tagsName,
                           Node documentNode,
                           String workspace) throws Exception {    
    Node publicFolksonomyTreeNode = getNode(workspace, treePath, WCMCoreUtils.getUserSessionProvider());
    Node targetNode = getTargetNode(documentNode);
    for (String tag : tagsName) {
      try {
        // find tag node
        Node tagNode = publicFolksonomyTreeNode.hasNode(tag) ? publicFolksonomyTreeNode.getNode(tag)
                                                            : publicFolksonomyTreeNode.addNode(tag);
        // add symlink and total
        if (targetNode != null && !existSymlink(tagNode, targetNode)) {
          linkManager.createLink(tagNode, targetNode);
          long total = tagNode.hasProperty(EXO_TOTAL) ? tagNode.getProperty(EXO_TOTAL).getLong()
                                                     : 0;
          tagNode.setProperty(EXO_TOTAL, total + 1);
          if (!tagNode.isNodeType(EXO_TAGGED))
            tagNode.addMixin(EXO_TAGGED);
        } else {
          if (!tagNode.hasProperty(EXO_TOTAL))
            tagNode.setProperty(EXO_TOTAL, 0);
        }
        publicFolksonomyTreeNode.getSession().save();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can't add tag '" + tag + "' to node: " + targetNode.getPath()
            + " in public folksonomy tree!");
        }
      }
    }
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
  @Deprecated
  public void addSiteTag(String siteName,
                         String[] tagsName,
                         Node node,
                         String repository,
                         String workspace) throws Exception {
    addSiteTag(siteName, tagsName, node, workspace);
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
  @Deprecated
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String repository,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception {
    return getAllDocumentsByTag(tagPath, workspace, sessionProvider);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllDocumentsByTag(String tagPath,
                                         String workspace,
                                         SessionProvider sessionProvider) throws Exception {
    List<Node> ret = new ArrayList<Node>();
    Node tagNode = getNode(workspace, tagPath, sessionProvider);
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
  @Deprecated
  public List<Node> getAllGroupTags(String[] roles, String repository, String workspace) throws Exception {
    return getAllGroupTags(roles, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllGroupTags(String[] roles, String workspace) throws Exception {
    Set<Node> tagSet = new TreeSet<Node>(new NodeComparator());
    for (String group : roles) {
      Node groupFolksonomyNode = getGroupFolksonomyFolder(group, workspace);
      NodeIterator nodeIter = groupFolksonomyNode.getNodes();
      while (nodeIter.hasNext()) {
        Node tag = nodeIter.nextNode();
        if (!((Node) tag.getAncestor(1)).isNodeType(EXO_TRASH_FOLDER)) {
          tagSet.add(tag);
        }
      }
    }
    return new ArrayList<Node>(tagSet);
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllGroupTags(String role, String repository, String workspace) throws Exception {
    return getAllGroupTags(role, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllGroupTags(String role, String workspace) throws Exception {
    List<Node> tagSet = new ArrayList<Node>();
    Node groupFolksonomyNode = getGroupFolksonomyFolder(role, workspace);
    NodeIterator nodeIter = groupFolksonomyNode.getNodes();
    while (nodeIter.hasNext()) {
      tagSet.add(nodeIter.nextNode());
    }
    return tagSet;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllPrivateTags(String userName, String repository, String workspace) throws Exception {
    Node userFolksonomyNode = getUserFolksonomyFolder(userName);
    return getAllPrivateTags(userName);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllPrivateTags(String userName) throws Exception {
    Node userFolksonomyNode = getUserFolksonomyFolder(userName);
    return getChildNodes(userFolksonomyNode);
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllPublicTags(String treePath, String repository, String workspace) throws Exception {
    Node publicFolksonomyTreeNode = getNode(workspace, treePath);
    return getAllPublicTags(treePath, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getAllPublicTags(String treePath, String workspace) throws Exception {
    Node publicFolksonomyTreeNode = getNode(workspace, treePath);
    return getChildNodes(publicFolksonomyTreeNode);
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getAllSiteTags(String siteName, String repository, String workspace) throws Exception {
    return getAllSiteTags(siteName, workspace);
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
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      String repositoryName = sessionProvider.getCurrentRepository().getConfiguration().getName();
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
  @Deprecated
  public List<Node> getAllTagStyle(String repository, String workspace) throws Exception {
   return getAllTagStyle(workspace);
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
  @Deprecated
  public String getTagStyle(String tagStylePath, String repository, String workspace) throws Exception {
    return getTagStyle(tagStylePath, workspace);
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
   * {@inheritDoc}
   */
  @Deprecated
  public void init(String repository) throws Exception {
    init();
  }

  /**
   * init all avaiable TagStylePlugin
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

    for (TagPermissionPlugin plugin : tagPermissionPlugin_) {
      try {
        tagPermissionList.addAll(plugin.initPermission());
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("can not init tag permission: ", e);
        }
      }
    }

  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Node modifyTagName(String tagPath, String newTagName, String repository, String workspace) throws Exception {
    return modifyTagName(tagPath, newTagName, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public Node modifyTagName(String tagPath, String newTagName, String workspace) throws Exception {
    Node oldTagNode = getNode(workspace, tagPath);
    if (oldTagNode.getParent().hasNode(newTagName))
      throw new ItemExistsException("node " + newTagName + " has already existed!");

    StringBuilder newPath = new StringBuilder(oldTagNode.getParent().getPath()).append('/')
                                                                               .append(newTagName);

    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    session.move(tagPath, newPath.toString());
    session.save();
    return getNode(workspace, newPath.toString());
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void removeTag(String tagPath, String repository, String workspace) throws Exception {
    removeTag(tagPath, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeTag(String tagPath, String workspace) throws Exception {
    Node tagNode = getNode(workspace, tagPath);
    Node parentNode = tagNode.getParent();
    tagNode.remove();
    parentNode.getSession().save();
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void removeTagOfDocument(String tagPath, Node document, String repository, String workspace) throws Exception {
    removeTagOfDocument(tagPath, document, workspace);
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeTagOfDocument(String tagPath, Node document, String workspace) throws Exception {
    Node tagNode = getNode(workspace, tagPath);
    NodeIterator nodeIter = tagNode.getNodes();
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
  } 

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void updateTagStyle(String styleName,
                             String tagRange,
                             String htmlStyle,
                             String repository,
                             String workspace) throws Exception {
    updateTagStyle(styleName, tagRange, htmlStyle, workspace);
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
  @Deprecated
  public void addTagStyle(String styleName,
                          String tagRange,
                          String htmlStyle,
                          String repository,
                          String workspace) throws Exception {
    addTagStyle(styleName, tagRange, htmlStyle, workspace);
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
  @Deprecated
  public void removeTagsOfNodeRecursively(Node node,
                                          String repository,
                                          String workspace,
                                          String username,
                                          String groups) throws Exception {
    removeTagsOfNodeRecursively(node, workspace, username, groups);
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
    return ret;
  }

  private Node getGroupFolksonomyFolder(String group, String workspace) throws Exception {
    // code for running
    String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_ALIAS);
    // String folksonomyPath =
    // nodeHierarchyCreator.getJcrPath(GROUP_FOLKSONOMY_ALIAS);
    String folksonomyPath = "ApplicationData/Tags";
    Node groupsNode = getNode(workspace, groupsPath);
    return groupsNode.getNode(group.substring(1)).getNode(folksonomyPath);
  }

  private Node getUserFolksonomyFolder(String userName) throws Exception {
    // code for running
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, userName);
    String folksonomyPath = nodeHierarchyCreator.getJcrPath(USER_FOLKSONOMY_ALIAS);
    return userNode.getNode(folksonomyPath);
  }

  private Node getNode(String workspace, String path) throws Exception {
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    return (Node) sessionProvider.getSession(workspace, manageableRepository).getItem(path);
  }

  private Node getNode(String workspace, String path, SessionProvider sessionProvider) throws Exception {
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();

    RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

    return (Node) sessionProvider.getSession(workspace, manageableRepository).getItem(path);
  }

  private boolean existSymlink(Node parentNode, Node targetNode) throws Exception {
    NodeIterator nodeIter = parentNode.getNodes();
    while (nodeIter.hasNext()) {
      Node link = nodeIter.nextNode();
      Node pointTo = null;
      try {
        if (linkManager.isLink(link))
          pointTo = linkManager.getTarget(link);
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

  @Deprecated
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String repository, String workspace) throws Exception {

    return getLinkedTagsOfDocument(documentNode, workspace);
  }
  
  public List<Node> getLinkedTagsOfDocument(Node documentNode, String workspace) throws Exception {

    Set<Node> ret = new HashSet<Node>();
    // prepare query
    StringBuilder queryStr = new StringBuilder("SELECT * FROM ").append(EXO_TAGGED);
    ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
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

  @Deprecated
  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String repository,
                                                   String workspace) throws Exception {
    return getLinkedTagsOfDocumentByScope(scope, value, documentNode, workspace);
  }

  public List<Node> getLinkedTagsOfDocumentByScope(int scope,
                                                   String value,
                                                   Node documentNode,
                                                   String workspace) throws Exception {

    List<Node> ret = new ArrayList<Node>();
    if (scope == PRIVATE) {
      Node userFolksonomyNode = getUserFolksonomyFolder(value);
      NodeIterator iter = userFolksonomyNode.getNodes();
      while (iter.hasNext()) {
        Node tagNode = iter.nextNode();
        if (existSymlink(tagNode, documentNode))
          ret.add(tagNode);
      }
    }

    else if (scope == PUBLIC) {
      String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
      Node publicFolksonomyTreeNode = getNode(workspace, publicTagNodePath);
      NodeIterator iter = publicFolksonomyTreeNode.getNodes();
      while (iter.hasNext()) {
        Node tagNode = iter.nextNode();
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
        NodeIterator iter = groupFolksonomyNode.getNodes();
        while (iter.hasNext()) {
          Node tagNode = iter.nextNode();
          if (existSymlink(tagNode, documentNode))
            ret.add(tagNode);
        }
      }
    }
    return ret;
  }  
  /**
   * Add new users or groups into tagPermissionPlugin_
   *
   * @param usersOrGroups
   */
  public void addTagPermissionPlugin(ComponentPlugin plugin) {
    if (plugin instanceof TagPermissionPlugin)
      tagPermissionPlugin_.add((TagPermissionPlugin) plugin);
  }

  /**
   * {@inheritDoc}
   */
  public void addTagPermission(String usersOrGroups) {
    if (!tagPermissionList.contains(usersOrGroups))
      tagPermissionList.add(usersOrGroups);
  }

  /**
   * {@inhetirDoc}
   */
  public void removeTagPermission(String usersOrGroups) {
    tagPermissionList.remove(usersOrGroups);
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getTagPermissionList() {
    return new ArrayList<String>(tagPermissionList);
  }

  /**
   * {@inheritDoc}
   * 
   * @see NewFolksonomyServiceImpl#canEditTag(Node, int, List)
   * @see NewFolksonomyServiceImpl#canEditTag(String, String, int, List)
   */
  @Deprecated 
  public boolean canEditTag(int scope, List<String> memberships) {
    if (scope == PUBLIC) {
      if (tagPermissionList != null)
        for (String membership : memberships) {
          if (tagPermissionList.contains(membership))
            return true;
          if (membership.contains(":")) {
            if (tagPermissionList.contains("*" + membership.substring(membership.indexOf(":"))))
              return true;
          }
        }
      return false;
    }
    return true;
  }
  
  public boolean canEditTag(String workspace, String tagName, int scope, List<String> memberships) throws Exception {
    if (scope == PUBLIC) {
      String tagPath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH) + '/' + tagName;
      Node tagNode = getNode(workspace, tagPath);      
      return canEditPublicTag(tagNode, scope, memberships);
    }
    return true;
  }
  
  public boolean canEditTag(Node tagNode, int scope, List<String> memberships) throws Exception {
    if (scope == PUBLIC) {
      return canEditPublicTag(tagNode, scope, memberships);
    }
    return true;
  }
  
  /**
   * used to check a public tag could be edited or not
   * @param tagNode
   * @param scope
   * @param memberships
   * @return
   * @throws Exception
   */
  private boolean canEditPublicTag(Node tagNode, int scope, List<String> memberships) throws Exception {
    //check if tagNode is not a public tag
    if (!tagNode.getPath().startsWith(nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH))) {
      return false;
    }
    
    //tag owner can edit tag 
    if (tagNode.hasProperty("exo:owner")) {
      String owner = tagNode.getProperty("exo:owner").getString();
      String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
      if (owner != null && currentUser != null && owner.equals(currentUser)) {
        return true;
      }
    }

    //check tag permission
    if (tagPermissionList != null)
      for (String membership : memberships) {
        if (tagPermissionList.contains(membership))
          return true;
        if (membership.contains(":")) {
          if (tagPermissionList.contains("*" + membership.substring(membership.indexOf(":"))))
            return true;
        }
      }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<String> getAllTagNames(String repository, String workspace, int scope, String value) throws Exception {
    return getAllTagNames(workspace, scope, value);
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
      ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
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

}
