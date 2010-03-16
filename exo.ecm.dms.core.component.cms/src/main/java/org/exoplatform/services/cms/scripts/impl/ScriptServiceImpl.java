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
package org.exoplatform.services.cms.scripts.impl;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.codehaus.groovy.control.CompilationFailedException;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.BaseResourceLoaderService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.ResourceConfig;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ScriptServiceImpl extends BaseResourceLoaderService implements ScriptService, EventListener {

  private GroovyClassLoader groovyClassLoader_ ;
  private RepositoryService repositoryService_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;   
  List<ScriptPlugin> plugins_ = new ArrayList<ScriptPlugin>() ;
  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(ScriptServiceImpl.class);
  
  /**
   * Constructor method
   * Init repositoryService, configurationManager, nodeHierarchyCreator, caService, dmsConfiguration   
   * @param repositoryService       RepositoryService
   * @param cservice                ConfigurationManager
   * @param nodeHierarchyCreator    NodeHierarchyCreator
   * @param cacheService            CacheService
   * @param dmsConfiguration        DMSConfiguration
   * @throws Exception
   */
  public ScriptServiceImpl(RepositoryService repositoryService, ConfigurationManager cservice,
      NodeHierarchyCreator nodeHierarchyCreator, CacheService cacheService, 
      DMSConfiguration dmsConfiguration) throws Exception {    
    super(cservice, nodeHierarchyCreator, repositoryService, cacheService, dmsConfiguration);
    groovyClassLoader_ = createGroovyClassLoader();
    repositoryService_ = repositoryService ; 
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
    dmsConfiguration_ = dmsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  public void start() {    
    try {
      initPlugins();      
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }
  }

  /**
   * add ScriptPlugin
   * @param plugin  ComponentPlugin
   * @see           ScriptPlugin
   * @see           ComponentPlugin  
   */
  public void addScriptPlugin(ComponentPlugin plugin) {
    if(plugin instanceof ScriptPlugin) {      
      plugins_.add((ScriptPlugin)plugin) ;
    }
  }

  /**
   * init Plugin
   * @see       Session
   * @see       ScriptPlugin
   * @see       RepositoryEntry
   * @see       DMSRepositoryConfiguration
   * @see       ObservationManager
   * @throws Exception
   */
  private void initPlugins() throws Exception{
    Session session = null ;
    String scriptsPath = getBasePath();
    for(ScriptPlugin plugin : plugins_) {
      String scriptsLocation = plugin.getPredefineScriptsLocation();
      if(plugin.getAutoCreateInNewRepository()) {
        List<RepositoryEntry> repositories = repositoryService_.getConfig().getRepositoryConfigurations() ;                
        DMSRepositoryConfiguration dmsRepoConfig = null;
        for(RepositoryEntry repo : repositories) {
          dmsRepoConfig = dmsConfiguration_.getConfig(repo.getName());
          session = repositoryService_.getRepository(repo.getName()).getSystemSession(dmsRepoConfig.getSystemWorkspace());          
          Iterator<ObjectParameter> iter = plugin.getScriptIterator() ;
          while(iter.hasNext()) {
            init(session,(ResourceConfig) iter.next().getObject(),scriptsLocation) ;            
          }
          ObservationManager obsManager = session.getWorkspace().getObservationManager();
          obsManager.addEventListener(this, Event.PROPERTY_CHANGED, scriptsPath, true, null, null, true);
          session.save();
          session.logout();
        }        
      }
      String repository = plugin.getInitRepository() ;
      if(repository == null) {
        repository = repositoryService_.getDefaultRepository().getConfiguration().getName();
      }
      ManageableRepository mRepository = repositoryService_.getRepository(repository) ;
      DMSRepositoryConfiguration dmsDefaultRepoConfig = dmsConfiguration_.getConfig(repository);
      session = mRepository.getSystemSession(dmsDefaultRepoConfig.getSystemWorkspace()) ;          
      Iterator<ObjectParameter> iter = plugin.getScriptIterator() ;
      while(iter.hasNext()) {
        init(session,(ResourceConfig) iter.next().getObject(),scriptsLocation) ;            
      }
      ObservationManager obsManager = session.getWorkspace().getObservationManager();
      obsManager.addEventListener(this, Event.PROPERTY_CHANGED, scriptsPath, true, null, null, true);
      session.save();
      session.logout();
    }
  }

  /**
   * get Base Script Path
   * @see       NodeHierarchyCreator 
   * @return    String
   */
  protected String getBasePath() { return nodeHierarchyCreator_.getJcrPath(BasePath.CMS_SCRIPTS_PATH); }    

  /**
   * {@inheritDoc}
   */
  public void initRepo(String repository) throws Exception {
    ManageableRepository mRepository = repositoryService_.getRepository(repository) ;
    String scriptsPath = getBasePath();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    Session session = mRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;
    for(ScriptPlugin plugin : plugins_) {
      if(!plugin.getAutoCreateInNewRepository()) continue ;
      String scriptsLocation = plugin.getPredefineScriptsLocation();
      Iterator<ObjectParameter> iter = plugin.getScriptIterator() ;                           
      while(iter.hasNext()) {
        init(session,(ResourceConfig) iter.next().getObject(),scriptsLocation) ;            
      }      
      ObservationManager obsManager = session.getWorkspace().getObservationManager();
      obsManager.addEventListener(this, Event.PROPERTY_CHANGED, scriptsPath, true, null, null, true);

    }
    session.save();
    session.logout();
  }
  
  /**
   * {@inheritDoc}
   */
  public Node getECMScriptHome(String repository,SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider);
    return getNodeByAlias(BasePath.ECM_EXPLORER_SCRIPTS,session);        
  }        

  /**
   * {@inheritDoc}
   */
  public Node getCBScriptHome(String repository,SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider);
    return getNodeByAlias(BasePath.CONTENT_BROWSER_SCRIPTS,session);    
  }

