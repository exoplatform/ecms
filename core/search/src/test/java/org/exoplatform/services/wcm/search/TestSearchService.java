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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.BaseSearchTest;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jul 14, 2009
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-search-configuration.xml")
})
public class TestSearchService extends BaseSearchTest {

  public void setUp() throws Exception {
    super.setUp();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
  }
  
  protected void addChildNodes(Node parentNode)throws Exception{
    PageService pageService = getService(PageService.class);
    pomSession = pomManager.getSession();
    if (pomManager.getSession() == null) pomSession = pomManager.openSession();
    PageContext page = pageService.loadPage(new PageKey(new SiteKey("portal", "classic"), "testpage"));
    if(page == null){
      PageState pageState = new PageState("testpage", "test page", true,
                                          "testpage", null, null, null, null);
      page = new  PageContext(new PageKey(new SiteKey("portal", "classic"), "testpage"), pageState);
      pageService.savePage(page);
    }

    Node webContentNode = null;
    HashMap<String, String> context = null;
    webContentNode = createWebcontentNode(parentNode, "webcontent0" , null, null, null);
    if(!webContentNode.isNodeType("metadata:siteMetadata")) webContentNode.addMixin("metadata:siteMetadata");
    wcmPublicationService.enrollNodeInLifecycle(webContentNode, DumpPublicationPlugin.LIFECYCLE_NAME);
    context = new HashMap<String, String>();
//      context.put(DumpPublicationPlugin.CURRENT_REVISION_NAME, webContentNode.getName());
    publicationPlugin.changeState(webContentNode, PublicationDefaultStates.PUBLISHED, context);

    webContentNode = createWebcontentNode(parentNode, "webcontent1", null, null, null);
    if(!webContentNode.isNodeType("metadata:siteMetadata")) webContentNode.addMixin("metadata:siteMetadata");
    wcmPublicationService.enrollNodeInLifecycle(webContentNode, DumpPublicationPlugin.LIFECYCLE_NAME);
    context = new HashMap<String, String>();
//      context.put(DumpPublicationPlugin.CURRENT_REVISION_NAME, webContentNode.getName());
    publicationPlugin.changeState(webContentNode, PublicationDefaultStates.DRAFT, context);

    session.save();
    pomSession.close();
  }

  protected void populateAdditionalSearchData(Node siteNode, String parentNode, int nodesCount) {
    Node resolvedParentNode = siteNode;
      if (!StringUtils.isEmpty(parentNode) && ("documents".equals(parentNode) || "web contents".equals(parentNode))) {
        try {
          if (siteNode.hasNode(parentNode)) {
            resolvedParentNode = siteNode.getNode(parentNode);
          } else {
            resolvedParentNode = siteNode.addNode(parentNode);
            session.save();
          }
        } catch (RepositoryException e) {
          // Do nothing
        }
      } else {
        return;
      }

      if (("documents".equals(parentNode))) {
        populateDocumentNodes(resolvedParentNode, nodesCount);
      } else {
        populateWebContentNodes(resolvedParentNode, nodesCount);
      }
    }

