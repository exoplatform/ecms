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
package org.exoplatform.services.wcm.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.search.QueryCriteria.DATE_RANGE_SELECTED;
import org.exoplatform.services.wcm.search.QueryCriteria.DatetimeRange;
import org.exoplatform.services.wcm.search.QueryCriteria.QueryProperty;
import org.exoplatform.services.wcm.utils.SQLQueryBuilder;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.LOGICAL;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.ORDERBY;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.PATH_TYPE;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.QueryTermHelper;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.pham@exoplatform.com
 * Oct 8, 2008
 */
public class SiteSearchServiceImpl implements SiteSearchService {

  /** The live portal manager service. */
  private LivePortalManagerService livePortalManagerService;

  /** The ecm template service. */
  private TemplateService templateService;

  /** The wcm configuration service. */
  private WCMConfigurationService configurationService;

  /** The jcr repository service. */
  private RepositoryService repositoryService;

  /** The exclude node types. */
  private CopyOnWriteArraySet<String> excludeNodeTypes = new CopyOnWriteArraySet<String>();

  /** The include node types. */
  private CopyOnWriteArraySet<String> includeNodeTypes = new CopyOnWriteArraySet<String>();

  /** The exclude mime types. */
  private CopyOnWriteArraySet<String> excludeMimeTypes = new CopyOnWriteArraySet<String>();

  /** The include mime types. */
  private CopyOnWriteArraySet<String> includeMimeTypes = new CopyOnWriteArraySet<String>();

