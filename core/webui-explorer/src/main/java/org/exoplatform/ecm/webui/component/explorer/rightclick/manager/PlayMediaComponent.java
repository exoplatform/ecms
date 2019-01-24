/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Oct 30, 2009
 */
@ComponentConfig(
    events = {
      @EventConfig(listeners = PlayMediaComponent.PlayMediaActionListener.class)
    }
)
public class PlayMediaComponent  extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{ new IsNotInTrashFilter(),
                                               new IsNotTrashHomeNodeFilter() });
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  private boolean accept(Node node) throws Exception {
    if (!node.isNodeType(Utils.NT_FILE)) return false;
    String mimeType = node.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(Utils.MIME_TYPE, mimeType);
    if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, "VideoAudio", context)) {
        return true;
    }
    return false;
  }

  public static class PlayMediaActionListener extends UIWorkingAreaActionListener<PlayMediaComponent> {
    public void processEvent(Event<PlayMediaComponent> event) throws Exception {
      PlayMediaComponent playMedia = event.getSource();
      UIJCRExplorer uiExplorer = playMedia.getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = playMedia.getParent();
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      UIApplication uiApp = uiWorkingArea.getAncestorOfType(UIApplication.class);
      uiDocumentWorkspace.removeChildById("PlayMedia");
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      Matcher matcher = null;
      String wsName = null;
      Node tempNode = null;
      if(srcPath.indexOf(";") > -1) {
        String[] paths = srcPath.split(";");
        List<Node> nodes = new ArrayList<Node>();
        for(String path : paths) {
          matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(path);
          if (matcher.find()) {
            wsName = matcher.group(1);
            srcPath = matcher.group(2);
          } else {
            throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
          }
          tempNode = uiExplorer.getNodeByPath(srcPath, uiExplorer.getSessionByWorkspace(wsName));
          if (playMedia.accept(tempNode)) nodes.add(tempNode);
          if (nodes.size() == 0) {
            uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.unavaiable-supported-media-file",
                null,ApplicationMessage.WARNING));
            
            return;
          }
        }
      } else {
        matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
        if (matcher.find()) {
          wsName = matcher.group(1);
          srcPath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
        }
        try {
          // Use the method getNodeByPath because it is link aware
          uiExplorer.setSelectNode(wsName, srcPath);
        } catch(PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
              null,ApplicationMessage.WARNING));
          
          return;
        }
      }

      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
    }
  }


  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
