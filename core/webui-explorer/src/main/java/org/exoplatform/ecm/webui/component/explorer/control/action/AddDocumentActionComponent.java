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

import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentFormController;
import org.exoplatform.ecm.webui.component.explorer.search.UISearchResult;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = AddDocumentActionComponent.AddDocumentActionListener.class)
     }
 )
public class AddDocumentActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotNtFileFilter(),
                                              new CanAddNodeFilter(),
                                              new IsNotDocumentFilter(),
                                              new IsNotLockedFilter(),
                                              new IsCheckedOutFilter(),
                                              new IsNotTrashHomeNodeFilter(),
                                              new IsNotInTrashFilter(),
                                              new IsNotEditingDocumentFilter()});

  @UIExtensionFilters
  public static List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void addDocument(Event<? extends UIComponent> event,
                          UIJCRExplorer uiExplorer,
                          UIApplication uiApp,
                          UIComponent uiComp,
                          WebuiRequestContext context) throws Exception {
    if (event != null)
      context = event.getRequestContext();

    UIDocumentFormController uiController =
      uiComp.createUIComponent(UIDocumentFormController.class, null, null);
    uiController.setCurrentNode(uiExplorer.getCurrentNode());
    uiController.setRepository(uiExplorer.getRepositoryName());
    if(uiController.getListFileType().isEmpty()) {
      uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.empty-file-type", null,
          ApplicationMessage.WARNING));
      return;
    }
//    uiExplorer.setPathBeforeEditing(uiExplorer.getCurrentPath());
    uiController.init();

    UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
    UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    if(!uiDocumentWorkspace.isRendered()) {
      uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
      uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
    }
    uiDocumentWorkspace.getChild(UIDocumentContainer.class).setRendered(false);
    uiDocumentWorkspace.getChild(UISearchResult.class).setRendered(false);
    UIDocumentFormController controller = uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
    if (controller != null) {
      controller.getChild(UIDocumentForm.class).releaseLock();
      controller.getChild(UIDocumentForm.class).clearRemovedNode();
    }
    uiDocumentWorkspace.addChild(uiController);
    uiController.bindContentType();
    uiController.setRendered(true);
    context.addUIComponentToUpdateByAjax(uiWorkingArea);
    if (event != null)
      uiExplorer.updateAjax(event);
  }

  public static class AddDocumentActionListener extends UIActionBarActionListener<AddDocumentActionComponent> {
    public void processEvent(Event<AddDocumentActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
      addDocument(event, uiExplorer, uiApp, event.getSource(), null);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
