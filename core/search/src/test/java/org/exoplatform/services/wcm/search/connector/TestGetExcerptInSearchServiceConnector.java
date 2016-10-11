package org.exoplatform.services.wcm.search.connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.search.base.BaseSearchTest;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.router.Router;

import com.coremedia.iso.boxes.CompositionTimeToSample.Entry;
import com.sun.star.i18n.Calendar;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-search-configuration.xml")
})

public class TestGetExcerptInSearchServiceConnector extends BaseSearchTest {
  
  private SearchServiceConnector documentSearch_;
  private SearchServiceConnector fileSearch_;
  
  public void setUp() throws Exception {
    super.setUp();
    session.save();
    applyUserSession("john", "gtn",COLLABORATION_WS);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    documentSearch_ = WCMCoreUtils.getService(DocumentSearchServiceConnector.class);
    fileSearch_ = WCMCoreUtils.getService(FileSearchServiceConnector.class);
  }
  
  public void testDisplayInExcerptOfSearchResult() throws Exception {
    String data = "La vie est belle";
    String keyword = "bella~";
    String title = "Bella Vita";
    Node parentNode = (Node)session.getItem("/sites content/live/classic/web contents");
    // Add article
    Node article = parentNode.addNode(title, "exo:article");
    article.setProperty("exo:title", title);
    article.setProperty("exo:text", title);
    wcmPublicationService.enrollNodeInLifecycle(article, "Simple publication");
    publicationPlugin.changeState(article, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
    //Add file
    Node file = addFile(parentNode, title, data);
    wcmPublicationService.enrollNodeInLifecycle(file, "Simple publication");
    publicationPlugin.changeState(file, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
    //Add webcontent
    Node webContent1 = createWebcontentNode(parentNode,title , data, data, data);
    wcmPublicationService.enrollNodeInLifecycle(webContent1, "Simple publication");
    publicationPlugin.changeState(webContent1, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
    Node webContent2 = createWebcontentNode(parentNode,title , data, data, data);
    wcmPublicationService.enrollNodeInLifecycle(webContent2, "Simple publication");
    publicationPlugin.changeState(webContent2, PublicationDefaultStates.PUBLISHED, new HashMap<String, String>());
    Collection<String> sites = new ArrayList<String>();
    sites.add("classic");
    session.save();
    Collection<SearchResult> resultDocumentSearch = documentSearch_.search(new SearchContext(new Router(new ControllerDescriptor()),
                                                                               "intranet"),
                                                             keyword,
                                                             sites,
                                                             0,
                                                             20,
                                                             "title",
                                                             "asc");
    assertEquals(3, resultDocumentSearch.size());
    String excerpt = "";
    for(SearchResult searchResultDocument : resultDocumentSearch) {
      excerpt = searchResultDocument.getExcerpt();
      assertEquals("", excerpt);
    }
    
    Collection<SearchResult> resultFileSearch = fileSearch_.search(new SearchContext(new Router(new ControllerDescriptor()),
                                                                               "intranet"),
                                                             keyword,
                                                             sites,
                                                             0,
                                                             20,
                                                             "title",
                                                             "asc");
    assertEquals(1, resultFileSearch.size());
    for(SearchResult searchResultFile : resultFileSearch) {
      excerpt = searchResultFile.getExcerpt();
      assertEquals("", excerpt);
    }
   }    
  protected Node createWebcontentNode(Node parentNode,
                                      String nodeName,
                                      String htmlData,
                                      String cssData,
                                      String jsData) throws Exception {
    Node webcontent = parentNode.addNode(nodeName, "exo:webContent");
    webcontent.setProperty("exo:title", nodeName);
    webcontent.setProperty(NodetypeConstant.EXO_DATE_CREATED, new GregorianCalendar());
    Node htmlNode;
    try {
      htmlNode = webcontent.getNode("default.html");
      htmlNode.addMixin("exo:owneable");
    } catch (Exception ex) {
      htmlNode = webcontent.addNode("default.html", "nt:file");
    }
    if (!htmlNode.isNodeType("exo:htmlFile"))
      htmlNode.addMixin("exo:htmlFile");
    Node htmlContent;
    try {
      htmlContent = htmlNode.getNode("jcr:content");
      htmlContent.addMixin("exo:owneable");
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
      jsFolder.addMixin("exo:owneable");
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
      jsContent.addMixin("exo:owneable");
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
      cssFolder.addMixin("exo:owneable");
    } catch (Exception ex) {
      cssFolder = webcontent.addNode("css", "exo:cssFolder");
    }
    Node cssNode;
    try {
      cssNode = cssFolder.getNode("default.css");
      cssNode.addMixin("exo:owneable");
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
      cssContent.addMixin("exo:owneable");
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
      mediaFolder.addMixin("exo:owneable");
    } catch (Exception ex) {
      mediaFolder = webcontent.addNode("medias");
    }
    if (!mediaFolder.hasNode("images"))
      mediaFolder.addNode("images", "nt:folder");
    if (!mediaFolder.hasNode("videos"))
      mediaFolder.addNode("videos", "nt:folder");
    if (!mediaFolder.hasNode("audio"))
      mediaFolder.addNode("audio", "nt:folder");
    if(!webcontent.hasNode("documents")) {
      Node document = webcontent.addNode("documents","nt:unstructured");
    }
    session.save();
    return webcontent;
  }
  public void tearDown() throws Exception {
    super.tearDown();
  }
  private Node addFile(Node parentNode, String name, String data) throws Exception {
    Node file = parentNode.addNode(name, "nt:file");
    file.addMixin("exo:sortable");
    file.setProperty("exo:title", name);
    Node content = file.addNode("jcr:content", "nt:resource");
    content.setProperty("jcr:encoding", "UTF-8");
    content.setProperty("jcr:mimeType", "text/html");
    content.setProperty("jcr:lastModified", new Date().getTime());
    content.setProperty("jcr:data", data);
    
    file.addMixin(NodetypeConstant.EXO_DATETIME);
    file.setProperty(NodetypeConstant.EXO_DATE_CREATED, new GregorianCalendar());
    session.save();
    return file;
  }
  private String getExcerpt(String term) throws RepositoryException
  {
     QueryManager queryManager = session.getWorkspace().getQueryManager();
     Query query =
        queryManager.createQuery("select  rep:excerpt() from exo:webContent where "
           + "contains(., '"+term+"')", Query.SQL);
     QueryResult result = query.execute();
     RowIterator rows = result.getRows();

     Value v = rows.nextRow().getValue("rep:excerpt(.)");
     if (v != null)
     {
        return v.getString();
     }
     else
     {
        return null;
     }
  }

}
