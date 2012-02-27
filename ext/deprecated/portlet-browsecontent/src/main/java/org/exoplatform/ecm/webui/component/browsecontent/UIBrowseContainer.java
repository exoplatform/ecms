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
package org.exoplatform.ecm.webui.component.browsecontent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.WindowState;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.scripts.DataTransfer;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 14, 2006 5:15:47 PM
 */
@ComponentConfigs(
    {
      @ComponentConfig(
          events = {
              @EventConfig(listeners = UIBrowseContainer.ChangeNodeActionListener.class),
              @EventConfig(listeners = UIBrowseContainer.BackActionListener.class),
              @EventConfig(listeners = UIBrowseContainer.ViewByTagActionListener.class),
              @EventConfig(listeners = UIBrowseContainer.BackViewActionListener.class),
              @EventConfig(listeners = UIBrowseContainer.SelectActionListener.class),
              @EventConfig(listeners = UIBrowseContainer.ChangePageActionListener.class)
          }
      ),
      @ComponentConfig(
          type = UIPageIterator.class, id = "UICBPageIterator",
          template = "system:/groovy/webui/core/UIPageIterator.gtmpl",
          events = @EventConfig(listeners = UIBrowseContainer.ShowPageActionListener.class)
      )
    }
)

public class UIBrowseContainer extends UIContainer {

  final public static String           CATEGORYPATH          = "categoryPath";

  final public static String           CURRENTNODE           = "currentNode";

  final public static String           HISTORY               = "history";

  final public static String           ISSHOWALLDOCUMENT     = "isShowAllDocument";

  final public static String           ISSHOWCATEGORYTREE    = "isShowCategoryTree";

  final public static String           ISSHOWDOCUMENTBYTAG   = "isShowDocumentByTag";

  final public static String           ISSHOWDOCUMENTDETAIL  = "isShowDocumentDetail";

  final public static String           ISSHOWDOCUMENTLIST    = "isShowDocumentList";

  final public static String           ISSHOWPAGEACTION      = "isShowPageAction";

  final public static String           ISSHOWSEARCHFORM      = "isShowSearchForm";

  final public static String           KEY_CURRENT           = "currentNode";

  final public static String           KEY_SELECTED          = "selectedNode";

  final public static String           NODESHISTORY          = "nodesHistory";

  final public static String           OLDTEMPLATE           = "oldTemplate";

  final public static String           ROOTNODE              = "rootNode";

  final public static String           ROWPERBLOCK           = "rowPerBlock";

  final public static String           SELECTEDTAB           = "selectedTab";

  final public static String           TAGPATH               = "tagPath";

  final public static String           TEMPLATEDETAIL        = "templateDetail";

  final public static String           TEMPLATEPATH          = "templatePath";

  final public static String           TREELIST              = "TreeList";

  final public static String           TREEROOT              = "treeRoot";

  final public static String           USECASE               = "usecase";

  private static final Log             LOG                   = ExoLogger.getLogger(UIBrowseContainer.class);

  private String categoryPath_;
  private String currentPath_;
  private String rootPath_;
  private String selectedTabPath_;

  private String detailTemplate_;

  private boolean isShowAllDocument_ ;

  private boolean isShowCategoriesTree_ = true;
  private boolean isShowDetailDocument_ = false;
  private boolean isShowDocumentByTag_ = false;
  private boolean isShowDocumentList_  = false;

  private boolean isShowPageAction_;
  private boolean isShowSearchForm_;
  private JCRResourceResolver jcrTemplateResourceResolver_;
  private boolean isSetted_ = false;
  private int totalRecord_;
  private String wsName_ = null;

  private LinkedList<String>           nodesHistory_         = new LinkedList<String>();

  private Map<String, Node>            nodesHistoryMap_      = new HashMap<String, Node>();

  private int rowPerBlock_ = 6;
  private String tagPath_;
  private String keyValue_;

  private String templatePath_;
  private BCTreeNode treeRoot_;
  private UIPageIterator uiPageIterator_;

  private HashMap<String, WindowState> windowState_ = new HashMap<String, WindowState>();
  private String windowId_;

  private List<Node> listHistoryNode = new ArrayList<Node>();

  @SuppressWarnings("unused")
  public UIBrowseContainer() throws Exception {
    ManageViewService vservice = getApplicationComponent(ManageViewService.class);
    uiPageIterator_ = addChild(UIPageIterator.class, "UICBPageIterator", "UICBPageIterator");
    addChild(UITagList.class, null, null);
    UICategoryTree uiTree = createUIComponent(UICategoryTree.class, null, null);
    addChild(uiTree);
    addChild(UIToolBar.class, null, null);
    addChild(UISearchController.class, null, null);
    addChild(UIDocumentDetail.class, null, "DocumentDetail");
  }

  public void changeNode(Node selectNode) throws Exception {
    setShowAllChildren(false);
    setShowDocumentByTag(false);
    setShowDocumentDetail(false);
    if(selectNode.equals(getRootNode())) {
      setCurrentNodePath(null);
      setSelectedTabPath(null);
    } else {
      setSelectedTabPath(selectNode.getPath());
      setCurrentNodePath(selectNode.getPath());
      setPageIterator(getSubDocumentList(getSelectedTab()));
    }
  }

  public String[] getActions() { return new String[] {"back"};}

  public SessionProvider getAnonimProvider() { return SessionProviderFactory.createAnonimProvider(); }

  public String getCategoryPath() { return categoryPath_; }

