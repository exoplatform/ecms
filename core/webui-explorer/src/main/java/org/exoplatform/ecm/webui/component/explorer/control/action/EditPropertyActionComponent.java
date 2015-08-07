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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsContainBinaryFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsEditableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/7/15
 * Edit property for nt:file
 */
@ComponentConfig(
                 events = {
                     @EventConfig(listeners = EditPropertyActionComponent.EditPropertyActionListener.class)
                 }
    )

public class EditPropertyActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsDocumentFilter(), new IsEditableFilter(), new CanSetPropertyFilter(),
      new IsNotLockedFilter(), new IsCheckedOutFilter(), new IsNotInTrashFilter(), new IsNotEditingDocumentFilter(),
      new IsContainBinaryFilter() });

  @UIExtensionFilters
  public static List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class EditPropertyActionListener extends UIActionBarActionListener<EditPropertyActionComponent> {
    public void processEvent(Event<EditPropertyActionComponent> event) throws Exception {
      EditPropertyActionComponent uicomp = event.getSource();
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class);
      Node selectedNode = null;
      if (nodePath != null && nodePath.length() != 0) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
        String wsName = null;
        if (matcher.find()) {
          wsName = matcher.group(1);
          nodePath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
        }
        Session session = uiExplorer.getSessionByWorkspace(wsName);
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
        try {
          // Use the method getNodeByPath because it is link aware
          selectedNode = uiExplorer.getNodeByPath(nodePath, session);
        } catch (PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,
                  ApplicationMessage.WARNING));

          return;
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null,
                  ApplicationMessage.WARNING));

          return;
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          return;
        }
      }
      if (selectedNode == null)  selectedNode = uiExplorer.getCurrentNode();
      uiExplorer.setSelectNode(selectedNode.getPath());
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      EditDocumentActionComponent.editDocument(event, null, uicomp, uiExplorer, selectedNode, uiApp);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