//  public boolean hasCBScript(String repository) throws Exception {    
//    return getCBScriptHome(repository).hasNodes();
//  }

  /**
   * get CBSCcripts
   * @param repository    String
   *                      The name of Repository
   * @param provider      SessionProvider
   * @see                 SessionProvider
   * @return
   * @throws Exception 
   */
  public List<Node> getCBScripts(String repository,SessionProvider provider) throws Exception {
    List<Node> scriptList = new ArrayList<Node>() ;
    Node cbScriptHome = getCBScriptHome(repository,provider) ;
    for(NodeIterator iter = cbScriptHome.getNodes(); iter.hasNext() ;) {
      scriptList.add(iter.nextNode()) ;
    }      
    return scriptList;    
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getECMActionScripts(String repository,SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider);
    return getScriptList(BasePath.ECM_ACTION_SCRIPTS, session);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getECMInterceptorScripts(String repository,SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider);
    return getScriptList(BasePath.ECM_INTERCEPTOR_SCRIPTS, session);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getECMWidgetScripts(String repository,SessionProvider provider) throws Exception {
    Session session = getSession(repository,provider);
    return getScriptList(BasePath.ECM_WIDGET_SCRIPTS,session);
  } 

  /**
   * {@inheritDoc}
   */
  public String getBaseScriptPath() throws Exception {   
    return getBasePath() ;
  }

  /**
   * get ECMCategoriesPath
   * @see       NodeHierarchyCreator 
   * @return
   * @throws Exception
   */
  public String[] getECMCategoriesPath() throws Exception {
    String[] categoriesPath 
    = { nodeHierarchyCreator_.getJcrPath(BasePath.ECM_ACTION_SCRIPTS),
        nodeHierarchyCreator_.getJcrPath(BasePath.ECM_INTERCEPTOR_SCRIPTS), 
        nodeHierarchyCreator_.getJcrPath(BasePath.ECM_WIDGET_SCRIPTS) } ;
    return categoriesPath;
  }

  /**
   * get CBCategoriesPath
   * @see       NodeHierarchyCreator 
   * @return
   * @throws Exception
   */
  public String[] getCBCategoriesPath() throws Exception {
    String[] categoriesPath = { nodeHierarchyCreator_.getJcrPath(BasePath.CONTENT_BROWSER_SCRIPTS)} ;
    return categoriesPath;
  }

  /**
   * {@inheritDoc}
   */
  public String getScriptAsText(String scriptName, String repository) throws Exception {
    return getResourceAsText(scriptName, repository);
  }

  @SuppressWarnings("unused")
  /**
   * {@inheritDoc}
   */
  public CmsScript getScript(String scriptName, String repository) throws Exception {
    CmsScript scriptObject = (CmsScript) resourceCache_.get(scriptName);
    if (scriptObject != null) return scriptObject;
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    scriptObject = (CmsScript) container.getComponentInstance(scriptName);
    if(scriptObject !=null ) {
      resourceCache_.put(scriptName,scriptObject) ;
      return scriptObject;
    }
    groovyClassLoader_ = createGroovyClassLoader();
    Class scriptClass = groovyClassLoader_.loadClass(scriptName) ;        
    container.registerComponentImplementation(scriptName, scriptClass); 
    scriptObject = (CmsScript) container.getComponentInstance(scriptName);
    resourceCache_.put(scriptName, scriptObject) ;

    return scriptObject;
  }

  /**
   * {@inheritDoc}
   */
  public void addScript(String name, String text, String repository,SessionProvider provider) throws Exception {
    addResource(name, text, repository,provider);
    removeFromCache(name) ;
  }

  /**
   * {@inheritDoc}
   */
  public void removeScript(String scriptName, String repository,SessionProvider provider) throws Exception {
    removeResource(scriptName, repository,provider);
    removeFromCache(scriptName) ;
  }    

  /**
   * Get ScriptHome
   * @param scriptAlias     String
   *                        The alias of script
   * @param session         Session
   * @see                   NodeHierarchyCreator
   * @return
   * @throws Exception
   */
  private Node getScriptHome(String scriptAlias, Session session) throws Exception {
    String path = nodeHierarchyCreator_.getJcrPath(scriptAlias) ;               
    return (Node)session.getItem(path);
  }

  /**
   * get Script List with the following param
   * @param scriptAlias   String
   *                      The alias of script
   * @param session       Session
   * @see                   NodeHierarchyCreator
   * @return
   * @throws Exception
   */
  private List<Node> getScriptList(String scriptAlias,Session session) throws Exception {
    List<Node> scriptList = new ArrayList<Node>() ;
    Node scriptHome = getScriptHome(scriptAlias,session) ;
    for(NodeIterator iter = scriptHome.getNodes(); iter.hasNext() ;) {
      scriptList.add(iter.nextNode()) ;
    }      
    return scriptList;
  }  

  /**
   * remove From Cache
   * @param scriptName    String
   *                      The name of script
   * @see                 ExoContainer
   * @see                 ExoContainerContext                          
   */
  protected void removeFromCache(String scriptName){  
    try{
      Object cachedobject = resourceCache_.get(scriptName);
      if (cachedobject != null) {        
        resourceCache_.remove(scriptName) ;
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        container.unregisterComponent(scriptName);
//        Class scriptClass = (Class)cachedobject ;
//        groovyClassLoader_.removeFromCache(scriptClass) ;
      }
    }catch (Exception e) {
    }        
  }

  /**
   * onEvent
   * @param events      EventIterator
   * @see               Session
   * @see               DMSRepositoryConfiguration
   * @see               ManageableRepository
   */
  public void onEvent(EventIterator events) {
    while (events.hasNext()) {
      Event event = events.nextEvent();
      String path = null;
      Session jcrSession = null ;
      try {
        path = event.getPath();
        List<RepositoryEntry> repositories = repositoryService_.getConfig().getRepositoryConfigurations() ;
        DMSRepositoryConfiguration dmsRepoConfig = null;
        for(RepositoryEntry repo : repositories) {
          try {
            ManageableRepository manageableRepository = repositoryService_.getRepository(repo.getName()) ;
            dmsRepoConfig = dmsConfiguration_.getConfig(repo.getName());
            jcrSession = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
            Property property = (Property) jcrSession.getItem(path);
            if ("jcr:data".equals(property.getName())) {
              Node node = property.getParent();
              //TODO: Script cache need to redesign to support store scripts in diffirence repositories 
              removeFromCache(node.getName());             
            }
            jcrSession.logout();
          }catch (Exception e) { 
            jcrSession.logout();
            continue ;
          }
        }        
      } catch (Exception e) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * create Groovy ClassLoader
   * @see   SessionProvider
   * @return
   */
  private GroovyClassLoader createGroovyClassLoader() {
    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();    
    return new GroovyClassLoader(parentLoader) {
      @SuppressWarnings("unchecked")
      protected Class findClass(String className) throws ClassNotFoundException {
        String repository = null ;
        String filename = null ;
        String nodeName = null ;
        if(className.indexOf(":") > -1) {
          String[] array = className.split(":") ;
          repository = array[0] ;
          nodeName = array[1] ;
          filename = array[1].replace('.', File.separatorChar) + ".groovy";
        }else {
          nodeName = className ;
          filename = className.replace('.', File.separatorChar) + ".groovy";
        }
        InputStream in = null;
        SessionProvider provider = SessionProvider.createSystemProvider() ;
        try {
          Node scriptsHome = getResourcesHome(repository,provider);
          Node scriptNode = scriptsHome.getNode(nodeName);
          in = scriptNode.getProperty("jcr:data").getStream();
          provider.close();
        } catch (Exception e) {
          provider.close();
          throw new ClassNotFoundException("Could not read " + nodeName + ": " + e);
        }
        try {
          return parseClass(in, filename);
        } catch (CompilationFailedException e2) {
          throw new ClassNotFoundException("Syntax error in " + filename
              + ": " + e2);
        }
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  public Node getScriptNode(String scriptName, String repository,SessionProvider provider) throws Exception {
    try {
      Node scriptHome = getResourcesHome(repository,provider) ;
      return scriptHome.getNode(scriptName) ;      
    }catch (Exception e) {
      return null;
    }
  }
  
  /**
   * Return session of the specified repository
   * @param repository      String
   *                        The name of repository
   * @param provider        SessionProvider                    
   * @return
   * @see                   SessionProvider
   * @see                   ManageableRepository
   * @see                   DMSRepositoryConfiguration
   * @throws Exception
   */
  private Session getSession(String repository, SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    // String systemWokspace = manageableRepository.getConfiguration().getSystemWorkspaceName();
    return provider.getSession(dmsRepoConfig.getSystemWorkspace(), manageableRepository);
  }
  
  /**
   * Get Node By Alias 
   * @param alias       String  
   *                    The alias of the specefied node
   * @param session     Session
   * @see               NodeHierarchyCreator
   * @see               Session
   * @return
   * @throws Exception
   */
  private Node getNodeByAlias(String alias,Session session) throws Exception {
    String path = nodeHierarchyCreator_.getJcrPath(alias) ;
    return (Node)session.getItem(path);
  }
  
}
