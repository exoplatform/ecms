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
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 10, 2008 4:28:44 PM
 */
@ComponentConfigs(
    {
      @ComponentConfig(lifecycle = UIContainerLifecycle.class),
      @ComponentConfig(
          type = UIBreadcumbs.class, id = "BreadcumbTaxonomyECMAdmin",
          template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
          events = @EventConfig(listeners = UITaxonomyManager.SelectPathActionListener.class)
      )
    }
)
public class UITaxonomyManager extends UIAbstractManager {
  
  static private String TAXONOMIES_ALIAS = "exoTaxonomiesPath" ;
  static private String EXO_ECM_ALIAS = "exoECMSystemPath" ;
  
  public static final String PERMISSION_ID_POPUP = "TaxonomyViewPermissionPopup";
  
  private String selectedPath_ = null ;

  public UITaxonomyManager() throws Exception {
    addChild(UIBreadcumbs.class, "BreadcumbTaxonomyECMAdmin", "BreadcumbTaxonomyECMAdmin");
    addChild(UITaxonomyTree.class, null, null) ;
    addChild(UITaxonomyWorkingArea.class, null, null) ;
  }
  
  @Override
  public void init() throws Exception {}
  
  public void refresh() throws Exception {
    update();
  }
  
  public void update() throws Exception {
    UITaxonomyTree uiTree = getChild(UITaxonomyTree.class) ;
    uiTree.update() ;
    UITaxonomyWorkingArea uiTaxonomyWorkingArea = getChild(UITaxonomyWorkingArea.class);
    uiTaxonomyWorkingArea.update();
  }
  
  public void update(String parentPath) throws Exception {
    UITaxonomyTree uiTree = getChild(UITaxonomyTree.class) ;
    uiTree.setNodeSelect(parentPath) ;
    UITaxonomyWorkingArea uiWorkingArea = getChild(UITaxonomyWorkingArea.class) ;
    uiWorkingArea.setSelectedPath(parentPath) ;
    uiWorkingArea.update() ;    
  }
  
  public Node getRootNode() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class) ;
    return (Node)getSession().getItem(nodeHierarchyCreator.getJcrPath(EXO_ECM_ALIAS)) ;
  }
  
  public Node getTaxonomyNode() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class) ;
    return (Node)getSession().getItem(nodeHierarchyCreator.getJcrPath(TAXONOMIES_ALIAS)) ;
  }
  
  public void setSelectedPath(String selectedPath) { selectedPath_ = selectedPath ; }
  public String getSelectedPath() { return selectedPath_ ; }
  
  public Node getNodeByPath(String path) throws Exception {
    return (Node) getSession().getItem(path) ;
  }
  
  public String getRepository() throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
    PortletPreferences pref = pcontext.getRequest().getPreferences() ;
    String repository = pref.getValue(Utils.REPOSITORY, "") ;
    return repository ;
  }
  
  private String getDmsSystemWorkspaceName(String repository) {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return dmsRepoConfig.getSystemWorkspace();
  }
  
  public Session getSession() throws Exception {
    String repositoryName = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    String workspace = getDmsSystemWorkspaceName(repositoryName) ;
    return SessionProviderFactory.createSystemProvider().getSession(workspace, getRepository(repositoryName)) ;
  }
  
  public ManageableRepository getRepository(String repositoryName) throws Exception{
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    return repositoryService.getCurrentRepository();
  }
  
  public void initPopup(String path) throws Exception {
    removeChildById("TaxonomyPopup");
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "TaxonomyPopup");
    uiPopup.setWindowSize(600, 250);
    UITaxonomyForm uiTaxoForm = createUIComponent(UITaxonomyForm.class, null, null);
    uiPopup.setUIComponent(uiTaxoForm);
    uiTaxoForm.setParent(path);
    uiPopup.setRendered(true);
    uiPopup.setShow(true);
  }

  public UIPopupContainer initPopupPermission(String id) throws Exception {
    removeChildById(id) ;
    return addChild(UIPopupContainer.class, null, id) ;
  }
  
  public void onChange(Node currentNode) throws Exception {
    UIBreadcumbs uiBreadcumbs = getChild(UIBreadcumbs.class);
    List<LocalPath> listLocalPath = new ArrayList<LocalPath>();
    String path = currentNode.getPath().trim();
    String taxonomyPath = getTaxonomyNode().getPath();
    if (path.startsWith(taxonomyPath)) {
      String subTaxonomy = path.substring(taxonomyPath.length(), path.length());
      String[] arrayPath = subTaxonomy.split("/");
      if (arrayPath.length > 0) {
        for (int i = 0; i < arrayPath.length; i++) {
          if (!arrayPath[i].trim().equals("")) {
            UIBreadcumbs.LocalPath localPath1 = new UIBreadcumbs.LocalPath(arrayPath[i].trim(), arrayPath[i].trim());
            listLocalPath.add(localPath1);
          }
        }
      }
    } 
    uiBreadcumbs.setPath(listLocalPath);
  }
  
  public void changeGroup(String groupId, Object context) throws Exception {    
    String stringPath = getTaxonomyNode().getPath() + "/";    
    UIBreadcumbs uiBreadcumb = getChild(UIBreadcumbs.class);
    if (groupId == null) groupId = "";
    List<LocalPath> listLocalPath = uiBreadcumb.getPath();
    if (listLocalPath == null || listLocalPath.size() == 0) return;
    List<String> listLocalPathString = new ArrayList<String>();
    for (LocalPath localPath : listLocalPath) {
      listLocalPathString.add(localPath.getId().trim());
    }
    if (listLocalPathString.contains(groupId)) {
      int index = listLocalPathString.indexOf(groupId);
      if (index == listLocalPathString.size() - 1) return;
      for (int i = listLocalPathString.size() - 1; i > index; i--) {
        listLocalPathString.remove(i);
        listLocalPath.remove(i);
      }
      uiBreadcumb.setPath(listLocalPath);
      for (int i = 0; i < listLocalPathString.size(); i++) {
        String pathName = listLocalPathString.get(i);
        if (pathName != null && pathName.length() > 0) {
          stringPath += pathName.trim();
          if (i < listLocalPathString.size() - 1) stringPath += "/";
        }
      }
      UITaxonomyTree uiTaxonomyTree = getChild(UITaxonomyTree.class);
      uiTaxonomyTree.setNodeSelect(stringPath);
    }
  }
  
  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource() ;
      UITaxonomyManager uiTaxonomyManager = uiBreadcumbs.getParent() ;
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiBreadcumbs.setSelectPath(objectId);    
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId() ;
      uiTaxonomyManager.changeGroup(selectGroupId, event.getRequestContext()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManager) ;
    }
  }
}
