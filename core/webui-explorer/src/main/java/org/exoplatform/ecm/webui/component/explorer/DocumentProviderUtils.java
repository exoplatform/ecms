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
package org.exoplatform.ecm.webui.component.explorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.link.NodeLinkAware;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.base.LazyPageList;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.services.wcm.search.base.QueryData;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 9, 2009
 * 1:48:20 PM
 */
public class DocumentProviderUtils {

  private static final String   Contents_Document_Type             = "Content";
  
  private static final String FAVORITE_ALIAS = "userPrivateFavorites";
  
  private static final Log LOG  = ExoLogger.getLogger(DocumentProviderUtils.class.getName());  
  
  private static final String[] prohibitedSortType = {
                            NodetypeConstant.SORT_BY_NODESIZE,
                            NodetypeConstant.SORT_BY_NODETYPE, 
                            NodetypeConstant.SORT_BY_DATE, };
  
  private static DocumentProviderUtils docProviderUtil_ = new DocumentProviderUtils();
  private List<String> folderTypes_;
  
  private DocumentProviderUtils() {
  }
  
  public static DocumentProviderUtils getInstance() { return docProviderUtil_; }
  
  public boolean canSortType(String sortType) {
    for (String type : prohibitedSortType) {
      if (type.equals(sortType)) {
        return false;
      }
    }
    return true;
  }

  public LazyPageList<NodeLinkAware> getPageList(String ws, String path, Preference pref, 
                          Set<String> allItemFilter, Set<String> allItemByTypeFilter,
                          NodeLinkAware parent) throws Exception {
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    NodeFinder nodeFinder = WCMCoreUtils.getService(NodeFinder.class);
    String statement;
    try {
      Node node = (Node)nodeFinder.getItem(ws, path);
      if (linkManager.isLink(node)) {
        path = linkManager.getTarget(node).getPath();
      }else{
        path = node.getPath();
      }
      statement = getStatement(ws, path, pref, allItemFilter, allItemByTypeFilter);
    } catch (Exception e) {
      statement = null;
    }
    QueryData queryData = new QueryData(statement, ws, Query.SQL, 
                                        WCMCoreUtils.getRemoteUser().equals(WCMCoreUtils.getSuperUser()));
    return PageListFactory.createLazyPageList(queryData, pref.getNodesPerPage(), new NodeLinkAwareCreator(parent));
  }
  
  private String getStatement(String ws, String path, Preference pref, 
                             Set<String> allItemsFilterSet, Set<String> allItemsByTypeFilter) 
                                 throws Exception {
    StringBuilder buf = new StringBuilder();
    //path
    buf = addPathParam(buf, path);
    //jcrEnable
    buf = addJcrEnableParam(buf, ws, path, pref);
    //show non document
    buf = addShowNonDocumentType(buf, pref, allItemsByTypeFilter);
    //show hidden node
    buf = addShowHiddenNodeParam(buf, pref);
    //owned by me
    buf = addOwnedByMeParam(buf, allItemsFilterSet); 
    //favorite
    buf = addFavoriteParam(buf, ws, allItemsFilterSet);
    //all items by type
    buf = addAllItemByType(buf, allItemsByTypeFilter);
    //sort
    buf = addSortParam(buf, pref);
    return (buf == null) ? null : buf.toString();
  }
  
