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
package org.exoplatform.services.wcm.metadata.web;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.metadata.PageMetadataService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Nov 3, 2008
 */
public class PageMetadataRequestFilter implements Filter {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(PageMetadataRequestFilter.class);
  
  /** The Constant PCV_PARAMETER_REGX. */
  public final static String PCV_PARAMETER_REGX           = "(.*)/(.*)/(.*)";

  /* (non-Javadoc)
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig config) throws ServletException {
  }

  /**
   * This method will filter a request to a portal page in wcm context to set page title, page metadata for the page
   * These information is very important for search engine can indexing the page in Internet environment.
   *
   * @param servletRequest request
   * @param servletResponse response
   * @param chain the filter chain
   *
   * @throws IOException, ServletException
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ServletException the servlet exception
   */

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
  throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) servletRequest;
    try {
      boolean check = checkAndSetMetadataIfRequestToPCVPortlet(req);
      if(!check)
        setPortalMetadata(req);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when doFilter: ", e);
      }
    }
    chain.doFilter(servletRequest,servletResponse);
  }

  /**
   * Sets the portal metadata.
   *
   * @param req the new portal metadata
   *
   * @throws Exception the exception
   */
  private void setPortalMetadata(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    PageMetadataService metadataRegistry = getService(PageMetadataService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    HashMap<String,String> metadata = metadataRegistry.getPortalMetadata(sessionProvider, pathInfo);
    if(metadata != null)
      req.setAttribute(PortalRequestContext.REQUEST_METADATA, metadata);
  }

  /**
   * Check and set metadata if request to pcv portlet.
   *
   * @param req the req
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  private boolean checkAndSetMetadataIfRequestToPCVPortlet(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    if(pathInfo == null) return false;
    WCMConfigurationService configurationService = getService(WCMConfigurationService.class);
    String parameterizedPageURI = configurationService.getRuntimeContextParam(WCMConfigurationService.PARAMETERIZED_PAGE_URI);
    String printPreviewPageURI = configurationService.getRuntimeContextParam(WCMConfigurationService.PRINT_VIEWER_PAGE);
    int index = pathInfo.indexOf(parameterizedPageURI);
    String parameter = null;
    if(index<1 && printPreviewPageURI != null) {
      index = pathInfo.indexOf(printPreviewPageURI);
      if(index<1) return false;
      parameter = pathInfo.substring(index + printPreviewPageURI.length() + 1);
    }else {
      parameter = pathInfo.substring(index + parameterizedPageURI.length() + 1);
    }
    if(parameter == null )
      return false;
    if(!parameter.matches(PCV_PARAMETER_REGX)) return false;
    int firstSlash = parameter.indexOf("/");
    int secondSlash = parameter.indexOf("/",firstSlash +1);
    String workspace = parameter.substring(firstSlash + 1, secondSlash);
    String nodeIdentifier = parameter.substring(secondSlash + 1);
    RepositoryService repositoryService = getService(RepositoryService.class);
    SessionProvider sessionProvider =WCMCoreUtils.getSystemSessionProvider();
    Node node = null;
    try {
      Session session = sessionProvider.getSession(workspace,
                                                   repositoryService.getCurrentRepository());
      if(nodeIdentifier.indexOf("/")<0) {
        node = session.getNodeByUUID(nodeIdentifier);
      }else {
        node = (Node)session.getItem("/" + nodeIdentifier);
      }
    } catch (ItemNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }catch (PathNotFoundException e) {
      req.setAttribute("ParameterizedContentViewerPortlet.data.object",new ItemNotFoundException());
    }catch (AccessControlException e) {
      req.setAttribute("ParameterizedContentViewerPortlet.data.object",e);
    }catch (Exception e) {
      req.setAttribute("ParameterizedContentViewerPortlet.data.object",new ItemNotFoundException());
    }
    if(node != null) {
      req.setAttribute("ParameterizedContentViewerPortlet.data.object",node);
      PageMetadataService pageMetadataService = getService(PageMetadataService.class);
      Map<String,String> pageMetadata = pageMetadataService.extractMetadata(node);
      String title = pageMetadata.get(PageMetadataService.PAGE_TITLE);
      if(title != null) {
        req.setAttribute(PortalRequestContext.REQUEST_TITLE,title);
      }
      req.setAttribute(PortalRequestContext.REQUEST_METADATA,pageMetadata);
      if (node.hasProperty("exo:title")) {
        req.setAttribute("WCM.Content.Title",node.getProperty("exo:title").getValue().getString());
      }
      req.setAttribute("WCM.Content.Title", node.getName());
      return true;
    }
    return false;
  }

  /**
   * Gets the service.
   *
   * @param clazz the clazz
   *
   * @return the service
   */
  private <T> T getService(Class<T> clazz) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }

  /* (non-Javadoc)
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
  }
}
