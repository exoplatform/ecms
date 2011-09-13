/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.rightclick.action;

import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.rightclick.viewinfor.UIViewInfoManager;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 29, 2010
 */
@ComponentConfig(events = { @EventConfig(listeners = ViewInfoActionComponent.ViewInfoActionListener.class) })
public class ViewInfoActionComponent extends UIAbstractManagerComponent{

  /**
   * @author hai_lethanh
   * class used to handle event raised on ViewFileInfoActionComponent
   */
  public static class ViewInfoActionListener extends UIWorkingAreaActionListener<ViewInfoActionComponent> {

    @Override
    protected void processEvent(Event<ViewInfoActionComponent> event) throws Exception {
      ViewInfoActionComponent uicomp = event.getSource();
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer objUIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);

      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      Node selectedNode = null;
      if (nodePath != null && nodePath.length() != 0) {
        //get workspace & selected node path
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
        String wsName = null;
        if (matcher.find()) {
          wsName = matcher.group(1);
          nodePath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
        }

        //get session
        Session session = uiExplorer.getSessionByWorkspace(wsName);

        //get UIApplication
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);

        //get selected node
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

      //show popup
      UIViewInfoManager uiViewInfoManager = uiExplorer.createUIComponent(UIViewInfoManager.class, null, null);
      uiViewInfoManager.setSelectedNode(selectedNode);
      objUIPopupContainer.activate(uiViewInfoManager, 600, 360);
      event.getRequestContext().addUIComponentToUpdateByAjax(objUIPopupContainer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
