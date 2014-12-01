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
package org.exoplatform.services.cms.views.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.TemplateConfig;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ManageViewPlugin extends BaseComponentPlugin {

  private static String ECM_EXPLORER_TEMPLATE = "ecmExplorerTemplate" ;
  public static final String EDITED_CONFIGURED_VIEWS = "EditedConfiguredViews";
  public static final String EDITED_CONFIGURED_VIEWS_TEMPLATES = "EditedConfiguredViewsTemplate";
  private InitParams params_ ;
  private RepositoryService repositoryService_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  private ConfigurationManager cservice_ ;
  private boolean autoCreateInNewRepository_ = false ;
  private String predefinedViewsLocation_ = "war:/conf/dms/artifacts";
  private DMSConfiguration dmsConfiguration_;
  private TemplateService templateService;
  private Set<String> configuredTemplate_;
  private Set<String> configuredViews_;

  public ManageViewPlugin(RepositoryService repositoryService, InitParams params, ConfigurationManager cservice,
      NodeHierarchyCreator nodeHierarchyCreator, DMSConfiguration dmsConfiguration) throws Exception {
    params_ = params ;
    repositoryService_ = repositoryService ;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
    cservice_ = cservice ;
    ValueParam autoInitParam = params.getValueParam("autoCreateInNewRepository") ;
    if(autoInitParam !=null) {
      autoCreateInNewRepository_ = Boolean.parseBoolean(autoInitParam.getValue()) ;
    }
    ValueParam predefinedViewLocation = params.getValueParam("predefinedViewsLocation");
    if(predefinedViewLocation != null) {
      predefinedViewsLocation_ = predefinedViewLocation.getValue();
    }
    dmsConfiguration_ = dmsConfiguration;
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  public void init() throws Exception {
    if(autoCreateInNewRepository_) {
      importPredefineViews() ;
      return ;
    }
    return;
  }

  private void importPredefineViews() throws Exception {
    configuredTemplate_ = new HashSet<String>();
    configuredViews_ = new HashSet<String>();
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    String viewsPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_VIEWS_PATH);
    String templatesPath = nodeHierarchyCreator_.getJcrPath(BasePath.CMS_VIEWTEMPLATES_PATH);
    String warViewPath = predefinedViewsLocation_ + templatesPath.substring(templatesPath.lastIndexOf("exo:ecm") + 7) ;
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    Session session = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace()) ;
    ViewConfig viewObject = null ;
    TemplateConfig templateObject = null ;
    Node viewHomeNode = (Node)session.getItem(viewsPath) ;
    while(it.hasNext()) {
      Object object = it.next().getObject();
      if(object instanceof ViewConfig) {
        viewObject = (ViewConfig)object ;
        String viewNodeName = viewObject.getName();
        configuredViews_.add(viewNodeName);
        if(viewHomeNode.hasNode(viewNodeName) || Utils.getAllEditedConfiguredData(
          this.getClass().getSimpleName(), EDITED_CONFIGURED_VIEWS, true).contains(viewNodeName)) continue ;
        Node viewNode = addView(viewHomeNode,viewNodeName,viewObject.getPermissions(),
                                viewObject.isHideExplorerPanel(), viewObject.getTemplate()) ;
        for(Tab tab:viewObject.getTabList()) {
          addTab(viewNode,tab.getTabName(),tab.getButtons()) ;
        }
      }else if(object instanceof TemplateConfig) {
        templateObject = (TemplateConfig) object;
        addTemplate(templateObject,session,warViewPath);
        configuredTemplate_.add(templateObject.getName());
      }
    }
    session.save();
    session.logout();
  }

  private Node addView(Node viewManager, String viewName, String permissions, boolean hideExplorerPanel, String template) 
  throws Exception {
    Node contentNode = viewManager.addNode(viewName, "exo:view");
    contentNode.setProperty("exo:accessPermissions", permissions);
    contentNode.setProperty("exo:template", template);
    if(contentNode.hasProperty("exo:hideExplorerPanel")) {
      contentNode.setProperty("exo:hideExplorerPanel", hideExplorerPanel);
    }
    viewManager.save() ;
    return contentNode ;
  }

  private void addTab(Node view, String tabName, String buttons) throws Exception {
    Node tab ;
    if(view.hasNode(tabName)){
      tab = view.getNode(tabName) ;
    }else {
      tab = view.addNode(tabName, "exo:tab");
    }
    tab.setProperty("exo:buttons", buttons);
    view.save() ;
  }

  private void addTemplate(TemplateConfig tempObject, Session session, String warViewPath) throws Exception {
    String type = tempObject.getTemplateType() ;
    String alias = "" ;
    if(type.equals(ECM_EXPLORER_TEMPLATE)) {
      alias = BasePath.ECM_EXPLORER_TEMPLATES ;
    }
    String templateHomePath = nodeHierarchyCreator_.getJcrPath(alias) ;
    Node templateHomeNode = (Node)session.getItem(templateHomePath) ;
    String templateName = tempObject.getName() ;
    if(templateHomeNode.hasNode(templateName) || Utils.getAllEditedConfiguredData(
      this.getClass().getSimpleName(), EDITED_CONFIGURED_VIEWS_TEMPLATES, true).contains(templateName)) return;
    String warPath = warViewPath + tempObject.getWarPath() ;
    InputStream in = cservice_.getInputStream(warPath) ;
    templateService.createTemplate(templateHomeNode, templateName, templateName, in, new String[] {"*"});
  }
  
  public Set<String> getConfiguredTemplates() {
    return configuredTemplate_;
  }
  
  public Set<String> getConfiguredViews() {
    return configuredViews_;
  }
  
}