  public List<?> getCurrentList() throws Exception {
    return uiPageIterator_.getCurrentPageData();
  }
  public Node getCurrentNode() throws Exception {
    if(getNodeByPath(currentPath_) == null) return getNodeByPath(rootPath_);
    return getNodeByPath(currentPath_);
  }
  public List<Node> getDocumentByTag()throws Exception {
    String workspace = getWorkSpace();
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    List<String> documentsType = templateService.getDocumentTemplates();
    List<Node> documentsOnTag = new ArrayList<Node>();
    WebuiRequestContext ctx = WebuiRequestContext.getCurrentInstance();
    SessionProvider sessionProvider = (ctx.getRemoteUser() == null) ?
                                      SessionProviderFactory.createAnonimProvider() :
                                      SessionProviderFactory.createSessionProvider();

    try {
      for(Node node : newFolksonomyService.getAllDocumentsByTag(tagPath_, workspace, sessionProvider)) {
        if(documentsType.contains(node.getPrimaryNodeType().getName())) {
          documentsOnTag.add(node);
        }
      }
    } catch (PathNotFoundException ex) {
      // in case : tagPath_ has changed due to name of current displayed tag changed
    }
    return documentsOnTag;
  }
  public String getIcons(Node node, String type) throws Exception {
    try {
      return Utils.getNodeTypeIcon(node, type);
    } catch(Exception e) {
      return "";
    }
  }
  public String getImage(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class);
    InputStreamDownloadResource dresource;
    Node contentNode = null;
    if(node.hasNode(Utils.EXO_IMAGE)) {
      contentNode = node.getNode(Utils.EXO_IMAGE);
    } else if(node.hasNode(Utils.JCR_CONTENT)) {
      if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) return "";
      contentNode = node.getNode(Utils.JCR_CONTENT);
      String mimeType = contentNode.getProperty(Utils.JCR_MIMETYPE).getString();
      if(mimeType.startsWith("text")) return contentNode.getProperty(Utils.JCR_DATA).getString();
    }
    if(contentNode == null) return null;
    InputStream input = contentNode.getProperty(Utils.JCR_DATA).getStream();
    if(input.available() == 0) return null;
    dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(node.getName());
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }

  public String getImage(Node node, String nodeTypeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class);
    InputStreamDownloadResource dresource;
    Node contentNode = null;
    if(node.hasNode(nodeTypeName)) {
      contentNode = node.getNode(nodeTypeName);
    } else if(node.hasNode(Utils.JCR_CONTENT)) {
      if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) return "";
      contentNode = node.getNode(Utils.JCR_CONTENT);
      String mimeType = contentNode.getProperty(Utils.JCR_MIMETYPE).getString();
      if(mimeType.startsWith("text")) return contentNode.getProperty(Utils.JCR_DATA).getString();
    }
    if(contentNode == null) return null;
    InputStream input = contentNode.getProperty(Utils.JCR_DATA).getStream();
    if(input.available() == 0) return null;
    dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(node.getName());
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }

  public int getItemPerPage() {
    return Integer.parseInt(getPortletPreferences().getValue(Utils.CB_NB_PER_PAGE, ""));
  }

  public Node getNodeByPath(String nodePath) throws Exception {
    if (nodePath == null) return null;
    NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
    try{
      if(wsName_ == null) wsName_ = getWorkSpace();
      return (Node) nodeFinder.getItem(wsName_, nodePath);
    } catch(PathNotFoundException path) {
      // Target node with other workspace
      RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
      String[] wsNames = repositoryService.getCurrentRepository().getWorkspaceNames();
      for(String wsName : wsNames) {
        Node targetNode = null;
        try {
          targetNode = (Node) nodeFinder.getItem(wsName, nodePath);
        } catch (Exception e) {
          targetNode = null;
        }
        if (targetNode != null) return targetNode;
      }
      if (LOG.isErrorEnabled()) {
        LOG.error("PathNotFoundException when get node by path = " + nodePath, path);
      }
      return null;
    } catch(AccessDeniedException ace) {
      if (LOG.isErrorEnabled()) {
        LOG.error("AccessDeniedException when get node by path = " + nodePath, ace);
      }
      return null;
    } catch(Exception e){
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when get node by path = " + nodePath, e);
      }
      return null;
    }
  }

  public Node getNodeByPath(String nodePath, String workspace) throws Exception {
    NodeFinder nodeFinder = getApplicationComponent(NodeFinder.class);
    try{
      return (Node) nodeFinder.getItem(workspace, nodePath);
    } catch(PathNotFoundException path) {
      return null;
    } catch(AccessDeniedException ace) {
      return null;
    } catch(Exception e){
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when get node by path = " + nodePath + " in workspace " + workspace, e);
      }
      return null;
    }
  }

  public String getCapacityOfFile(Node file) throws Exception {
    Node contentNode = file.getNode(Utils.JCR_CONTENT);
    InputStream in = contentNode.getProperty(Utils.JCR_DATA).getStream();
    float capacity = in.available()/1024;
    String strCapacity = Float.toString(capacity);
    if(strCapacity.indexOf(".") > -1) return strCapacity.substring(0, strCapacity.lastIndexOf("."));
    return strCapacity;
  }

  public boolean isPublishedNode(Node node) throws Exception {
    if (isAllowPublish()) {
      PublicationService publicationService = getApplicationComponent(PublicationService.class);
      if(publicationService.isNodeEnrolledInLifecycle(node) &&
          publicationService.getCurrentState(node).equals("published"))
        return true;
    }
    return false;
  }

  public Node getViewNode(Node node) throws Exception {
    if(isPublishedNode(node)) {
      PublicationService publicationService = getApplicationComponent(PublicationService.class);
      return publicationService.getNodePublish(node, null);
    }
    return node;
  }

  private void addNodePublish(List<Node> listNode, Node node) throws Exception {
    if(isPublishedNode(node)) listNode.add(node);
    else listNode.add(node);
  }

  /**
   * Return a list of Node in the Query use case
   *
   * @param  recordNumber Number of expected records
   * @return list of Nodes corresponding to the query
   * @throws Exception if there was a problem while issuing the query
   */
  public List<Node> getNodeByQuery(int recordNumber) throws Exception {
    // Returned list of documents
    List<Node> queryDocuments = new ArrayList<Node>();

    try {
      QueryResult queryResult = null;

      if (Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_QUERY_ISNEW,""))) {
        // New query
        queryResult = getQueryResultNew();
      } else {
        // Stored query
        queryResult = getQueryResultStored();
      }

      // Add the required number of items to the returned list
      NodeIterator iter = queryResult.getNodes();
      int count = 0;
      while (iter.hasNext() && (count++ != recordNumber)) {
        Node node = iter.nextNode();
        if (isAllowPublish()) {
          if(!isPublishedNode(node)) continue;
        }
        queryDocuments.add(node);
      }
    } catch(Exception e) {
      // Display the stack trace
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occured while loading the result", e);
      }
    }

    return queryDocuments;
  }

  /**
   * Returns the results of a new query
   *
   * @param  recordNumber Number of expected records
   * @return query results
   */
  public QueryResult getQueryResultNew() throws Exception {
    // Retrieve the query statement
    String queryStatement = getQueryStatement();

    // Prepare the query
    QueryManager queryManager = getSession(false).getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryStatement, getQueryLanguage());

    // Execute the query and return results
    return query.execute();
  }

  /**
   * Returns the results of a saved query
   *
   * @param  recordNumber Number of expected records
   * @return query results
   */
  public QueryResult getQueryResultStored() throws Exception {
    QueryService queryService = getApplicationComponent(QueryService.class);

    String queryPath = getPortletPreferences().getValue(Utils.CB_QUERY_STORE,"");
    String workspace = getWorkSpace();
    return queryService.execute(queryPath, workspace, getSystemProvider(),
        getSession(true).getUserID());
  }

  public boolean nodeIsLocked(Node node) throws Exception {
    if(node.isLocked()) {
      return !Utils.isLockTokenHolder(node);
    }
    return false;
  }

  public boolean hasAddPermission(Node node) {
    ExtendedNode eNode = (ExtendedNode)node;
    try{
      eNode.checkPermission(PermissionType.ADD_NODE);
      return true;
    } catch(Exception ac){
      return false;
    }
  }

  public void setKeyValue(String keyValue) { keyValue_ = keyValue; };
  public String getKeyValue() { return keyValue_; }

  public Node getNodeByUUID(String uuid) throws Exception{
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    String[] workspaces = manageRepo.getWorkspaceNames();
    for(String ws : workspaces) {
      try{
        if(Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_FILTER_CATEGORY, ""))){
          return SessionProviderFactory.createSessionProvider().getSession(ws, manageRepo).getNodeByUUID(uuid);
        }
        return SessionProviderFactory.createSystemProvider().getSession(ws, manageRepo).getNodeByUUID(uuid);
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }

  public  List<Node> getNodeByQuery(int recoderNumber,Session session) throws Exception {
    List<Node> queryDocuments = new ArrayList<Node>();
    QueryManager queryManager = null;
    try{
      queryManager = session.getWorkspace().getQueryManager();
    }catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occured while retrieving the query manager", e);
      }
      return queryDocuments;
    }
    String queryStatiement = getQueryStatement();
    if(!Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_QUERY_ISNEW,""))) {
      String queryPath = getPortletPreferences().getValue(Utils.CB_QUERY_STORE,"");
      Node queryNode = getNodeByPath(queryPath);
      queryStatiement = queryNode.getProperty("jcr:statement").getString();
    }
    Query query = queryManager.createQuery(queryStatiement, getQueryLanguage());
    QueryResult queryResult = query.execute();
    NodeIterator iter = queryResult.getNodes();
    int count = 0;
    while (iter.hasNext() && (count++ != recoderNumber)) {
      Node node = iter.nextNode();
      addNodePublish(queryDocuments, node);
    }
    return queryDocuments;
  }

  public List<Node> getNodeByQuery(String queryType, String queryString) throws Exception {
    List<Node> queryDocuments = new ArrayList<Node>();
    try {
      ManageableRepository repository = getRepositoryService().getCurrentRepository();
      String workspace = repository.getConfiguration().getDefaultWorkspaceName();
      QueryManager queryManager = null;
      Session session = getSystemProvider().getSession(workspace, repository);
      queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, queryType);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        addNodePublish(queryDocuments, node);
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when execute query: " + queryString, e);
      }
    }
    totalRecord_ = queryDocuments.size();
    return queryDocuments;
  }
  public LinkedList<String> getNodesHistory() { return nodesHistory_; }

  public int getTotalNodeByQuery() { return totalRecord_;}

  public int getNumberOfPage() {
    return uiPageIterator_.getAvailablePage();
  }
  public String getOwner(Node node) throws Exception{
    if(node.hasProperty("exo:owner")) {
      return node.getProperty("exo:owner").getString();
    }
    return IdentityConstants.ANONIM;
  }
  @SuppressWarnings("unchecked")
  public Map getPathContent() throws Exception {
    TemplateService templateService  = getApplicationComponent(TemplateService.class);
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    List<String> templates = templateService.getDocumentTemplates();
    List<String> tabList = new ArrayList<String>();
    List<String> subCategoryList = new ArrayList<String>();
    List<Node> subDocumentList = new ArrayList<Node>();
    Map content = new HashMap();
    boolean isShowDocument = isEnableChildDocument();
    boolean isShowReferenced = isEnableRefDocument();
    int itemCounter = getRowPerBlock();
    if (isShowAllDocument()) itemCounter = getItemPerPage();
    if (selectedTabPath_.equals(currentPath_)) {
      Node currentNode = getCurrentNode();
      LinkManager linkManager = getApplicationComponent(LinkManager.class);
      if (currentNode.isNodeType(Utils.EXO_SYMLINK)) {
        currentNode = linkManager.getTarget(currentNode);
      }
      NodeIterator tabIter = currentNode.getNodes();
      while(tabIter.hasNext()) {
        Node childNode = tabIter.nextNode();
        if(canRead(childNode) && !childNode.isNodeType(Utils.EXO_HIDDENABLE)) {
          NodeType nt = childNode.getPrimaryNodeType();
          if (templates.contains(nt.getName())&&(isShowDocument)) {
            subDocumentList.add(childNode);
          }
          if (isShowReferenced) subDocumentList.addAll(getReferences(repositoryService,
              childNode, isShowAllDocument(), subDocumentList.size(), templates));
          if (isCategories(childNode) && (!templates.contains(nt.getName()))) {
            Map childOfSubCategory = new HashMap();
            List<Node> subCategoryDoc = new ArrayList<Node>();
            List<String> subCategoryCat = new ArrayList<String>();
            Node tempChildNode = childNode;
            if (childNode.isNodeType(Utils.EXO_SYMLINK)) {
              try {
                childNode = linkManager.getTarget(childNode);
              } catch (ItemNotFoundException ie) {
                continue;
              } catch (Exception e) {
                continue;
              }
            }
            NodeIterator item = childNode.getNodes();
            while (item.hasNext()) {
              Node node = item.nextNode();
              if (canRead(node) && !node.isNodeType(Utils.EXO_HIDDENABLE)) {
                NodeType nodeType = node.getPrimaryNodeType();
                String typeName = nodeType.getName();
                if(node.isNodeType(Utils.EXO_SYMLINK)) {
                  typeName = node.getProperty(Utils.EXO_PRIMARYTYPE).getString();
                }
                if (templates.contains(typeName)&&(isShowDocument)) {
                  if (subCategoryDoc.size() < getRowPerBlock()) {
                    if (isAllowPublish()) {
                      PublicationService publicationService = getApplicationComponent(PublicationService.class);
                      Node nodecheck = publicationService.getNodePublish(node, null);
                      if (nodecheck != null) {
                        subCategoryDoc.add(node);
                        if(PermissionUtil.canRead(node) && node.isNodeType(Utils.EXO_SYMLINK)) {
                          try {
                            linkManager.getTarget(node);
                          } catch (ItemNotFoundException ie) {
                            subCategoryDoc.remove(node);
                          } catch (Exception e) {
                            subCategoryDoc.remove(node);
                          }
                        }
                      }
                    } else {
                      subCategoryDoc.add(node);
                      if(PermissionUtil.canRead(node) && node.isNodeType(Utils.EXO_SYMLINK)) {
                        try {
                          linkManager.getTarget(node);
                        } catch (ItemNotFoundException ie) {
                          subCategoryDoc.remove(node);
                        } catch (Exception e) {
                          subCategoryDoc.remove(node);
                        }
                      }
                    }
                  }
                }
                if (isCategories(node)&&(!templates.contains(nodeType.getName()))) {
                  if (isAllowPublish()) {
                    PublicationService publicationService = getApplicationComponent(PublicationService.class);
                    Node nodecheck = publicationService.getNodePublish(node, null);
                    if (nodecheck != null) {
                      subCategoryCat.add(nodecheck.getPath());
                    }
                  } else {
                    subCategoryCat.add(node.getPath());
                  }
                }
              }
            }
            if(isShowReferenced) subCategoryDoc.addAll(getReferences(repositoryService, childNode,
                false, subCategoryDoc.size(), templates));
            childOfSubCategory.put("doc", subCategoryDoc);
            childOfSubCategory.put("sub", subCategoryCat);
            childNode = tempChildNode;
            String path = childNode.getPath();
            String keyPath = path.substring(path.lastIndexOf("/") + 1);
            content.put(keyPath, childOfSubCategory);
            subCategoryList.add(path);
          }
        }
      }
      content.put("tabList", tabList);
      content.put("subDocumentList", subDocumentList);
      content.put("subCategoryList", subCategoryList);
      return content;
    }
    NodeIterator tabIter = null;
    try {
      tabIter = getCurrentNode().getNodes();
    } catch(Exception e) {
      tabIter = getRootNode().getNodes();
    }
    while(tabIter.hasNext()) {
      Node tab = tabIter.nextNode();
      if(canRead(tab) && !tab.isNodeType(Utils.EXO_HIDDENABLE)) {
        if(!templates.contains(tab.getPrimaryNodeType().getName())){
          if(isCategories(tab)) tabList.add(tab.getPath());
          if(tab.getPath().equals(getSelectedTab().getPath())) {
            NodeIterator childs = tab.getNodes();
            while(childs.hasNext()) {
              Node child = childs.nextNode();
              String nt = child.getPrimaryNodeType().getName();
              if(Utils.isSymLink(child)) nt = child.getProperty(Utils.EXO_PRIMARYTYPE).getString();
              if(templates.contains(nt) && (isShowDocument)) {
                if(subDocumentList.size() < itemCounter) subDocumentList.add(child);
              }
              if(isCategories(child) && !templates.contains(nt)){
                Map childOfSubCategory = getChildOfSubCategory(repositoryService, child, templates);
                content.put(child.getName(), childOfSubCategory);
                subCategoryList.add(child.getPath());
              }
            }
            if(isShowReferenced) subDocumentList.addAll(getReferences(repositoryService,
                getSelectedTab(), isShowAllDocument(), subDocumentList.size(), templates));
          }
        }
      }
    }
    content.put("tabList", tabList);
    content.put("subCategoryList", subCategoryList);
    content.put("subDocumentList", subDocumentList);
    List<String> history = new ArrayList<String>();
    Node currentNode = null;
    try {
      currentNode = getCurrentNode();
      currentNode.getParent();
    } catch(Exception e) {
      currentNode = getRootNode();
    }
    if(!currentNode.getPath().equals("/") &&
        currentNode.getSession().getWorkspace().getName().equals(getWorkSpace())) {
      Node parent = currentNode.getParent();
      if(!parent.getPath().equals(getRootNode().getPath())) content.put("previous", parent.getPath());
      history = getHistory(templates, parent);
    }
    content.put(HISTORY, history);
    return content;
  }

  public PortletPreferences getPortletPreferences() {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletRequest prequest = pcontext.getRequest();
    PortletPreferences portletPref = prequest.getPreferences();
    return portletPref;
  }

  public String getQueryLanguage() {
    return getPortletPreferences().getValue(Utils.CB_QUERY_LANGUAGE, "");
  }
  public String getQueryStatement() {
    return getPortletPreferences().getValue(Utils.CB_QUERY_STATEMENT, "");
  }

  public String getRepository() {
    try {
      return getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                             .getConfiguration()
                                                             .getName();
    } catch (RepositoryException e) {
      return null;
    }
  }

  public String getUserName() {
    try {
      return getSession(getWorkSpace()).getUserID();
    } catch (Exception e) {
      return "";
    }
  }

  public String getRootPath() { return rootPath_; }

  public Node getRootNode() throws Exception {
    return getNodeByPath(rootPath_);
  }

  public int getRowPerBlock() { return rowPerBlock_; }

  public void setSelectedTabPath(String selectedTabPath) {
    if(selectedTabPath == null) selectedTabPath = rootPath_;
    else selectedTabPath_ = selectedTabPath;
  }
  public Node getSelectedTab() throws Exception {
    if(selectedTabPath_ == null) return getCurrentNode();
    return getNodeByPath(selectedTabPath_);
  }

  public Session getSession(boolean flag) throws Exception{
    Session session = null;
    String workspace = "";
    if (flag) {
      workspace = getDmsSystemWorkspace();
    } else {
      workspace = getWorkSpace();
    }
    ManageableRepository manageableRepository = getRepositoryService().getCurrentRepository();
    if(categoryPath_ != null && categoryPath_.startsWith("/jcr:system")) {
      if(!Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_FILTER_CATEGORY, ""))){
        session = getSystemProvider().getSession(workspace,manageableRepository);
      } else {
        session = getSessionProvider().getSession(workspace,manageableRepository);
      }
    } else {
      if(SessionProviderFactory.isAnonim()) {
        session = getAnonimProvider().getSession(workspace,manageableRepository);
      } else {
        session = getSessionProvider().getSession(workspace,manageableRepository);
      }
    }
    return session;
  }

  @Deprecated
  public Session getSession(String repository, String workspace) throws Exception{
    return getSession(workspace);
  }
  
  public Session getSession(String workspace) throws Exception{
    Session session = null;
    ManageableRepository manageableRepository = getRepositoryService().getCurrentRepository();
    if(categoryPath_ != null && categoryPath_.startsWith("/jcr:system")) {
      if(!Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_FILTER_CATEGORY, ""))){
        session = getSystemProvider().getSession(workspace,manageableRepository);
      } else {
        session = getSessionProvider().getSession(workspace,manageableRepository);
      }
    } else {
      if(WCMCoreUtils.isAnonim()) {
        session = getAnonimProvider().getSession(workspace,manageableRepository);
      } else {
        session = getSessionProvider().getSession(workspace,manageableRepository);
      }
    }
    return session;
  }  

  public SessionProvider getSessionProvider() { return SessionProviderFactory.createSessionProvider(); }

  public List<Node> getSubDocumentList(Node selectedNode) throws Exception {
    List<Node> subDocumentList = new ArrayList<Node>();
    if (selectedNode == null) return subDocumentList;
    LinkManager linkManager = getApplicationComponent(LinkManager.class);
    if (selectedNode.isNodeType(Utils.EXO_SYMLINK)) {
      selectedNode = linkManager.getTarget(selectedNode);
    }
    TemplateService templateService  = getApplicationComponent(TemplateService.class);
    List<String> templates = templateService.getDocumentTemplates();
    try {
      NodeIterator item = selectedNode.getNodes();
      if (isEnableChildDocument()) {
        while (item.hasNext()) {
          Node node = item.nextNode();
          String typeName = node.getPrimaryNodeType().getName();
          if(node.isNodeType(Utils.EXO_SYMLINK)) {
            typeName = node.getProperty(Utils.EXO_PRIMARYTYPE).getString();
          }
          if (templates.contains(typeName)) {
            if (canRead(node)) {
              if(node.isNodeType(Utils.EXO_SYMLINK)) {
                try {
                  linkManager.getTarget(node);
                } catch (ItemNotFoundException ie) {
                  subDocumentList.remove(node);
                } catch (Exception e) {
                  subDocumentList.remove(node);
                }
              }
              if (isAllowPublish()) {
                if(!isPublishedNode(node)) continue;
              }
              subDocumentList.add(node);
            }
          }
        }
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      return new ArrayList<Node>();
    }
    if(isEnableRefDocument()) subDocumentList.addAll(getReferences(getRepositoryService(),
        selectedNode, isShowAllDocument(), subDocumentList.size(), templates));

    return subDocumentList;
  }

  public SessionProvider getSystemProvider() { return SessionProviderFactory.createSystemProvider(); }
  public List<Node> getTagLink() throws Exception {
    String workspace = getWorkSpace();
    String user = getSession(workspace).getUserID();
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
    return newFolksonomyService.getAllPrivateTags(user);
  }
  public String getTagPath() { return this.tagPath_; }

  public Map<String ,String> getTagStyle() throws Exception {
    String workspace = getWorkSpace();
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
    Map<String , String> tagStyle = new HashMap<String ,String>();
    for(Node tag : newFolksonomyService.getAllTagStyle(workspace)) {
      tagStyle.put(tag.getName(), tag.getProperty("exo:htmlStyle").getValue().getString());
    }
    return tagStyle;
  }

  public String getTemplate() {
    PortletRequestContext context = PortletRequestContext.getCurrentInstance();
    PortletRequest portletRequest = context.getRequest();
    WindowState currentWindowState = portletRequest.getWindowState();
    if(windowState_.containsKey(windowId_)) {
      WindowState keptWindowState = windowState_.get(windowId_);
      if(isShowDetailDocument_ && currentWindowState.equals(WindowState.NORMAL) &&
          keptWindowState.equals(WindowState.MAXIMIZED)) {
        setShowDocumentDetail(false);
        windowState_.clear();
        return templatePath_;
      }
    }
    if(isShowDetailDocument_) return detailTemplate_;
    return templatePath_;
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    if(jcrTemplateResourceResolver_ == null) newJCRTemplateResourceResolver();
    return jcrTemplateResourceResolver_;
  }

  public List<Node> getSortedListNode(Node node, boolean isASC) throws Exception {
    NodeIterator nodeIter = node.getNodes();
    List<Node> nodes = new ArrayList<Node>();
    while(nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      nodes.add(childNode);
    }
    if(isASC) Collections.sort(nodes, new NodeNameASCComparator());
    else Collections.sort(nodes, new NodeNameDESCComparator());
    return nodes;
  }

  public List<Node> getSortedListFolderNode(Node node, boolean isASC) throws Exception {
    NodeIterator nodeIter = node.getNodes();
    List<Node> nodes = new ArrayList<Node>();
    while(nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      String primaryType = childNode.getPrimaryNodeType().getName();
      if(primaryType.equals(Utils.NT_UNSTRUCTURED) || primaryType.equals(Utils.NT_FOLDER)) {
        nodes.add(childNode);
      }
    }
    if(isASC) Collections.sort(nodes, new NodeNameASCComparator());
    else Collections.sort(nodes, new NodeNameDESCComparator());
    return nodes;
  }

  public List<Node> getSortedListNodeByDate(Node node, boolean isASC) throws Exception {
    NodeIterator nodeIter = node.getNodes();
    List<Node> nodes = new ArrayList<Node>();
    while(nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      nodes.add(childNode);
    }
    if(isASC) Collections.sort(nodes, new DateASCComparator());
    else Collections.sort(nodes, new DateDESCComparator());
    return nodes;
  }

  public boolean isSymLink(String nodePath) throws Exception {
    Node node = (Node)getSession(false).getItem(nodePath);
    if(Utils.isSymLink(node)) return true;
    return false;
  }

  static public class NodeNameDESCComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        String name1 = ((Node)o1).getName();
        String name2 = ((Node)o2).getName();
        return name2.compareToIgnoreCase(name1);
      } catch(Exception e) {
        return 0;
      }
    }
  }

  static public class NodeNameASCComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        String name1 = ((Node)o1).getName();
        String name2 = ((Node)o2).getName();
        return name1.compareToIgnoreCase(name2);
      } catch(Exception e) {
        return 0;
      }
    }
  }

  static public class DateASCComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        Date date1 = ((Node)o1).getProperty(Utils.EXO_CREATED_DATE).getDate().getTime();
        Date date2 = ((Node)o2).getProperty(Utils.EXO_CREATED_DATE).getDate().getTime();
        return date1.compareTo(date2);
      } catch(Exception e) {
        return 0;
      }
    }
  }

  static public class DateDESCComparator implements Comparator<Object> {
    public int compare(Object o1, Object o2) throws ClassCastException {
      try {
        Date date1 = ((Node)o1).getProperty(Utils.EXO_CREATED_DATE).getDate().getTime();
        Date date2 = ((Node)o2).getProperty(Utils.EXO_CREATED_DATE).getDate().getTime();
        return date2.compareTo(date1);
      } catch(Exception e) {
        return 0;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public Map getTreeContent() throws Exception {
    TemplateService templateService  = getApplicationComponent(TemplateService.class);
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    List templates = templateService.getDocumentTemplates();
    List<String> subCategoryList = new ArrayList<String>();
    List<Node> subDocumentList = new ArrayList<Node>();
    Node currentNode = getCurrentNode();
    Map content = new HashMap();
    if (currentNode != null) {
      LinkManager linkManager = getApplicationComponent(LinkManager.class);
      if (currentNode.isNodeType(Utils.EXO_SYMLINK)) {
        currentNode = linkManager.getTarget(currentNode);
      }
      NodeIterator childIter = currentNode.getNodes();
      boolean isShowDocument = isEnableChildDocument();
      boolean isShowReferenced = isEnableRefDocument();
      while(childIter.hasNext()) {
        Node child = childIter.nextNode();
        if(canRead(child) && !child.isNodeType(Utils.EXO_HIDDENABLE)) {
          String typeName = child.getPrimaryNodeType().getName();
          if(child.isNodeType(Utils.EXO_SYMLINK)) {
            typeName = child.getProperty(Utils.EXO_PRIMARYTYPE).getString();
          }
          if(templates.contains(typeName)&&(isShowDocument)) {
            if(canRead(child)) subDocumentList.add(child);
          } else {
            if(isCategories(child)) {
              Map childOfSubCategory = getChildOfSubCategory(repositoryService, child, templates);
              String path = child.getPath();
              String keyPath = path.substring(path.lastIndexOf("/") + 1);
              content.put(keyPath, childOfSubCategory);
              subCategoryList.add(path);
            }
          }
        }
      }

      if(isShowReferenced) subDocumentList.addAll(getReferences(repositoryService,
          currentNode, isShowAllDocument(), subDocumentList.size(), templates));
      content.put("subCategoryList", subCategoryList);
      content.put("subDocumentList", subDocumentList);
    }
    return content;
  }
  public BCTreeNode getTreeRoot() { return treeRoot_;  }
  public UIPageIterator getUIPageIterator() throws Exception {
    return uiPageIterator_;
  }

  public String getUseCase() {
    return getPortletPreferences().getValue(Utils.CB_USECASE, "");
  }

  public void setWorkspaceName(String wsName) { wsName_ = wsName; }

  public String getWorkSpace() {
    return getPortletPreferences().getValue(Utils.WORKSPACE_NAME, "");
  }

  @Deprecated
  public String getDMSSystemWorkspace(String repository) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    DMSConfiguration dmsConfiguration = (DMSConfiguration)
        container.getComponentInstanceOfType(DMSConfiguration.class);

    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return dmsRepoConfig.getSystemWorkspace();
  }
  
  public String getDMSSystemWorkspace() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    DMSConfiguration dmsConfiguration = (DMSConfiguration)
        container.getComponentInstanceOfType(DMSConfiguration.class);

    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return dmsRepoConfig.getSystemWorkspace();
  }  

  public void initToolBar(boolean showTree, boolean showPath,boolean showSearch) throws Exception {
    UIToolBar toolBar = getChild(UIToolBar.class);
    toolBar.setEnableTree(showTree);
    toolBar.setEnablePath(showPath);
    toolBar.setEnableSearch(showSearch);
    toolBar.setRendered(true);
  }

  public boolean isCommentAndVote() { return (isShowVoteForm() || isShowCommentForm());}

  public boolean isEnableChildDocument() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_CHILD_DOCUMENT, ""));
  }

  public boolean isAllowPublish() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_ALLOW_PUBLISH, ""));
  }

  public boolean isEnableRefDocument() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_REF_DOCUMENT, ""));
  }

  public boolean isEnableToolBar() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_VIEW_TOOLBAR, ""));
  }
  public boolean isRootNode() throws Exception {return getCurrentNode().equals(getRootNode());}
  public boolean isShowAllDocument() { return this.isShowAllDocument_; }

  public boolean isShowCategoryTree() { return isShowCategoriesTree_; }
  public boolean isShowCommentForm() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_VIEW_COMMENT, ""));
  }
  public boolean isShowDocumentByTag() { return isShowDocumentByTag_; }

  public boolean isShowDocumentDetail() { return isShowDetailDocument_; }
  public boolean isShowDocumentList() { return this.isShowDocumentList_; }
  public boolean isShowSearchForm() { return isShowSearchForm_;  }

  public boolean isShowTagmap() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_VIEW_TAGMAP, ""));
  }

  public boolean isShowVoteForm() {
    return Boolean.parseBoolean(getPortletPreferences().getValue(Utils.CB_VIEW_VOTE, ""));
  }

  public void loadPortletConfig(PortletPreferences preferences) throws Exception {
    String tempName = preferences.getValue(Utils.CB_TEMPLATE, "");
    String repoName = getRepository();
    ManageViewService viewService = getApplicationComponent(ManageViewService.class);
    setShowDocumentByTag(false);
    setShowDocumentDetail(false);
    if(getUseCase().equals(Utils.CB_USE_JCR_QUERY)) {
      setTemplate(viewService.getTemplateHome(BasePath.CB_QUERY_TEMPLATES,
          SessionProviderFactory.createSystemProvider()).getNode(tempName).getPath());
      if(isShowCommentForm() || isShowVoteForm()) initToolBar(false, false, false);
      if(!isShowDocumentByTag()) setPageIterator(getNodeByQuery(-1));
      return;
    }
    if(getUseCase().equals(Utils.CB_USE_SCRIPT)) {
      setTemplate(viewService.getTemplateHome(BasePath.CB_SCRIPT_TEMPLATES,
                                              SessionProviderFactory.createSystemProvider())
                             .getNode(tempName)
                             .getPath());
      if (isShowCommentForm() || isShowVoteForm()) initToolBar(false, false, false);
      String scriptName = preferences.getValue(Utils.CB_SCRIPT_NAME, "");
      if (!isShowDocumentByTag()) setPageIterator(getNodeByScript(repoName, scriptName));
      return;
    }
    String categoryPath = preferences.getValue(Utils.JCR_PATH, "");
    NodeFinder nodeFinder_ =  getApplicationComponent(NodeFinder.class);
    LinkManager linkManager_ = getApplicationComponent(LinkManager.class);
    Node categoryNode = null;
    try {
      categoryNode = (Node)nodeFinder_.getItem(getWorkSpace(), categoryPath);
      setRootPath(categoryPath);
      setCategoryPath(categoryPath);
      setSelectedTabPath(categoryPath);
      setCurrentNodePath(categoryPath);
    } catch (Exception e) {
      setRootPath(null);
      setCategoryPath(null);
      setSelectedTabPath(null);
      setCurrentNodePath(null);
    }
    if (linkManager_.isLink(categoryNode)) {
      if (linkManager_.isTargetReachable(categoryNode)) {
        categoryNode = linkManager_.getTarget(categoryNode);
      }
    }
    if(getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
      setTemplate(viewService.getTemplateHome(BasePath.CB_PATH_TEMPLATES,
                                              SessionProviderFactory.createSystemProvider())
                             .getNode(tempName)
                             .getPath());
      initToolBar(false, isEnableToolBar(), isEnableToolBar());
      if(getTemplateName().equals(TREELIST)) {
        if(isEnableToolBar()) initToolBar(true, false, true);
        getChild(UICategoryTree.class).setTreeRoot(getRootNode());
        if (getCurrentNode() != null)
          getChild(UICategoryTree.class).buildTree(getCurrentNode().getPath());
      }
      if(!isShowDocumentByTag()) setPageIterator(getSubDocumentList(getSelectedTab()));
      return;
    }
    if (getUseCase().equals(Utils.CB_USE_DOCUMENT)) {
      setTemplateDetail(viewService.getTemplateHome(BasePath.CB_DETAIL_VIEW_TEMPLATES,
                                                    SessionProviderFactory.createSystemProvider())
                                   .getNode(tempName)
                                   .getPath());
      String documentPath;
      if ((categoryPath.equals("/")) && (preferences.getValue(Utils.CB_DOCUMENT_NAME, "").indexOf("/") == 0))
        documentPath = preferences.getValue(Utils.CB_DOCUMENT_NAME, "");
      else
        documentPath = categoryPath + preferences.getValue(Utils.CB_DOCUMENT_NAME, "");
      Node documentNode = null;
      try{
        documentNode = (Node)nodeFinder_.getItem(getWorkSpace(), documentPath);
        if (linkManager_.isLink(documentNode)) {
          if (linkManager_.isTargetReachable(documentNode)) {
            documentNode = linkManager_.getTarget(documentNode);
          }
        }
      }catch (Exception e) {
        setRootPath(null);
        setCategoryPath(null);
        setSelectedTabPath(null);
        setCurrentNodePath(null);
        return;
      }
      viewDocument(documentNode, false);
      if(isEnableToolBar()) initToolBar(false, false, false);
      return;
    }
  }

  public String getDmsSystemWorkspace() {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return dmsRepoConfig.getSystemWorkspace();
  }

  public void newJCRTemplateResourceResolver() {
    try{
      jcrTemplateResourceResolver_ = new JCRResourceResolver(Utils.EXO_TEMPLATEFILE);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occured while creating a new JCR Resource resolver", e);
      }
    }
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    try {
      getApplicationComponent(RepositoryService.class).getCurrentRepository();
      if (getCurrentNode() != null) super.processRender(context);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occured while displaying the content", e);
      }
      getAncestorOfType(UIBrowseContentPortlet.class).setPorletMode(PortletMode.HELP);
      return;
    }
  }

  public void record(String str) { getNodesHistory().add(str); }

  public void refreshContent() throws Exception{
    if(!showPageAction()) {
      if(isShowDocumentByTag()) {
        setPageIterator(getDocumentByTag());
      } else {
        if(getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
          if(getCategoryPath() == null || getRootNode() == null) {
            //UIBrowseContentPortlet uiPorlet = getAncestorOfType(UIBrowseContentPortlet.class);
            //uiPorlet.setPorletMode(PortletMode.HELP);
            //uiPorlet.reload();
          } else if(getNodeByPath(getSelectedTab().getPath()) == null ||
              getNodeByPath(getCurrentNode().getPath()) == null) {
            setSelectedTabPath(null);
            setCurrentNodePath(null);
          }
          setPageIterator(getSubDocumentList(getSelectedTab()));
        } else if(getUseCase().equals(Utils.CB_USE_SCRIPT)) {
          PortletPreferences preferences = getPortletPreferences();
          String tempName = preferences.getValue(Utils.CB_TEMPLATE, "");
          String repoName = preferences.getValue(Utils.REPOSITORY, "");
          ManageViewService viewService = getApplicationComponent(ManageViewService.class);
          setTemplate(viewService.getTemplateHome(BasePath.CB_SCRIPT_TEMPLATES,
                                                  SessionProviderFactory.createSystemProvider())
                                 .getNode(tempName)
                                 .getPath());
          if (isShowCommentForm() || isShowVoteForm()) initToolBar(false, false, false);
          String scriptName = preferences.getValue(Utils.CB_SCRIPT_NAME, "");
          if (!isShowDocumentByTag()) setPageIterator(getNodeByScript(repoName, scriptName));
        } else if(getUseCase().equals(Utils.CB_USE_JCR_QUERY)) {
          setPageIterator(getNodeByQuery(-1));
        } else if(getUseCase().equals(Utils.USE_DOCUMENT)) {
          if (getChild(UIDocumentDetail.class).isValidNode()) {
            getChild(UIDocumentDetail.class).setRendered(true);
          } else {
            getChild(UIDocumentDetail.class).setRendered(false);
            UIBrowseContentPortlet uiPortlet = getAncestorOfType(UIBrowseContentPortlet.class);
            uiPortlet.getChild(UIPopupContainer.class).deActivate();
          }
        }
        if (isShowDocumentDetail()) {
          UIDocumentDetail uiDocumentDetail = getChild(UIDocumentDetail.class);
          if (getChild(UIDocumentDetail.class).isValidNode()) {
            getChild(UIDocumentDetail.class).setRendered(true);
          } else {
            if (isShowDocumentByTag() && isShowDocumentDetail()) {
              setShowDocumentDetail(false);
              uiDocumentDetail.setRendered(false);
            } else {
              setShowDocumentByTag(false);
              setShowDocumentDetail(false);
              uiDocumentDetail.setRendered(false);
              if (getUseCase().equals(Utils.CB_USE_FROM_PATH) && getHistory() != null) {
                setCurrentNodePath(getHistory().get(UIBrowseContainer.KEY_CURRENT).getPath());
                setSelectedTabPath(getHistory().get(UIBrowseContainer.KEY_SELECTED).getPath());
                getHistory().clear();
              }
              UIBrowseContentPortlet uiPortlet = getAncestorOfType(UIBrowseContentPortlet.class);
              uiPortlet.getChild(UIPopupContainer.class).deActivate();
            }
          }
        }
      }
      setShowPageAction(false);
    }
  }

  public void setCategoryPath(String path) {
    this.categoryPath_ = path;
  }

  public void setCurrentNodePath(String currentPath) {
    if(currentPath == null) currentPath = rootPath_;
    currentPath_ = currentPath;
  }

  public void setPageIterator(List<Node> data) throws Exception {
    if (!isSetted_) {
      ListAccess<Node> nodeList = new ListAccessImpl<Node>(Node.class, data);
      LazyPageList<Node> dataPageList = new LazyPageList<Node>(nodeList, getItemPerPage());
      uiPageIterator_.setPageList(dataPageList);
    }
  }

  public boolean isSetted() { return isSetted_; }

  public void setPageStatus(boolean isSetted) { isSetted_ = isSetted; }

  public void setRowPerBlock(int number) { this.rowPerBlock_ = number; }

  public void setShowAllChildren(boolean isShowAll) {
    this.isShowAllDocument_ = isShowAll;
  }

  public void setShowCategoryTree(boolean  isShowCategoryTree) {
    this.isShowCategoriesTree_ = isShowCategoryTree;
  }

  public void setShowDocumentByTag(boolean isShowByTag) {
    this.isShowDocumentByTag_ = isShowByTag;
  }
  public void setShowDocumentDetail(boolean isShowDocument) {
    this.isShowDetailDocument_ = isShowDocument;
  }

  public void setShowDocumentList(boolean isShowDocumentList){
    this.isShowDocumentList_ = isShowDocumentList;
  }

  public void setShowSearchForm(boolean isShowSearch) {
    this.isShowSearchForm_ = isShowSearch;
  }

  public void setTagPath(String tagPath) { this.tagPath_ = tagPath; }

  public void setTemplate(String temp) { this.templatePath_ = temp; }

  public void setTreeRoot(Node node) throws Exception { this.treeRoot_ = new BCTreeNode(node); }

  public void storeHistory() throws Exception {
    getHistory().clear();
    getHistory().put(KEY_CURRENT, getCurrentNode());
    getHistory().put(KEY_SELECTED, getSelectedTab());
  }

  public List<Node> getListHistoryNode() {
    listHistoryNode.remove(null);
    return listHistoryNode;
  }

  public List<Node> storeListHistory(Node selectedNode) throws Exception {
    LinkManager linkManager = getApplicationComponent(LinkManager.class);
    Node rootNode = getRootNode();
    Node parentNode = null;
    int countHistoryNode = listHistoryNode.size();
    if(rootNode != null && selectedNode.getPath().equals(rootNode.getPath())) {
      listHistoryNode.clear();
    } else {
      parentNode = selectedNode.getParent();
      if ((parentNode != null) && (countHistoryNode > 0)) {
        Node tempNode = listHistoryNode.get(countHistoryNode - 1);
        if (tempNode.isNodeType(Utils.EXO_SYMLINK)) {
          try {
            tempNode = linkManager.getTarget(tempNode);
          } catch (ItemNotFoundException itemNotFoundException) {
            return getListHistoryNode();
          }
          if (tempNode.getPath().equals(parentNode.getPath())) parentNode = null;
        }
      }
      Node grandParentNode = null;
      if (listHistoryNode.size() == 0) {
        grandParentNode = rootNode;
      } else {
        if (listHistoryNode.get(listHistoryNode.size() - 1) != null) {
          grandParentNode = listHistoryNode.get(listHistoryNode.size() - 1);
        }
      }
      // If symlink1/symlink2
      if ((grandParentNode != null) && (grandParentNode.hasNodes())) {
        NodeIterator nodeIter = grandParentNode.getNodes();
        while(nodeIter.hasNext()) {
          Node child = nodeIter.nextNode();
          Node tempChild = child;
          if (child.isNodeType(Utils.EXO_SYMLINK)) {
            try {
              child = linkManager.getTarget(child);
            } catch (ItemNotFoundException ie) {
              continue;
            } catch (Exception e) {
              continue;
            }
          }
          if ((parentNode != null) && (child.getPath().equals(parentNode.getPath()))) {
            parentNode = tempChild;
            break;
          }
        }
      }
    }
    if (listHistoryNode.size() == 0) {
      if ((parentNode != null) && (!listHistoryNode.contains(parentNode))) listHistoryNode.add(parentNode);
      if (!listHistoryNode.contains(selectedNode)) listHistoryNode.add(selectedNode);
    } else {
      if (listHistoryNode.contains(selectedNode)) {
        List<Node> listHistoryTmp = new ArrayList<Node>();
        boolean addOk = true;
        countHistoryNode = listHistoryNode.size();
        for (int i=0; i<countHistoryNode; i++) {
          if (listHistoryNode.get(i).getPath().equals(selectedNode.getPath())) {
            listHistoryTmp.add(listHistoryNode.get(i));
            addOk = false;
          }
          if (addOk) listHistoryTmp.add(listHistoryNode.get(i));
        }
        listHistoryNode.clear();
        listHistoryNode.addAll(listHistoryTmp);
      } else {
        countHistoryNode = listHistoryNode.size();
        for (int i=0; i<countHistoryNode; i++) {
          if ((parentNode != null) && (listHistoryNode.get(i) != null) && (listHistoryNode.get(i) == parentNode)) {
            if (!listHistoryNode.contains(parentNode)) {
              listHistoryNode.add(i+1, parentNode);
              if (!listHistoryNode.contains(selectedNode)) listHistoryNode.add(i+2, selectedNode);
            } else {
              if (!listHistoryNode.contains(selectedNode)) listHistoryNode.add(i+1, selectedNode);
            }
          } else {
            if (!listHistoryNode.contains(parentNode) && (parentNode != null)) {
              if ((parentNode != null) && (countHistoryNode > 1)) {
                Node tempNode = listHistoryNode.get(countHistoryNode - 2);
                if (tempNode.isNodeType(Utils.EXO_SYMLINK)) {
                  tempNode = linkManager.getTarget(tempNode);
                  if (tempNode.getPath().equals(parentNode.getPath())) parentNode = null;
                }
              }
              if (parentNode != null) listHistoryNode.add(parentNode);
            }
            if (parentNode != null) {
              if(parentNode.hasNodes()) {
                NodeIterator nodeIter = parentNode.getNodes();
                while(nodeIter.hasNext()) {
                  Node child = nodeIter.nextNode();
                  listHistoryNode.remove(child);
                }
              }
            }
            if (!listHistoryNode.contains(selectedNode)) listHistoryNode.add(selectedNode);
          }
          countHistoryNode = listHistoryNode.size();
        }
      }
    }
    if (listHistoryNode.contains(rootNode)) listHistoryNode.remove(rootNode);
    if (rootNode != null && rootNode.isNodeType(Utils.EXO_SYMLINK) && (listHistoryNode.size() > 0)) {
      Node historyNode1 = listHistoryNode.get(0);
      Node targetRootNode = linkManager.getTarget(rootNode);
      if (historyNode1.getPath().equals(targetRootNode.getPath())) listHistoryNode.remove(historyNode1);
    }

    return getListHistoryNode();
  }

  public void viewDocument(Node docNode ,boolean hasDocList) throws Exception {
    setShowDocumentDetail(true);
    setShowDocumentList(hasDocList);
    UIDocumentDetail uiDocumetDetail = getChild(UIDocumentDetail.class);
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    uiDocumetDetail.setOriginalNode(docNode);
    if(isAllowPublish()) {
      if(isPublishedNode(docNode)) {
        uiDocumetDetail.setNode(publicationService.getNodePublish(docNode, null));
      }
    } else {
      uiDocumetDetail.setNode(docNode);
    }
    uiDocumetDetail.setLanguage(null);
    uiDocumetDetail.setRendered(true);
  }

  protected Map<String, Node>  getHistory() { return nodesHistoryMap_; }

  protected List<Node> getNodeByScript(String repository,String scriptName) throws Exception {
    DataTransfer data = new DataTransfer();
    ScriptService scriptService = getApplicationComponent(ScriptService.class);
    data.setWorkspace(getPortletPreferences().getValue(Utils.WORKSPACE_NAME, ""));
    data.setRepository(repository);
    Node scripts = scriptService.getCBScriptHome(SessionProviderFactory.createSystemProvider());
    try {
      CmsScript cmsScript = scriptService.getScript(scripts.getName()+ "/" + scriptName );
      cmsScript.execute(data);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error occured while executing the script", e);
      }
      return new ArrayList<Node>();
    }
    List<Node> listNode = data.getContentList();
    List<Node> listNodeAfterCheck = new ArrayList<Node>();
    for (Node node : listNode) {
      if (isAllowPublish()) {
        if(!isPublishedNode(node)) continue;
      }
      listNodeAfterCheck.add(node);
    }
    return listNodeAfterCheck;
  }

  protected String getTemlateDetail() { return detailTemplate_; }
  protected String getTemplateName() {
    return getPortletPreferences().getValue(Utils.CB_TEMPLATE, "");
  }

  protected void historyBack() throws Exception {
    if(getTemplateName().equals(TREELIST)) {
      setSelectedTabPath(null);
      setCurrentNodePath(getNodeByPath(getNodesHistory().removeLast()).getPath());
    } else {
      setSelectedTabPath(getNodeByPath(getNodesHistory().removeLast()).getPath());
    }
  }

  protected void historyNext() {}

  protected boolean isCategories(Node node) throws RepositoryException {
    NodeType nodeType = node.getPrimaryNodeType();
    String primaryTypeName = nodeType.getName();
    if(node.isNodeType(Utils.EXO_SYMLINK)) {
      primaryTypeName = node.getProperty(Utils.EXO_PRIMARYTYPE).getString();
    }
    for(String type : Utils.CATEGORY_NODE_TYPES) {
      if(primaryTypeName.equals(type)) return true;
    }
    return false;
  }

  protected boolean isCategories(NodeType nodeType) {
    for(String type : Utils.CATEGORY_NODE_TYPES) {
      if(nodeType.getName().equals(type)) return true;
    }
    return false;
  }
