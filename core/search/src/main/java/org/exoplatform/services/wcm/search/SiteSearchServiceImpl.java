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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.portlet.PortletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.search.QueryCriteria.DATE_RANGE_SELECTED;
import org.exoplatform.services.wcm.search.QueryCriteria.DatetimeRange;
import org.exoplatform.services.wcm.search.QueryCriteria.QueryProperty;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.ArrayNodePageList;
import org.exoplatform.services.wcm.search.base.NodeSearchFilter;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.search.connector.BaseSearchServiceConnector;
import org.exoplatform.services.wcm.utils.SQLQueryBuilder;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.COMPARISON_TYPE;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.LOGICAL;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.ORDERBY;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.PATH_TYPE;
import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.QueryTermHelper;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
/**
 * The SiteSearchService component is used in the Search portlet that allows users
 * to find all information matching with your given keyword.
 */
public class SiteSearchServiceImpl implements SiteSearchService {

  /** Allow administrators to enable/disable the fuzzy search mechanism. */
  private static final String IS_ENABLED_FUZZY_SEARCH = "isEnabledFuzzySearch";

  /** Allow the approximate level between the input keyword and the found key results.
   * In case of the invalid configuration, the default value is set to 0.8. */
  private static final String FUZZY_SEARCH_INDEX = "fuzzySearchIndex";
  
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

  private boolean isEnabledFuzzySearch = true;
  
