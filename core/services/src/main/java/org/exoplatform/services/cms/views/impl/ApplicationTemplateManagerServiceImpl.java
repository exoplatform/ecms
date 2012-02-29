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
package org.exoplatform.services.cms.views.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.PortletTemplatePlugin;
import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 15, 2008
 */
public class ApplicationTemplateManagerServiceImpl implements ApplicationTemplateManagerService, Startable {

  private static Log log = ExoLogger.getLogger(ApplicationTemplateManagerService.class);
  private RepositoryService repositoryService;

  private List<PortletTemplatePlugin> portletTemplatePlugins = new ArrayList<PortletTemplatePlugin>();
  Map<String,List<String>> managedApplicationNames = new HashMap<String,List<String>>();
  private Map<String, String> storedWorkspaces = new HashMap<String,String>();

  private String basedApplicationTemplatesPath;

  private DMSConfiguration dmsConfiguration_;

  private NodeHierarchyCreator hierarchyCreator;

  private TemplateService templateService;
  
  private Map<String,Set<String>> configuredTemplates_;

  /**
   * Instantiates a new application template manager service impl.
   *
   * @param repositoryService       RepositoryService
   * @param hierarchyCreator        NodeHierarchyCreator
   * @param params                  InitParams
   * @param dmsConfiguration        DMSConfiguration
   * @see RepositoryService
   * @see NodeHierarchyCreator
   * @see DMSConfiguration
   *
   * @throws Exception the exception
   */
  public ApplicationTemplateManagerServiceImpl(RepositoryService repositoryService,
      NodeHierarchyCreator hierarchyCreator, DMSConfiguration dmsConfiguration) throws Exception {
    this.repositoryService = repositoryService;
    dmsConfiguration_ = dmsConfiguration;
    this.hierarchyCreator = hierarchyCreator;
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  /**
   * {@inheritDoc}
   */
  public void addPlugin(PortletTemplatePlugin portletTemplatePlugin) throws Exception {
    portletTemplatePlugins.add(portletTemplatePlugin);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getTemplatesByApplication(String repository, String portletName,
      SessionProvider provider) throws Exception {
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getTemplatesByApplication(String portletName,
      SessionProvider provider) throws Exception {
    return null;
  }  

  /**
   * {@inheritDoc}
   */
  public void addTemplate(Node portletTemplateHome, PortletTemplateConfig config) throws Exception {
    Node category = null;
    try {
      category = portletTemplateHome.getNode(config.getCategory());
    } catch (Exception e) {
      category = portletTemplateHome.addNode(config.getCategory(),"nt:unstructured");
      portletTemplateHome.save();
    }
    if (!category.hasNode(config.getTemplateName())) {
      templateService.createTemplate(category,
                                     config.getTemplateName(),
                                     new ByteArrayInputStream(config.getTemplateData().getBytes()),
                                     new String[] { "*" });
    }
    Set<String> templateSet = configuredTemplates_.get(portletTemplateHome.getName());
    if (templateSet == null) {
      templateSet = new HashSet<String>();
    }
    templateSet.add(category.getName() + "/" + config.getTemplateName());
    configuredTemplates_.put(portletTemplateHome.getName(), templateSet);
  }

  /**
   * Gets the application template home.
   * @param repository        String
   *                          The name of repository
   * @param portletName       String
   *                          The name of portlet
   * @param provider          SessionProvider
   * @see SessionProvider
   * @return the application template home
   * @throws Exception the exception
   */
  @Deprecated
  public Node getApplicationTemplateHome(String repository,
                                         String portletName,
                                         SessionProvider provider) throws Exception {
    return getApplicationTemplateHome(portletName, provider);
  }
  
  /**
   * {@inheritDoc}
   */
  public Node getApplicationTemplateHome(String portletName, SessionProvider provider) throws Exception {
    Node basedApplicationTemplateHome = getBasedApplicationTemplatesHome(provider);
    return basedApplicationTemplateHome.getNode(portletName);
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getAllManagedPortletName(String repository) throws Exception {
    return managedApplicationNames.get(repository);
  }
  
  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Node getTemplateByName(String repository, String portletName, String category,
      String templateName, SessionProvider sessionProvider) throws Exception {
    return getTemplateByName(portletName, category, templateName, sessionProvider);
  }
  
  /**
   * {@inheritDoc}
   */
  public Node getTemplateByName(String portletName, String category,
      String templateName, SessionProvider sessionProvider) throws Exception {
    Node basedApplicationTemplateHome = getBasedApplicationTemplatesHome(sessionProvider);
    return basedApplicationTemplateHome.getNode(portletName + "/" + category + "/" + templateName);
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<Node> getTemplatesByCategory(String repository, String portletName, String category,
      SessionProvider sessionProvider) throws Exception {
    return getTemplatesByCategory(portletName, category, sessionProvider);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<Node> getTemplatesByCategory(String portletName, String category,
      SessionProvider sessionProvider) throws Exception {
    Node basedApplicationTemplateHome = getBasedApplicationTemplatesHome(sessionProvider);
    Node applicationHome = basedApplicationTemplateHome.getNode(portletName);
    Node categoryNode = applicationHome.getNode(category);
    List<Node> templateNodes = new ArrayList<Node>();
    for(NodeIterator iterator = categoryNode.getNodes();iterator.hasNext();) {
      templateNodes.add(iterator.nextNode());
    }
    return templateNodes;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public Node getTemplateByPath(String repository,
                                String templatePath,
                                SessionProvider sessionProvider) throws Exception {
    return getTemplateByPath(templatePath, sessionProvider);
  }
  
  /**
   * {@inheritDoc}
   */
  public Node getTemplateByPath(String templatePath, SessionProvider sessionProvider) throws Exception {
    Node basedTemplateNode = getBasedApplicationTemplatesHome(sessionProvider);
    return (Node) basedTemplateNode.getSession().getItem(templatePath);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void removeTemplate(String repository, String portletName, String category,
      String templateName, SessionProvider sessionProvider) throws Exception {
    removeTemplate(portletName, category, templateName, sessionProvider);
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeTemplate(String portletName, String category,
      String templateName, SessionProvider sessionProvider) throws Exception {
    Node templateNode = getTemplateByName(portletName,category,templateName, sessionProvider );
    Session session = templateNode.getSession();
    templateNode.remove();
    session.save();
  }  

  /**
   * Gets the based application templates home.
   *
   * @param sessionProvider       SessionProvider
   * @param repository            String
   *                              The name of repository
   * @return the based application templates home
   * @see SessionProvider
   * @throws Exception the exception
   */
  private Node getBasedApplicationTemplatesHome(SessionProvider sessionProvider) throws Exception {
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session =
      sessionProvider.getSession(dmsRepoConfig.getSystemWorkspace(),manageableRepository);
    Node basedTemplateHome = (Node)session.getItem(basedApplicationTemplatesPath);
    return basedTemplateHome;
  }


  /**
   * Import predefined template to db.
   *
   * @param storedTemplateHomeNode the stored template home node
   * @see   Node
   * @throws Exception the exception
   */
  private void importPredefinedTemplateToDB(Node storedTemplateHomeNode) throws Exception {
    HashMap<String, List<PortletTemplateConfig>>  map = new HashMap<String,List<PortletTemplateConfig>>();
    String repository = ((ManageableRepository) storedTemplateHomeNode.getSession().getRepository()).getConfiguration()
                                                                                                    .getName();
    List<String> managedApplicationsPerRepo = managedApplicationNames.get(repository);
    if(managedApplicationsPerRepo == null) {
      managedApplicationsPerRepo = new ArrayList<String>();
    }
    for(PortletTemplatePlugin plugin:portletTemplatePlugins) {
      String portletName = plugin.getPortletName();
      if(!managedApplicationsPerRepo.contains(portletName)) {
        managedApplicationsPerRepo.add(portletName);
      }
      List<PortletTemplateConfig> list = map.get(portletName);
      if(list == null) {
        list = new ArrayList<PortletTemplateConfig>();
      }
      list.addAll(plugin.getPortletTemplateConfigs());
      map.put(portletName,list);
    }
    for(String portletName: managedApplicationsPerRepo) {
      Node templateNode = storedTemplateHomeNode.hasNode(portletName) ? 
                          storedTemplateHomeNode.getNode(portletName) : 
                          storedTemplateHomeNode.addNode(portletName,"nt:unstructured");
      storedTemplateHomeNode.save();
      for(PortletTemplateConfig config: map.get(portletName)) {
        addTemplate(templateNode,config);
      }
    }
    managedApplicationNames.put(repository,managedApplicationsPerRepo);
    storedTemplateHomeNode.getSession().save();
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    configuredTemplates_ = new HashMap<String, Set<String>>();
    RepositoryEntry repositoryEntry = null;
    try {
      repositoryEntry = repositoryService.getCurrentRepository().getConfiguration();
    } catch (RepositoryException e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
    }

    String repoName = repositoryEntry.getName();
    String workspaceName = repositoryEntry.getSystemWorkspaceName();
    storedWorkspaces.put(repoName,workspaceName);
    basedApplicationTemplatesPath = hierarchyCreator.getJcrPath(BasePath.CMS_VIEWTEMPLATES_PATH);

    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    for(Iterator<String> repositories = storedWorkspaces.keySet().iterator(); repositories.hasNext();) {
      String repository = repositories.next();
      try {
        Node storedTemplateHome = getBasedApplicationTemplatesHome(sessionProvider);
        importPredefinedTemplateToDB(storedTemplateHome);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Exception when import predefine application template into repository: " + repository, e);
        }
      }
    }
    sessionProvider.close();
//    //clear all template plugin to optimize memomry
//    portletTemplatePlugins.clear();
//    portletTemplatePlugins = null;
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }

  @Override
  public Set<String> getConfiguredAppTemplateMap(String portletName) {
    return configuredTemplates_ == null ? null : configuredTemplates_.get(portletName);
  }
}