    private void populateDocumentNodes(Node parentNode, int nodesCount) {
      for (int i = 0; i < nodesCount; i++) {
        Node dummyFile;
        String fileNamePrefix = "dummyFile";
        try {
          dummyFile = parentNode.addNode(fileNamePrefix + i, "nt:file");
          Node imageContent = dummyFile.addNode("jcr:content", "nt:resource");
          imageContent.setProperty("jcr:encoding", "UTF-8");
          imageContent.setProperty("jcr:mimeType", "text/html");
          imageContent.setProperty("jcr:lastModified", new Date().getTime());
          imageContent.setProperty("jcr:data", "A file with duplication searchKey.");
          wcmPublicationService.enrollNodeInLifecycle(dummyFile, DumpPublicationPlugin.LIFECYCLE_NAME);
          publicationPlugin.changeState(dummyFile, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
        } catch (RepositoryException e) {
          // Do nothing
        } catch (IncorrectStateUpdateLifecycleException e) {
          // Do nothing
        } catch (Exception e) {
          // Do nothing
        }
      }
    }

    protected  void populateWebContentNodes(Node parentNode, int nodesCount) {
      String webContentNamePrefix = "dummyWebContent";
      // html data for web content node: Must hold a common keyword "searchKey"
      String htmlData = "The default.html file and yes it holds duplication searchKey.";
      String cssData = "The default.css file.";
      String jsData = "The default.js file.";
      StringBuilder webContentName;
      for (int i = 0; i < nodesCount; i++) {
        webContentName = new StringBuilder();
        try {
          Node populatedNode = createWebcontentNode(parentNode,
          webContentName.append(webContentNamePrefix).append(i).toString(),htmlData,cssData,jsData);
          if (!populatedNode.isNodeType("metadata:siteMetadata")) {
            populatedNode.addMixin("metadata:siteMetadata");
          }
          wcmPublicationService.enrollNodeInLifecycle(populatedNode, DumpPublicationPlugin.LIFECYCLE_NAME);
          publicationPlugin.changeState(populatedNode, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
        } catch (Exception e) {
          // Do nothing
        }
      }
    }

    protected Node createWebcontentNode(Node parentNode,
                                      String nodeName,
                                      String htmlData,
                                      String cssData,
                                      String jsData) throws Exception {
    Node webcontent = parentNode.addNode(nodeName, "exo:webContent");
    webcontent.setProperty("exo:title", nodeName);
    Node htmlNode;
    try {
      htmlNode = webcontent.getNode("default.html");
    } catch (Exception ex) {
      htmlNode = webcontent.addNode("default.html", "nt:file");
    }
    if (!htmlNode.isNodeType("exo:htmlFile"))
      htmlNode.addMixin("exo:htmlFile");
    Node htmlContent;
    try {
      htmlContent = htmlNode.getNode("jcr:content");
    } catch (Exception ex) {
      htmlContent = htmlNode.addNode("jcr:content", "nt:resource");
    }
    htmlContent.setProperty("jcr:encoding", "UTF-8");
    htmlContent.setProperty("jcr:mimeType", "text/html");
    htmlContent.setProperty("jcr:lastModified", new Date().getTime());
    if (htmlData == null)
      htmlData = "This is the default.html file.";
    htmlContent.setProperty("jcr:data", htmlData);
    if (!htmlContent.isNodeType("exo:webContentChild"))
      htmlContent.addMixin("exo:webContentChild");

    Node jsFolder;
    try {
      jsFolder = webcontent.getNode("js");
    } catch (Exception ex) {
      jsFolder = webcontent.addNode("js", "exo:jsFolder");
    }
    Node jsNode;
    try {
      jsNode = jsFolder.getNode("default.js");
    } catch (Exception ex) {
      jsNode = jsFolder.addNode("default.js", "nt:file");
    }
    if (!jsNode.isNodeType("exo:jsFile"))
      jsNode.addMixin("exo:jsFile");
    jsNode.setProperty("exo:active", true);
    jsNode.setProperty("exo:priority", 1);
    jsNode.setProperty("exo:sharedJS", true);

    Node jsContent;
    try {
      jsContent = jsNode.getNode("jcr:content");
    } catch (Exception ex) {
      jsContent = jsNode.addNode("jcr:content", "nt:resource");
    }
    jsContent.setProperty("jcr:encoding", "UTF-8");
    jsContent.setProperty("jcr:mimeType", "text/javascript");
    jsContent.setProperty("jcr:lastModified", new Date().getTime());
    if (jsData == null)
      jsData = "This is the default.js file.";
    jsContent.setProperty("jcr:data", jsData);
    if (!jsContent.isNodeType("exo:webContentChild"))
      jsContent.addMixin("exo:webContentChild");

    Node cssFolder;
    try {
      cssFolder = webcontent.getNode("css");
    } catch (Exception ex) {
      cssFolder = webcontent.addNode("css", "exo:cssFolder");
    }
    Node cssNode;
    try {
      cssNode = cssFolder.getNode("default.css");
    } catch (Exception ex) {
      cssNode = cssFolder.addNode("default.css", "nt:file");
    }
    if (!cssNode.isNodeType("exo:cssFile"))
      cssNode.addMixin("exo:cssFile");
    cssNode.setProperty("exo:active", true);
    cssNode.setProperty("exo:priority", 1);
    cssNode.setProperty("exo:sharedCSS", true);

    Node cssContent;
    try {
      cssContent = cssNode.getNode("jcr:content");
    } catch (Exception ex) {
      cssContent = cssNode.addNode("jcr:content", "nt:resource");
    }
    cssContent.setProperty("jcr:encoding", "UTF-8");
    cssContent.setProperty("jcr:mimeType", "text/css");
    cssContent.setProperty("jcr:lastModified", new Date().getTime());
    if (cssData == null)
      cssData = "This is the default.css file.";
    cssContent.setProperty("jcr:data", cssData);
    if (!cssContent.isNodeType("exo:webContentChild"))
      cssContent.addMixin("exo:webContentChild");

    Node mediaFolder;
    try {
      mediaFolder = webcontent.getNode("medias");
    } catch (Exception ex) {
      mediaFolder = webcontent.addNode("medias");
    }
    if (!mediaFolder.hasNode("images"))
      mediaFolder.addNode("images", "nt:folder");
    if (!mediaFolder.hasNode("videos"))
      mediaFolder.addNode("videos", "nt:folder");
    if (!mediaFolder.hasNode("audio"))
      mediaFolder.addNode("audio", "nt:folder");
    session.save();
    return webcontent;
  }
  
  private AbstractPageList<ResultNode> getSearchResult(boolean isSearchContent, int searchItemPerPage) throws Exception{
    return siteSearchService.searchSiteContents(WCMCoreUtils.getSystemSessionProvider(),
                                                queryCriteria, searchItemPerPage, isSearchContent);
  }

  protected String[] getWebContentSearchedDocTypes() {
    List<String> docTypes = null;
    try {
      docTypes = WCMCoreUtils.getService(TemplateService.class).getDocumentTemplates();
    } catch (Exception e) {
    }
    return docTypes.toArray(new String[]{});
  }

  /**
   * Test case 1: search all node (includes have or don't have publication property)
   * in shared portal and not live mode. In this case, set parameters:<br>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = false<br>
   *
   * @throws Exception the exception
   */
  public void testSearchSharedPortalNotLiveMode() throws Exception {
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(false);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    AbstractPageList<ResultNode> pageList = getSearchResult(true, seachItemsPerPage);
    assertEquals(2, pageList.getPage(1).size());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    int searchItemPerPage = 1;
    AbstractPageList<ResultNode> pageList = getSearchResult(true,searchItemPerPage);
    assertEquals(1, pageList.getPage(1).size());
    assertEquals(2, pageList.getTotalNodes());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(false);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    AbstractPageList<ResultNode> pageList = getSearchResult(true, seachItemsPerPage);
    assertEquals(4, pageList.getTotalNodes());
    assertEquals(4, pageList.getPage(1).size());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    int searchItemPerPage = 2;
    AbstractPageList<ResultNode> pageList = getSearchResult(true, searchItemPerPage);
    assertEquals(2, pageList.getPage(1).size());
    assertEquals(4, pageList.getTotalNodes());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    int searchItemsPerPage = 2;
    AbstractPageList<ResultNode> pageList = getSearchResult(true, searchItemsPerPage);
    assertEquals(2, pageList.getPage(1).size());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(false);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    AbstractPageList<ResultNode> pageList = getSearchResult(true, seachItemsPerPage);
    assertEquals(4, pageList.getTotalNodes());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(false);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    AbstractPageList<ResultNode> pageList = getSearchResult(true, seachItemsPerPage);
    assertEquals(2, pageList.getTotalNodes());
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
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());
    int searchItemsPerPage = 1;
    AbstractPageList<ResultNode> pageList = getSearchResult(true, searchItemsPerPage);
    assertEquals(1, pageList.getPage(1).size());
    assertEquals(2, pageList.getTotalNodes());
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
//  public void testSearchPagesLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//  }

  /**
   * Test case 10:Test search pages.
   * Search all pages in all portals and not live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = false<br>
   */
//  public void testSearchPages() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchSelectedPortal = null;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 11:Test search pages.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchPagesSharedLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchIsLiveMode = true;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 12:Test search pages.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = shared<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchPagesShared() throws Exception {
//    this.searchDocumentChecked = false;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  //------------------------------------- test with not document or page --------------------------------------------------------------------

  /**
   * Test case 13:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchNotPagesDocument_AllPortalLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchPageChecked = false;
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 14:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchNotPagesDocument_AllPortalNotLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchPageChecked = false;
//    this.searchSelectedPortal = null;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 15:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchNotPagesDocument_SharedLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchPageChecked = false;
//    this.searchIsLiveMode = true;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 16:Test search contents are not document or page in all portal.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = false<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchNotPagesDocument_SharedNoLiveMode() throws Exception {
//    this.searchDocumentChecked = false;
//    this.searchPageChecked = false;
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 17:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = false<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  @SuppressWarnings("deprecation")
//  public void testSearchPagesDocument_Date() throws Exception {
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    Date date = new Date(2009, 05, 05);
//    GregorianCalendar calFrom = new GregorianCalendar() ;
//    calFrom.setTime(date);
//    date = new Date();
//    GregorianCalendar calTo = new GregorianCalendar() ;
//    calTo.setTime(date);
//    DatetimeRange datetimeRange = new DatetimeRange(calFrom, calTo);
//    queryCriteria.setCreatedDateRange(datetimeRange);
//    queryCriteria.setSearchWebpage(false);
//    queryCriteria.setSiteName(null);
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//    assertEquals(10, pageList.getTotalNodes());
//  }

  /**
   * Test case 18:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   */
//  public void testSearchPagesDocument_NotFultextSearch() throws Exception {
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    queryCriteria.setFulltextSearch(false);
//    queryCriteria.setSearchWebpage(false);
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(0, pageList.getPage(1).size());
//  }

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
//  public void testSearchPagesDocument_ContentType() throws Exception {
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    queryCriteria.setSearchWebpage(false);
//    queryCriteria.setContentTypes(new String[]{"exo:webContent", "exo:htmlFile"});
//    queryCriteria.setKeyword(null);
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(5, pageList.getPage(1).size());
//  }

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
//  public void testSearchPagesDocument_Property() throws Exception {
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    this.searchKeyword = "This is*";
//    QueryProperty queryProperty1 = queryCriteria.new QueryProperty();
//    queryProperty1.setName("jcr:data");
//    queryProperty1.setValue("This is the");
//    QueryProperty queryProperty2 = queryCriteria.new QueryProperty();
//    queryProperty2.setName("jcr:data");
//    queryProperty2.setValue("the default.css file");
//    queryCriteria.setQueryMetadatas(new QueryProperty[]{queryProperty1, queryProperty2});
//    queryCriteria.setSearchWebpage(false);
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(0, pageList.getPage(1).size());
//  }

  /**
   * Test case 21:Test search contents is document or page in all portal. And search
   * with created date and modified Date.
   * Search all pages in share and live mode. With this case, values of parameters are:<br/>
   * searchPageChecked = true<br>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * keyWord = null;
   */
//  public void testSearchPagesDocument_CategoryUUIDS() throws Exception{
//    this.searchIsLiveMode = true;
//    this.searchSelectedPortal = null;
//    Node documentNode = ((Node)session.getItem("/sites content/live/shared/documents")).getNode("webcontent0");
//    Node livenode = ((Node)session.getItem("/sites content/live/classic/web contents")).getNode("webcontent0");
//    queryCriteria.setSearchWebpage(false);
//    queryCriteria.setCategoryUUIDs(new String[]{documentNode.getUUID(), livenode.getUUID()});
//    AbstractPageList<ResultNode> pageList = getSearchResult();
//    assertEquals(0, pageList.getAvailable());
//  }

  public void testSearchByProperty()throws Exception{
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(new String[] {"dc:description"});
    assertEquals(0, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }

  public void testSearchByDocumentType()throws Exception{
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(null);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFulltextSearch(true);
    String documentType = "exo:webContent";
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setContentTypes(documentType.split(","));
    assertEquals(2, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }

  public void testSearchByDocumentAuthor()throws Exception{
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(null);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    String author = "root";
    queryCriteria.setAuthors(new String[]{author});
    assertEquals(4, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }

  public void testSearchByMimeTypes()throws Exception{
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(null);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setMimeTypes(new String[]{"exo:webContent", "exo:siteBreadcrumb"});
    AbstractPageList<ResultNode> pageList = siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true);
    assertEquals(4, pageList.getTotalNodes());
  }

  public void testSearchByTagUUID() throws Exception{
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("shared");
    queryCriteria.setKeyword(null);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFulltextSearch(true);
    queryCriteria.setFulltextSearchProperty(null);
    queryCriteria.setMimeTypes(new String[]{"exo:webContent", "exo:siteBreadcrumb"});
    Node node = (Node)session.getItem("/sites content/live/classic/web contents/webcontent0");
    String uuid = node.getUUID();
    queryCriteria.setTagUUIDs(new String[]{uuid});
    assertEquals(4, siteSearchService.searchSiteContents(sessionProvider, queryCriteria, 10, true).getTotalNodes());
  }




  /**
  * Test case 26: Search all nodes (includes have or don't have publication property)
  * in acme portal and not live mode.
  * This test case aims to check items duplication when the page list size is higher
  * than the default AbstractPageList.DEFAULT_BUFFER_SIZE.
  * Search page update are based on offset an limit of the QueryCriteria updates. Then
  * results are paginated using a pageSize local variable

  * Parameters are set to:<br>
  * searchSelectedPortal = acme<br>
  * keyword = "duplication searchKey"<br>
  * searchPageChecked = false<br>
  * searchDocumentChecked = true<br>
  * searchWebContentChecked = true<br>
  * searchIsLiveMode = false<br>
  * searchFuzzySearch = true<br>
  *
  * @throws Exception the exception
  */
  public void testSearchByOffsetAndLimitWithNoDuplication() throws Exception {
    int pageSize = 10;
    int offset = 0;
    int limit = pageSize;
    boolean isItemDuplicated = false;

    queryCriteria.setSiteName("acme");
    queryCriteria.setKeyword(duplicationSearchKeyword);
    queryCriteria = new QueryCriteria();
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);

    // First query should retrieve from offset to limit
    queryCriteria.setOffset(offset);
    queryCriteria.setLimit(limit);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());

    String assertionMsg = "Returned search results should have no duplicates in different pages: %s";
    /* Temp ResultNodes list which is aimed to hold always the
      previous page result. Those should then be used for comparison */
    List auxList = getSearchResult(true, 10).currentPage();

    int nbResultForCurrentPage = auxList.size();

    List<Integer> hashResults = new ArrayList<Integer>();

    while (nbResultForCurrentPage==limit && !isItemDuplicated) {
      offset+=limit;
      queryCriteria.setOffset(offset);
      queryCriteria.setLimit(limit);
      List<ResultNode> resultNodes = getSearchResult(true, 10).currentPage();
      nbResultForCurrentPage = resultNodes.size();
      int i=0;
      while (i<resultNodes.size() && !isItemDuplicated){
        ResultNode node = resultNodes.get(i);
        Integer hash = new Integer(node.hashCode());
        if (hashResults.contains(hash)) {
          isItemDuplicated=true;
          assertionMsg = String.format(assertionMsg,"Node: \"" + node.getPath() + "\" is duplicated at offset "+(offset-limit));
        } else {
          hashResults.add(new Integer(hash));
        }
        i++;
      }
    }
    assertFalse(assertionMsg, isItemDuplicated);
  }

  /**
   * Test case 27: Search all nodes (includes have or don't have publication property)
   * in acme portal and not live mode.
   * This test case aims to check items duplication when the page list size is higher
   * than the default AbstractPageList.DEFAULT_BUFFER_SIZE.

   * Search page update are based on the PageList#getPage which internally will populate
   * the new page nodes and increment the current page index: ECMS-6444
   * Parameters are set to:<br>
   * searchSelectedPortal = acme<br>
   * keyword = "duplication searchKey"<br>
   * searchPageChecked = false<br>

   * searchDocumentChecked = true<br>
   * searchWebContentChecked = true<br>
   * searchIsLiveMode = false<br>
   * searchFuzzySearch = true<br>
   *
   * @throws Exception the exception
   */
  public void testSearchByPageUpdateWithNoDuplication() throws Exception {
    boolean isItemDuplicated = false;
    int pageSize = 10;
    int offset = 0;
    int limit = pageSize;

    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName("acme");
    queryCriteria.setKeyword(duplicationSearchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    // Retrieve all nodes from 0 to 20
    queryCriteria.setOffset(0);
    queryCriteria.setLimit(20);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());

    String assertionMsg = "Returned search results should have no duplicates in different pages: %s";
    /* Temp ResultNodes list which is aimed to hold always the
      previous page result. Those should then be used for comparison */
    List auxList = getSearchResult(false, 10).currentPage();
    List<Integer> hashResults = new ArrayList<Integer>();

    int nbResultForCurrentPage = auxList.size();
    while (nbResultForCurrentPage==limit && !isItemDuplicated) {
      offset+=limit;
      queryCriteria.setOffset(offset);
      queryCriteria.setLimit(limit);
      List<ResultNode> resultNodes = getSearchResult(false, 10).currentPage();
      nbResultForCurrentPage = resultNodes.size();
      int i=0;
      while (i<resultNodes.size() && !isItemDuplicated){
        ResultNode node = resultNodes.get(i);
        Integer hash = new Integer(node.hashCode());
        if (hashResults.contains(hash)) {
          isItemDuplicated=true;
          assertionMsg = String.format(assertionMsg,"Node: \"" + node.getPath() + "\" is duplicated at offset "+(offset-limit));
        } else {
          hashResults.add(new Integer(hash));
        }
        i++;
      }
    }
    assertFalse(assertionMsg, isItemDuplicated);
  }

  /**
   * Test case 28: Test search document repetitively without offset setting
   * Each round is the same with testSearchDocumentLiveMode() (Test case 5)
   * Repeat 3 rounds to check cache eviction
   * Query results are stored in ArrayNodePageList
   * Search all documents in all sites which are in live mode (having publication property).
   * With this case, the parameter values are:<br/>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = null<br>
   * searchIsLiveMode = true<br>
   * no setting of offset<br>
   * 
   */
  public void testRepeatSearchDocument_ArrayNodePageList() throws Exception {
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(searchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());

    int searchItemsPerPage = 2;
    AbstractPageList<ResultNode> pageList;
    int searchRounds = 3;

    for (int i = 1; i <= searchRounds; i++) {
      System.out.println("- Search round " + i + "/" + searchRounds);
      pageList = getSearchResult(true, searchItemsPerPage);
      assertEquals("Wrong result number at round " + i + ": ", 2, pageList.getPage(1).size());
      assertEquals("Wrong total number at round " + i + ": ", 4, pageList.getTotalNodes());
    }  
  }

  /**
   * Test case 29: Test search document repetitively without offset setting
   * Each round is the same with testSearchDocumentLiveMode() (Test case 5)
   * Repeat 3 rounds to check cache eviction
   * Query results are stored in QueryResultPageList
   * Search all documents in all sites which are in live mode
   * and contain "duplicationSearchKeyword"
   * With this case, the parameter values are:<br/>
   * searchDocumentChecked = true<br>
   * searchSelectedPortal = acme<br>
   * searchIsLiveMode = true<br>
   * no setting of offset<br>
   * 
   */
  public void testRepeatSearchDocument_QueryResultPageList() throws Exception {
    queryCriteria = new QueryCriteria();
    queryCriteria.setSiteName(null);
    queryCriteria.setKeyword(duplicationSearchKeyword);
    queryCriteria.setSearchDocument(true);
    queryCriteria.setSearchWebContent(true);
    queryCriteria.setLiveMode(true);
    queryCriteria.setSearchWebpage(false);
    queryCriteria.setFuzzySearch(true);
    queryCriteria.setContentTypes(getWebContentSearchedDocTypes());

    int searchItemsPerPage = 100;
    AbstractPageList<ResultNode> pageList;
    int searchRounds = 3;

    for (int i = 1; i <= searchRounds; i++) {
      System.out.println("- Search round " + i + "/" + searchRounds);
      pageList = getSearchResult(true, searchItemsPerPage);
      // 20 web contents + 101 documents
      assertEquals("Wrong result number of page 1 at round " + i + ": ", searchItemsPerPage, pageList.getPage(1).size());
      assertEquals("Wrong result number of page 2 at round " + i + ": ", 21, pageList.getPage(2).size());
    }  
  }
  

  public void tearDown() throws Exception {
    super.tearDown();
  }

}
