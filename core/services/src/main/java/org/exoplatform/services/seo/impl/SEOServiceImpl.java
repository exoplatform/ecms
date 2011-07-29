/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.seo.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.GregorianCalendar;
import java.io.InputStream;
import java.io.StringWriter;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.ecm.utils.MessageDigester;
import java.util.ArrayList;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 17, 2011  
 */
public class SEOServiceImpl implements SEOService {
  
  private LivePortalManagerService livePortalManagerService;
  
  private RepositoryService repositoryService;
  private ExoCache<String, Object> cache;
  
  public static String      METADATA_BASE_PATH                  = "SEO";
  public static String      METADATA_PAGE_PATH                  = "pages";
  public static String      METADATA_CONTENT_PATH               = "contents";
  public static String      SITEMAP_NAME                        = "sitemap";
  public static String      ROBOTS_NAME                         = "robots";
  private static String     PUBLIC_MODE                         = "public";
  private static String     PRIVATE_MODE                        = "private";   
  
  private String robotsindexOptions = null;
  private String robotsfollowOptions = null;
  private String frequencyOptions = null;
  
  private boolean isCached = true;
  /**
   * Constructor method
   * 
   * @param initParams The initial parameters
   * @throws Exception
   */
  public SEOServiceImpl (InitParams initParams) throws Exception { 
    ValueParam valueParam = initParams.getValueParam("robotsindex");
    if(valueParam != null)
      robotsindexOptions = valueParam.getValue();
    else
      robotsindexOptions = "index,noindex";
    valueParam = initParams.getValueParam("robotsfollow");
    if(valueParam != null)
      robotsfollowOptions = valueParam.getValue();
    else
      robotsfollowOptions = "follow,nofollow";
    valueParam = initParams.getValueParam("frequency");
    if(valueParam != null)
      frequencyOptions = valueParam.getValue();
    else
      frequencyOptions = "Always,Hourly,Daily,Weekly,Monthly,Yearly,Never";
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
    this.repositoryService = repositoryService;    
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    this.livePortalManagerService = livePortalManagerService; 
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance("wcm.seo");
  }
  /**
   *{@inheritDoc}
   */
  public String getRobotsIndexOptions() {
    return robotsindexOptions;
  }
  /**
   * {@inheritDoc}
   */
  public String getRobotsFollowOptions() {
    return robotsfollowOptions;
  }
  /**
   * {@inheritDoc}
   */
  public String getFrequencyOptions() {
    return frequencyOptions;
  }
  /**
   * {@inheritDoc}
   */
  public void storePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent) throws Exception { 
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = null;
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
    String uri = metaModel.getUri(); 
    String pageReference = metaModel.getPageReference();
    String title = metaModel.getTitle();
    String language = metaModel.getLanguage();
    String keyword = metaModel.getKeywords();
    String description = metaModel.getDescription();
    String robots = metaModel.getRobotsContent();
    String fullStatus = metaModel.getFullStatus();
    boolean sitemap = metaModel.getSitemap();
    float priority = metaModel.getPriority();
    String frequency = metaModel.getFrequency();      
    session = dummyNode.getSession();
    if (!dummyNode.hasNode(METADATA_BASE_PATH)) {        
      dummyNode.addNode(METADATA_BASE_PATH);      
      session.save();
    } 
    Node seoNode = null;
    if(onContent) {
      seoNode = session.getNodeByUUID(uri);
    } else {
      session = sessionProvider.getSession("portal-system", WCMCoreUtils.getRepository());
      String uuid = Util.getUIPortal().getSelectedUserNode().getId();      
      seoNode = session.getNodeByUUID(uuid);
    }    
    if (seoNode.hasNode("exo:pageMetadata")) {      
        Node node = seoNode.getNode("exo:pageMetadata");        
        node.setProperty("exo:metaTitle", title);
        node.setProperty("exo:metaKeywords", keyword);
        node.setProperty("exo:metaDescription", description);  
        node.setProperty("exo:metaFully", fullStatus);
        if(!onContent) {
          node.setProperty("exo:metaRobots", robots);
          node.setProperty("exo:metaSitemap", sitemap);
          node.setProperty("exo:metaPriority", priority);
          node.setProperty("exo:metaFrequency", frequency);
          updateSiteMap(uri, priority, frequency, sitemap, portalName);
        }
        String hash = getHash(pageReference, language);
        cache.put(hash, metaModel);     
    } else {      
      String hash = null;       
      seoNode.addMixin("exo:pageMetadata");            
      seoNode.setProperty("exo:metaTitle", title);
      seoNode.setProperty("exo:metaKeywords", keyword);
      seoNode.setProperty("exo:metaDescription", description);
      seoNode.setProperty("exo:metaLanguage", language);
      seoNode.setProperty("exo:metaFully", fullStatus);        
      if(onContent) {
        seoNode.setProperty("exo:metaUri", seoNode.getUUID());
        hash = getHash(seoNode.getUUID(), language);
      }
      else {
        seoNode.setProperty("exo:metaUri", pageReference);
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);  
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
        hash = getHash(pageReference, language);
      }
      cache.put(hash, metaModel);  
    }
    session.save();
  }
  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getContentMetadata(ArrayList params, String pageLanguage) throws Exception {      
    PageMetadataModel metaModel = null; 
    String pageUri = null;
    Node contentNode = null;
    for(int i = 0;i < params.size();i++) {
      contentNode = this.getContentNode(params.get(i).toString());
      if(contentNode != null) break;
    }     
    String hash = getHash(contentNode.getUUID(), pageLanguage);    
    if(cache.get(hash) != null) 
      metaModel = (PageMetadataModel)cache.get(hash);     
    if(metaModel == null) {
      if(contentNode.hasNode("exo:pageMetadata")) {
        metaModel = new PageMetadataModel();   
        metaModel.setLanguage(pageLanguage);
        metaModel.setUri(pageUri);
        Node currentNode = contentNode.getNode("exo:pageMetadata");
        if (currentNode.hasProperty("exo:metaTitle"))       
          metaModel.setTitle((currentNode.getProperty("exo:metaTitle")).getString());      
        if (currentNode.hasProperty("exo:metaKeywords"))       
          metaModel.setKeywords((currentNode.getProperty("exo:metaKeywords")).getString());
        if (currentNode.hasProperty("exo:metaDescription"))       
          metaModel.setDescription((currentNode.getProperty("exo:metaDescription")).getString());
        if (currentNode.hasProperty("exo:metaLanguage"))       
          metaModel.setLanguage((currentNode.getProperty("exo:metaLanguage")).getString());        
        if (currentNode.hasProperty("exo:metaRobots"))       
          metaModel.setRobotsContent((currentNode.getProperty("exo:metaRobots")).getString());      
        if (currentNode.hasProperty("exo:metaSitemap"))       
          metaModel.setSiteMap(Boolean.parseBoolean((currentNode.getProperty("exo:metaSitemap")).getString()));      
        if (currentNode.hasProperty("exo:metaPriority"))       
          metaModel.setPriority(Long.parseLong((currentNode.getProperty("exo:metaPriority")).getString()));      
        if (currentNode.hasProperty("exo:metaFrequency"))       
          metaModel.setFrequency((currentNode.getProperty("exo:metaFrequency")).getString());
        cache.put(hash, metaModel);
      }      
    }
    return metaModel;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getPageMetadata(String pageUri, String pageLanguage) throws Exception {      
    PageMetadataModel metaModel = null;     
    String hash = getHash(pageUri, pageLanguage);    
    if(cache.get(hash) != null) 
      metaModel = (PageMetadataModel)cache.get(hash);     
    if(metaModel == null) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession("portal-system", WCMCoreUtils.getRepository());
      String uuid = Util.getUIPortal().getSelectedUserNode().getId();
      Node pageNode = session.getNodeByUUID(uuid);
      
      if(pageNode.hasNode("exo:pageMetadata")) {
        Node currentNode = pageNode.getNode("exo:pageMetadata");
        metaModel = new PageMetadataModel();
        if (currentNode.hasProperty("exo:metaTitle"))       
          metaModel.setTitle((currentNode.getProperty("exo:metaTitle")).getString());      
        if (currentNode.hasProperty("exo:metaKeywords"))       
          metaModel.setKeywords((currentNode.getProperty("exo:metaKeywords")).getString());
        if (currentNode.hasProperty("exo:metaDescription"))       
          metaModel.setDescription((currentNode.getProperty("exo:metaDescription")).getString());
        if (currentNode.hasProperty("exo:metaLanguage"))       
          metaModel.setLanguage((currentNode.getProperty("exo:metaLanguage")).getString());        
        if (currentNode.hasProperty("exo:metaRobots"))       
          metaModel.setRobotsContent((currentNode.getProperty("exo:metaRobots")).getString());      
        if (currentNode.hasProperty("exo:metaSitemap"))       
          metaModel.setSiteMap(Boolean.parseBoolean((currentNode.getProperty("exo:metaSitemap")).getString()));      
        if (currentNode.hasProperty("exo:metaPriority"))       
          metaModel.setPriority(Long.parseLong((currentNode.getProperty("exo:metaPriority")).getString()));      
        if (currentNode.hasProperty("exo:metaFrequency"))       
          metaModel.setFrequency((currentNode.getProperty("exo:metaFrequency")).getString());
        cache.put(hash, metaModel);
      }   
    }
    return metaModel;
  }
  
  
  /**
   * {@inheritDoc}
   */
  public void removePageMetadata(PageMetadataModel metaModel, boolean onContent) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = this.getSession(sessionProvider);
    String pageUri = "";
    if(onContent) pageUri = getStandardURL(metaModel.getUri());
    else pageUri = metaModel.getPageReference();       
    String language = metaModel.getLanguage();    
    String query = "select * from exo:pageMetadata  WHERE exo:metaUri LIKE '"
      + pageUri + " AND exo:metaLanguage LIKE " + "'" + language + "'";
    QueryManager queryManager = session.getWorkspace()
    .getQueryManager();
    QueryImpl queryImp = (QueryImpl) queryManager.createQuery(query,
        Query.SQL);
    QueryResult queryResult = queryImp.execute();
    NodeIterator nodeIterator = queryResult.getNodes();
    
    if (nodeIterator.getSize() > 0) {
      while (nodeIterator.hasNext()) {
        Node currentNode = nodeIterator.nextNode();
        currentNode.remove();
      }
      session.save();
    }
    //Remove metadata for this uri from cache
    String hash = getHash(pageUri, language);
    cache.remove(hash);
  }  
  
  /**
   * Returns jcr current session
   * 
   * @param sessionProvider The session provider
   * @return
   * @throws Exception
   */
  private Session getSession(SessionProvider sessionProvider)
  throws Exception {
    ManageableRepository currentRepo = this.repositoryService
    .getCurrentRepository();
    return sessionProvider.getSession(currentRepo.getConfiguration()
        .getDefaultWorkspaceName(), currentRepo);
  }
  /**
   * Update sitemap content for portal
   * 
   * @param uri The uri of page
   * @param priority The priority of page
   * @param frequency The frequency of page
   * @param visibleSitemap 
   * visibleSitemap = true page is visible on sitemap
   * visibleSitemap = false page is invisible on sitemap
   * @param portalName The portal name
   * @throws Exception
   */
  public void updateSiteMap(String uri, float priority, String frequency, boolean visibleSitemap, String portalName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
    Session session = dummyNode.getSession();
    uri = getStandardURL(uri);
    String uri_clone = "";
    String public_path = "/"+PUBLIC_MODE+"/";
    String private_path = "/"+PRIVATE_MODE+"/";
    if(uri.indexOf(public_path) > 0)
      uri_clone = uri.replaceFirst(public_path, private_path);
    else if (uri.indexOf(private_path) > 0)
      uri_clone = uri.replaceFirst(private_path, public_path);
    
    String sitemapData = "";
    if(!dummyNode.getNode(METADATA_BASE_PATH).hasNode(SITEMAP_NAME)) {
      dummyNode.getNode(METADATA_BASE_PATH).addNode(SITEMAP_NAME, "nt:file");
      Node simapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/" + SITEMAP_NAME);
      Node sitemapNode = simapFolder.addNode("jcr:content", "nt:resource");
      sitemapNode.setProperty("jcr:mimeType", "text/xml");
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      //root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("urlset");
      doc.appendChild(rootElement);
      if(visibleSitemap) {
        //Create element in sitemap for uri
        Element urlElement = doc.createElement("url");
        rootElement.appendChild(urlElement);        
        Element locElement = doc.createElement("loc"); 
        locElement.setTextContent(uri);
        urlElement.appendChild(locElement);        
        Element freqElement = doc.createElement("changefreq");        
        freqElement.setTextContent(frequency);
        urlElement.appendChild(freqElement);        
        Element priorityElement = doc.createElement("priority");        
        priorityElement.setTextContent(String.valueOf(priority));   
        urlElement.appendChild(priorityElement);
        //Create element in sitemap for uri_clone
        if(uri_clone != null && uri_clone.length() > 0) {
          urlElement = doc.createElement("url");
          rootElement.appendChild(urlElement);        
          locElement = doc.createElement("loc"); 
          locElement.setTextContent(uri_clone);
          urlElement.appendChild(locElement);        
          freqElement = doc.createElement("changefreq");        
          freqElement.setTextContent(frequency);
          urlElement.appendChild(freqElement);        
          priorityElement = doc.createElement("priority");        
          priorityElement.setTextContent(String.valueOf(priority));   
          urlElement.appendChild(priorityElement);
        }
      }
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      transformer.transform(source, new StreamResult(writer)); 
      sitemapData = writer.toString();
      sitemapNode.setProperty("jcr:data", sitemapData);      
      sitemapNode.setProperty("jcr:lastModified", new GregorianCalendar());
      session.save();
    } else {
      Node sitemapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/" + SITEMAP_NAME);
      Node sitemapNode = sitemapFolder.getNode("jcr:content");
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      InputStream stream = sitemapNode.getProperty("jcr:data").getStream();
      Document doc = docBuilder.parse(stream);
      
      // normalize text representation
      boolean fLoc = false;
      doc.getDocumentElement ().normalize ();
      Element root = doc.getDocumentElement(); 
      ArrayList<org.w3c.dom.Node> arrNodes = new ArrayList<org.w3c.dom.Node>();
      org.w3c.dom.NodeList listOfUrls = doc.getElementsByTagName("url");
      for(int i = 0; i < listOfUrls.getLength(); i++) {        
        org.w3c.dom.Node urlNode = listOfUrls.item(i);
        if(urlNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE){
          Element urlElement = (Element)urlNode;
          org.w3c.dom.NodeList locList = urlElement.getElementsByTagName("loc");           
          Element locElement = (Element)locList.item(0);   
          // The location is exist
          String locationValue = locElement.getChildNodes().item(0).getNodeValue();
          if(locationValue != null & (locationValue.trim().equals(uri) || locationValue.trim().equals(uri_clone))) {
            fLoc = true;
            if(visibleSitemap) {
              org.w3c.dom.Node freqNode = urlElement.getElementsByTagName("changefreq").item(0);
              freqNode.setTextContent(frequency);
              org.w3c.dom.Node priorityNode = urlElement.getElementsByTagName("priority").item(0);
              priorityNode.setTextContent(String.valueOf(priority));
            } else {              
              arrNodes.add(urlNode);              
            }
          }
        }       
      }
      //Remove element from sitemap.xml
      if(arrNodes != null && arrNodes.size() > 0) {
        for(int i=0; i<arrNodes.size(); i++) {
          root.removeChild(arrNodes.get(i));
        }
      }
      //Update xml document for sitemap
      if(!fLoc && visibleSitemap) {
        //Create element in sitemap for uri
        Element urlElement = doc.createElement("url");
        Element locElement = doc.createElement("loc");
        locElement.setTextContent(uri);
        Element freqElement = doc.createElement("changefreq");
        freqElement.setTextContent(frequency);
        Element priorityElement = doc.createElement("priority");
        priorityElement.setTextContent(String.valueOf(priority));
        urlElement.appendChild(locElement);
        urlElement.appendChild(freqElement);
        urlElement.appendChild(priorityElement);
        root.appendChild(urlElement);          
        //create element in sitemap for uri_clone
        if(uri_clone != null && uri_clone.length() > 0) {
          urlElement = doc.createElement("url");
          locElement = doc.createElement("loc");
          locElement.setTextContent(uri_clone);
          freqElement = doc.createElement("changefreq");
          freqElement.setTextContent(frequency);
          priorityElement = doc.createElement("priority");
          priorityElement.setTextContent(String.valueOf(priority));
          urlElement.appendChild(locElement);
          urlElement.appendChild(freqElement);
          urlElement.appendChild(priorityElement);
          root.appendChild(urlElement);
        }
      }
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      transformer.transform(source, new StreamResult(writer)); 
      sitemapData = writer.toString();
      sitemapNode.setProperty("jcr:data", sitemapData);
      sitemapNode.setProperty("jcr:lastModified", new GregorianCalendar());
    }
    
    if(sitemapData != null && sitemapData.length() > 0) {
      String hash = getHash(portalName, null);
      cache.put(hash, sitemapData);
    }
    session.save();
  }
  /**
   * {@inheritDoc}
   */
  public String getSitemap(String portalName) throws Exception {
    String sitemapContent = null;
    String hash = getHash(portalName, null);
    if(cache.get(hash) != null)     
      sitemapContent = (String)cache.get(hash);   
        
    if(sitemapContent ==null || sitemapContent.length() == 0) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
      Session session = dummyNode.getSession();
      if(dummyNode.hasNode(METADATA_BASE_PATH) && dummyNode.getNode(METADATA_BASE_PATH).hasNode(SITEMAP_NAME)) {
        Node sitemapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/" + SITEMAP_NAME);
        Node sitemapNode = sitemapFolder.getNode("jcr:content");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        InputStream stream = sitemapNode.getProperty("jcr:data").getStream();
        Document doc = docBuilder.parse(stream);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer)); 
        sitemapContent = writer.toString();
      } else {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("urlset");
        doc.appendChild(rootElement);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer)); 
        sitemapContent = writer.toString();
      }
      session.save();
    }
    return sitemapContent;
  }
  /**
   * Returns hash
   * 
   * @param uri The uri of page
   * @param language The language of page
   * @return
   * @throws Exception
   */
  private String getHash(String uri,  String language) throws Exception{
    String key = uri;
    if(language != null) key += "&&" + language;
    return MessageDigester.getHash(key);
  }
  
  private String getStandardURL(String path) throws Exception {
    if(path.substring(path.length()-1, path.length()).equals("/"))
      path = path.substring(0,path.length()-1);
    return path;
  }
  
  public Node getContentNode(String seoPath) throws Exception {
    Node seoNode = null;    
    if(seoPath != null && seoPath.length() > 0) {
      String tmpPath = seoPath.trim();
      if(tmpPath.startsWith("/"))
        tmpPath = tmpPath.substring(1,tmpPath.length());
      String[] arrPath = tmpPath.split("/");
      if(arrPath != null && arrPath.length > 3) {
        String repo = arrPath[0];
        String ws = arrPath[1];
        if(repo != null && ws != null) {
          String nodePath = tmpPath.substring(tmpPath.indexOf(ws) + ws.length(),tmpPath.length());
          if(nodePath != null && nodePath.length() > 0) {
            ManageableRepository manageRepo = WCMCoreUtils.getRepository();
            Session session = WCMCoreUtils.getUserSessionProvider().getSession(ws, manageRepo) ;            
            if(session.getItem(nodePath).isNode()) {
              seoNode = (Node)session.getItem(nodePath);
            }
          }
        }
      }
    }      
    return seoNode;
  }
  
  @Managed
  @ManagedDescription("Is the cache used ?")
  public boolean isCached() {
    return isCached;
  }
  
  @Managed
  @ManagedDescription("How many nodes in the cache ?")
  public int getCachedEntries() {
    return this.cache.getCacheSize();
  }

  @Managed
  @ManagedDescription("Activate/deactivate the composer cache ?")
  public void setCached(@ManagedDescription("Enable/Disable the cache ?") @ManagedName("isCached") boolean isCached) {
    this.isCached = isCached;
  }
}
