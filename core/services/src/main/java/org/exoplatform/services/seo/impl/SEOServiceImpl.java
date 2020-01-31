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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.ecm.utils.MessageDigester;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOConfig;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 17, 2011
 */
public class SEOServiceImpl implements SEOService {

  private ExoCache<String, Object> cache;

  public static final String EMPTY_CACHE_ENTRY = "EMPTY";
  public static String METADATA_BASE_PATH = "SEO";
  final static public String LANGUAGES    = "seo-languages";
  public static String METADATA_PAGE_PATH = "pages";
  public static String METADATA_CONTENT_PATH = "contents";
  public static String SITEMAP_NAME = "sitemaps";
  public static String ROBOTS_NAME = "robots";
  private static String PUBLIC_MODE = "public";
  private static String PRIVATE_MODE = "private";

  private final static String CACHE_NAME = "ecms.seo";

  private List<String> robotsindex = new ArrayList<String>();
  private List<String> robotsfollow = new ArrayList<String>();
  private List<String> frequency = new ArrayList<String>();
  private SEOConfig seoConfig = null;

  private boolean isCached = true;

  private ListenerService listenerService;

  private static final Logger log = LoggerFactory.getLogger(SEOServiceImpl.class);

  /**
   * Constructor method
   *
   * @param initParams
   *          The initial parameters
   * @throws Exception
   */
  public SEOServiceImpl(InitParams initParams, ListenerService listenerService) throws Exception {
    ObjectParameter param = initParams.getObjectParam("seo.config");
    if (param != null) {
      seoConfig = (SEOConfig) param.getObject();
      robotsindex = seoConfig.getRobotsIndex();
      robotsfollow = seoConfig.getRobotsFollow();
      frequency = seoConfig.getFrequency();
    }
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(
            CACHE_NAME);
    this.listenerService = listenerService;
  }

