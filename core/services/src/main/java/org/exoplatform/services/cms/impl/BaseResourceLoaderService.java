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
package org.exoplatform.services.cms.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

public abstract class BaseResourceLoaderService implements Startable{

  protected NodeHierarchyCreator nodeHierarchyCreator_;

  protected RepositoryService    repositoryService_;

  protected ConfigurationManager cservice_;

  protected ExoCache             resourceCache_;

  /**
   * DMS configuration which used to store informations
   */
  private DMSConfiguration       dmsConfiguration_;

  /**
   * Constructor method
   * Init cservice, nodeHierarchyCreator, repositoryService, cacheService, dmsConfiguration
   * @param cservice                ConfigurationManager
   * @param nodeHierarchyCreator    NodeHierarchyCreator
   * @param repositoryService       RepositoryService
   * @param cacheService            CacheService
   * @param dmsConfiguration        DMSConfiguration
   * @throws Exception
   */
  public BaseResourceLoaderService(ConfigurationManager cservice,
      NodeHierarchyCreator nodeHierarchyCreator, RepositoryService repositoryService,
      CacheService cacheService, DMSConfiguration dmsConfiguration) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositoryService_ = repositoryService;
    cservice_ = cservice;
    resourceCache_ = cacheService.getCacheInstance(this.getClass().getName());
    dmsConfiguration_ = dmsConfiguration;
  }

  /**
   * get BasePath
   * @return
   */
  abstract protected String getBasePath();

  /**
   * remove From Cache
   * @param resourceName    String
   *                        The name of resource
   */
  abstract protected void removeFromCache(String resourceName);

  /**
   * {@inheritDoc}
   */
  public void start(){};

  /**
   * {@inheritDoc}
   */
  public void stop(){};

  /**
   * init
   * @param session           Session
   * @param resourceConfig    ResourceConfig
   * @param location          String
   *                          The code of location
   * @see                     Session
   * @see                     ResourceConfig
   * @throws Exception
   */
  protected void init(Session session, ResourceConfig resourceConfig, String location) throws Exception {
    addScripts(session, resourceConfig.getRessources(),location) ;
  }

  /**
   * add Script with following param
   * @param session       Session
   * @param resources     List
   * @param location      String
   * @see                 ResourceConfig
   * @throws Exception
   */
  protected void addScripts(Session session, List resources,String location) throws Exception{
    String resourcesPath = getBasePath();
    if (resources.size() == 0) return;
    try {
      String firstResourceName = ((ResourceConfig.Resource) resources.get(0)).getName();
      session.getItem(resourcesPath + "/" + firstResourceName);
      return;
    } catch (PathNotFoundException e) {
      Node root = session.getRootNode();
      Node resourcesHome = (Node) session.getItem(resourcesPath);
      String warPath = location + resourcesPath.substring(resourcesPath.lastIndexOf("/")) ;
      for (Iterator iter = resources.iterator(); iter.hasNext();) {
        ResourceConfig.Resource resource = (ResourceConfig.Resource) iter.next();
        String name = resource.getName();
        String path = warPath + "/" + name;
        InputStream in = cservice_.getInputStream(path);
        addResource(resourcesHome, name, in);
      }
      root.save();
    }
  }

  /**
   * add Resource
   * @param resourcesHome     Node
   * @param resourceName      String
   * @param in                InputStream
   * @throws Exception
   */
  public void addResource(Node resourcesHome, String resourceName, InputStream in)
  throws Exception {
    Node contentNode = null;
    if(resourceName.lastIndexOf("/")>-1) {
      String realParenPath = StringUtils.substringBeforeLast(resourceName,"/") ;
      Node parentResource = resourcesHome.getNode(realParenPath) ;
      resourcesHome = parentResource ;
      resourceName = StringUtils.substringAfterLast(resourceName,"/") ;
    }
    try {
      Node script = resourcesHome.getNode(resourceName);
      contentNode = script.getNode(NodetypeConstant.JCR_CONTENT);
      if(!contentNode.isCheckedOut()) contentNode.checkout() ;
    } catch (PathNotFoundException e) {
      Node script = resourcesHome.addNode(resourceName, NodetypeConstant.NT_FILE);
      contentNode = script.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.EXO_RESOURCES);
      contentNode.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
      contentNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, "application/x-groovy");
    }
    contentNode.setProperty(NodetypeConstant.JCR_DATA, in);
    contentNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
    resourcesHome.save() ;
  }

  /**
   * get ResourcesHome
   * @param sessionProvider   SessionProvider
   * @see                     SessionProvider
   * @see                     DMSRepositoryConfiguration
   * @see                     ManageableRepository
   * @return
   * @throws Exception
   */
  protected Node getResourcesHome(SessionProvider sessionProvider) throws Exception {
    ManageableRepository manageableRepository = null;
    manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    Session session = sessionProvider.getSession(dmsRepoConfig.getSystemWorkspace(),
                                                 manageableRepository);
    String resourcesPath = getBasePath();
    return (Node) session.getItem(resourcesPath);
  }

  /**
   * get Resource As Text
   * @param resourceName    String
   * @param repository      String
   *                        The name of repository
   * @deprecated Since WCM 2.1 you should use {@link #getResourceAsStream(String, String)} instead.
   * @see
   * @return                SessionProvider
   * @throws Exception
   */
  @Deprecated
  public String getResourceAsText(String resourceName, String repository) throws Exception {
    return getResourceAsText(resourceName);
  }
  
  /**
   * get Resource As Text
   * @param resourceName    String
   * @deprecated Since WCM 2.1 you should use {@link #getResourceAsStream(String, String)} instead.
   * @see
   * @return                SessionProvider
   * @throws Exception
   */
  public String getResourceAsText(String resourceName) throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Node resourcesHome = getResourcesHome(sessionProvider);
      Node resourceNode = resourcesHome.getNode(resourceName);
      String text = resourceNode.getNode("jcr:content").getProperty("jcr:data").getString();
      return text;
    } finally {
      sessionProvider.close();
    }
  }  

  /**
   * get Resource As Stream
   * @param resourceName    String
   * @param repository      String
   *                        The name of repository
   * @see
   * @return                SessionProvider
   * @throws Exception
   */
  @Deprecated
  public InputStream getResourceAsStream(String resourceName, String repository) throws Exception {
    return getResourceAsStream(resourceName);
  }
  
  /**
   * get Resource As Stream
   * @param resourceName    String
   * @see
   * @return                SessionProvider
   * @throws Exception
   */
  public InputStream getResourceAsStream(String resourceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node resourcesHome = getResourcesHome(sessionProvider);
    Node resourceNode = resourcesHome.getNode(resourceName);
    InputStream stream = resourceNode.getNode("jcr:content").getProperty("jcr:data").getStream();
    return stream;
  }  

  /**
   * get Resources
   * @param repository          String
   *                            The name of repository
   * @param sessionProvider     SessionProvider
   * @see                       SessionProvider
   * @return
   * @throws Exception
   */
  @Deprecated
  public NodeIterator getResources(String repository,SessionProvider sessionProvider) throws Exception {
    return getResources(sessionProvider);
  }
  
  /**
   * get Resources
   * @param sessionProvider     SessionProvider
   * @see                       SessionProvider
   * @return
   * @throws Exception
   */
  public NodeIterator getResources(SessionProvider sessionProvider) throws Exception {
    Node resourcesHome = getResourcesHome(sessionProvider);
    return resourcesHome.getNodes();
  }

  /**
   * Check has Resources
   * @param repository        String
   *                          The name of repository
   * @param sessionProvider   SessionProvider
   * @see                     SessionProvider
   * @return
   * @throws Exception
   */
  @Deprecated
  public boolean hasResources(String repository,SessionProvider sessionProvider) throws Exception {
    return hasResources(sessionProvider);
  }
  
  /**
   * Check has Resources
   * @param sessionProvider   SessionProvider
   * @see                     SessionProvider
   * @return
   * @throws Exception
   */
  public boolean hasResources(SessionProvider sessionProvider) throws Exception {
    Node resourcesHome = getResourcesHome(sessionProvider);
    return resourcesHome.hasNodes();
  }  

  /**
   * add Resource
   * @param name          String
   *                      The name of resource
   * @param text          String
   * @param repository    String
   *                      The name of repository
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @throws Exception
   */
  @Deprecated
  public void addResource(String name, String text, String repository,SessionProvider provider) throws Exception {
    addResource(name, text, provider);
  }
  
  /**
   * add Resource
   * @param name          String
   *                      The name of resource
   * @param text          String
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @throws Exception
   */
  public void addResource(String name, String text,SessionProvider provider) throws Exception {
    Node resourcesHome = getResourcesHome(provider);
    InputStream in = new ByteArrayInputStream(text.getBytes());
    addResource(resourcesHome, name, in);
    resourcesHome.save();
  }  

  /**
   * remove Resource
   * @param resourceName    String
   *                        The name of resource
   * @param repository      String
   *                        The name of repository
   * @param provider        SessionProvider
   * @see                   SessionProvider
   * @throws Exception
   */
  @Deprecated
  public void removeResource(String resourceName, String repository,SessionProvider provider) throws Exception {
    removeResource(resourceName, provider);
  }
  
  /**
   * remove Resource
   * @param resourceName    String
   *                        The name of resource
   * @param provider        SessionProvider
   * @see                   SessionProvider
   * @throws Exception
   */
  public void removeResource(String resourceName,SessionProvider provider) throws Exception {
    removeFromCache(resourceName);
    Node resourcesHome = getResourcesHome(provider);
    Node resource2remove = resourcesHome.getNode(resourceName);
    resource2remove.remove();
    resourcesHome.save();
  }  
}
