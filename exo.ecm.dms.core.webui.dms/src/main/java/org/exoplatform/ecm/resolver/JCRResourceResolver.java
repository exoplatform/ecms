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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:07:02 PM
 */
public class JCRResourceResolver extends ResourceResolver {      
  protected String repository ; 
  protected String workspace ;      
  protected String propertyName ;

  /**
   * Instantiates a new jCR resource resolver 
   * to load template that stored as a property of node in jcr
   * 
   * @param repository the repository
   * @param workspace the workspace
   * @param propertyName the property name
   */
  public JCRResourceResolver(String repository,String workspace,String propertyName) {
    this.repository = repository ;
    this.workspace = workspace;    
    this.propertyName = propertyName ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResource(java.lang.String)
   */
  @SuppressWarnings("unused")
  public URL getResource(String url) throws Exception {
    throw new Exception("This method is not  supported") ;  
  }

  /** 
   * @param url URL must be like jcr:path with path is node path 
   * @see org.exoplatform.resolver.ResourceResolver#getInputStream(java.lang.String)
   */
  public InputStream getInputStream(String url) throws Exception  {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService = 
      (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class) ;
    ManageableRepository manageableRepository = repositoryService.getRepository(repository) ;
    //Use system session to access jcr resource
    SessionProvider provider = SessionProviderFactory.createSystemProvider();
    Session session = provider.getSession(workspace,manageableRepository);
    Node node = (Node)session.getItem(removeScheme(url)) ;
    return new ByteArrayInputStream(node.getProperty(propertyName).getString().getBytes()) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResources(java.lang.String)
   */
  @SuppressWarnings("unused")
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
  @SuppressWarnings("unused")
  public boolean isModified(String url, long lastAccess) {  return false ; }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#createResourceId(java.lang.String)
   */
  public String createResourceId(String url) { return url ; }

  /* (non-Javadoc)
   * @see org.exoplatform.resolver.ResourceResolver#getResourceScheme()
   */
  public String getResourceScheme() {  return "jcr:" ; }

}