  /**
   *{@inheritDoc}
   */
  public List<String> getRobotsIndexOptions() {
    return robotsindex;
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getRobotsFollowOptions() {
    return robotsfollow;
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getFrequencyOptions() {
    return frequency;
  }

  /**
   * {@inheritDoc}
   */
  public void storeMetadata(PageMetadataModel metaModel, String portalName,
                            boolean onContent, String language) throws Exception {
    String uri = metaModel.getUri();
    String cachedHash = getPageOrContentCacheKey(uri, language);
    if (cache.get(cachedHash) != null) {
      cache.remove(cachedHash);
    }
    String pageReference = metaModel.getPageReference();
    cachedHash = getPageOrContentCacheKey(metaModel.getPageReference(), language);
    if (cache.get(cachedHash) != null) {
      cache.remove(cachedHash);
    }

    // Inherit from parent page
    /*
     * if(!onContent) { if(metaModel.getPageParent() != null &&
     * metaModel.getPageParent().length() > 0) { PageMetadataModel parentModel =
     * this.getPageMetadata(metaModel.getPageParent()); if(parentModel != null)
     * { metaModel = parentModel; metaModel.setUri(uri);
     * metaModel.setPageReference(pageReference); } } }
     */
    String title = metaModel.getTitle();
    String keyword = metaModel.getKeywords();
    String description = metaModel.getDescription();
    String robots = metaModel.getRobotsContent();
    String fullStatus = metaModel.getFullStatus();
    boolean sitemap = metaModel.getSitemap();
    float priority = metaModel.getPriority();
    String frequency = metaModel.getFrequency();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = null;
    LivePortalManagerService livePortalManagerService = WCMCoreUtils
        .getService(LivePortalManagerService.class);
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider,
                                                            portalName);
    session = dummyNode.getSession();
    if (!dummyNode.hasNode(METADATA_BASE_PATH)) {
      dummyNode.addNode(METADATA_BASE_PATH);
      session.save();
    }
    //Store robots data
    updateRobots(dummyNode, portalName);    
    // Store sitemap data
    Node node = null;
    if (onContent) {
      node = session.getNodeByUUID(uri);
      if (!node.isNodeType("mix:referenceable")) {
        node.addMixin("mix:referenceable");
      }
    } else {
      node = getNavNode();
      if (!node.isNodeType("exo:seoMetadata")) {
        node.addMixin("exo:seoMetadata");
      }
    }    

    Node languageNode = null;
    if(node.hasNode(LANGUAGES))
      languageNode = node.getNode(LANGUAGES);
    else
      languageNode = node.addNode(LANGUAGES);
    if(languageNode.canAddMixin("exo:hiddenable"))
      languageNode.addMixin("exo:hiddenable");
    session.save();
    Node seoNode = null;
    if(languageNode.hasNode(language)) seoNode = languageNode.getNode(language);
    else seoNode = languageNode.addNode(language);
    if (!seoNode.isNodeType("mix:referenceable")) {
      seoNode.addMixin("mix:referenceable");
    }
    if (seoNode.isNodeType("exo:pageMetadata")) {
      seoNode.setProperty("exo:metaKeywords", keyword);
      seoNode.setProperty("exo:metaDescription", description);
      seoNode.setProperty("exo:metaFully", fullStatus);
      if (!onContent) {
        seoNode.setProperty("exo:metaTitle", title);
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
      }
      String hash = null;
      if (onContent)
        hash = cachedHash;
      else
        hash = getPageOrContentCacheKey(pageReference, language);
      if (hash != null)
        cache.put(hash, metaModel);
    } else {
      String hash = null;
      seoNode.addMixin("exo:pageMetadata");
      seoNode.setProperty("exo:metaKeywords", keyword);
      seoNode.setProperty("exo:metaDescription", description);
      seoNode.setProperty("exo:metaFully", fullStatus);
      if (onContent) {
        seoNode.setProperty("exo:metaUri", seoNode.getUUID());
        hash = getHash(seoNode.getUUID() + language);
      } else {      	
        seoNode.setProperty("exo:metaTitle", title);
        seoNode.setProperty("exo:metaUri", pageReference);
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
        hash = getPageOrContentCacheKey(pageReference, language);
      }
      if (hash != null)
        cache.put(hash, metaModel);
    }
    session.save();
    notify(SAVE_SEO, metaModel);
  }

  private String getPageOrContentCacheKey(String uri, String language) throws Exception {
    return getHash(uri + language);
  } 

  public String getState(String path, String language, boolean onContent) throws Exception{
    String state = "Empty";
    Node node = null;
    String hash = null;
    PageMetadataModel metaModel = null;
    if(onContent) {
      node = getContentNode(path);
      hash = node == null ? StringUtils.EMPTY : getPageOrContentCacheKey(node.getUUID(), language);
      metaModel = (PageMetadataModel) getCachedEntry(hash, false);
      if(metaModel != null) return metaModel.getFullStatus();

    } else {
      hash = getPageOrContentCacheKey(path, language);
      metaModel = (PageMetadataModel) getCachedEntry(hash, true);
      if (isNullObject(metaModel)) {
        return null;
      }
      if(metaModel != null) return metaModel.getFullStatus();
      node = getNavNode();
    }
    if(node.hasNode(LANGUAGES+"/"+language)) {
      Node seoNode = node.getNode(LANGUAGES+"/"+language);
      if (seoNode.isNodeType("exo:pageMetadata") && seoNode.hasProperty("exo:metaFully"))
        return seoNode.getProperty("exo:metaFully").getString();  			
    }
    return state;
  }

  private boolean isNullObject(PageMetadataModel metaModel) {
    return metaModel == PageMetadataModel.NULL_PAGE_METADATA_MODEL;
  }

  private Object getCachedEntry(String hash, boolean cacheNull) {
    Object object = cache.get(hash);
    if (cacheNull) {
      if( object == null) {
        cache.put(hash, EMPTY_CACHE_ENTRY);
      } else if(EMPTY_CACHE_ENTRY.equals(object)) {
        return PageMetadataModel.NULL_PAGE_METADATA_MODEL;
      }
    }
    return object;
  }

  public PageMetadataModel getMetadata(ArrayList<String> params,
                                       String pageReference, String language) throws Exception {
    PageMetadataModel metaModel = null;
    if (params != null) {
      metaModel = getContentMetadata(params, language);
      if(metaModel == null) metaModel = getPageMetadata(pageReference, language);
    } else {
      metaModel = getPageMetadata(pageReference, language);
    }
    return metaModel;
  }

  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getContentMetadata(ArrayList<String> params, String language)
      throws Exception {
    PageMetadataModel metaModel = null;
    String pageUri = null;
    Node contentNode = null;
    for (int i = 0; i < params.size(); i++) {
      contentNode = this.getContentNode(params.get(i).toString());
      if (contentNode != null)
        break;
    }

    if (contentNode == null)
      return null;
    if (!contentNode.isNodeType("mix:referenceable")) {
      contentNode.addMixin("mix:referenceable");
    }
    String hash = getPageOrContentCacheKey(contentNode.getUUID(), language);
    metaModel = (PageMetadataModel) getCachedEntry(hash, false);

    if (metaModel == null && contentNode.hasNode(LANGUAGES+"/"+language)) {
      //Getting seo node by language
      Node seoNode = contentNode.getNode(LANGUAGES+"/"+language);
      if (!seoNode.isNodeType("mix:referenceable")) {
        seoNode.addMixin("mix:referenceable");
      }
      if (seoNode.isNodeType("exo:pageMetadata")) {
        metaModel = new PageMetadataModel();
        metaModel.setUri(pageUri);        
        if (seoNode.hasProperty("exo:metaKeywords"))
          metaModel.setKeywords((seoNode.getProperty("exo:metaKeywords"))
                                .getString());
        if (seoNode.hasProperty("exo:metaDescription"))
          metaModel.setDescription((seoNode
              .getProperty("exo:metaDescription")).getString());
        if (seoNode.hasProperty("exo:metaRobots"))
          metaModel
          .setRobotsContent((seoNode.getProperty("exo:metaRobots"))
                            .getString());
        if (seoNode.hasProperty("exo:metaSitemap"))
          metaModel.setSiteMap(Boolean.parseBoolean((seoNode
              .getProperty("exo:metaSitemap")).getString()));
        if (seoNode.hasProperty("exo:metaPriority"))
          metaModel.setPriority(Long.parseLong((seoNode
              .getProperty("exo:metaPriority")).getString()));
        if (seoNode.hasProperty("exo:metaFrequency"))
          metaModel.setFrequency((seoNode.getProperty("exo:metaFrequency"))
                                 .getString());
        if (seoNode.hasProperty("exo:metaFully"))
          metaModel.setFullStatus((seoNode.getProperty("exo:metaFully"))
                                  .getString());
        cache.put(hash, metaModel);
      }
    }
    return metaModel;
  }

  public Map<String, PageMetadataModel> getPageMetadatas(String id, String siteName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    LivePortalManagerService livePortalManagerService = WCMCoreUtils
            .getService(LivePortalManagerService.class);
    Node livePortalNode;
    try {
       livePortalNode = livePortalManagerService.getLivePortal(sessionProvider, siteName);
    } catch (PathNotFoundException ex) {
      livePortalNode = null;
    }
    //
    if (livePortalNode != null) {
      Node navNode = null;
      if (!livePortalNode.hasNode(NAVIGATION)) {
        navNode = livePortalNode.addNode(NAVIGATION);
      } else {
        navNode = livePortalNode.getNode(NAVIGATION);
      }
      //

      Node node = null;
      if (!navNode.hasNode(id)) {
        node = navNode.addNode(id);
      } else {
        node = navNode.getNode(id);
      }

      Node languageNode = null;
      if(node.hasNode(LANGUAGES)) {
        languageNode = node.getNode(LANGUAGES);
      } else {
        languageNode = node.addNode(LANGUAGES);
      }

      if(languageNode.canAddMixin("exo:hiddenable")) {
        languageNode.addMixin("exo:hiddenable");
      }

      Map<String, PageMetadataModel> metadataModels = new HashMap<>();
      Iterator<Node> children = languageNode.getNodes();
      while (children.hasNext()) {
        Node child = children.next();
        String language = child.getName();
        Node seoNode = languageNode.getNode(language);
        if(seoNode.isNodeType("exo:pageMetadata")) {
          PageMetadataModel metaModel = new PageMetadataModel();
          if (seoNode.hasProperty("exo:metaTitle"))
            metaModel.setTitle((seoNode.getProperty("exo:metaTitle")).getString());
          if (seoNode.hasProperty("exo:metaKeywords"))
            metaModel.setKeywords((seoNode.getProperty("exo:metaKeywords"))
                    .getString());
          if (seoNode.hasProperty("exo:metaDescription"))
            metaModel
                    .setDescription((seoNode.getProperty("exo:metaDescription"))
                            .getString());
          if (seoNode.hasProperty("exo:metaRobots"))
            metaModel.setRobotsContent((seoNode.getProperty("exo:metaRobots"))
                    .getString());
          if (seoNode.hasProperty("exo:metaSitemap"))
            metaModel.setSiteMap(Boolean.parseBoolean((seoNode
                    .getProperty("exo:metaSitemap")).getString()));
          if (seoNode.hasProperty("exo:metaPriority"))
            metaModel.setPriority(Long.parseLong((seoNode
                    .getProperty("exo:metaPriority")).getString()));
          if (seoNode.hasProperty("exo:metaFrequency"))
            metaModel.setFrequency((seoNode.getProperty("exo:metaFrequency"))
                    .getString());
          if (seoNode.hasProperty("exo:metaFully"))
            metaModel.setFullStatus((seoNode.getProperty("exo:metaFully"))
                    .getString());
          metadataModels.put(language, metaModel);
        }
      }
      return metadataModels;
    } else {
      return Collections.emptyMap();
    }
  }

  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getPageMetadata(String pageUri, String language) throws Exception {
    PageMetadataModel metaModel = null;
    String hash = getPageOrContentCacheKey(pageUri, language);
    metaModel = (PageMetadataModel) getCachedEntry(hash, true);
    if (isNullObject(metaModel)) {
      return null;
    }
    if (metaModel == null) {
      Node pageNode = getNavNode();

      if (pageNode != null && pageNode.hasNode(LANGUAGES+"/"+language)) {
        Node seoNode = pageNode.getNode(LANGUAGES+"/"+language);
        if(seoNode.isNodeType("exo:pageMetadata")) {
          metaModel = new PageMetadataModel();
          if (seoNode.hasProperty("exo:metaTitle"))
            metaModel.setTitle((seoNode.getProperty("exo:metaTitle")).getString());
          if (seoNode.hasProperty("exo:metaKeywords"))
            metaModel.setKeywords((seoNode.getProperty("exo:metaKeywords"))
                                  .getString());
          if (seoNode.hasProperty("exo:metaDescription"))
            metaModel
            .setDescription((seoNode.getProperty("exo:metaDescription"))
                            .getString());
          if (seoNode.hasProperty("exo:metaRobots"))
            metaModel.setRobotsContent((seoNode.getProperty("exo:metaRobots"))
                                       .getString());
          if (seoNode.hasProperty("exo:metaSitemap"))
            metaModel.setSiteMap(Boolean.parseBoolean((seoNode
                .getProperty("exo:metaSitemap")).getString()));
          if (seoNode.hasProperty("exo:metaPriority"))
            metaModel.setPriority(Long.parseLong((seoNode
                .getProperty("exo:metaPriority")).getString()));
          if (seoNode.hasProperty("exo:metaFrequency"))
            metaModel.setFrequency((seoNode.getProperty("exo:metaFrequency"))
                                   .getString());
          if (seoNode.hasProperty("exo:metaFully"))
            metaModel.setFullStatus((seoNode.getProperty("exo:metaFully"))
                                    .getString());
          cache.put(hash, metaModel);
        }
      }
    }
    return metaModel;
  }

  public List<Locale> getSEOLanguages(String portalName, String seoPath, boolean onContent) throws Exception {  	
    List<Locale> languages = new ArrayList<Locale>();
    Node languagesNode = null;
    if(onContent) {
      Node contentNode = null;
      contentNode = getContentNode(seoPath);
      if (contentNode != null && contentNode.hasNode(LANGUAGES)) languagesNode = contentNode.getNode(LANGUAGES);  	
    } else {
      Node pageNode = getNavNode();

      if (pageNode != null && pageNode.hasNode(LANGUAGES)) languagesNode = pageNode.getNode(LANGUAGES);
    }
    if(languagesNode != null) {
      NodeIterator iter = languagesNode.getNodes();
      while(iter.hasNext()) {   
        String lang = iter.nextNode().getName();
        String[] arr = lang.split("_");
        if(arr.length > 1) {
          languages.add(new Locale(arr[0],arr[1]));    			
        } else languages.add(new Locale(lang));
      }    	
      Collections.sort(languages, new SEOItemComparator());
      return languages;
    } 
    return languages;
  }

  class SEOItemComparator implements Comparator<Locale> {
    @Override
    public int compare(Locale locale1, Locale locale2) {
      return locale1.getDisplayName().compareTo(locale2.getDisplayName());
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removePageMetadata(PageMetadataModel metaModel,
                                 String portalName, boolean onContent, String language) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    ManageableRepository currentRepo = WCMCoreUtils.getRepository();
    Session session = sessionProvider.getSession(currentRepo.getConfiguration()
                                                 .getDefaultWorkspaceName(), currentRepo);
    String hash = "";
    Node node = null;
    String nodePath = null;
    SiteKey siteKey = null;
    if (onContent) {
      node = session.getNodeByUUID(metaModel.getUri());
    } else {
      node = getNavNode();
      nodePath = Util.getUIPortal().getSelectedUserNode().getURI();
      UserNavigation userNavigation = Util.getUIPortal().getSelectedUserNode().getNavigation();
      siteKey = userNavigation.getKey();
    }    
    Node seoNode = null;
    if(node.hasNode(LANGUAGES+"/"+language)) 
      seoNode = node.getNode(LANGUAGES+"/"+language);

    if (seoNode != null) {
      seoNode.remove();
      if (onContent)
        hash = getHash(metaModel.getUri() + language);
      else
        hash = getHash(metaModel.getPageReference() + language);
      cache.remove(hash);
    }
    session.save();
    if (StringUtils.isNotBlank(nodePath)) {
      metaModel.setUri(nodePath);
      metaModel.setSiteKey(siteKey);
      notify(SEO_REMOVE, metaModel);
    }
  }

  /**
   * Update sitemap content for portal
   *
   * @param uri
   *          The uri of page
   * @param priority
   *          The priority of page
   * @param frequency
   *          The frequency of page
   * @param visibleSitemap
   *          visibleSitemap = true page is visible on sitemap visibleSitemap =
   *          false page is invisible on sitemap
   * @param portalName
   *          The portal name
   * @throws Exception
   */
  public void updateSiteMap(String uri, float priority, String frequency,
                            boolean visibleSitemap, String portalName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    LivePortalManagerService livePortalManagerService = WCMCoreUtils
        .getService(LivePortalManagerService.class);
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider,
                                                            portalName);
    Session session = dummyNode.getSession();
    uri = getStandardURL(uri);
    if(uri == null) uri = "";
    String uri_clone = "";
    String public_path = "/" + PUBLIC_MODE + "/";
    String private_path = "/" + PRIVATE_MODE + "/";
    if (uri.indexOf(public_path) > 0)
      uri_clone = uri.replaceFirst(public_path, private_path);
    else if (uri.indexOf(private_path) > 0)
      uri_clone = uri.replaceFirst(private_path, public_path);

    String sitemapData = "";
    if (!dummyNode.getNode(METADATA_BASE_PATH).hasNode(SITEMAP_NAME)) {
      dummyNode.getNode(METADATA_BASE_PATH).addNode(SITEMAP_NAME, "nt:file");
      Node simapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/"
          + SITEMAP_NAME);
      Node sitemapNode = simapFolder.addNode("jcr:content", "nt:resource");
      sitemapNode.setProperty("jcr:mimeType", "text/xml");
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      // root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("urlset");
      rootElement.setAttribute("xmlns",
          "http://www.sitemaps.org/schemas/sitemap/0.9");
      rootElement.setAttribute("xmlns:xsi",
          "http://www.w3.org/2001/XMLSchema-instance");
      rootElement
      .setAttribute(
                    "xsi:schemaLocation",
          "http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
      doc.appendChild(rootElement);
      if (visibleSitemap) {
        // Create element in sitemap for uri
        Element urlElement = doc.createElement("url");
        rootElement.appendChild(urlElement);
        Element locElement = doc.createElement("loc");
        locElement.setTextContent(uri);
        urlElement.appendChild(locElement);
        Element freqElement = doc.createElement("changefreq");
        freqElement.setTextContent(frequency);
        urlElement.appendChild(freqElement);
        Element priorityElement = doc.createElement("priority");
        if(priority >= 0) {        	
          priorityElement.setTextContent(String.valueOf(priority));
          urlElement.appendChild(priorityElement);
        }
        // Create element in sitemap for uri_clone
        if (uri_clone != null && uri_clone.length() > 0) {
          urlElement = doc.createElement("url");
          rootElement.appendChild(urlElement);
          locElement = doc.createElement("loc");
          locElement.setTextContent(uri_clone);
          urlElement.appendChild(locElement);
          freqElement = doc.createElement("changefreq");
          freqElement.setTextContent(frequency);
          urlElement.appendChild(freqElement);          
          if(priority >= 0) {
            priorityElement = doc.createElement("priority");
            priorityElement.setTextContent(String.valueOf(priority));
            urlElement.appendChild(priorityElement);
          }
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
      Node sitemapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/"
          + SITEMAP_NAME);
      Node sitemapNode = sitemapFolder.getNode("jcr:content");
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
          .newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      InputStream stream = sitemapNode.getProperty("jcr:data").getStream();
      Document doc = docBuilder.parse(stream);

      // normalize text representation
      boolean fLoc = false;
      doc.getDocumentElement().normalize();
      Element root = doc.getDocumentElement();
      ArrayList<org.w3c.dom.Node> arrNodes = new ArrayList<org.w3c.dom.Node>();
      org.w3c.dom.NodeList listOfUrls = doc.getElementsByTagName("url");
      for (int i = 0; i < listOfUrls.getLength(); i++) {
        org.w3c.dom.Node urlNode = listOfUrls.item(i);
        if (urlNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
          Element urlElement = (Element) urlNode;
          org.w3c.dom.NodeList locList = urlElement.getElementsByTagName("loc");
          Element locElement = (Element) locList.item(0);
          // The location is exist
          if(locElement.getChildNodes().item(0) != null) {
            String locationValue = locElement.getChildNodes().item(0).getNodeValue();
            if (locationValue != null & (locationValue.trim().equals(uri) || locationValue.trim().equals(uri_clone))) {
              fLoc = true;
              if (visibleSitemap) {
                org.w3c.dom.Node freqNode = urlElement.getElementsByTagName(
                    "changefreq").item(0);
                freqNode.setTextContent(frequency);              
                if(priority >= 0) {
                  org.w3c.dom.Node priorityNode = urlElement.getElementsByTagName("priority").item(0);
                  if(priorityNode == null) {
                    Element priorityElement = doc.createElement("priority");
                    priorityElement.setTextContent(String.valueOf(priority));
                    urlElement.appendChild(priorityElement);
                  } else priorityNode.setTextContent(String.valueOf(priority));
                }
              } else {
                arrNodes.add(urlNode);
              }
            }
          }
        }
      }
      // Remove element from sitemap.xml
      if (arrNodes != null && arrNodes.size() > 0) {
        for (int i = 0; i < arrNodes.size(); i++) {
          root.removeChild(arrNodes.get(i));
        }
      }
      // Update xml document for sitemap
      if (!fLoc && visibleSitemap) {
        // Create element in sitemap for uri
        Element urlElement = doc.createElement("url");
        Element locElement = doc.createElement("loc");
        locElement.setTextContent(uri);
        Element freqElement = doc.createElement("changefreq");
        freqElement.setTextContent(frequency);
        urlElement.appendChild(locElement);
        urlElement.appendChild(freqElement);
        Element priorityElement = doc.createElement("priority");
        if(priority >= 0) {
          priorityElement.setTextContent(String.valueOf(priority));
          urlElement.appendChild(priorityElement);
        }        
        root.appendChild(urlElement);
        // create element in sitemap for uri_clone
        if (uri_clone != null && uri_clone.length() > 0) {
          urlElement = doc.createElement("url");
          locElement = doc.createElement("loc");
          locElement.setTextContent(uri_clone);
          freqElement = doc.createElement("changefreq");
          freqElement.setTextContent(frequency);
          urlElement.appendChild(locElement);
          urlElement.appendChild(freqElement);
          if(priority >= 0) {
            priorityElement = doc.createElement("priority");
            priorityElement.setTextContent(String.valueOf(priority));
            urlElement.appendChild(priorityElement);
          }          
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

    if (sitemapData != null && sitemapData.length() > 0) {
      String hash = getSiteMapCacheKey(portalName);
      cache.put(hash, sitemapData);
    }
    session.save();
  }

  public void updateRobots(Node dummyNode, String portalName) throws Exception {
    if (!dummyNode.getNode(METADATA_BASE_PATH).hasNode(ROBOTS_NAME)) {
      dummyNode.getNode(METADATA_BASE_PATH).addNode(ROBOTS_NAME, "nt:file");
      Node robotsFolder = dummyNode.getNode(METADATA_BASE_PATH + "/"
          + ROBOTS_NAME);
      Node robotsNode = robotsFolder.addNode("jcr:content", "nt:resource");
      robotsNode.setProperty("jcr:mimeType", "text/plain");      
      PortalRequestContext ctx = Util.getPortalRequestContext();
      StringBuffer robotsContent = new StringBuffer("# robots.txt \n");
      robotsContent.append("User-agent: * \n");
      robotsContent.append("Disallow: \n");
      if(ctx.getRequest() != null) {
        if (ctx.getRequest().getServerPort() != 80) {
          robotsContent.append("Sitemap: ")
          .append(ctx.getRequest().getScheme())
          .append("://")
          .append(ctx.getRequest().getServerName())
          .append(":")
          .append(ctx.getRequest().getServerPort())
          .append(ctx.getPortalURI())
          .append("sitemaps.xml \n");
        } else {
          robotsContent.append("Sitemap: ")
          .append(ctx.getRequest().getScheme())
          .append("://")
          .append(ctx.getRequest().getServerName())
          .append(ctx.getPortalURI())
          .append("sitemaps.xml \n");
        }
      }
      robotsNode.setProperty("jcr:data", robotsContent.toString());
      robotsNode.setProperty("jcr:lastModified", new GregorianCalendar());
      cache.put(getRobotsCacheKey(portalName), robotsContent.toString());
    }
  }

  private String getRobotsCacheKey(String portalName) throws Exception {
    return getHash(portalName + ROBOTS_NAME);
  }

  /**
   * {@inheritDoc}
   */
  public String getSitemap(String portalName) throws Exception {
    String hash = getSiteMapCacheKey(portalName);
    String sitemapContent = (String) getCachedEntry(hash, false);

    if (sitemapContent == null || sitemapContent.length() == 0) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      LivePortalManagerService livePortalManagerService = WCMCoreUtils
          .getService(LivePortalManagerService.class);
      Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider,
                                                              portalName);
      Session session = dummyNode.getSession();
      if (dummyNode.hasNode(METADATA_BASE_PATH)
          && dummyNode.getNode(METADATA_BASE_PATH).hasNode(SITEMAP_NAME)) {
        Node sitemapFolder = dummyNode.getNode(METADATA_BASE_PATH + "/"
            + SITEMAP_NAME);
        Node sitemapNode = sitemapFolder.getNode("jcr:content");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
            .newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        InputStream stream = sitemapNode.getProperty("jcr:data").getStream();
        Document doc = docBuilder.parse(stream);
        TransformerFactory transformerFactory = TransformerFactory
            .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));
        sitemapContent = writer.toString();
      } else {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory
            .newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("urlset");
        doc.appendChild(rootElement);
        TransformerFactory transformerFactory = TransformerFactory
            .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));
        sitemapContent = writer.toString();
      }
      session.save();
      cache.put(hash, sitemapContent);
    }
    return sitemapContent;
  }

  private String getSiteMapCacheKey(String portalName) throws Exception {
    return getHash(portalName + SITEMAP_NAME);
  }

  /**
   * {@inheritDoc}
   */
  public String getRobots(String portalName) throws Exception {
    String hash = getRobotsCacheKey(portalName);
    String robotsCache = (String) getCachedEntry(hash, false);

    if (robotsCache != null && robotsCache.trim().length() > 0) {
      return robotsCache;
    }

    StringBuffer robotsContent = null;
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
    Session session = dummyNode.getSession();
    if (dummyNode.hasNode(METADATA_BASE_PATH)
        && dummyNode.getNode(METADATA_BASE_PATH).hasNode(ROBOTS_NAME)) {
      Node robotsFolder = dummyNode.getNode(METADATA_BASE_PATH + "/" + ROBOTS_NAME);
      Node robotsNode = robotsFolder.getNode("jcr:content");
      robotsContent = new StringBuffer(robotsNode.getProperty("jcr:data").getValue().getString());
    } else {
      robotsContent = new StringBuffer("# robots.txt \n");
      robotsContent.append("User-agent: * \n");
      robotsContent.append("Disallow: \n");
    }
    session.save();
    cache.put(hash, robotsContent.toString());
    return robotsContent.toString();
  }

  /**
   * Returns hash
   *
   * @param uri The uri of page
   * @return
   * @throws Exception
   */
  public String getHash(String uri) throws Exception {
    String key = uri;
    return MessageDigester.getHash(key);
  }

  private String getStandardURL(String path) throws Exception {
    if(path != null) {
      if (path.substring(path.length() - 1, path.length()).equals("/"))
        path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  private Node getNavNode() throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    LivePortalManagerService livePortalManagerService = WCMCoreUtils
            .getService(LivePortalManagerService.class);
    UIPortal uiPortal = Util.getUIPortal();
    if (uiPortal == null) {
      return null;
    }

    Node livePortalNode = livePortalManagerService.getLivePortal(sessionProvider, uiPortal.getName());
    if (livePortalNode != null) {
      String id = uiPortal.getSelectedUserNode().getId();

      Node navNode = null;
      if (!livePortalNode.hasNode(NAVIGATION)) {
        navNode = livePortalNode.addNode(NAVIGATION);
        if (navNode.canAddMixin("exo:hiddenable")) {
          navNode.addMixin("exo:hiddenable");
        }
      } else {
        navNode = livePortalNode.getNode(NAVIGATION);
      }
      //

      Node node = null;
      if (!navNode.hasNode(id)) {
        node = navNode.addNode(id);
      } else {
        node = navNode.getNode(id);
      }
      return node;
    } else {
      throw new IllegalStateException("live portal node not found " + uiPortal.getName());
    }
  }

  public Node getContentNode(String seoPath) throws Exception {
    Node seoNode = null;
    if (seoPath != null && seoPath.length() > 0) {
      String tmpPath = seoPath.trim();
      if (tmpPath.startsWith("/"))
        tmpPath = tmpPath.substring(1, tmpPath.length());
      String[] arrPath = tmpPath.split("/");
      if (arrPath != null && arrPath.length > 3) {
        String repo = arrPath[0];
        String ws = arrPath[1];
        if (repo != null && ws != null) {
          boolean isWs = false;
          String nodePath = tmpPath.substring(
                                              tmpPath.indexOf(ws) + ws.length(), tmpPath.length());
          if (nodePath != null && nodePath.length() > 0) {
            ManageableRepository manageRepo = WCMCoreUtils.getRepository();
            List<WorkspaceEntry> wsList = manageRepo.getConfiguration().getWorkspaceEntries();
            for (int i = 0; i < wsList.size(); i++) {
              WorkspaceEntry wsEntry = (WorkspaceEntry) wsList.get(i);
              if (wsEntry.getName().equals(ws)) {
                isWs = true;
                break;
              }
            }
            if (isWs) {
              Session session = WCMCoreUtils.getUserSessionProvider()
                  .getSession(ws, manageRepo);
              nodePath = nodePath.replaceAll("//", "/");
              if (session.getItem(nodePath) != null) {
                if (session.getItem(nodePath).isNode()) {
                  seoNode = (Node) session.getItem(nodePath);
                }
              }
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
  public void setCached(
                        @ManagedDescription("Enable/Disable the cache ?") @ManagedName("isCached") boolean isCached) {
    this.isCached = isCached;
  }

  private void notify(String name, PageMetadataModel metadataModel) {
    try {
      listenerService.broadcast(name, this, metadataModel);
    } catch (Exception e) {
      log.error("Error when delivering notification " + name + " for SEO " + metadataModel, e);
    }
  }
}
