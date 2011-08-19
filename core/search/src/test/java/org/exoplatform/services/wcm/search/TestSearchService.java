/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.search.QueryCriteria.DatetimeRange;
import org.exoplatform.services.wcm.search.QueryCriteria.QueryProperty;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 14, 2009
 */
public class TestSearchService extends BaseWCMTestCase {
  QueryCriteria queryCriteria = new QueryCriteria();

  private SiteSearchService siteSearchService;

  private WCMPublicationService wcmPublicationService;
  
  private WebpagePublicationPlugin publicationPlugin ;
  
  private UserPortalConfigService userPortalConfigService;

  private String searchKeyword = "This is";

  private SessionProvider sessionProvider;
  
  private POMSessionManager pomManager;
  
  private POMSession  pomSession;
  
  
  private boolean searchPageChecked = true; 

  private boolean searchDocumentChecked = true;

  private String searchSelectedPortal = "shared";

  private int seachItemsPerPage = 100;

  private boolean searchIsLiveMode = false;
  
  public void setUp() throws Exception {
    super.setUp();
    queryCriteria = new QueryCriteria();
    siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);
    userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    pomManager = WCMCoreUtils.getService(POMSessionManager.class);
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    
    publicationPlugin = new DumpPublicationPlugin();
    publicationPlugin.setName(DumpPublicationPlugin.LIFECYCLE_NAME);
    wcmPublicationService.addPublicationPlugin(publicationPlugin);
    
    addDocuments();
    
