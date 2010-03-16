/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software ; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation ; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY ; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program ; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.tree.selectmany ;

import java.util.ArrayList ;
import java.util.List ;

import javax.jcr.Node ;

import org.exoplatform.ecm.webui.popup.UIPopupComponent ;
import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector ;
import org.exoplatform.ecm.webui.tree.UINodeTreeBuilder ;
import org.exoplatform.portal.webui.util.SessionProviderFactory ;
import org.exoplatform.services.cms.categories.CategoriesService ;
import org.exoplatform.services.jcr.RepositoryService ;
import org.exoplatform.webui.config.annotation.ComponentConfig ;
import org.exoplatform.webui.config.annotation.ComponentConfigs ;
import org.exoplatform.webui.config.annotation.EventConfig ;
import org.exoplatform.webui.core.UIBreadcumbs ;
import org.exoplatform.webui.core.UIPopupWindow ;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath ;
import org.exoplatform.webui.event.Event ;
import org.exoplatform.webui.event.EventListener ;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 *          dzungdev@gmail.com"
 * Aug 11, 2008  
 */
@ComponentConfigs(
    {
      @ComponentConfig(
          template = "classpath:groovy/ecm/webui/UIContainerWithAction.gtmpl"
      ),
      @ComponentConfig(
          type = UIBreadcumbs.class, id = "BreadcumbCategories",
          template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
          events = @EventConfig(listeners = UICategoriesSelector.SelectPathActionListener.class)
      )
    }
)
public class UICategoriesSelector extends UIBaseNodeTreeSelector implements UIPopupComponent {
//  final static public String[] ACTIONS = {"Close"} ;
  private List<String> existedCategoryList = new ArrayList<String>() ;
  private String pathTaxonomy = "" ;
  
  public UICategoriesSelector() throws Exception {
    addChild(UIBreadcumbs.class, "BreadcumbCategories", "BreadcumbCategories") ;
    addChild(UINodeTreeBuilder.class, null, null) ;
    addChild(UICategoriesSelectPanel.class, null, null) ;
    addChild(UISelectedCategoriesGrid.class, null, null).setRendered(false) ;
  }
  
//  public String[] getActions() { return ACTIONS  ; }

  public void init() throws Exception {
    CategoriesService categoriesService = getApplicationComponent(CategoriesService.class) ;
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    String repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName() ;
    Node rootCategories = categoriesService.getTaxonomyHomeNode(repositoryName, SessionProviderFactory.createSessionProvider()) ;
    Node rootCategoryTree = rootCategories ;
    if (rootCategories != null) pathTaxonomy = rootCategories.getPath() + "/" ;
    UINodeTreeBuilder builder = getChild(UINodeTreeBuilder.class) ;
    builder.setRootTreeNode(rootCategoryTree) ;

    UICategoriesSelectPanel uiCategoriesSelectPanel = getChild(UICategoriesSelectPanel.class) ;
    uiCategoriesSelectPanel.updateGrid() ;
    
    UISelectedCategoriesGrid categoriesGrid = getChild(UISelectedCategoriesGrid.class) ;
    categoriesGrid.setSelectedCategories(existedCategoryList) ;
    if (existedCategoryList.size() > 0) {
      categoriesGrid.setRendered(true) ;
    }
    categoriesGrid.updateGrid(categoriesGrid.getUIPageIterator().getCurrentPage());
  }

