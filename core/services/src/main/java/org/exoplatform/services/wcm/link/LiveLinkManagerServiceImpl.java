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
package org.exoplatform.services.wcm.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS Author : Phan Le Thanh Chuong
 * chuong_phan@exoplatform.com Aug 6, 2008
 */
public class LiveLinkManagerServiceImpl implements LiveLinkManagerService {

  /** The broken links cache. */
  private ExoCache<String, List<String>>                 brokenLinksCache;

  /** The configuration service. */
  private WCMConfigurationService  configurationService;

  /** The repository service. */
  private RepositoryService        repositoryService;

  /** The live portal manager service. */
  private LivePortalManagerService livePortalManagerService;

  /** The internal server path. */
  private String internalServerPath;

  /** The log. */
  final private static Log LOG = ExoLogger.getLogger(LiveLinkManagerServiceImpl.class.getName());

  private final static String CACHE_NAME = "ecms.LiveLinkManagerService";

  /**
   * Instantiates a new live link manager service impl.
   *
   * @param configurationService the configuration service
   * @param repositoryService the repository service
   * @param livePortalManagerService the live portal manager service
   * @param cacheService the cache service
   * @param initParams the init params
   *
   * @throws Exception the exception
   */
  public LiveLinkManagerServiceImpl(
      WCMConfigurationService   configurationService,
      RepositoryService         repositoryService,
      LivePortalManagerService  livePortalManagerService,
      CacheService              cacheService,
      InitParams initParams) throws Exception {
    this.configurationService = configurationService;
    this.repositoryService = repositoryService;
    this.livePortalManagerService = livePortalManagerService;
    this.brokenLinksCache = cacheService.getCacheInstance(CACHE_NAME);

    PropertiesParam propertiesParam = initParams.getPropertiesParam("server.config");
    String scheme = propertiesParam.getProperty("scheme");
    String hostName = propertiesParam.getProperty("hostName");
    String port = propertiesParam.getProperty("port");
    StringBuilder builder = new StringBuilder();
    builder.append(scheme).append("://").append(hostName).append(":").append(port);
    internalServerPath = builder.toString();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#getBrokenLinks(java.lang.String)
   */
  public List<LinkBean> getBrokenLinks(String portalName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node portal = livePortalManagerService.getLivePortal(sessionProvider, portalName);
    String path = portal.getPath();
    Session session = portal.getSession();
    List<LinkBean> listBrokenLinks = new ArrayList<LinkBean>();
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery("select * from exo:webContent where jcr:path like '" + path + "/%'", Query.SQL);
    QueryResult results = query.execute();
    NodeIterator iter = results.getNodes();
    for (;iter.hasNext();) {
      Node webContent = iter.nextNode();
      List<String> listBrokenUrls = getBrokenLinks(webContent);
      for (String brokenUrl : listBrokenUrls) {
        LinkBean linkBean = new LinkBean(brokenUrl, LinkBean.STATUS_BROKEN);
        listBrokenLinks.add(linkBean);
      }
    }
    return listBrokenLinks;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#getBrokenLinks(javax.jcr.Node)
   */
  public List<String> getBrokenLinks(Node webContent) throws Exception {
    List<String> listBrokenUrls = (List<String>) brokenLinksCache.get(webContent.getUUID());
    if (listBrokenUrls == null || listBrokenUrls.size() == 0) {
      listBrokenUrls = new ArrayList<String>();
      if (webContent.hasProperty("exo:links")) {
        for (Value value : webContent.getProperty("exo:links").getValues()) {
          String link = value.getString();
          LinkBean linkBean = LinkBean.parse(link);
          if (linkBean.isBroken()) {
            listBrokenUrls.add(linkBean.getUrl());
          }
        }
        brokenLinksCache.put(webContent.getUUID(), listBrokenUrls);
      }
    }
    return listBrokenUrls;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#validateLink()
   */
  public void updateLinks() throws Exception {
    try {
      Collection<NodeLocation> nodeLocationCollection = configurationService.getAllLivePortalsLocation();
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = null;
      for (NodeLocation nodeLocation : nodeLocationCollection) {
        String workspace = nodeLocation.getWorkspace();
        String path = nodeLocation.getPath();
        ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
        session = sessionProvider.getSession(workspace, manageableRepository);
        updateLinkStatus(session, "select * from exo:linkable where jcr:path like '" + path + "/%'");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform updateLinks: ", e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#validateLink(java.lang.String)
   */
  public void updateLinks(String portalName) throws Exception {
    try {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, portalName);
      String path = portal.getPath();
      Session session = portal.getSession();
      updateLinkStatus(session, "select * from exo:linkable where jcr:path like '" + path + "/%'");
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform updateLinks: ", e);
      }
    }
  }

  /**
   * Update link status.
   *
   * @param session the session
   * @param queryCommand the query command
   *
   * @throws Exception the exception
   */
  private void updateLinkStatus(Session session, String queryCommand) throws Exception{
    List<String> listBrokenLinks = new ArrayList<String>();
    ValueFactory valueFactory = session.getValueFactory();
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryCommand, Query.SQL);
    QueryResult results = query.execute();
    NodeIterator iter = results.getNodes();
    for (; iter.hasNext();) {
      Node webContent = iter.nextNode();
      if (!webContent.isCheckedOut() || webContent.isLocked()
          || (webContent.isCheckedOut() && !webContent.getParent().isCheckedOut())) {
        continue;
      }
      Property links = webContent.getProperty("exo:links");
      Value[] oldValues = links.getValues();
      Value[] newValues = new Value[oldValues.length];
      for (int iValues = 0; iValues < oldValues.length; iValues++) {
        String oldLink = oldValues[iValues].getString();
        if (!oldLink.equals("")) {
          LinkBean linkBean = LinkBean.parse(oldLink);
          String oldUrl = linkBean.getUrl();
          String oldStatus = getLinkStatus(oldUrl);
          String updatedLink = new LinkBean(oldUrl, oldStatus).toString();
          if (LOG.isInfoEnabled()) {
            LOG.info(updatedLink);
          }
          newValues[iValues] = valueFactory.createValue(updatedLink);
          if (oldStatus.equals(LinkBean.STATUS_BROKEN)) {
            listBrokenLinks.add(oldUrl);
          }
        }
      }
      webContent.setProperty("exo:links",newValues);
      brokenLinksCache.put(webContent.getUUID(), listBrokenLinks);
    }
    session.save();
  }

  /**
   * Gets the link status.
   *
   * @param strUrl the str url
   *
   * @return the link status
   */
  private String getLinkStatus(String strUrl) {
    try {
      String fullUrl = strUrl;
      if(strUrl.startsWith("/")) {
        fullUrl = internalServerPath + strUrl;
      }
      fullUrl = fullUrl.replaceAll(" ","%20");
      HttpClient httpClient = new HttpClient(new SimpleHttpConnectionManager());
      GetMethod getMethod = new GetMethod(fullUrl);
      if(httpClient.executeMethod(getMethod) == 200) {
        return LinkBean.STATUS_ACTIVE;
      }
      return LinkBean.STATUS_BROKEN;
    } catch (Exception e) {
      if (LOG.isInfoEnabled()) {
        LOG.info("URL Link: \"" + strUrl + "\" is broken");
      }
      return LinkBean.STATUS_BROKEN;
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#extractLinks(org.exoplatform.services.html.HTMLDocument)
   */
  public List<String> extractLinks(Node htmlFile) throws Exception {
    String htmlData = htmlFile.getNode("jcr:content").getProperty("jcr:data").getString();
    List<String> listHyperlink = new ArrayList<String>();
    HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
    List<HTMLLinkExtractor.HtmlLink> htmlLinks = htmlLinkExtractor.grabHTMLLinks(htmlData);
    for (HTMLLinkExtractor.HtmlLink link : htmlLinks) {
      if (!listHyperlink.contains(link.toString()))
        listHyperlink.add(link.toString());
    }
    return listHyperlink;
  }


  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.link.LiveLinkManagerService#updateLinks(javax.jcr.Node, java.util.List)
   */
  public void updateLinkDataForNode(Node webContent, List<String> newLinks) throws Exception {
    ValueFactory valueFactory = webContent.getSession().getValueFactory();
    if (webContent.canAddMixin("exo:linkable")) {
      webContent.addMixin("exo:linkable");
    }
    // get old link from exo:links property
    List<String> listExtractedLink = new ArrayList<String>();
    if (webContent.hasProperty("exo:links")) {
      Property property = webContent.getProperty("exo:links");
      for (Value value : property.getValues()) {
        listExtractedLink.add(value.getString());
      }
    }
    // compare, remove old link, add new link, create new List
    List<String> listResult = new ArrayList<String>();

    for (String extractedLink : listExtractedLink) {
      for (String newUrl : newLinks) {
        if (LinkBean.parse(extractedLink).getUrl().equals(newUrl)) {
          listResult.add(extractedLink);
        }
      }
    }
    List<String> listTemp = new ArrayList<String>();
    listTemp.addAll(newLinks);

    for (String newUrl : newLinks) {
      for (String extractedLink : listExtractedLink) {
        if (newUrl.equals(LinkBean.parse(extractedLink).getUrl())) {
          listTemp.set(newLinks.indexOf(newUrl), "");
        }
      }
    }

    for (String strTemp : listTemp) {
      if (!strTemp.equals("")) {
        listResult.add(strTemp);
      }
    }

    // Create an array of value to add to exo:links property
    Value[] values = new Value[listResult.size()];
    for(String url: listResult) {
      if (url.indexOf(LinkBean.STATUS) < 0) {
        LinkBean linkBean = new LinkBean(url, LinkBean.STATUS_UNCHECKED);
        values[listResult.indexOf(url)] = valueFactory.createValue(linkBean.toString());
      } else {
        values[listResult.indexOf(url)] = valueFactory.createValue(url);
      }
    }
    webContent.setProperty("exo:links", values);
    webContent.save();
  }

}
