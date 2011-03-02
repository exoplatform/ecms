/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.control.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UISimpleCategoriesAddedList;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UISimpleCategoryManager;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009  
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ManageSimpleCategoriesActionComponent.ManageSimpleCategoriesActionListener.class)
     }
 )
@Deprecated
public class ManageSimpleCategoriesActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{new IsNotRootNodeFilter(), new IsCheckedOutFilter(), new CanSetPropertyFilter(), new IsNotLockedFilter(), new IsDocumentFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  
  public static class ManageSimpleCategoriesActionListener extends UIActionBarActionListener<ManageSimpleCategoriesActionComponent> {
    public void processEvent(Event<ManageSimpleCategoriesActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      String repository = uiExplorer.getRepositoryName();
      ManageableRepository manaRepository = 
        uiActionBar.getApplicationComponent(RepositoryService.class).getRepository(repository);
      String workspaceName = manaRepository.getConfiguration().getSystemWorkspaceName();
      NodeHierarchyCreator nodeHierarchyCreator = uiActionBar.getApplicationComponent(NodeHierarchyCreator.class);
      uiExplorer.setIsHidePopup(true);
      UISimpleCategoryManager uiSimpleCategoryManager = uiExplorer.createUIComponent(UISimpleCategoryManager.class, null, null);
      UIOneNodePathSelector uiNodePathSelector = uiSimpleCategoryManager.getChild(UIOneNodePathSelector.class);
      uiNodePathSelector.setIsDisable(workspaceName, true);
      uiNodePathSelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
      String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.EXO_TAXONOMIES_PATH);      
      uiNodePathSelector.setRootNodeLocation(repository, workspaceName, rootTreePath);
      uiNodePathSelector.init(uiExplorer.getSessionProvider());
      UISimpleCategoriesAddedList uiSimpleCategoriesAddedList = uiSimpleCategoryManager.getChild(UISimpleCategoriesAddedList.class);
      uiNodePathSelector.setSourceComponent(uiSimpleCategoriesAddedList, null);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(uiSimpleCategoryManager, 630, 500);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }
}
