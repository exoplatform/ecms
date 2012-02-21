/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.resolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.resolver.ResourceKey;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:07:02 PM
 */
public class JCRResourceResolver extends ResourceResolver {
  protected String repository ;
  protected String workspace ;
  protected String propertyName ;
  /** The log. */
  private static Log LOG = ExoLogger.getLogger("ecm:JCRResourceResolver");
  private TemplateService templateService;

  /**
   * Instantiates a new jCR resource resolver
   * to load template that stored as a property of node in jcr
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param propertyName the property name
   * @deprecated Since WCM 2.1 you don't need to add the property's name as a parameter anymore
   */
  @Deprecated
  public JCRResourceResolver(String repository,String workspace,String propertyName) {
    this(repository, workspace);
  }

  /**
   * Instantiates a new jCR resource resolver
   * to load template that stored as a property of node in jcr
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param propertyName the property name
   */
  @Deprecated
  public JCRResourceResolver(String repository,String workspace) {
    this.repository = repository ;
    this.workspace = workspace;
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }
  
  /**
   * Instantiates a new jCR resource resolver to load template that stored as a
   * property of node in jcr
   * 
   * @param workspace the workspace
   */
  public JCRResourceResolver(String workspace) {
    this.workspace = workspace;
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResource(java.lang.String)
   */
  public URL getResource(String url) throws Exception {
    throw new Exception("This method is not  supported") ;
  }

  /**
   * @param url URL must be like jcr:path with path is node path
   * @see org.exoplatform.resolver.ResourceResolver#getInputStream(java.lang.String)
   */
  public InputStream getInputStream(String url) throws Exception  {
    SessionProvider provider = WCMCoreUtils.getSystemSessionProvider();
    Session session = provider.getSession(workspace, WCMCoreUtils.getRepository());
    ByteArrayInputStream inputStream = null;
    try {
      Node template = (Node)session.getItem(removeScheme(url));
      inputStream = new ByteArrayInputStream(templateService.getTemplate(template).getBytes());
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when try to process with url");
      }
    } finally {
      session.logout();
    }
    return inputStream;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResources(java.lang.String)
   */
  public List<URL> getResources(String url) throws Exception {
    throw new Exception("This method is not  supported") ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getInputStreams(java.lang.String)
   */
  public List<InputStream> getInputStreams(String url) throws Exception {
    ArrayList<InputStream>  inputStreams = new ArrayList<InputStream>(1) ;
    inputStreams.add(getInputStream(url)) ;
    return inputStreams ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#isModified(java.lang.String, long)
   */
  public boolean isModified(String url, long lastAccess) {  return false ; }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#createResourceId(java.lang.String)
   */
  public String createResourceId(String url) { return url ; }

  @Override
  public ResourceKey createResourceKey(String url) {
    return new ResourceKey(url.hashCode(), url);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResourceScheme()
   */
  public String getResourceScheme() {  return "jcr:" ; }

}
