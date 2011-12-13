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
package org.exoplatform.wcm.webui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;


/**
 * Created by The eXo Platform SAS
 * Author : anh.do
 * anh.do@exoplatform.com, anhdn86@gmail.com
 * Mar 2, 2009
 */
public class WikiLinkParser {

  /** The data source. */
  private String dataSource;

  /** The Constant XWIKI_LINK. */
  public final static Pattern XWIKI_LINK = Pattern.compile("(\\[)([\\w\\W&&[^\\]]]+)(\\>)([:/\\w\\s\\.]+)(\\])");

  /** The links. */
  Map<String, String> links = new HashMap<String, String>();

  /**
   * Instantiates a new wiki link parser.
   *
   * @param source the source
   */
  public WikiLinkParser(String source) {
    this.dataSource = source;
  }

  /**
   * Sets the data source.
   *
   * @param dataSource the new data source
   */
  public void setDataSource(String dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Gets the data source.
   *
   * @return the data source
   */
  public String getDataSource() {
    return this.dataSource;
  }

  /**
   * Gets the navigations.
   *
   * @return the navigations
   *
   * @throws Exception the exception
   */
  /**
   * TODO: the API for loading navigations was changed (replaced [PageNavigation, PageNode] by [UserNavigation, UserNode])
   * after refactoring, PageNavigationUtils class was removed from inside API so we can't use this class any more
   * 
   * WikiLinkParser class is useless in ECMS project now, 
   * so we've temporarily commented this method and we will refactor it later
   */  
  /*protected static List<PageNavigation> getNavigations() throws Exception {
    
    List<PageNavigation> allNav = Util.getUIPortal().getNavigations();
    String remoteUser = Util.getPortalRequestContext().getRemoteUser();
    List<PageNavigation> result = new ArrayList<PageNavigation>();
    for (PageNavigation nav : allNav) {      
      result.add(PageNavigationUtils.filter(nav, remoteUser));
    }    
    return result;
    return new ArrayList<PageNavigation>();
  }*/

  /**
   * Gets the base uri.
   *
   * @return the base uri
   */
  private String getBaseURI() {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    HttpServletRequest servletRequest = portalRequestContext.getRequest();
    WCMConfigurationService configurationService = (WCMConfigurationService) ExoContainerContext.
        getCurrentContainer().getComponentInstanceOfType(WCMConfigurationService.class);
    String wikiContext = configurationService.getRuntimeContextParam(WCMConfigurationService.CREATE_WIKI_PAGE_URI);
    
    NodeURL nodeURL = portalRequestContext.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, portalRequestContext.getPortalOwner(), wikiContext);
    nodeURL.setResource(resource);
    
    return servletRequest.getScheme() + "://" + servletRequest.getServerName() + ":"
        + servletRequest.getServerPort() + nodeURL.toString();
  }

  /**
   * Generate link.
   *
   * @param uri the uri
   * @param label the label
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  private String generateLink(String uri, String label) throws Exception {
    if (uri == null || uri.trim().length() == 0 || uri.startsWith(".") || uri.endsWith(".")) return "#";
    if (uri.contains("//")) {
      return "<a href=\"" + uri.trim() + "\">" + label + "</a>";
    }
    uri = uri.replace('.', '/').replace(" ", "");
    String temp = uri;
    String parent = "";
    String newURI = "";
    String correctLink = "";
    
    /**
    * TODO: the API for loading navigations was changed (replaced [PageNavigation, PageNode] by [UserNavigation, UserNode])
    * after refactoring, PageNavigationUtils class was removed from inside API so we can't use this class any more
    * 
    * WikiLinkParser class is useless in ECMS project now, 
    * so we've temporarily commented this method and we will refactor it later
    */   
    //    List<PageNavigation> navs = getNavigations();
    //    PageNode pageNode = null;
    //    for (PageNavigation navigation : navs) {
    //      pageNode = PageNavigationUtils.searchPageNodeByUri(navigation, uri);      
    //      while (pageNode == null) {
    //        uri = uri.substring(0, uri.lastIndexOf('/'));
    //        pageNode = PageNavigationUtils.searchPageNodeByUri(navigation, uri);
    //      }
    //      if (pageNode != null) break;
    //    }
    if (uri.length() != temp.length()) {
      parent = uri;
      newURI = temp.substring(uri.length() + 1);
      correctLink = getBaseURI() + "?parentUri=" + parent + "&pageUri=" + newURI;
      return label  + " <a href=\"" + correctLink + "\">?</a>";
    } else {
      correctLink = getBaseURI() + temp;
      return "<a href=\"" + correctLink + "\">" + label + "</a>";
    }
  }

  /**
   * Correct links.
   *
   * @param list the list
   *
   * @throws Exception the exception
   */
  private void correctLinks(List<String> list) throws Exception {
    String [] arr = links.keySet().toArray(new String [0]);
    for (int i = list.size() - 1; i >= 0; i -- ) {
      String key = arr[list.size() - i - 1];
      String label = links.get(key);
      String correctLink = generateLink(key, label);
      dataSource = dataSource.replace(list.get(i), correctLink);
    }
  }

  /**
   * Parses the html.
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  public String parseHTML() throws Exception {
    dataSource = dataSource.replace("&gt;", ">");
    Matcher matcher = XWIKI_LINK.matcher(dataSource);
    List<String> list = new ArrayList<String>();
    while(matcher.find()) {
      String link = matcher.group(4);
      String label = matcher.group(2);
      list.add(dataSource.substring(matcher.start(), matcher.end()));
      links.put(link, label);
    }
    correctLinks(list);
    return dataSource;
  }
}
