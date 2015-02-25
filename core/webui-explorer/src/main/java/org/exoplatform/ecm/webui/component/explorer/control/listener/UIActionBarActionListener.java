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
package org.exoplatform.ecm.webui.component.explorer.control.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.ext.UIExtensionEventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
public abstract class UIActionBarActionListener<T extends UIComponent> extends UIExtensionEventListener<T> {

  private static final Log LOG  = ExoLogger.getLogger(UIActionBarActionListener.class.getName());
  /**
   * {@inheritDoc}
   */
  @Override
  protected Map<String, Object> createContext(Event<T> event) throws Exception {
    Map<String, Object> context = new HashMap<String, Object>();
    UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    String nodePath = event.getRequestContext().getRequestParameter(UIComponent.OBJECTID);
    try {
      Node currentNode;
      if (nodePath != null && nodePath.length() != 0 && !nodePath.contains(";")) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
        String wsName = null;
        if (matcher.find()) {
          wsName = matcher.group(1);
          nodePath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + nodePath + "'");
        }
        Session session = uiExplorer.getSessionByWorkspace(wsName);
        // Use the method getNodeByPath because it is link aware
        currentNode = uiExplorer.getNodeByPath(nodePath, session);
      } else {
        currentNode = uiExplorer.getCurrentNode();
      }

      WebuiRequestContext requestContext = event.getRequestContext();
      UIApplication uiApp = requestContext.getUIApplication();
      context.put(UIActionBar.class.getName(), uiActionBar);
      context.put(UIJCRExplorer.class.getName(), uiExplorer);
      context.put(UIApplication.class.getName(), uiApp);
      context.put(Node.class.getName(), currentNode);
      context.put(WebuiRequestContext.class.getName(), requestContext);
    } catch(PathNotFoundException pne) {
      throw new MessageException(new ApplicationMessage("UIPopupMenu.msg.path-not-found", null,
          ApplicationMessage.WARNING)) ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem occurs", e);
      }
    }
    return context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getExtensionType() {
    return ManageViewService.EXTENSION_TYPE;
  }
}
