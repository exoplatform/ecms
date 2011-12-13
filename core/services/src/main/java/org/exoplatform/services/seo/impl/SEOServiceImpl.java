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
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.ecm.utils.MessageDigester;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOConfig;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 17, 2011
 */
public class SEOServiceImpl implements SEOService {
  private ExoCache<String, Object> cache;

  public static String METADATA_BASE_PATH = "SEO";
  public static String METADATA_PAGE_PATH = "pages";
  public static String METADATA_CONTENT_PATH = "contents";
  public static String SITEMAP_NAME = "sitemaps";
  public static String ROBOTS_NAME = "robots";
  private static String PUBLIC_MODE = "public";
  private static String PRIVATE_MODE = "private";

  private List<String> robotsindex = new ArrayList<String>();
  private List<String> robotsfollow = new ArrayList<String>();
  private List<String> frequency = new ArrayList<String>();
  private SEOConfig seoConfig = null;

  private boolean isCached = true;

  /**
   * Constructor method
   *
   * @param initParams
   *          The initial parameters
   * @throws Exception
   */
  public SEOServiceImpl(InitParams initParams) throws Exception {
    ObjectParameter param = initParams.getObjectParam("seo.config");
    if (param != null) {
      seoConfig = (SEOConfig) param.getObject();
      robotsindex = seoConfig.getRobotsIndex();
      robotsfollow = seoConfig.getRobotsFollow();
      frequency = seoConfig.getFrequency();
    }
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(
        "wcm.seo");
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
  public void storePageMetadata(PageMetadataModel metaModel, String portalName,
      boolean onContent) throws Exception {
    String uri = metaModel.getUri();
    String pageReference = metaModel.getPageReference();
    // Inherit from parent page
    /*
     * if(!onContent) { if(metaModel.getPageParent() != null &&
     * metaModel.getPageParent().length() > 0) { PageMetadataModel parentModel =
     * this.getPageMetadata(metaModel.getPageParent()); if(parentModel != null)
     * { metaModel = parentModel; metaModel.setUri(uri);
     * metaModel.setPageReference(pageReference); } } }
     */
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
      robotsNode.setProperty("jcr:data", robotsContent.toString());
      robotsNode.setProperty("jcr:lastModified", new GregorianCalendar());
      cache.put(getHash(portalName + ROBOTS_NAME), robotsContent);
    }
    // Store sitemap.xml file
    Node seoNode = null;
    if (onContent) {
      seoNode = session.getNodeByUUID(uri);
      if (!seoNode.isNodeType("mix:referenceable")) {
        seoNode.addMixin("mix:referenceable");
      }
    } else {
      session = sessionProvider.getSession("portal-system", WCMCoreUtils
          .getRepository());
      String uuid = Util.getUIPortal().getSelectedUserNode().getId();
      seoNode = session.getNodeByUUID(uuid);
    }
    if (seoNode.isNodeType("exo:pageMetadata")) {
      seoNode.setProperty("exo:metaKeywords", keyword);
      seoNode.setProperty("exo:metaDescription", description);
      seoNode.setProperty("exo:metaFully", fullStatus);
      if (!onContent) {
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
      }
      String hash = null;
      if (onContent)
        hash = getHash(uri);
      else
        hash = getHash(pageReference);
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
        hash = getHash(seoNode.getUUID());
      } else {
        seoNode.setProperty("exo:metaUri", pageReference);
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
        hash = getHash(pageReference);
      }
      if (hash != null)
        cache.put(hash, metaModel);
    }
    session.save();
  }

  public PageMetadataModel getMetadata(ArrayList<String> params,
      String pageReference) throws Exception {
    PageMetadataModel metaModel = null;
    if (params != null) {
      metaModel = getContentMetadata(params);
    } else {
      metaModel = getPageMetadata(pageReference);
    }
    return metaModel;
  }

  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getContentMetadata(ArrayList<String> params)
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
    String hash = getHash(contentNode.getUUID());
    if (cache.get(hash) != null)
      metaModel = (PageMetadataModel) cache.get(hash);
    if (metaModel == null) {
      if (contentNode.isNodeType("exo:pageMetadata")) {
        metaModel = new PageMetadataModel();
        metaModel.setUri(pageUri);
        if (contentNode.hasProperty("exo:metaKeywords"))
          metaModel.setKeywords((contentNode.getProperty("exo:metaKeywords"))
              .getString());
        if (contentNode.hasProperty("exo:metaDescription"))
          metaModel.setDescription((contentNode
              .getProperty("exo:metaDescription")).getString());
        if (contentNode.hasProperty("exo:metaRobots"))
          metaModel
              .setRobotsContent((contentNode.getProperty("exo:metaRobots"))
                  .getString());
        if (contentNode.hasProperty("exo:metaSitemap"))
          metaModel.setSiteMap(Boolean.parseBoolean((contentNode
              .getProperty("exo:metaSitemap")).getString()));
        if (contentNode.hasProperty("exo:metaPriority"))
          metaModel.setPriority(Long.parseLong((contentNode
              .getProperty("exo:metaPriority")).getString()));
        if (contentNode.hasProperty("exo:metaFrequency"))
          metaModel.setFrequency((contentNode.getProperty("exo:metaFrequency"))
              .getString());
        if (contentNode.hasProperty("exo:metaFully"))
          metaModel.setFullStatus((contentNode.getProperty("exo:metaFully"))
              .getString());
        cache.put(hash, metaModel);
      }
    }
    return metaModel;
  }

  /**
   * {@inheritDoc}
   */
  public PageMetadataModel getPageMetadata(String pageUri) throws Exception {
    PageMetadataModel metaModel = null;
    String hash = getHash(pageUri);
    if (cache.get(hash) != null)
      metaModel = (PageMetadataModel) cache.get(hash);
    if (metaModel == null) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession("portal-system",
          WCMCoreUtils.getRepository());
      String uuid = Util.getUIPortal().getSelectedUserNode().getId();
      Node pageNode = session.getNodeByUUID(uuid);
      if (pageNode != null && pageNode.isNodeType("exo:pageMetadata")) {
        metaModel = new PageMetadataModel();
        if (pageNode.hasProperty("exo:metaKeywords"))
          metaModel.setKeywords((pageNode.getProperty("exo:metaKeywords"))
              .getString());
        if (pageNode.hasProperty("exo:metaDescription"))
          metaModel
              .setDescription((pageNode.getProperty("exo:metaDescription"))
                  .getString());
        if (pageNode.hasProperty("exo:metaRobots"))
          metaModel.setRobotsContent((pageNode.getProperty("exo:metaRobots"))
              .getString());
        if (pageNode.hasProperty("exo:metaSitemap"))
          metaModel.setSiteMap(Boolean.parseBoolean((pageNode
              .getProperty("exo:metaSitemap")).getString()));
        if (pageNode.hasProperty("exo:metaPriority"))
          metaModel.setPriority(Long.parseLong((pageNode
              .getProperty("exo:metaPriority")).getString()));
        if (pageNode.hasProperty("exo:metaFrequency"))
          metaModel.setFrequency((pageNode.getProperty("exo:metaFrequency"))
              .getString());
        if (pageNode.hasProperty("exo:metaFully"))
          metaModel.setFullStatus((pageNode.getProperty("exo:metaFully"))
              .getString());
        cache.put(hash, metaModel);
      }
    }
    return metaModel;
  }

  /**
   * {@inheritDoc}
   */
  public void removePageMetadata(PageMetadataModel metaModel,
      String portalName, boolean onContent) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) container
        .getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository currentRepo = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(currentRepo.getConfiguration()
        .getDefaultWorkspaceName(), currentRepo);
    String hash = "";
    Node seoNode = null;
    if (onContent) {
      seoNode = session.getNodeByUUID(metaModel.getUri());
    } else {
      session = sessionProvider.getSession("portal-system", WCMCoreUtils
          .getRepository());
      String uuid = Util.getUIPortal().getSelectedUserNode().getId();
      seoNode = session.getNodeByUUID(uuid);
    }
    if (seoNode.isNodeType("exo:pageMetadata")) {
      seoNode.removeMixin("exo:pageMetadata");
      if (onContent)
        hash = getHash(metaModel.getUri());
      else
        hash = getHash(metaModel.getPageReference());
      cache.remove(hash);
    }
    session.save();
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
        priorityElement.setTextContent(String.valueOf(priority));
        urlElement.appendChild(priorityElement);
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
          String locationValue = locElement.getChildNodes().item(0)
              .getNodeValue();
          if (locationValue != null
              & (locationValue.trim().equals(uri) || locationValue.trim()
                  .equals(uri_clone))) {
            fLoc = true;
            if (visibleSitemap) {
              org.w3c.dom.Node freqNode = urlElement.getElementsByTagName(
                  "changefreq").item(0);
              freqNode.setTextContent(frequency);
              org.w3c.dom.Node priorityNode = urlElement.getElementsByTagName(
                  "priority").item(0);
              priorityNode.setTextContent(String.valueOf(priority));
            } else {
              arrNodes.add(urlNode);
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
        Element priorityElement = doc.createElement("priority");
        priorityElement.setTextContent(String.valueOf(priority));
        urlElement.appendChild(locElement);
        urlElement.appendChild(freqElement);
        urlElement.appendChild(priorityElement);
        root.appendChild(urlElement);
        // create element in sitemap for uri_clone
        if (uri_clone != null && uri_clone.length() > 0) {
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

    if (sitemapData != null && sitemapData.length() > 0) {
      String hash = getHash(portalName + SITEMAP_NAME);
      cache.put(hash, sitemapData);
    }
    session.save();
  }

  /**
   * {@inheritDoc}
   */
  public String getSitemap(String portalName) throws Exception {
    String sitemapContent = null;
    String hash = getHash(portalName + SITEMAP_NAME);
    if (cache.get(hash) != null)
      sitemapContent = (String) cache.get(hash);

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

  /**
   * {@inheritDoc}
   */
  public String getRobots(String portalName) throws Exception {
    String hash = getHash(portalName + ROBOTS_NAME);
    String robotsCache = (String) cache.get(hash);
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
   * @param uri
   *          The uri of page
   * @param language
   *          The language of page
   * @return
   * @throws Exception
   */
  public String getHash(String uri) throws Exception {
    String key = uri;
    return MessageDigester.getHash(key);
  }

  private String getStandardURL(String path) throws Exception {
    if (path.substring(path.length() - 1, path.length()).equals("/"))
      path = path.substring(0, path.length() - 1);
    return path;
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
            ArrayList<WorkspaceEntry> wsList = manageRepo.getConfiguration()
                .getWorkspaceEntries();
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
}