  /**
   * Instantiates a new site search service impl.
   * 
   * @param portalManagerService the portal manager service
   * @param templateService the template service
   * @param configurationService the configuration service
   * @param repositoryService the repository service
   * @param initParams the init params
   * 
   * @throws Exception the exception
   */
  public SiteSearchServiceImpl(LivePortalManagerService portalManagerService, TemplateService templateService,
      WCMConfigurationService configurationService, RepositoryService repositoryService, InitParams initParams) throws Exception {
    this.livePortalManagerService = portalManagerService;
    this.templateService = templateService;
    this.repositoryService = repositoryService;
    this.configurationService = configurationService;        
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.SiteSearchService#addExcludeIncludeDataTypePlugin(org.exoplatform.services.wcm.search.ExcludeIncludeDataTypePlugin)
   */
  public void addExcludeIncludeDataTypePlugin(ExcludeIncludeDataTypePlugin plugin) {    
    excludeNodeTypes.addAll(plugin.getExcludeNodeTypes());
    excludeMimeTypes.addAll(plugin.getExcludeMimeTypes());
    includeMimeTypes.addAll(plugin.getIncludeMimeTypes());
    includeNodeTypes.addAll(plugin.getIncludeNodeTypes());    
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.SiteSearchService#searchSiteContents(org.exoplatform.services.wcm.search.QueryCriteria, org.exoplatform.services.jcr.ext.common.SessionProvider, int)
   */
  public WCMPaginatedQueryResult searchSiteContents(SessionProvider sessionProvider, QueryCriteria queryCriteria, int pageSize, boolean isSearchContent) throws Exception {
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    NodeLocation location = configurationService.getLivePortalsLocation(currentRepository.getConfiguration().getName());    
    Session session = sessionProvider.getSession(location.getWorkspace(),currentRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    long startTime = System.currentTimeMillis();
    QueryResult queryResult = searchSiteContent(queryCriteria, queryManager);  
    String suggestion = getSpellSuggestion(queryCriteria.getKeyword(),currentRepository);
    long queryTime = System.currentTimeMillis() - startTime;
    WCMPaginatedQueryResult paginatedQueryResult = null;
    if(queryResult.getNodes().getSize()>250) {
      paginatedQueryResult = new WCMPaginatedQueryResult( queryResult, queryCriteria, pageSize, isSearchContent); 
    }else {      
      paginatedQueryResult = new SmallPaginatedQueryResult(queryResult, queryCriteria, pageSize, isSearchContent);
    }       
    paginatedQueryResult.setQueryTime(queryTime);    
    paginatedQueryResult.setSpellSuggestion(suggestion);    
    return paginatedQueryResult;
  }  

  /**
   * Gets the spell suggestion.
   * 
   * @param checkingWord the checking word
   * @param manageableRepository the manageable repository
   * 
   * @return the spell suggestion
   * 
   * @throws Exception the exception
   */
  private String getSpellSuggestion(String checkingWord, ManageableRepository manageableRepository) throws Exception{
    //Retrieve spell suggestion in special way to avoid access denied exception  
    String suggestion = null;
    Session session = null;
    try{
      session = manageableRepository.getSystemSession(manageableRepository.getConfiguration().getDefaultWorkspaceName());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("SELECT rep:spellcheck() FROM nt:base WHERE jcr:path like '/' AND SPELLCHECK('" + checkingWord + "')", Query.SQL);
      RowIterator rows = query.execute().getRows();
      Value value = rows.nextRow().getValue("rep:spellcheck()");    
      if (value != null) {
        suggestion = value.getString();
      }
    }catch (Exception e) {
    }finally{
      if(session != null)
        session.logout();
    }   
    return suggestion;
  }

  /**
   * Search site content.
   * 
   * @param queryCriteria the query criteria
   * @param queryManager the query manager
   * 
   * @return the query result
   * 
   * @throws Exception the exception
   */
  private QueryResult searchSiteContent(QueryCriteria queryCriteria, QueryManager queryManager) throws Exception {
    SQLQueryBuilder queryBuilder = new SQLQueryBuilder();    
    mapQueryTypes(queryCriteria, queryBuilder);
    if(queryCriteria.isFulltextSearch()) {
      mapQueryPath(queryCriteria, queryBuilder);
      mapFulltextQueryTearm(queryCriteria, queryBuilder); 
    }else {
      searchByNodeName(queryCriteria,queryBuilder);
    }    
    mapCategoriesCondition(queryCriteria,queryBuilder);
    mapDatetimeRangeSelected(queryCriteria,queryBuilder);
    mapMetadataProperties(queryCriteria,queryBuilder);
    orderBy(queryCriteria, queryBuilder);
    String queryStatement = queryBuilder.createQueryStatement();
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    return query.execute();
  }

  /**
   * Map query path.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   * 
   * @throws Exception the exception
   */
  private void mapQueryPath(final QueryCriteria queryCriteria,final SQLQueryBuilder queryBuilder) throws Exception {
    queryBuilder.setQueryPath(getSitePath(queryCriteria), PATH_TYPE.DECENDANTS);
  }

  /**
   * Gets the site path.
   * 
   * @param queryCriteria the query criteria
   * 
   * @return the site path
   * 
   * @throws Exception the exception
   */
  private String getSitePath(final QueryCriteria queryCriteria) throws Exception {
    String siteName = queryCriteria.getSiteName();
    String sitePath = null;
    if (siteName != null) {
      sitePath = livePortalManagerService.getPortalPathByName(siteName);      
    } else {
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      sitePath = configurationService.getLivePortalsLocation(repository).getPath();
    }
    return sitePath;
  }

  /**
   * Map query term.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void mapFulltextQueryTearm(final QueryCriteria queryCriteria,
      final SQLQueryBuilder queryBuilder) {    
    String keyword = queryCriteria.getKeyword();
    if(keyword == null || keyword.length() == 0)
      return;
    QueryTermHelper queryTermHelper = new QueryTermHelper();    
    String queryTerm = null;
    keyword = keyword.replaceAll("'","''");
    if (keyword.contains("*") || keyword.contains("?") || keyword.contains("~")) {
      queryTerm = queryTermHelper.contains(keyword).buildTerm();
    } else {
      queryTerm = queryTermHelper.contains(keyword).allowFuzzySearch().buildTerm();
    }      
    String scope = queryCriteria.getFulltextSearchProperty();
    if(QueryCriteria.ALL_PROPERTY_SCOPE.equals(scope) || scope == null) {
      queryBuilder.contains(null, queryTerm, LOGICAL.NULL);
    }else {
      queryBuilder.contains(scope, queryTerm, LOGICAL.NULL);
    }                   
  }

  /**
   * Search by node name.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   * 
   * @throws Exception the exception
   */
  private void searchByNodeName(final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder) throws Exception {    
    queryBuilder.queryByNodeName(getSitePath(queryCriteria),queryCriteria.getKeyword());
  }      

  /**
   * Map datetime range selected.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void mapDatetimeRangeSelected(final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder) {
    DATE_RANGE_SELECTED selectedDateRange = queryCriteria.getDateRangeSelected();
    if(selectedDateRange == null) return;
    if(DATE_RANGE_SELECTED.CREATED == selectedDateRange) {
      DatetimeRange createdDateRange = queryCriteria.getCreatedDateRange();
      queryBuilder.betweenDates("exo:dateCreated",createdDateRange.getFromDate(),createdDateRange.getToDate(),LOGICAL.AND);
    }else if(DATE_RANGE_SELECTED.MODIFIDED == selectedDateRange){
      DatetimeRange modifiedDateRange = queryCriteria.getLastModifiedDateRange();
      queryBuilder.betweenDates("exo:dateModified",modifiedDateRange.getFromDate(),modifiedDateRange.getToDate(),LOGICAL.AND);
    }else if(DATE_RANGE_SELECTED.START_PUBLICATION == selectedDateRange) {
      throw new UnsupportedOperationException();
    }else if(DATE_RANGE_SELECTED.END_PUBLICATION == selectedDateRange) {
      throw new UnsupportedOperationException();
    }    
  }  

  /**
   * Map categories condition.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void mapCategoriesCondition(QueryCriteria queryCriteria, SQLQueryBuilder queryBuilder) {
    String[] categoryUUIDs = queryCriteria.getCategoryUUIDs();
    if(categoryUUIDs == null) return;
    queryBuilder.openGroup(LOGICAL.AND);
    queryBuilder.like("exo:category",categoryUUIDs[0],LOGICAL.NULL);
    if(categoryUUIDs.length>1) {
      for(int i=1; i<categoryUUIDs.length; i++) {
        queryBuilder.like("exo:category",categoryUUIDs[i],LOGICAL.OR);
      }
    }
    queryBuilder.closeGroup();
  }

  /**
   * Map metadata properties.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void mapMetadataProperties(final QueryCriteria queryCriteria, SQLQueryBuilder queryBuilder) {
    QueryProperty[] queryProperty = queryCriteria.getQueryMetadatas();
    if(queryProperty == null) return;
    queryBuilder.openGroup(LOGICAL.AND);
    queryBuilder.like(queryProperty[0].getName(),queryProperty[0].getName(),LOGICAL.NULL);
    if(queryProperty.length>1) {
      for(int i=1; i<queryProperty.length; i++) {
        queryBuilder.like(queryProperty[i].getName(),queryProperty[i].getName(),LOGICAL.OR);
      }
    }
    queryBuilder.closeGroup();
  }

  /**
   * Map query specific node types.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   * @param nodeTypeManager the node type manager
   * 
   * @throws Exception the exception
   */
  private void mapQuerySpecificNodeTypes( final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder, final NodeTypeManager nodeTypeManager) throws Exception {
    String[] contentTypes = queryCriteria.getContentTypes();
    NodeType fistType = nodeTypeManager.getNodeType(contentTypes[0]);    
    queryBuilder.openGroup(LOGICAL.AND);
    if (fistType.isMixin()) {
      queryBuilder.like("jcr:mixinTypes", contentTypes[0], LOGICAL.NULL);
    } else {
      queryBuilder.equal("jcr:primaryType", contentTypes[0], LOGICAL.NULL);
    }
    if(contentTypes.length>1) {
      for (int i=1; i<contentTypes.length; i++) {
        String type = contentTypes[i];
        NodeType nodetype = nodeTypeManager.getNodeType(type);
        if (nodetype.isMixin()) {
          queryBuilder.like("jcr:mixinTypes", type, LOGICAL.OR);
        } else {
          queryBuilder.equal("jcr:primaryType", type, LOGICAL.OR);
        }
      } 
    }    
    queryBuilder.closeGroup();
    //Remove some specific mimtype    
    queryBuilder.openGroup(LOGICAL.AND_NOT);
    queryBuilder.like("jcr:mixinTypes", "exo:cssFile", LOGICAL.NULL);
    queryBuilder.like("jcr:mixinTypes","exo:jsFile",LOGICAL.OR);
    queryBuilder.closeGroup();
  }
  
  /**
   * Map query types.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   * 
   * @throws Exception the exception
   */
  private void mapQueryTypes(final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder) throws Exception {    
    queryBuilder.selectTypes(null);    
    // Searh on nt:base
    queryBuilder.fromNodeTypes(null);
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    NodeTypeManager manager = currentRepository.getNodeTypeManager();
    // Query all documents for specific content types
    String[] contentTypes = queryCriteria.getContentTypes();
    if(contentTypes != null && contentTypes.length>0 && queryCriteria.getKeyword() == null) {
      mapQuerySpecificNodeTypes(queryCriteria,queryBuilder,manager);
      return;
    }    
    List<String> selectedNodeTypes = templateService.getDocumentTemplates(currentRepository.getConfiguration().getName());    
    queryBuilder.openGroup(LOGICAL.AND);
    queryBuilder.equal("jcr:primaryType", "nt:resource", LOGICAL.NULL);
    // query on exo:rss-enable nodetypes for title, summary field
    queryBuilder.equal("jcr:mixinTypes", "exo:rss-enable", LOGICAL.OR);
    // query on metadata nodetype
    List<String> publicatioTypes = new ArrayList<String>(4);
    for (NodeTypeIterator iterator = manager.getAllNodeTypes(); iterator
    .hasNext();) {
      NodeType nodeType = iterator.nextNodeType();
      if (nodeType.isNodeType("publication:webpagesPublication")) {
        publicatioTypes.add(nodeType.getName());
        continue;
      }
      if (!nodeType.isNodeType("exo:metadata"))
        continue;
      if (nodeType.isMixin()) {
        queryBuilder.equal("jcr:mixinTypes", nodeType.getName(), LOGICAL.OR);
      } else {
        queryBuilder.equal("jcr:primaryType", nodeType.getName(), LOGICAL.OR);
      }
    }
    for (String type : selectedNodeTypes) {
      NodeType nodetype = manager.getNodeType(type);
      if (nodetype.isMixin()) {
        queryBuilder.like("jcr:mixinTypes", type, LOGICAL.OR);
      } else {
        queryBuilder.equal("jcr:primaryType", type, LOGICAL.OR);
      }
    }
    queryBuilder.closeGroup();    
    //unwanted document types: exo:cssFile, exo:jsFile
    if(excludeMimeTypes.size()<1) return;
    queryBuilder.openGroup(LOGICAL.AND_NOT);
    String[] mimetypes = excludeMimeTypes.toArray(new String[]{});      
    queryBuilder.equal("jcr:mimeType",mimetypes[0],LOGICAL.NULL);
    for(int i=1; i<mimetypes.length; i++) {
      queryBuilder.equal("jcr:mimeType",mimetypes[i],LOGICAL.OR);
    }       
    queryBuilder.closeGroup();
    //Unwanted document by mixin nodetypes
    queryBuilder.openGroup(LOGICAL.AND_NOT);
    queryBuilder.like("jcr:mixinTypes", "exo:cssFile", LOGICAL.NULL);
    queryBuilder.like("jcr:mixinTypes","exo:jsFile",LOGICAL.OR);
    queryBuilder.closeGroup();
  }

  /**
   * Order by.
   * 
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void orderBy(final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder) {
    queryBuilder.orderBy("jcr:score", ORDERBY.DESC);
  }  
}
