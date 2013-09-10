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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.lang.StringUtils;
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
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class ScriptServiceImpl extends BaseResourceLoaderService implements ScriptService, EventListener {

  private GroovyClassLoader groovyClassLoader_ ;
  List<ScriptPlugin> plugins_ = new ArrayList<ScriptPlugin>() ;
  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(ScriptServiceImpl.class.getName());
  private Set<String> configuredScripts_;

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
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
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
    configuredScripts_ = new HashSet<String>();
    Session session = null ;
    String scriptsPath = getBasePath();
    for(ScriptPlugin plugin : plugins_) {
      String scriptsLocation = plugin.getPredefineScriptsLocation();
      if(plugin.getAutoCreateInNewRepository()) {
        DMSRepositoryConfiguration dmsRepoConfig = null;
        dmsRepoConfig = dmsConfiguration_.getConfig();
        session = repositoryService_.getCurrentRepository().getSystemSession(dmsRepoConfig.getSystemWorkspace());
        Iterator<ObjectParameter> iter = plugin.getScriptIterator() ;
        while(iter.hasNext()) {
          ResourceConfig resourceConfig = (ResourceConfig) iter.next().getObject();
          init(session,resourceConfig,scriptsLocation);
          addConfigScripts(resourceConfig);
        }
        ObservationManager obsManager = session.getWorkspace().getObservationManager();
        obsManager.addEventListener(this, Event.PROPERTY_CHANGED, scriptsPath, true, null, null, true);
        session.save();
        session.logout();
      }
      ManageableRepository mRepository = repositoryService_.getCurrentRepository();
      DMSRepositoryConfiguration dmsDefaultRepoConfig = dmsConfiguration_.getConfig();
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
  public void initRepo() throws Exception {
    configuredScripts_ = new HashSet<String>();
    ManageableRepository mRepository = repositoryService_.getCurrentRepository();
    String scriptsPath = getBasePath();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    Session session = mRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;
    for(ScriptPlugin plugin : plugins_) {
      if(!plugin.getAutoCreateInNewRepository()) continue ;
      String scriptsLocation = plugin.getPredefineScriptsLocation();
      Iterator<ObjectParameter> iter = plugin.getScriptIterator() ;
      while(iter.hasNext()) {
        ResourceConfig resourceConfig = (ResourceConfig) iter.next().getObject();
        init(session, resourceConfig,scriptsLocation);
        addConfigScripts(resourceConfig);
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
  public Node getECMScriptHome(SessionProvider provider) throws Exception {
    Session session = getSession(provider);
    return getNodeByAlias(BasePath.ECM_EXPLORER_SCRIPTS,session);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getECMActionScripts(SessionProvider provider) throws Exception {
    Session session = getSession(provider);
    return getScriptList(BasePath.ECM_ACTION_SCRIPTS, session);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getECMInterceptorScripts(SessionProvider provider) throws Exception {
    Session session = getSession(provider);
    return getScriptList(BasePath.ECM_INTERCEPTOR_SCRIPTS, session);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getECMWidgetScripts(SessionProvider provider) throws Exception {
    Session session = getSession(provider);
    return getScriptList(BasePath.ECM_WIDGET_SCRIPTS,session);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBaseScriptPath() throws Exception {
    return getBasePath() ;
  }

  /**
   * {@inheritDoc}}
   */
  @Override
  public String getScriptAsText(Node script) throws Exception {
    return script.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_DATA).getString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized CmsScript getScript(String scriptName) throws Exception {
    CmsScript scriptObject = resourceCache_.get(scriptName);
    if (scriptObject != null) return scriptObject;
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    try {
      scriptObject = (CmsScript) container.getComponentInstance(scriptName);
      if(scriptObject !=null ) {
        resourceCache_.put(scriptName, scriptObject) ;
        return scriptObject;
      }
    } catch (NoClassDefFoundError e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
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
  @Override
  public void addScript(String name, String text, SessionProvider provider) throws Exception {
    addScript(name, name, text, provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addScript(String name, String description, String text, SessionProvider provider) throws Exception {
    Node resourcesHome = getResourcesHome(provider);
    InputStream in = new ByteArrayInputStream(text.getBytes());
    addResource(resourcesHome, name, description, in);
    removeFromCache(name) ;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeScript(String scriptName, SessionProvider provider) throws Exception {
    removeResource(scriptName, provider);
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
  @Override
  protected void removeFromCache(String scriptName){
    try{
      Object cachedobject = resourceCache_.get(scriptName);
      if (cachedobject != null) {
        resourceCache_.remove(scriptName) ;
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        container.unregisterComponent(scriptName);
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
  }

  /**
   * onEvent
   * @param events      EventIterator
   * @see               Session
   * @see               DMSRepositoryConfiguration
   * @see               ManageableRepository
   */
  @Override
  public void onEvent(EventIterator events) {
    while (events.hasNext()) {
      Event event = events.nextEvent();
      String path = null;
      Session jcrSession = null ;
      try {
        path = event.getPath();
        DMSRepositoryConfiguration dmsRepoConfig = null;
        try {
          ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
          dmsRepoConfig = dmsConfiguration_.getConfig();
          jcrSession = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
          Property property = (Property) jcrSession.getItem(path);
          if ("jcr:data".equals(property.getName())) {
            Node node = property.getParent().getParent();
            removeFromCache(StringUtils.removeStart(StringUtils.removeStart(node.getPath(), this.getBaseScriptPath()), "/"));
          }
          jcrSession.logout();
        } catch (Exception e) {
          jcrSession.logout();
          continue ;
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
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
        String filename = null ;
        String nodeName = null ;
        if(className.indexOf(":") > -1) {
          String[] array = className.split(":") ;
          nodeName = array[1] ;
          filename = array[1].replace('.', File.separatorChar) + ".groovy";
        }else {
          nodeName = className ;
          filename = className.replace('.', File.separatorChar) + ".groovy";
        }
        String scriptContent = null;
        try {
          scriptContent = WCMCoreUtils.getService(BaseResourceLoaderService.class).getResourceAsText(nodeName);
        } catch (Exception e) {
          throw new ClassNotFoundException("Could not read " + nodeName + ": " + e);
        }
        try {
          return parseClass(scriptContent, filename);
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
  @Override
  public Node getScriptNode(String scriptName,SessionProvider provider) throws Exception {
    try {
      Node scriptHome = getResourcesHome(provider) ;
      return scriptHome.getNode(scriptName) ;
    }catch (Exception e) {
      return null;
    }
  }


  /**
   * Return session of the current repository
   * @param provider        SessionProvider
   * @return
   * @see                   SessionProvider
   * @see                   ManageableRepository
   * @see                   DMSRepositoryConfiguration
   * @throws Exception
   */
  private Session getSession(SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
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

  /**
   * {@inheritDoc}}
   */
  @Override
  public Set<String> getAllConfiguredScripts() {
    return configuredScripts_;
  }

  private void addConfigScripts(ResourceConfig resourceConfig) {
    for (Object obj :  resourceConfig.getRessources()) {
      if (obj instanceof ResourceConfig.Resource) {
        ResourceConfig.Resource resource = (ResourceConfig.Resource)obj;
        String name = resource.getName();
        if (name.indexOf("/") >=0 ) {
          name = name.substring(name.lastIndexOf("/") + 1);
        }
        configuredScripts_.add(name);
      }
    }
  }

}