//  protected void setRootNode(Node node) { this.rootNode_ = node; }

  protected void setRootPath(String rootPath) { rootPath_ = rootPath; }

  protected void setShowPageAction(boolean isShowPage) { this.isShowPageAction_ = isShowPage; }

  protected void setTemplateDetail(String template) { this.detailTemplate_ = template; }

  protected boolean showPageAction() { return isShowPageAction_; }

  private boolean canRead(Node node) {
    ExtendedNode eNode = (ExtendedNode)node;
    try{
      eNode.checkPermission(PermissionType.READ);
      return true;
    } catch(Exception ac){
      return false;
    }
  }

  private Map<String, List<? extends Object>> getChildOfSubCategory(RepositoryService repositoryService, Node subCat,
      List<?> documentTemplates) throws Exception {
    LinkManager linkManager = getApplicationComponent(LinkManager.class);
    List<String> subCategories = new ArrayList<String>();
    List<Node> childDocOrReferencedDoc = new ArrayList<Node>();
    Map<String, List<? extends Object>> childMap = new HashMap<String, List<? extends Object>>();
    NodeIterator items  =  subCat.getNodes();
    boolean isShowDocument = isEnableChildDocument();
    boolean isShowReferenced = isEnableRefDocument();
    while (items.hasNext()) {
      Node item = items.nextNode();
      if (canRead(item)) {
        NodeType nt = item.getPrimaryNodeType();
        String typeName = nt.getName();
        if(item.isNodeType(Utils.EXO_SYMLINK)) {
          typeName = item.getProperty(Utils.EXO_PRIMARYTYPE).getString();
        }
        if (documentTemplates.contains(typeName) && isShowDocument){
          if (childDocOrReferencedDoc.size() < getRowPerBlock()) {
            if (isAllowPublish()) {
              PublicationService publicationService = getApplicationComponent(PublicationService.class);
              Node nodecheck = publicationService.getNodePublish(item, null);
              if (nodecheck != null) {
                childDocOrReferencedDoc.add(item);
              }
            } else {
              childDocOrReferencedDoc.add(item);
            }
          }
          if(PermissionUtil.canRead(item) && item.isNodeType(Utils.EXO_SYMLINK)) {
            try {
              linkManager.getTarget(item);
            } catch (ItemNotFoundException ie) {
              childDocOrReferencedDoc.remove(item);
            } catch (Exception e) {
              childDocOrReferencedDoc.remove(item);
            }
          }
        } else {
          if (isCategories(item)) subCategories.add(item.getPath());
        }
      }
    }
    if (isShowReferenced) childDocOrReferencedDoc.addAll(getReferences(repositoryService, subCat,
        false, childDocOrReferencedDoc.size(), documentTemplates));
    childMap.put("sub", subCategories);
    childMap.put("doc", childDocOrReferencedDoc);
    return childMap;
  }

  private List<String> getHistory(List<String> documentTemplates, Node parentNode) throws Exception {
    List<String> historyList = new ArrayList<String>();
    NodeIterator iter = parentNode.getNodes();
    while(iter.hasNext()) {
      Node node = iter.nextNode();
      String nt = node.getPrimaryNodeType().getName();
      if(!documentTemplates.contains(nt)) historyList.add(node.getPath());
    }
    return historyList;
  }
  private List<Node> getReferences(RepositoryService repositoryService, Node node, boolean isShowAll,
      int size, List<?> templates) throws Exception {
    List<Node> refDocuments = new ArrayList<Node>();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    SessionProvider provider = null;
    if (WCMCoreUtils.isAnonim()) {
      provider = getSystemProvider();
    } else {
      provider = getSessionProvider();
    }
    if (isEnableRefDocument() && isReferenceableNode(node)) {
      String uuid = node.getUUID();
      String[] workspaces = manageableRepository.getWorkspaceNames();
      int itemCounter = getRowPerBlock() - size;
      if (isShowAll) itemCounter = getItemPerPage() - size;
      for (String workspace : workspaces) {
        Session session = provider.getSession(workspace,manageableRepository);
        try {
          Node taxonomyNode = session.getNodeByUUID(uuid);
          PropertyIterator iter = taxonomyNode.getReferences();
          while (iter.hasNext() && (refDocuments.size() < itemCounter)) {
            Node refNode = iter.nextProperty().getParent();
            if (templates.contains(refNode.getPrimaryNodeType().getName())) {
              if (isAllowPublish()) {
                PublicationService publicationService = getApplicationComponent(PublicationService.class);
                Node nodecheck = publicationService.getNodePublish(refNode, null);
                if (nodecheck != null) {
                  refDocuments.add(refNode);
                }
              } else {
                refDocuments.add(refNode);
              }
            }
          }
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
      }
    }
    return refDocuments;
  }

  private RepositoryService getRepositoryService() {
    return getApplicationComponent(RepositoryService.class);
  }

  private boolean isReferenceableNode(Node node) throws Exception {
    NodeType[] nodeTypes = node.getMixinNodeTypes();
    for(NodeType type : nodeTypes) {
      if(type.getName().equals(Utils.MIX_REFERENCEABLE)) return true;
    }
    return false;
  }

  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance();
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
                          portletRequestContext.getRequest().getServerName() + ":" +
                          String.format("%s",portletRequestContext.getRequest().getServerPort());
    return prefixWebDAV;
  }

  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName() ;
  }

  static public class BackActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      UIBrowseContainer uiContainer = event.getSource();
      TemplateService templateService  = uiContainer.getApplicationComponent(TemplateService.class);
      List<String> templates = templateService.getDocumentTemplates();
      Node historyNode = null;
      historyNode = uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT);
      if (historyNode == null) historyNode = uiContainer.getCurrentNode();
      if (uiContainer.listHistoryNode.size() > 0) {
        Node nodeTemp1 = uiContainer.listHistoryNode.get(uiContainer.listHistoryNode.size() - 1);
        if (uiContainer.listHistoryNode.size() > 1) {
          Node nodeTemp2 = uiContainer.listHistoryNode.get(uiContainer.listHistoryNode.size() - 2);
          try {
            Node nodeTemp2Parent = nodeTemp2.getParent();
            if (!historyNode.getPath().equals(nodeTemp2.getPath()) && historyNode.getPath().equals(nodeTemp2Parent.getPath()))
              uiContainer.listHistoryNode.remove(nodeTemp2);
          } catch (ItemNotFoundException ex) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(ex.getMessage());
            }
          }
        }
        uiContainer.listHistoryNode.remove(nodeTemp1);
      }
      if ((historyNode != null) && historyNode.isNodeType(Utils.EXO_SYMLINK)) {
        LinkManager linkManager = uiContainer.getApplicationComponent(LinkManager.class);
        historyNode = linkManager.getTarget(historyNode);
      }
      ManageViewService vservice = uiContainer.getApplicationComponent(ManageViewService.class);
      if(uiContainer.isShowDocumentByTag() && uiContainer.isShowDocumentDetail()) {
        UIDocumentDetail uiDocumentDetail = uiContainer.getChild(UIDocumentDetail.class);
        uiContainer.setShowDocumentDetail(false);
        uiDocumentDetail.setRendered(false);
        if(uiContainer.getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
          uiContainer.setCurrentNodePath(uiContainer.categoryPath_);
          uiContainer.setSelectedTabPath(uiContainer.categoryPath_);
        }
      } else if(uiContainer.isShowDocumentDetail() && historyNode != null &&
          templates.contains(historyNode.getPrimaryNodeType().getName())) {
        uiContainer.setShowDocumentByTag(false);
        UIDocumentDetail uiDocumentDetail = uiContainer.getChild(UIDocumentDetail.class);
        uiContainer.setShowDocumentDetail(true);
        uiDocumentDetail.setRendered(true);
        String detailTemplateName = uiContainer.getPortletPreferences().getValue(Utils.CB_BOX_TEMPLATE, "");
        uiContainer.setTemplateDetail(vservice.getTemplateHome(BasePath.CB_DETAIL_VIEW_TEMPLATES,
                                                               SessionProviderFactory.createSystemProvider())
                                              .getNode(detailTemplateName)
                                              .getPath());
        if (uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT) == null) {
          uiContainer.changeNode(historyNode.getParent());
          uiContainer.setCurrentNodePath(historyNode.getParent().getPath());
          uiContainer.setSelectedTabPath(historyNode.getParent().getPath());
        } else {
          uiContainer.viewDocument(historyNode, true);
          uiContainer.setCurrentNodePath(uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT).getPath());
          uiContainer.setSelectedTabPath(uiContainer.getHistory().get(UIBrowseContainer.KEY_SELECTED).getPath());
        }
        uiContainer.getHistory().clear();
      } else if(uiContainer.isShowDocumentDetail() && historyNode == null) {
        uiContainer.setShowDocumentByTag(false);
        UIDocumentDetail uiDocumentDetail = uiContainer.getChild(UIDocumentDetail.class);
        uiContainer.setShowDocumentDetail(false);
        uiDocumentDetail.setRendered(false);
        if(uiContainer.getUseCase().equals(Utils.CB_USE_JCR_QUERY)) {
          String tempName = uiContainer.getPortletPreferences().getValue(Utils.CB_TEMPLATE, "");
          uiContainer.setTemplate(vservice.getTemplateHome(BasePath.CB_QUERY_TEMPLATES,
                                                           SessionProviderFactory.createSystemProvider())
                                          .getNode(tempName)
                                          .getPath());
          if(uiContainer.isShowCommentForm() || uiContainer.isShowVoteForm()) uiContainer.initToolBar(false, false, false);
          if(!uiContainer.isShowDocumentByTag()) uiContainer.setPageIterator(uiContainer.getNodeByQuery(-1));
          return;
        }
        if(uiContainer.getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
          uiContainer.setCurrentNodePath(uiContainer.categoryPath_);
          uiContainer.setSelectedTabPath(uiContainer.categoryPath_);
        }
      } else {
        uiContainer.setShowDocumentByTag(false);
        UIDocumentDetail uiDocumentDetail = uiContainer.getChild(UIDocumentDetail.class);
        uiContainer.setShowDocumentDetail(false);
        uiDocumentDetail.setRendered(false);
        if (uiContainer.getUseCase().equals(Utils.CB_USE_FROM_PATH) && historyNode != null) {
          if (uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT) == null) {
            uiContainer.setCurrentNodePath(historyNode.getParent().getPath());
            uiContainer.setSelectedTabPath(historyNode.getParent().getPath());
          } else {
            uiContainer.setCurrentNodePath(uiContainer.getHistory().get(UIBrowseContainer.KEY_CURRENT).getPath());
            uiContainer.setSelectedTabPath(uiContainer.getHistory().get(UIBrowseContainer.KEY_SELECTED).getPath());
          }
          if(uiContainer.getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
            uiContainer.setPageIterator(uiContainer.getSubDocumentList(historyNode));
          }
          uiContainer.getHistory().clear();
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class BackViewActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      String normalState = event.getRequestContext().getRequestParameter("normalState");
      if(normalState != null) {
        ActionResponse response = event.getRequestContext().getResponse();
        response.setWindowState(WindowState.NORMAL);
      }
      UIBrowseContainer uiContainer = event.getSource();
      if(uiContainer.isShowDocumentDetail()) {
        UIDocumentDetail uiDocumentDetail = uiContainer.getChild(UIDocumentDetail.class);
        uiContainer.setShowDocumentDetail(false);
        uiDocumentDetail.setRendered(false);
      }
      uiContainer.refreshContent();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class ChangeNodeActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      String useMaxState = event.getRequestContext().getRequestParameter("useMaxState");
      UIBrowseContainer uiContainer = event.getSource();
      UIApplication uiApp = uiContainer.getAncestorOfType(UIApplication.class);
      PortletRequest portletRequest = event.getRequestContext().getRequest();
      uiContainer.windowId_ = portletRequest.getWindowID()
          + portletRequest.getPortletSession().getId();
      if (useMaxState != null) {
        ActionResponse response = event.getRequestContext().getResponse();
        response.setWindowState(WindowState.MAXIMIZED);
        if (!uiContainer.windowState_.containsKey(uiContainer.windowId_)) {
          uiContainer.windowState_.put(uiContainer.windowId_, WindowState.MAXIMIZED);
        }
      }
      uiContainer.setShowDocumentDetail(false);
      uiContainer.setShowAllChildren(false);
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      String catPath = event.getRequestContext().getRequestParameter("category");
      String wsName = event.getRequestContext().getRequestParameter("workspace");
      Node selectNode = null;
      if (wsName != null) {
        selectNode = uiContainer.getNodeByPath(objectId, wsName);
        if (uiContainer.wsName_ == null)
          uiContainer.wsName_ = wsName;
      } else {
        selectNode = uiContainer.getNodeByPath(objectId);
      }
      if (selectNode == null) {
        uiApp.addMessage(new ApplicationMessage("UIBrowseContainer.msg.invalid-node", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
        return;
      }
      uiContainer.storeListHistory(selectNode);
      if (uiContainer.listHistoryNode.size() > 0) {
        int index=0;
        int indexMark=0;
        List<Node> listRemovedNode = new ArrayList<Node>();
        for (Node itemNode : uiContainer.listHistoryNode) {
          index++;
          if (!itemNode.getPath().equals(selectNode.getPath()) && selectNode.isNodeType(Utils.EXO_SYMLINK)) indexMark = index;
          if ((indexMark > 0) && (index > indexMark)) listRemovedNode.add(itemNode);
        }
        uiContainer.listHistoryNode.removeAll(listRemovedNode);
      }
      uiContainer.storeHistory();
      if (selectNode.isNodeType(Utils.EXO_SYMLINK)) {
        LinkManager linkManager = uiContainer.getApplicationComponent(LinkManager.class);
        try {
          selectNode = linkManager.getTarget(selectNode);
          if (Utils.isInTrash(selectNode)) {
            uiApp.addMessage(new ApplicationMessage("UIBrowseContainer.msg.symlinkNode-no-targetNode", null,
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
            return;
          }
        } catch (PathNotFoundException pathNotFoundException) {
          uiApp.addMessage(new ApplicationMessage("UIBrowseContainer.msg.symlinkNode-no-targetNode", null,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
          return;
        } catch (ItemNotFoundException itemNotFoundException) {
          uiApp.addMessage(new ApplicationMessage("UIBrowseContainer.msg.symlinkNode-no-targetNode", null,
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
          return;
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected error", e);
          }
        }
      }
      TemplateService templateService = uiContainer.getApplicationComponent(TemplateService.class);
      List<String> templates = templateService.getDocumentTemplates();
      if (templates.contains(selectNode.getPrimaryNodeType().getName())) {
        if (catPath != null) {
          if (uiContainer.getUseCase().equals(Utils.CB_USE_FROM_PATH)) {
            uiContainer.setCategoryPath(catPath);
            Node currentCat = uiContainer.getNodeByPath(catPath);
            uiContainer.setPageIterator(uiContainer.getSubDocumentList(currentCat));
          }
        }
        ManageViewService vservice = uiContainer.getApplicationComponent(ManageViewService.class);
        String detailTemplateName = uiContainer.getPortletPreferences().getValue(
            Utils.CB_BOX_TEMPLATE, "");
        uiContainer.setTemplateDetail(vservice.getTemplateHome(BasePath.CB_DETAIL_VIEW_TEMPLATES,
                                                               SessionProviderFactory.createSystemProvider())
                                              .getNode(detailTemplateName)
                                              .getPath());
        uiContainer.viewDocument(selectNode, true);
      } else {
        String templateType = uiContainer.getPortletPreferences().getValue(Utils.CB_USECASE, "");
        if ((templateType.equals(Utils.CB_USE_JCR_QUERY))
            || (templateType.equals(Utils.CB_SCRIPT_NAME))) {
          UIApplication app = uiContainer.getAncestorOfType(UIApplication.class);
          app
              .addMessage(new ApplicationMessage("UIBrowseContainer.msg.template-notsupported",
                  null));
          
        } else {
          uiContainer.changeNode(selectNode);
          uiContainer.setPageIterator(uiContainer.getSubDocumentList(selectNode));
        }
      }
      uiContainer.setCurrentNodePath(objectId);
      uiContainer.setSelectedTabPath(objectId);
      event.getRequestContext().addUIComponentToUpdateByAjax(
          uiContainer.getAncestorOfType(UIBrowseContentPortlet.class));
    }
  }

  static public class SelectActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      UIBrowseContainer uiContainer = event.getSource();
      UICategoryTree cateTree = uiContainer.getChild(UICategoryTree.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      Node node = uiContainer.getNodeByPath(path);
      if(node == null) {
        UIApplication app = uiContainer.getAncestorOfType(UIApplication.class);
        app.addMessage(new ApplicationMessage("UIBrowseContainer.msg.invalid-node", null));
        
        return;
      }
      uiContainer.setShowDocumentDetail(false);
      uiContainer.setShowDocumentByTag(false);
      uiContainer.setShowAllChildren(false);
      uiContainer.setSelectedTabPath(path);
      uiContainer.setCurrentNodePath(path);
      cateTree.buildTree(node.getPath());
      uiContainer.setPageIterator(uiContainer.getSubDocumentList(uiContainer.getCurrentNode()));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static  public class ChangePageActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      UIPortal uiPortal = Util.getUIPortal();
      String uri  = event.getRequestContext().getRequestParameter(OBJECTID);
      
      /**
      * TODO: the API for loading navigations was changed 
      * (replaced [PageNavigation, PageNode] by [UserNavigation, UserNode])
      * it's required to use setName() for nodes instead of setUri()
      * 
      * UIBrowseContainer class is useless in ECMS project now, 
      * so we've temporarily commented this method and we will refactor it later
      */
//      PageNodeEvent<UIPortal> pnevent;
//      pnevent = new PageNodeEvent<UIPortal>(uiPortal, PageNodeEvent.CHANGE_PAGE_NODE, uri);
//      uiPortal.broadcast(pnevent, Event.Phase.PROCESS);
      //uiPortal.getSelectedNode().setUri(uri);
    }
  }

  static  public class ShowPageActionListener extends EventListener<UIPageIterator> {
    public void execute(Event<UIPageIterator> event) throws Exception {
      UIPageIterator uiPageIterator = event.getSource();
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiPageIterator.setCurrentPage(page);
      if(uiPageIterator.getParent() == null) return;
      UIBrowseContainer uiBCContainer = uiPageIterator.getAncestorOfType(UIBrowseContainer.class);
      uiBCContainer.setShowPageAction(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiBCContainer);
    }
  }

  static public class ViewByTagActionListener extends EventListener<UIBrowseContainer> {
    public void execute(Event<UIBrowseContainer> event) throws Exception {
      UIBrowseContainer uiContainer = event.getSource();
      String tagPath = event.getRequestContext().getRequestParameter(OBJECTID);
      uiContainer.setShowDocumentByTag(true);
      uiContainer.setTagPath(tagPath);
      uiContainer.setPageIterator(uiContainer.getDocumentByTag());
      uiContainer.storeHistory();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
}
