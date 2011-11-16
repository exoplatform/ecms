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

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UICategoriesAddedList;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UICategoryManager;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
       @EventConfig(listeners = ManageCategoriesActionComponent.ManageCategoriesActionListener.class)
     }
 )
public class ManageCategoriesActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsNotRootNodeFilter(), new IsCheckedOutFilter(), new CanSetPropertyFilter(),
      new IsNotLockedFilter(), new IsDocumentFilter() });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ManageCategoriesActionListener extends UIActionBarActionListener<ManageCategoriesActionComponent> {
    public void processEvent(Event<ManageCategoriesActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      String repository = uiExplorer.getRepositoryName();
      String workspaceName = null;
      uiExplorer.setIsHidePopup(true);
      UICategoryManager uiManager = uiExplorer.createUIComponent(UICategoryManager.class, null, null);
      UIOneTaxonomySelector uiOneTaxonomySelector = uiManager.getChild(UIOneTaxonomySelector.class);
      TaxonomyService taxonomyService = uiActionBar.getApplicationComponent(TaxonomyService.class);
      UIApplication uiApp = uiActionBar.getAncestorOfType(UIApplication.class);
      List<Node> lstNode = taxonomyService.getAllTaxonomyTrees();
      if (lstNode != null && lstNode.size() > 0) {
        uiOneTaxonomySelector.setRootTaxonomyName(lstNode.get(0).getName());
        workspaceName = lstNode.get(0).getSession().getWorkspace().getName();
        uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, lstNode.get(0).getPath());
        uiOneTaxonomySelector.setIsDisable(workspaceName, false);
        uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
        uiOneTaxonomySelector.init(uiExplorer.getSessionProvider());
        UICategoriesAddedList uiCateAddedList = uiManager.getChild(UICategoriesAddedList.class);
        uiOneTaxonomySelector.setSourceComponent(uiCateAddedList, null);
        UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
        UIPopupContainer.activate(uiManager, 650, 500);
        event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.not-exist-categories", null));
        
        uiExplorer.updateAjax(event);
      }
    }
  }
}