    queryCriteria.setSiteName(searchSelectedPortal);
    queryCriteria.setKeyword(searchKeyword);
    if (searchDocumentChecked) {
      queryCriteria.setSearchDocument(true);
      queryCriteria.setSearchWebContent(true);
    } else  {
      queryCriteria.setSearchDocument(false);
      queryCriteria.setSearchWebContent(false);
    }
    queryCriteria.setSearchWebpage(searchPageChecked);
    queryCriteria.setLiveMode(searchIsLiveMode);
  }

  private void addDocuments() throws Exception {
    Node classicPortal = (Node)session.getItem("/sites content/live/classic/web contents");
    addChildNodes(classicPortal); 
    
    Node sharedPortal = (Node)session.getItem("/sites content/live/shared/documents");
    addChildNodes(sharedPortal);
    
//  private void addAnotherNode() throws Exception{
//  Node parentNode = (Node)session.getItem("/sites content/live");
//  for(int i = 0; i < 10; i ++){
//    parentNode.addNode(parentNode.getName() + " anotherNode " + i);
//  }
//  session.save();
//}
  }
  
  private void addChildNodes(Node parentNode)throws Exception{
    if (pomManager.getSession() == null) pomSession = pomManager.openSession();
    Page page = userPortalConfigService.getPage("portal::classic::testpage");
    if(page == null){
      page = new Page();
      page.setPageId("portal::classic::testpage");
      page.setName("testpage");
      page.setOwnerType("portal");
      page.setOwnerId("classic");
      userPortalConfigService.create(page);
    }
    
    Node webContentNode = null;
    HashMap<String, String> context = null;
    // Create 5 nodes which have status is PUBLISHED
    for(int i = 0; i < 5; i++){
      webContentNode = createWebcontentNode(parentNode, "webcontent" + i, null, null, null);
      if(!webContentNode.isNodeType("metadata:siteMetadata"))webContentNode.addMixin("metadata:siteMetadata");
      wcmPublicationService.enrollNodeInLifecycle(webContentNode, DumpPublicationPlugin.LIFECYCLE_NAME);
      wcmPublicationService.publishContentSCV(webContentNode, page, parentNode.getName());
      context = new HashMap<String, String>();
//      context.put(DumpPublicationPlugin.CURRENT_REVISION_NAME, webContentNode.getName());
      publicationPlugin.changeState(webContentNode, PublicationDefaultStates.PUBLISHED, context);
    }

    // Create 5 nodes which have status is DRAFT
    for(int i = 5; i < 10; i++){
      webContentNode = createWebcontentNode(parentNode, "webcontent" + i, null, null, null);
      if(!webContentNode.isNodeType("metadata:siteMetadata"))webContentNode.addMixin("metadata:siteMetadata");
      wcmPublicationService.enrollNodeInLifecycle(webContentNode, DumpPublicationPlugin.LIFECYCLE_NAME);
      wcmPublicationService.publishContentSCV(webContentNode, page, parentNode.getName());
      context = new HashMap<String, String>();
//      context.put(DumpPublicationPlugin.CURRENT_REVISION_NAME, webContentNode.getName());
      publicationPlugin.changeState(webContentNode, PublicationDefaultStates.DRAFT, context);
    }
    session.save();
    pomSession.close();
  }

  private WCMPaginatedQueryResult getSearchResult() throws Exception{
    return siteSearchService.searchSiteContents(WCMCoreUtils.getSystemSessionProvider(), queryCriteria, seachItemsPerPage, false); 
  }

  /**
   * Test case 1: search all node (includes have or don't have publication property)
   * in shared portal and not live mode. In this case, set parameters:<br>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = false<br>
   * 
   * @throws Exception the exception
   */
  public void testSearchSharedPortalNotLiveMode() throws Exception {
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 2: search all node (includes have or don't have publication property)
   * in shared portal. In this case, set parameters:<br>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   * 
   * @throws Exception the exception
   */
  public void testSearchSharedPortalLiveMode() throws Exception {
    searchIsLiveMode = true;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 3: search all node (includes have or don't have publication property) in all portals.
   * In this case, set parameters:<br>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = false<br>
   * 
   * @throws Exception the exception
   */
  public void testSearchAllPortalNotLiveMode() throws Exception {
    searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(10, paginatedQueryResult.getTotalNodes());
    assertEquals(5, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 4: search nodes which are live mode in all portals.
   * In this case, set parameters:<br>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * 
   * @throws Exception the exception
   */
  public void testSearchAllPortalLiveMode() throws Exception {
    searchSelectedPortal = null;
    searchIsLiveMode = true;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  //---------------------------------------------- Test search document -----------------------------------------------------------
  /**
   * Test case 5: Test search document.
   * Search all documents in system (all portals) which are live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchDocumentLiveMode() throws Exception {
    this.searchPageChecked = false;
    this.searchDocumentChecked = true;
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 6:Test search document.
   * Search all documents in system (all portals). With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = false<br>
   */
  public void testSearchDocumentNotLiveMode() throws Exception {
    this.searchPageChecked = false;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 7:Test search document.
   * Search all documents in shared portal. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = false<br>
   */
  public void testSearchDocumentOfSharedPortal() throws Exception {
    this.searchPageChecked = false;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 8:Test search document.
   * Search all documents in shared portal. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchDocumentOfSharedPortalLiveMode() throws Exception {
    this.searchPageChecked = false;
    this.searchIsLiveMode = true;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  //------------------------------------------- Test search pages ------------------------------------------------------------------
  /**
   * Test case 9:Test search pages.
   * Search all pages in all portals. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchPagesLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 10:Test search pages.
   * Search all pages in all portals and not live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = false<br>
   */
  public void testSearchPages() throws Exception {
    this.searchDocumentChecked = false;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 11:Test search pages.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchPagesSharedLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchIsLiveMode = true;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 12:Test search pages.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchPagesShared() throws Exception {
    this.searchDocumentChecked = false;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  //------------------------------------- test with not document or page --------------------------------------------------------------------

  /**
   * Test case 13:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchNotPagesDocument_AllPortalLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchPageChecked = false;
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 14:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchNotPagesDocument_AllPortalNotLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchPageChecked = false;
    this.searchSelectedPortal = null;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 15:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchNotPagesDocument_SharedLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchPageChecked = false;
    this.searchIsLiveMode = true;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 16:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchNotPagesDocument_SharedNoLiveMode() throws Exception {
    this.searchDocumentChecked = false;
    this.searchPageChecked = false;
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 17:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  @SuppressWarnings("deprecation")
  public void testSearchPagesDocument_Date() throws Exception {
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    Date date = new Date(2009, 05, 05);
    GregorianCalendar calFrom = new GregorianCalendar() ;
    calFrom.setTime(date);
    date = new Date();
    GregorianCalendar calTo = new GregorianCalendar() ;
    calTo.setTime(date);
    DatetimeRange datetimeRange = new DatetimeRange(calFrom, calTo);
    queryCriteria.setCreatedDateRange(datetimeRange);
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
    assertEquals(10, paginatedQueryResult.getTotalNodes());
  }

  /**
   * Test case 18:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
  public void testSearchPagesDocument_NotFultextSearch() throws Exception {
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    queryCriteria.setFulltextSearch(false);
    WCMPaginatedQueryResult paginatedQueryResult = 
      (new SiteSearchServiceImplDump((SiteSearchServiceImpl)siteSearchService)).searchSiteContents(WCMCoreUtils.getSystemSessionProvider(), queryCriteria, seachItemsPerPage, false);
    assertEquals(0, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 19:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * keyWord = null;
   */
  public void testSearchPagesDocument_ContentType() throws Exception {
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    this.searchKeyword = null;
    queryCriteria.setContentTypes(new String[]{"exo:webContent", "exo:htmlFile"});
    WCMPaginatedQueryResult paginatedQueryResult = getSearchResult();
    assertEquals(5, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 20:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * keyWord = null;
   */
  public void testSearchPagesDocument_Property() throws Exception {
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    this.searchKeyword = "This is*";
    QueryProperty queryProperty1 = queryCriteria.new QueryProperty();
    queryProperty1.setName("jcr:data");
    queryProperty1.setValue("This is the");
    QueryProperty queryProperty2 = queryCriteria.new QueryProperty();
    queryProperty2.setName("jcr:data");
    queryProperty2.setValue("the default.css file");
    queryCriteria.setQueryMetadatas(new QueryProperty[]{queryProperty1, queryProperty2});
    WCMPaginatedQueryResult paginatedQueryResult = 
      (new SiteSearchServiceImplDump((SiteSearchServiceImpl)siteSearchService)).searchSiteContents(WCMCoreUtils.getSystemSessionProvider(), queryCriteria, seachItemsPerPage, false);      
    assertEquals(0, paginatedQueryResult.getPage(1).size());
  }

  /**
   * Test case 21:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * keyWord = null;
   * @throws RepositoryException 
   * @throws PathNotFoundException 
   */
  public void testSearchPagesDocument_CategoryUUIDS() throws Exception{
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = null;
    Node documentNode = ((Node)session.getItem("/sites content/live/shared/documents")).getNode("webcontent0");
    Node livenode = ((Node)session.getItem("/sites content/live/classic/web contents")).getNode("webcontent0");
    queryCriteria.setCategoryUUIDs(new String[]{documentNode.getUUID(), livenode.getUUID()});
    WCMPaginatedQueryResult paginatedQueryResult = new WCMPaginatedQueryResult(20);
    paginatedQueryResult.setQueryCriteria(this.queryCriteria);
    paginatedQueryResult = getSearchResult();
    assertEquals(0, paginatedQueryResult.getTotalNodes());
  }

  public void testSearchByProperty()throws Exception{
    this.searchIsLiveMode = true;
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty("dc:description");
    assertEquals(0, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }
  
  public void testSearchByDocumentType()throws Exception{
    String documentType = "exo:webContent";
    this.searchIsLiveMode = true;
    this.searchKeyword = null;
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setContentTypes(documentType.split(","));
    assertEquals(10, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }
  
  public void testSearchByDocumentAuthor()throws Exception{
    String author = "root";
    this.searchIsLiveMode = true;
    this.searchKeyword = null;
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setAuthors(new String[]{author});
    assertEquals(10, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }
  
  public void testSearchByMimeTypes()throws Exception{
    this.searchIsLiveMode = true;
    this.searchKeyword = null;
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setMimeTypes(new String[]{"exo:webContent", " exo:siteBreadcrumb"});
    assertEquals(10, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }
  
  public void testSearchByTagUUID() throws Exception{
    Node node = (Node)session.getItem("/sites content/live/classic/web contents/webcontent0");
    String uuid = node.getUUID();
    this.searchIsLiveMode = true;
    this.searchSelectedPortal = "classic";
    this.searchKeyword = null;
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setTagUUIDs(new String[]{uuid});
    assertEquals(10, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }
  
  protected void tearDown() throws Exception {
    super.tearDown();
    
    NodeIterator iterator = null; 
    Node classicPortal = (Node)session.getItem("/sites content/live/classic/web contents");
    iterator = classicPortal.getNodes();
    while (iterator.hasNext()) {
      iterator.nextNode().remove();
    }
    
    Node sharedPortal = (Node)session.getItem("/sites content/live/shared/documents");
    iterator = sharedPortal.getNodes();
    while (iterator.hasNext()) {
      iterator.nextNode().remove();
    }
    
    session.save();
  }
  
  private class WCMPaginatedQueryResultDump extends WCMPaginatedQueryResult {
    public WCMPaginatedQueryResultDump(int pageSize) {
      super(pageSize);
    }

    public WCMPaginatedQueryResultDump(QueryResult queryResult,
        QueryCriteria queryCriteria, int pageSize, long numTotal,
        boolean showTotalPagination, boolean isSearchContent) throws Exception {
      super(queryResult, queryCriteria, pageSize, numTotal, showTotalPagination, isSearchContent);
    }

    protected void populateCurrentPage(int page) throws Exception {
      if(page == currentPage_ && (currentListPage_ != null && !currentListPage_.isEmpty())) {
        return;
      }
      //checkAndSetPosition(page);

      SiteSearchService siteSearchService = WCMCoreUtils.getService(SiteSearchService.class);
      queryCriteria.setOffset((page-1)*getPageSize());
      QueryResult queryResult = siteSearchService.searchSiteContents(WCMCoreUtils.getSystemSessionProvider(), this.queryCriteria, this.getPageSize(), false).queryResult;
      populateCurrentListPage(queryResult);
      currentPage_ = page;
    }
  }
  
  private class SiteSearchServiceImplDump extends SiteSearchServiceImpl {

    public SiteSearchServiceImplDump(SiteSearchServiceImpl searchService) throws Exception {
      super(searchService.livePortalManagerService, searchService.templateService, searchService.configurationService,
          searchService.repositoryService, null);
      // TODO Auto-generated constructor stub
    }
    
    public WCMPaginatedQueryResult searchSiteContents(SessionProvider sessionProvider, QueryCriteria queryCriteria, int pageSize, boolean isSearchContent) throws Exception {
      ManageableRepository currentRepository = repositoryService.getCurrentRepository();
      NodeLocation location = configurationService.getLivePortalsLocation(currentRepository.getConfiguration().getName());    
      long startTime = System.currentTimeMillis();

      String pageMode = queryCriteria.getPageMode();
      boolean showTotalPagination = SiteSearchService.PAGE_MODE_PAGINATION.equals(pageMode);

      long numTotal = 0;
      int limit = SiteSearchService.PAGE_MODE_NONE.equals(pageMode)?pageSize : pageSize+1;

      if (showTotalPagination) {
        SessionProvider systemProvider = WCMCoreUtils.getSystemSessionProvider();
        Session session = systemProvider.getSession(location.getWorkspace(), currentRepository);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryResult queryResult = searchSiteContent(queryCriteria, queryManager);
        numTotal = queryResult.getNodes().getSize();
        limit--;
      }

      System.out.println("Location: " + location + ";;; Repo: " + currentRepository + ";;; Provider :" + sessionProvider);
      Session session = sessionProvider.getSession(location.getWorkspace(), currentRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      long offset = 0;
      if (queryCriteria.getOffset()>-1) offset = queryCriteria.getOffset();
      QueryResult queryResult = searchSiteContent(queryCriteria, queryManager, limit, offset);
      if (!showTotalPagination) numTotal = queryResult.getNodes().getSize();

      String suggestion = getSpellSuggestion(queryCriteria.getKeyword(),currentRepository);
      long queryTime = System.currentTimeMillis() - startTime;
      WCMPaginatedQueryResultDump paginatedQueryResult = null;
      paginatedQueryResult = new WCMPaginatedQueryResultDump( queryResult, queryCriteria, pageSize, numTotal, showTotalPagination, isSearchContent);
      paginatedQueryResult.setQueryTime(queryTime);
      paginatedQueryResult.setSpellSuggestion(suggestion);    
      return paginatedQueryResult;
    }  
    
  }
}