  private double fuzzySearchIndex = 0.8;
  
  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(SiteSearchServiceImpl.class.getName());

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
  public SiteSearchServiceImpl(LivePortalManagerService portalManagerService,
                               TemplateService templateService,
                               WCMConfigurationService configurationService,
                               RepositoryService repositoryService,
                               InitParams initParams) throws Exception {
    this.livePortalManagerService = portalManagerService;
    this.templateService = templateService;
    this.repositoryService = repositoryService;
    this.configurationService = configurationService;
    if (initParams != null) {
      ValueParam isEnabledFuzzySearchValue = initParams.getValueParam(IS_ENABLED_FUZZY_SEARCH);
      if (isEnabledFuzzySearchValue != null)
        isEnabledFuzzySearch = Boolean.parseBoolean(isEnabledFuzzySearchValue.getValue());
      ValueParam enabledFuzzySearchValue = initParams.getValueParam(FUZZY_SEARCH_INDEX);
      if (enabledFuzzySearchValue != null) {
        try {
          fuzzySearchIndex = Double.parseDouble(enabledFuzzySearchValue.getValue());
        } catch (NumberFormatException e) {
//          log.warn("The current fuzzySearchIndex value is not a number, default value 0.8 will be used");
          fuzzySearchIndex = 0.8;
        }
      }
      if (fuzzySearchIndex < 0 || fuzzySearchIndex >= 1) {
//        log.warn("The current fuzzySearchIndex value is out of range from 0 to 1, default value 0.8 will be used");
        fuzzySearchIndex = 0.8;
      }
    }

  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.search.SiteSearchService#
   * addExcludeIncludeDataTypePlugin
   * (org.exoplatform.services.wcm.search.ExcludeIncludeDataTypePlugin)
   */
  public void addExcludeIncludeDataTypePlugin(ExcludeIncludeDataTypePlugin plugin) {
    excludeNodeTypes.addAll(plugin.getExcludeNodeTypes());
    excludeMimeTypes.addAll(plugin.getExcludeMimeTypes());
    includeMimeTypes.addAll(plugin.getIncludeMimeTypes());
    includeNodeTypes.addAll(plugin.getIncludeNodeTypes());
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.SiteSearchService#searchSiteContents
   * (org.exoplatform.services.wcm.search.QueryCriteria,
   * org.exoplatform.services.jcr.ext.common.SessionProvider, int)
   */
  public AbstractPageList<ResultNode> searchSiteContents(SessionProvider sessionProvider,
                                                    QueryCriteria queryCriteria,
                                                    int pageSize,
                                                    boolean isSearchContent) throws Exception {
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    NodeLocation location = configurationService.getLivePortalsLocation();
    Session session = sessionProvider.getSession(location.getWorkspace(),currentRepository);
    if (queryCriteria.isSearchWebpage()) {
      session = sessionProvider.getSession("portal-system", WCMCoreUtils.getRepository());
    }
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    long startTime = System.currentTimeMillis();
    Query query = createQuery(queryCriteria, queryManager);
    String suggestion = getSpellSuggestion(queryCriteria.getKeyword(),currentRepository);
    AbstractPageList<ResultNode> pageList = null;
    if (LOG.isDebugEnabled()) {
      LOG.debug("execute query: " + query.getStatement().toLowerCase());
    }
    pageList = PageListFactory.createPageList(query.getStatement(),
                                              session.getWorkspace().getName(),
                                              query.getLanguage(),
                                              IdentityConstants.SYSTEM.equals(session.getUserID()),
                                              new NodeFilter(isSearchContent, queryCriteria),
                                              new DataCreator(),
                                              pageSize,
                                              (int)queryCriteria.getLimit(), queryCriteria);
    
    long queryTime = System.currentTimeMillis() - startTime;
    pageList.setQueryTime(queryTime);
    pageList.setSpellSuggestion(suggestion);
    return pageList;
  }
  
  /**
   * 
   */
  public AbstractPageList<ResultNode> searchPageContents(SessionProvider sessionProvider,
                                                      QueryCriteria queryCriteria,
                                                      int pageSize,
                                                      boolean isSearchContent) throws Exception {
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession("portal-system", currentRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    long startTime = System.currentTimeMillis();
    Query query = createSearchPageQuery(queryCriteria, queryManager);
    if (query == null) {
      return new ArrayNodePageList<ResultNode>(pageSize);
    }
    String suggestion = getSpellSuggestion(queryCriteria.getKeyword(), currentRepository);
    if (LOG.isDebugEnabled()) {
      LOG.debug("execute query: " + query.getStatement().toLowerCase());
    }
    AbstractPageList<ResultNode> pageList = PageListFactory.createPageList(query.getStatement(),
                                                                           session.getWorkspace()
                                                                                  .getName(),
                                                                           query.getLanguage(),
                                                                           true,
                                                                           new PageNodeFilter(),
                                                                           new PageDataCreator(),
                                                                           pageSize,
                                                                           0);

    long queryTime = System.currentTimeMillis() - startTime;
    pageList.setQueryTime(queryTime);
    pageList.setSpellSuggestion(suggestion);
    return pageList;
  }
  
  private Query createSearchPageQuery(QueryCriteria queryCriteria, QueryManager queryManager) throws Exception {
    SQLQueryBuilder queryBuilder = new SQLQueryBuilder();
    List<String> mopPages = this.searchPageByTitle(queryCriteria.getSiteName(),
                                                   queryCriteria.getKeyword());
    if (mopPages.size() == 0) {
      return null;
    }
    List<QueryProperty> queryProps = new ArrayList<QueryCriteria.QueryProperty>();
    for (String page : mopPages) {
      QueryProperty prop = queryCriteria.new QueryProperty();
      prop.setName("mop:page");
      prop.setValue(page);
      prop.setComparisonType(COMPARISON_TYPE.EQUAL);
      queryProps.add(prop);
    }
    QueryProperty prop = queryCriteria.new QueryProperty();
    prop.setName("exo:name");
    prop.setValue("mop:" + queryCriteria.getKeyword().toLowerCase());
    queryProps.add(prop);    
    queryCriteria.setQueryMetadatas(queryProps.toArray(new QueryProperty[queryProps.size()]));
    mapQueryTypes(queryCriteria, queryBuilder);
    if (queryCriteria.isFulltextSearch()) {
      mapQueryPath(queryCriteria, queryBuilder);
      mapFulltextQueryTearm(queryCriteria, queryBuilder, LOGICAL.OR);
    } else {
      searchByNodeName(queryCriteria, queryBuilder);
    }
    mapCategoriesCondition(queryCriteria,queryBuilder);
    mapDatetimeRangeSelected(queryCriteria,queryBuilder);
    mapMetadataProperties(queryCriteria,queryBuilder, LOGICAL.OR);
    orderBy(queryCriteria, queryBuilder);
    String queryStatement = queryBuilder.createQueryStatement();
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    return query;
  }

  /**
   * @return return the JCR path of the mop:page nodes that have gtn:name
   *         (page's title) containing the given specified <code>keyword</code>
   * @throws Exception
   */
  private List<String> searchPageByTitle(String siteName, String keyword) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession("portal-system", currentRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    QueryCriteria queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(siteName);
    queryCriteria.setKeyword(keyword);
    queryCriteria.setSearchWebpage(true);
    Query query = createSearchPageByTitleQuery(queryCriteria, queryManager);
    if (LOG.isDebugEnabled()) {
      LOG.debug("execute query: " + query.getStatement().toLowerCase());
    }
    List<String> pageList = PageListFactory.createPageList(query.getStatement(),
                                                           session.getWorkspace().getName(),
                                                           query.getLanguage(),
                                                           true,
                                                           new PageTitleDataCreator());
    return pageList;
  }
  
  /**
   * 
   * @param queryCriteria
   * @param queryManager
   * @return
   * @throws Exception
   */
  private Query createSearchPageByTitleQuery(QueryCriteria queryCriteria, QueryManager queryManager) throws Exception {
    SQLQueryBuilder queryBuilder = new SQLQueryBuilder();

    // select *
    queryBuilder.selectTypes(null);

    // from mop:page node type
    queryBuilder.fromNodeTypes(new String[] { "mop:page" });

    mapQueryPath(queryCriteria, queryBuilder);

    queryCriteria.setFulltextSearchProperty(new String[] {"gtn:name"});

    mapFulltextQueryTearm(queryCriteria, queryBuilder, LOGICAL.OR);

    String queryStatement = queryBuilder.createQueryStatement();
    Query query = queryManager.createQuery(queryStatement, Query.SQL);

    return query;
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
  private String getSpellSuggestion(String checkingWord, ManageableRepository manageableRepository) throws Exception {
    //Retrieve spell suggestion in special way to avoid access denied exception
    String suggestion = null;
    Session session = null;
    try{
      session = manageableRepository.getSystemSession(manageableRepository.getConfiguration().getDefaultWorkspaceName());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("SELECT rep:spellcheck() FROM nt:base WHERE jcr:path like '/' AND SPELLCHECK('"
                                                 + checkingWord + "')",
                                             Query.SQL);
      RowIterator rows = query.execute().getRows();
      Value value = rows.nextRow().getValue("rep:spellcheck()");
      if (value != null) {
        suggestion = value.getString();
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    } finally {
      if (session != null)
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
  private Query createQuery(QueryCriteria queryCriteria, QueryManager queryManager) throws Exception {
    SQLQueryBuilder queryBuilder = new SQLQueryBuilder();
    mapQueryTypes(queryCriteria, queryBuilder);
    if (queryCriteria.isFulltextSearch()) {
      mapQueryPath(queryCriteria, queryBuilder);
      mapFulltextQueryTearm(queryCriteria, queryBuilder, LOGICAL.OR);
    } else {
      searchByNodeName(queryCriteria, queryBuilder);
    }
    mapCategoriesCondition(queryCriteria,queryBuilder);
    mapDatetimeRangeSelected(queryCriteria, queryBuilder);
    mapMetadataProperties(queryCriteria, queryBuilder, LOGICAL.AND);
    orderBy(queryCriteria, queryBuilder);
    String queryStatement = queryBuilder.createQueryStatement();
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
//    System.out.println(queryStatement);
    return query;
  }

  /**
   * Map query path.
   *
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   *
   * @throws Exception the exception
   */
  private void mapQueryPath(final QueryCriteria queryCriteria, final SQLQueryBuilder queryBuilder) throws Exception {
    queryBuilder.setQueryPath(getPath(queryCriteria), PATH_TYPE.DECENDANTS);
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
  private String getPath(final QueryCriteria queryCriteria) throws Exception {
    String siteName = queryCriteria.getSiteName();
    //search page path
    if (queryCriteria.isSearchWebpage()) {
      if ("all".equals(siteName) || siteName == null || siteName.trim().length() == 0) {
        return PATH_PORTAL_SITES;
      }
      return PATH_PORTAL_SITES.concat("/mop:").concat(siteName);
    }
    //search document path
    if (queryCriteria.getSearchPath() != null) {
      return queryCriteria.getSearchPath();
    }
    String sitePath = null;
    if (siteName != null) {
      sitePath = livePortalManagerService.getPortalPathByName(siteName);
    } else {
      sitePath = configurationService.getLivePortalsLocation().getPath();
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
                                     final SQLQueryBuilder queryBuilder, LOGICAL condition) {
    String keyword = queryCriteria.getKeyword();
    if (keyword == null || keyword.length() == 0)
      return;
    QueryTermHelper queryTermHelper = new QueryTermHelper();
    String queryTerm = null;
    if (isEnabledFuzzySearch) {
      if (keyword.contains("*") || keyword.contains("?") || keyword.contains("~") || keyword.contains("\"")) {
        queryTerm = queryTermHelper.contains(keyword).buildTerm();
      } else if(queryCriteria.isFuzzySearch()) {
        queryTerm = queryTermHelper.contains(keyword).allowFuzzySearch(fuzzySearchIndex).buildTerm();
      } else {
        queryTerm = queryTermHelper.contains(keyword).buildTerm();
      }
    } else {
      if(!queryCriteria.isFuzzySearch()) {
        keyword = keyword.replace("~", "\\~");
        keyword = keyword.replace("*", "\\*");
        keyword = keyword.replace("?", "\\?");
      }
      queryTerm = queryTermHelper.contains(keyword).buildTerm();
    }
    String[] props = queryCriteria.getFulltextSearchProperty();
    if (props == null || props.length == 0 || QueryCriteria.ALL_PROPERTY_SCOPE.equals(props[0])) {
      queryBuilder.contains(null, queryTerm, LOGICAL.NULL);
    } else {
      queryBuilder.contains(props[0], queryTerm, LOGICAL.NULL);
      for (int i = 1; i < props.length; i++) {
        queryBuilder.contains(props[i], queryTerm, condition);
      }
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
  private void searchByNodeName(final QueryCriteria queryCriteria,
                                final SQLQueryBuilder queryBuilder) throws Exception {
    queryBuilder.queryByNodeName(getPath(queryCriteria), queryCriteria.getKeyword());
  }

  /**
   * Map datetime range selected.
   *
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void mapDatetimeRangeSelected(final QueryCriteria queryCriteria,
                                        final SQLQueryBuilder queryBuilder) {
    DATE_RANGE_SELECTED selectedDateRange = queryCriteria.getDateRangeSelected();
    if (selectedDateRange == null)
      return;
    if (DATE_RANGE_SELECTED.CREATED == selectedDateRange) {
      DatetimeRange createdDateRange = queryCriteria.getCreatedDateRange();
      queryBuilder.betweenDates("exo:dateCreated",
                                createdDateRange.getFromDate(),
                                createdDateRange.getToDate(),
                                LOGICAL.AND);
    } else if (DATE_RANGE_SELECTED.MODIFIDED == selectedDateRange) {
      DatetimeRange modifiedDateRange = queryCriteria.getLastModifiedDateRange();
      queryBuilder.betweenDates("exo:dateModified",
                                modifiedDateRange.getFromDate(),
                                modifiedDateRange.getToDate(),
                                LOGICAL.AND);
    } else if (DATE_RANGE_SELECTED.START_PUBLICATION == selectedDateRange) {
      throw new UnsupportedOperationException();
    } else if (DATE_RANGE_SELECTED.END_PUBLICATION == selectedDateRange) {
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
    if (categoryUUIDs == null)
      return;
    queryBuilder.openGroup(LOGICAL.AND);
    queryBuilder.like("exo:category", categoryUUIDs[0], LOGICAL.NULL);
    if (categoryUUIDs.length > 1) {
      for (int i = 1; i < categoryUUIDs.length; i++) {
        queryBuilder.like("exo:category", categoryUUIDs[i], LOGICAL.OR);
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
  private void mapMetadataProperties(final QueryCriteria queryCriteria, SQLQueryBuilder queryBuilder, LOGICAL condition) {
    QueryProperty[] queryProperty = queryCriteria.getQueryMetadatas();
    if (queryProperty == null || queryProperty.length == 0)
      return;
    queryBuilder.openGroup(condition);
    if (queryProperty[0].getComparisonType() == COMPARISON_TYPE.EQUAL) {
      queryBuilder.equal(queryProperty[0].getName(), queryProperty[0].getValue(), LOGICAL.NULL);
    } else {
      queryBuilder.like(queryProperty[0].getName(), queryProperty[0].getValue(), LOGICAL.NULL);
    }
    if (queryProperty.length > 1) {
      for (int i = 1; i < queryProperty.length; i++) {
        if (queryProperty[i].getComparisonType() == COMPARISON_TYPE.EQUAL) {
          queryBuilder.equal(queryProperty[i].getName(), queryProperty[i].getValue(), LOGICAL.OR);
        } else {
          queryBuilder.like(queryProperty[i].getName(), queryProperty[i].getValue(), LOGICAL.OR);
        }
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
  private void mapQuerySpecificNodeTypes(final QueryCriteria queryCriteria,
                                         final SQLQueryBuilder queryBuilder,
                                         final NodeTypeManager nodeTypeManager) throws Exception {
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
    queryBuilder.fromNodeTypes(queryCriteria.getNodeTypes());
    ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    NodeTypeManager manager = currentRepository.getNodeTypeManager();
    // Query all documents for specific content types
    String[] contentTypes = queryCriteria.getContentTypes();
    if ((contentTypes != null && contentTypes.length > 0 && queryCriteria.getKeyword() == null)
        || queryCriteria.isSearchWebpage()) {
      mapQuerySpecificNodeTypes(queryCriteria, queryBuilder, manager);
      return;
    }
    List<String> selectedNodeTypes =  
      (contentTypes != null && contentTypes.length > 0) ? Arrays.asList(contentTypes) :
                                                          templateService.getDocumentTemplates();
    queryBuilder.openGroup(LOGICAL.AND);
    if (selectedNodeTypes.contains("nt:file")) {
      queryBuilder.equal("jcr:primaryType", NodetypeConstant.NT_FILE, LOGICAL.NULL);
    } else {
      //searching only document, not file. In this case, search nt:resource with exo:webContentChild mixin
      queryBuilder.openGroup(null);
      queryBuilder.equal(NodetypeConstant.JCR_PRIMARY_TYPE, NodetypeConstant.NT_RESOURCE, LOGICAL.NULL);
      queryBuilder.equal(NodetypeConstant.JCR_MIXIN_TYPES, NodetypeConstant.EXO_WEBCONTENT_CHILD, LOGICAL.AND);
      queryBuilder.closeGroup();
    }
    // query on exo:rss-enable nodetypes for title, summary field
    //queryBuilder.equal("jcr:mixinTypes", "exo:rss-enable", LOGICAL.OR);
    for (String type : selectedNodeTypes) {
      NodeType nodetype = manager.getNodeType(type);
      if (nodetype.isMixin()) {
        if (selectedNodeTypes.contains("nt:file") || 
            !NodetypeConstant.EXO_CSS_FILE.equals(type) &&
            !NodetypeConstant.EXO_JS_FILE.equals(type) &&
            !NodetypeConstant.EXO_HTML_FILE.equals(type)) {
          queryBuilder.like("jcr:mixinTypes", type, LOGICAL.OR);
        } else {
          //searching only document, not file. In this case, search nt:resource with exo:webContentChild mixin
          queryBuilder.openGroup(LOGICAL.OR);
          queryBuilder.equal(NodetypeConstant.JCR_MIXIN_TYPES, type, LOGICAL.NULL);
          queryBuilder.equal(NodetypeConstant.JCR_MIXIN_TYPES, NodetypeConstant.EXO_WEBCONTENT_CHILD, LOGICAL.AND);
          queryBuilder.closeGroup();
        }
      } else {
        if(!type.equals(NodetypeConstant.NT_FILE)) {
          queryBuilder.equal("jcr:primaryType", type, LOGICAL.OR);
        }
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

    queryBuilder.openGroup(LOGICAL.AND_NOT);
    String[] _excludeNodeTypes = excludeNodeTypes.toArray(new String[]{});
    for(int i=0; i < _excludeNodeTypes.length; i++) {
      if(i==0) {
        queryBuilder.equal("jcr:mixinTypes", _excludeNodeTypes[i], LOGICAL.NULL);
      } else {
        queryBuilder.equal("jcr:mixinTypes", _excludeNodeTypes[i], LOGICAL.OR);
      }
    }
    queryBuilder.closeGroup();

  }

  /**
   * Order by.
   *
   * @param queryCriteria the query criteria
   * @param queryBuilder the query builder
   */
  private void orderBy(final QueryCriteria criteria, final SQLQueryBuilder queryBuilder) {
    String sortBy = "jcr:score";
    String orderBy = "desc";
    //sort by
    if (BaseSearchServiceConnector.sortByTitle.equals(criteria.getSortBy())) {
      sortBy = NodetypeConstant.EXO_TITLE;
    } else if (BaseSearchServiceConnector.sortByDate.equals(criteria.getSortBy())) {
      sortBy = NodetypeConstant.EXO_LAST_MODIFIED_DATE;
    }
    if (StringUtils.isNotBlank(criteria.getOrderBy())) {
      orderBy = criteria.getOrderBy();
    }
    queryBuilder.orderBy(sortBy, "desc".equals(orderBy) ? ORDERBY.DESC : ORDERBY.ASC);
  }
  
  public static class NodeFilter implements NodeSearchFilter {

    private boolean isSearchContent;
    private QueryCriteria queryCriteria;
    private TrashService trashService = WCMCoreUtils.getService(TrashService.class);

    public NodeFilter(boolean isSearchContent, QueryCriteria queryCriteria) {
      this.isSearchContent = isSearchContent;
      this.queryCriteria = queryCriteria;
    }
    
    @Override
    public Node filterNodeToDisplay(Node node) {
      try {
        if(trashService.isInTrash(node)) return null;
        Node displayNode = getNodeToCheckState(node);
        if(displayNode == null) return null;
        if (isSearchContent) return displayNode;
        NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(displayNode);
        WCMComposer wcmComposer = WCMCoreUtils.getService(WCMComposer.class);
        HashMap<String, String> filters = new HashMap<String, String>();
        filters.put(WCMComposer.FILTER_MODE, queryCriteria.isLiveMode() ? WCMComposer.MODE_LIVE
                                                                       : WCMComposer.MODE_EDIT);
        return wcmComposer.getContent(nodeLocation.getWorkspace(),
                                                                  nodeLocation.getPath(),
                                                                  filters,
                                                                  WCMCoreUtils.getSystemSessionProvider());
      } catch (Exception e) {
        return null;
      }
    }
    
    protected Node getNodeToCheckState(Node node)throws Exception{
      Node displayNode = node;
      if (displayNode.isNodeType("nt:resource")) {
        displayNode = node.getParent();
      }
      //return exo:webContent when exo:htmlFile found
      if (displayNode.isNodeType("exo:htmlFile")) {
        Node parent = displayNode.getParent();
        if (parent.isNodeType("exo:webContent")) return parent;
        return displayNode;
      }
     String[] contentTypes = queryCriteria.getContentTypes();
      for (String contentType : contentTypes) {
        if(displayNode.isNodeType(contentType)) {
          return displayNode;
        }
      }     
      return null;
    }
    
  }
  
  /**
   * 
   * @author ha_dangviet
   *
   */
  public static class PageNodeFilter implements NodeSearchFilter {

    @Override
    public Node filterNodeToDisplay(Node node) {
      try {
        if (!node.isNodeType("mop:navigation") && !node.isNodeType("mop:pagelink")
            && !node.isNodeType("gtn:language")) {
          return null;
        } else {
          return node;
        }
      } catch (RepositoryException e) {
        return null;
      }
    }

  }
  
  public static class DataCreator implements SearchDataCreator<ResultNode> {

    @Override
    public ResultNode createData(Node node, Row row) {
      try {
        return new ResultNode(node, row);
      } catch (Exception e) {
        return null;
      }
    }
    
  }
  
  public static class PageDataCreator implements SearchDataCreator<ResultNode> {

    private static HashSet<String> userNavigationUriList;
    
    public PageDataCreator() {
      userNavigationUriList = new HashSet<String>();
    }
    
    @Override
    public ResultNode createData(Node node, Row row) {
      try {
//        PortalRequestContext portalRequestContext = Util.getPortalRequestContext();        
//        PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
//        PortletRequest portletRequest = portletRequestContext.getRequest();
        
//        StringBuffer baseURI = new StringBuffer();
//        baseURI.append(portletRequest.getScheme()).append("://").append(portletRequest.getServerName());
//        if (portletRequest.getServerPort() != 80) {
//          baseURI.append(":").append(String.format("%s", portletRequest.getServerPort()));
//        }
//        baseURI.append(portalRequestContext.getPortalContextPath());
        if (node.isNodeType("mop:pagelink")) {
          node = node.getParent();
        }
        
        if (node.isNodeType("gtn:language")) {
          node = node.getParent().getParent();
        }
        String userNaviUri = "/" + WCMCoreUtils.getPortalName() + "/" + PageDataCreator.getUserNavigationURI(node).toString();
        if (userNavigationUriList.contains(userNaviUri)) {
          return null;
        }
        userNavigationUriList.add(userNaviUri);
        return new ResultNode(node, row, userNaviUri);
      } catch (Exception e) {
        return null;
      }
    }
    
    /**
     * Get user navigation URI from equivalent mop node in portal-system workspace
     * @param node the mop node in portal-system workspace
     * @return 
     * @throws RepositoryException
     */
    public static StringBuilder getUserNavigationURI(Node node) throws RepositoryException {
      if (!node.isNodeType("mop:portalsites")) {
        StringBuilder builder = getUserNavigationURI(node.getParent());
        String name = node.getName();
        if ("mop:children".equals(name) || "mop:default".equals(name)
            || "mop:rootnavigation".equals(name)) {
          return builder.append("");
        } else {
          if (builder.length() > 0) {
            builder.append("/");
          }
          return builder.append(node.getName().replaceFirst("mop:", ""));
        }
      } else {
        return new StringBuilder();
      }

    } 
    
  }  
  
  /**
   * 
   * @author ha_dangviet
   *
   */
  public static class PageTitleDataCreator implements SearchDataCreator<String> {

    @Override
    public String createData(Node node, Row row) {
      try {
        UserACL userACL = WCMCoreUtils.getService(UserACL.class);
        if (node.hasProperty("gtn:access-permissions")) {
          for (Value v : node.getProperty("gtn:access-permissions").getValues()) {
            if (userACL.hasPermission(v.getString())) {
              return node.getPath();
            }
          }
          return null;
        } else {
          return node.getPath();
        }
      } catch (RepositoryException e) {
        return null;
      }
    }
    
  }
}
