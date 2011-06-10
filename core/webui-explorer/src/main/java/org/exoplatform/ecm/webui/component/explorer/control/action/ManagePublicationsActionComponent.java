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
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanRemoveNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.HasPublicationLifecycleFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIActivePublication;
import org.exoplatform.ecm.webui.component.explorer.popup.admin.UIPublicationManager;
import org.exoplatform.services.ecm.publication.PublicationPresentationService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.ecm.publication.plugins.webui.UIPublicationLogList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ManagePublicationsActionComponent.ManagePublicationsActionListener.class)
     }
 )
public class ManagePublicationsActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new HasPublicationLifecycleFilter(),
      new IsDocumentFilter("UIActionBar.msg.manage-publication.not-supported-nodetype"),
      new IsNotRootNodeFilter("UIActionBar.msg.cannot-enable-publication-rootnode"),
      new CanSetPropertyFilter("UIActionBar.msg.access-denied"), new CanRemoveNodeFilter(),
      new IsNotLockedFilter()                         });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ManagePublicationsActionListener extends UIActionBarActionListener<ManagePublicationsActionComponent> {
    public void processEvent(Event<ManagePublicationsActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.setIsHidePopup(false);
      PublicationService publicationService = uiActionBar.getApplicationComponent(PublicationService.class);
      PublicationPresentationService publicationPresentationService = uiActionBar.
          getApplicationComponent(PublicationPresentationService.class);
      if (!publicationService.isNodeEnrolledInLifecycle(currentNode)) {
        UIActivePublication activePublication = uiActionBar.createUIComponent(UIActivePublication.class,null,null);
        if(publicationService.getPublicationPlugins().size() == 1) {
          activePublication.setRendered(false);
          uiExplorer.addChild(activePublication);
          String lifecycleName = publicationService.getPublicationPlugins().keySet().iterator().next();
          activePublication.enrolNodeInLifecycle(currentNode,lifecycleName,event.getRequestContext());
          return;
        }
        activePublication.setRendered(true);
        activePublication.refresh(activePublication.getUIPageIterator().getCurrentPage());
        UIPopupContainer.activate(activePublication, 600, 300);
        event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
        return;
      }
      UIContainer cont = uiActionBar.createUIComponent(UIContainer.class, null, null);
      UIForm uiForm = publicationPresentationService.getStateUI(currentNode, cont);
      if (uiForm instanceof UIPopupComponent) {
        //This is special case for wcm want to more than 2 tabs in PublicationManager
        //The uiForm in this case should be a UITabPane or UIFormTabPane and need be a UIPopupComponent
        UIPopupContainer.activate(uiForm, 700, 500);
      } else {
        UIPublicationManager uiPublicationManager =
          uiExplorer.createUIComponent(UIPublicationManager.class, null, null);
        uiPublicationManager.addChild(uiForm);
        uiPublicationManager.addChild(UIPublicationLogList.class, null, null).setRendered(false);
        UIPublicationLogList uiPublicationLogList =
          uiPublicationManager.getChild(UIPublicationLogList.class);
        UIPopupContainer.activate(uiPublicationManager, 700, 500);
        uiPublicationLogList.setNode(currentNode);
        uiPublicationLogList.updateGrid();
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
   }
  }
}