  /**
   * add path condition to query statement
   */
  private StringBuilder addPathParam(StringBuilder buf, String path) {
    buf.append("SELECT * FROM nt:base ");
    if (path != null) {
      if (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      buf.append("WHERE jcr:path LIKE '").append(path)
        .append("/%' AND NOT jcr:path LIKE '").append(path).append("/%/%' ");
    }
    return buf;
  }

  /**
   * add is_jcr_enable condition to query statement
   */
  private StringBuilder addJcrEnableParam(StringBuilder buf, String ws, String path, Preference pref) 
      throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    SessionProvider provider = WCMCoreUtils.getUserSessionProvider();
    Session session = provider.getSession(ws, WCMCoreUtils.getRepository());
    Node node = (Node)session.getItem(path);
    if(!pref.isJcrEnable() &&
        templateService.isManagedNodeType(node.getPrimaryNodeType().getName()) && 
        !(node.isNodeType(NodetypeConstant.NT_FOLDER) || node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED) )) {
      return null;
    }
    return buf;
  }
  
  /**
   * add show_non_document_type condition to query statement 
   */
  private StringBuilder addShowNonDocumentType(StringBuilder buf, Preference pref, Set<String> allItemsByTypeFilter) 
      throws Exception {
    if (buf == null) return null;
    if (!pref.isShowNonDocumentType() || allItemsByTypeFilter.contains(Contents_Document_Type)) {
      if (folderTypes_ == null) {
        folderTypes_ = getFolderTypes();
      }
      buf.append(" AND (");
      //nt:unstructured && nt:folder
      buf.append("( jcr:primaryType='").append(Utils.NT_UNSTRUCTURED)
      .append("') OR (exo:primaryType='").append(Utils.NT_UNSTRUCTURED).append("') ");
      buf.append(" OR ( jcr:primaryType='").append(Utils.NT_FOLDER)
      .append("') OR (exo:primaryType='").append(Utils.NT_FOLDER).append("') ");
      //supertype of nt:unstructured or nt:folder
      for (String fType : folderTypes_) {
        buf.append(" OR ( jcr:primaryType='").append(fType)
           .append("') OR (exo:primaryType='").append(fType).append("') ");
      }
      //all document type
      TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      List<String> docTypes = templateService.getDocumentTemplates();
      for (String docType : docTypes) {
        buf.append(" OR ( jcr:primaryType='").append(docType)
           .append("') OR (exo:primaryType='").append(docType).append("') ");
      }
      buf.append(" ) ");
    }
    return buf;
  }

  private List<String> getFolderTypes() {
    List<String> ret = new ArrayList<String>();
    NodeTypeManager nodeTypeManager = WCMCoreUtils.getRepository().getNodeTypeManager();
    try {
      for (NodeTypeIterator iter = nodeTypeManager.getAllNodeTypes(); iter.hasNext();) {
        NodeType type = iter.nextNodeType();
        if (type.isNodeType(NodetypeConstant.NT_FOLDER) || type.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)) {
          ret.add(type.getName());
        }
      }
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not get all node types", e.getMessage());
      }
    }
    return ret;
  }
  
  /**
   * add show_hidden_node condition to query statement 
   */
  private StringBuilder addShowHiddenNodeParam(StringBuilder buf, Preference pref) {
    if (buf == null) return null;
    if (!pref.isShowHiddenNode()) {
      buf.append(" AND ( NOT jcr:mixinTypes='").append(NodetypeConstant.EXO_HIDDENABLE).append("')");
    }
    return buf;
  }
  
  /**
   * add owned_by_me condition to query statement 
   */
  private StringBuilder addOwnedByMeParam(StringBuilder buf, Set<String> allItemsFilterSet) {
    if (buf == null) return null;
    if (allItemsFilterSet.contains(NodetypeConstant.OWNED_BY_ME)) {
      buf.append(" AND ( exo:owner='")
         .append(ConversationState.getCurrent().getIdentity().getUserId())
         .append("')");
    }
    return buf;
  }
  
  /**
   * add favorite condition to query statement 
   * @throws Exception 
   */
  private StringBuilder addFavoriteParam(StringBuilder buf, String ws, Set<String> allItemsFilterSet) 
      throws Exception {
    if (buf == null) return null;
    if (allItemsFilterSet.contains(NodetypeConstant.FAVORITE)) {
      NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      Node userNode =
          nodeHierarchyCreator.getUserNode(WCMCoreUtils.getUserSessionProvider(), 
                                           ConversationState.getCurrent().getIdentity().getUserId());
      String favoritePath = nodeHierarchyCreator.getJcrPath(FAVORITE_ALIAS);
      int count = 0;
      buf.append(" AND (");
      for (NodeIterator iter = userNode.getNode(favoritePath).getNodes();iter.hasNext();) {
        Node node = iter.nextNode();
        if (node.isNodeType(NodetypeConstant.EXO_SYMLINK) &&
            node.hasProperty(NodetypeConstant.EXO_WORKSPACE) &&
            ws.equals(node.getProperty(NodetypeConstant.EXO_WORKSPACE).getString())) {
          if (count ++ > 0) {
            buf.append(" OR ");
          }
          buf.append(" jcr:uuid='")
             .append(node.getProperty("exo:uuid").getString())
             .append("'");
        }
      }
      buf.append(" ) ");
      if (count == 0) {
        return null;
      } 
    }
    return buf;
  }
  
  /**
   * add mimetype condition to query statement 
   */
  private StringBuilder addAllItemByType(StringBuilder buf, Set<String> allItemsByTypeFilterSet) {
    if(allItemsByTypeFilterSet.isEmpty()) {
      return buf;
    }
    DocumentTypeService documentTypeService = WCMCoreUtils.getService(DocumentTypeService.class);
    StringBuilder buf1 = new StringBuilder(" AND (");
    int count = 0;
    for (String documentType : allItemsByTypeFilterSet) {
      for (String mimeType : documentTypeService.getMimeTypes(documentType)) {
        if (count++ > 0) {
          buf1.append(" OR ");
        }
        if (mimeType.endsWith("/")) { mimeType = mimeType.substring(0, mimeType.length() - 1); }
        buf1.append(" 'jcr:content/jcr:mimeType' like '").append(mimeType).append("/%'");
      }
    }
    buf1.append(" )");
    if (count > 0) {
      buf.append(buf1);
    }
    return buf;
  }

  /**
   * adds 'sort by' condition to query statement
   */
  private StringBuilder addSortParam(StringBuilder buf, Preference pref) {
    if (buf == null) return null;
    String type = "";
    if (NodetypeConstant.SORT_BY_NODENAME.equals(pref.getSortType())) { type="exo:name"; } 
    else if (NodetypeConstant.SORT_BY_CREATED_DATE.equals(pref.getSortType())) { 
      type = NodetypeConstant.EXO_DATE_CREATED;
    } else if (NodetypeConstant.SORT_BY_MODIFIED_DATE.equals(pref.getSortType())) {
      type = NodetypeConstant.EXO_LAST_MODIFIED_DATE;
    } else { type= pref.getSortType(); }
    buf.append(" ORDER BY ").append(type).append(" ");
    buf.append("Ascending".equals(pref.getOrder()) ? "ASC" : "DESC");
    return buf;
  }
  
  /**
   * Simple data creator, just creates the node result itself
   */
  public class NodeLinkAwareCreator implements SearchDataCreator<NodeLinkAware> {

    private NodeLinkAware parent;
    
    public NodeLinkAwareCreator(NodeLinkAware parent) {
      this.parent = parent;
    }
    
    @Override
    public NodeLinkAware createData(Node node, Row row) {
      try {
        return (NodeLinkAware)parent.getNode(StringUtils.substringAfterLast(node.getPath(), "/"));
      } catch (PathNotFoundException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Can not create NodeLinkAware ", e.getMessage());
        }
      } catch (RepositoryException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("Can not create NodeLinkAware ", e.getMessage());
        }
      }
      return null;
    }
  }
}
