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
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIRelationManager;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIRelationsAddedList;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
       @EventConfig(listeners = ManageRelationsActionComponent.ManageRelationsActionListener.class)
     }
 )
public class ManageRelationsActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsNotRootNodeFilter(), new IsCheckedOutFilter(), new CanSetPropertyFilter(),
      new IsNotLockedFilter()                         });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ManageRelationsActionListener extends UIActionBarActionListener<ManageRelationsActionComponent> {
    public void processEvent(Event<ManageRelationsActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.setIsHidePopup(true);
      RepositoryService repoService = uiActionBar.getApplicationComponent(RepositoryService.class);
      UIRelationManager uiRelationManager =
        uiExplorer.createUIComponent(UIRelationManager.class, null, null);
      RelationsService relateService =
        uiActionBar.getApplicationComponent(RelationsService.class);
      UIRelationsAddedList uiRelateAddedList =
        uiRelationManager.getChild(UIRelationsAddedList.class);
      List<Node> relations = relateService.getRelations(uiExplorer.getCurrentNode(), WCMCoreUtils.getUserSessionProvider());
      uiRelateAddedList.updateGrid(relations, 1);
      String repository = uiActionBar.getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
      String defaultWsName = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      UIOneNodePathSelector uiNodePathSelector = uiRelationManager.getChild(UIOneNodePathSelector.class);
      uiNodePathSelector.setIsDisable(defaultWsName, false);
      uiNodePathSelector.setRootNodeLocation(repository, defaultWsName, "/");
      TemplateService tservice = uiActionBar.getApplicationComponent(TemplateService.class);
      List<String> documentNodeType = tservice.getDocumentTemplates();
      String [] arrAcceptedNodeTypes = new String[documentNodeType.size()];
      documentNodeType.toArray(arrAcceptedNodeTypes) ;
      uiNodePathSelector.setAcceptedNodeTypesInPathPanel(arrAcceptedNodeTypes);
      uiNodePathSelector.setIsShowSystem(false);
      uiNodePathSelector.setAcceptedNodeTypesInTree(new String[] {Utils.NT_UNSTRUCTURED, Utils.NT_FOLDER});
      uiNodePathSelector.init(uiExplorer.getSessionProvider());
      uiNodePathSelector.setSourceComponent(uiRelateAddedList, null);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(uiRelationManager, 710, 500);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }
}