  public void onChange(Node currentNode, Object context) throws Exception {
    UICategoriesSelectPanel uiCategoriesSelectPanel = getChild(UICategoriesSelectPanel.class) ;
    uiCategoriesSelectPanel.setParentNode(currentNode) ;
    uiCategoriesSelectPanel.updateGrid() ;
    
    UIBreadcumbs uiBreadcumbs = getChild(UIBreadcumbs.class) ;
    List<LocalPath> listLocalPath = new ArrayList<LocalPath>() ;
    String path = currentNode.getPath().trim() ;
    
    if (path.startsWith(pathTaxonomy)) {
      path = path.substring(pathTaxonomy.length(), path.length());
    }    
    String[] arrayPath = path.split("/");
    if (arrayPath.length > 0) {
      for (int i = 0; i < arrayPath.length; i++) {
        if (!arrayPath[i].trim().equals("")) {
          UIBreadcumbs.LocalPath localPath1 = new UIBreadcumbs.LocalPath(arrayPath[i].trim(), arrayPath[i].trim());
          listLocalPath.add(localPath1);
        }
      }
    }
    uiBreadcumbs.setPath(listLocalPath) ;
  }

  public List<String> getExistedCategoryList() {
    return existedCategoryList ;
  }

  public void setExistedCategoryList(List<String> existedCategoryList) {
    this.existedCategoryList = existedCategoryList ; 
  }
  
  public void activate() throws Exception {    
  }
  
  public void deActivate() throws Exception {
  }
  
  public void changeGroup(String groupId, Object context) throws Exception {    
    String stringPath = pathTaxonomy ;    
    UIBreadcumbs uiBreadcumb = getChild(UIBreadcumbs.class) ;
    if (groupId == null) groupId = "" ;
    List<LocalPath> listLocalPath = uiBreadcumb.getPath() ;
    if (listLocalPath == null || listLocalPath.size() == 0) return ;
    List<String> listLocalPathString = new ArrayList<String>() ;
    for (LocalPath localPath : listLocalPath) {
      listLocalPathString.add(localPath.getId().trim()) ;
    }
    if (listLocalPathString.contains(groupId)) {
      int index = listLocalPathString.indexOf(groupId) ;
      if (index == listLocalPathString.size() - 1) return ;
      for (int i = listLocalPathString.size() - 1 ; i > index ; i--) {
        listLocalPathString.remove(i) ;
        listLocalPath.remove(i) ;
      }
      uiBreadcumb.setPath(listLocalPath) ;
      for (int i = 0 ; i < listLocalPathString.size() ; i++) {
        String pathName = listLocalPathString.get(i) ;
        if (pathName != null || !pathName.equals("")) {
          stringPath += pathName.trim() ;
          if (i < listLocalPathString.size() - 1) stringPath += "/" ;
        }
      }
      changeNode(stringPath, context) ;
    }
  }
  
  private void changeNode(String stringPath, Object context) throws Exception {
    UINodeTreeBuilder builder = getChild(UINodeTreeBuilder.class) ;
    builder.changeNode(stringPath, context) ;
    UIBaseNodeTreeSelector nodeTreeSelector = builder.getAncestorOfType(UIBaseNodeTreeSelector.class) ;      
    UICategoriesSelector uiCategoriesSelector = nodeTreeSelector.getChild(UICategoriesSelector.class) ;
    if (uiCategoriesSelector != null) uiCategoriesSelector.setRenderedChild(UICategoriesSelectPanel.class) ;
  }
  
  static public class CloseActionListener extends EventListener<UICategoriesSelector> {
    public void execute(Event<UICategoriesSelector> event) throws Exception {      
      UICategoriesSelector uiCategoriesSelector = event.getSource() ;
      UIPopupWindow uiPopup = uiCategoriesSelector.getParent() ;
      if(uiPopup != null) {
        uiPopup.setShow(false) ;
        uiPopup.setRendered(false) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup.getParent()) ;
        return ;
      }
      uiCategoriesSelector.deActivate() ;
    }
  }
  
  static  public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIBreadcumbs uiBreadcumbs = event.getSource()  ;
      UICategoriesSelector uiCategoriesSelector = uiBreadcumbs.getParent()  ;
      String objectId =  event.getRequestContext().getRequestParameter(OBJECTID)  ;
      uiBreadcumbs.setSelectPath(objectId) ;    
      String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId()  ;
      uiCategoriesSelector.changeGroup(selectGroupId, event.getRequestContext())  ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCategoriesSelector)  ;
    }
  }
}
