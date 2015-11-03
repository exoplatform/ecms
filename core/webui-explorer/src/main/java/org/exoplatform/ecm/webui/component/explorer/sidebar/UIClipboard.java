/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.util.LinkedList;
/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 20, 2006
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/sidebar/UIClipboard.gtmpl",
    events = {
        @EventConfig(listeners = UIClipboard.PasteActionListener.class),
        @EventConfig(listeners = UIClipboard.DeleteActionListener.class),
        @EventConfig(listeners = UIClipboard.ClearAllActionListener.class)
    }
  )

public class UIClipboard extends UIComponent {
  final static public String[] CLIPBOARD_BEAN_FIELD = {"path"} ;
  final static public String[]  CLIPBOARD_ACTIONS = {"Paste", "Delete"} ;

  private LinkedList<ClipboardCommand> clipboard_ ;

  public UIClipboard() throws Exception {
  }

  public String[] getBeanFields() {
    return CLIPBOARD_BEAN_FIELD ;
  }

  public String[] getBeanActions() {
    return  CLIPBOARD_ACTIONS ;
  }

  public LinkedList<ClipboardCommand> getClipboardData() throws Exception {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    ClipboardService  clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    clipboard_ = new LinkedList<ClipboardCommand>(clipboardService.getClipboardList(userId, false));
    return clipboard_;
  }

  static public class PasteActionListener extends EventListener<UIClipboard> {
    public void execute(Event<UIClipboard> event) throws Exception {
      UIClipboard uiClipboard = event.getSource() ;
      UIJCRExplorer uiExplorer = uiClipboard.getAncestorOfType(UIJCRExplorer.class);
      String id = event.getRequestContext().getRequestParameter(OBJECTID) ;
      int index = Integer.parseInt(id) ;
      ClipboardCommand selectedClipboard = uiClipboard.clipboard_.get(index-1);
      Node node = uiExplorer.getCurrentNode() ;
//      String nodePath = node.getPath();
//      String wsName = node.getSession().getWorkspace().getName();
      UIApplication app = uiClipboard.getAncestorOfType(UIApplication.class);
      try {
        //PasteManageComponent.processPaste(selectedClipboard, wsName + ":" + nodePath, event);
        PasteManageComponent.processPaste(selectedClipboard, node, event, uiExplorer);
        //uiWorkingArea.processPaste(selectedClipboard, wsName + ":" + nodePath, event);
        if (PasteManageComponent.isIsRefresh()) {
          uiExplorer.updateAjax(event);
        }

      } catch(PathNotFoundException path) {
        app.addMessage(new ApplicationMessage("PathNotFoundException.msg", null, ApplicationMessage.WARNING)) ;
        
        return ;
      } catch (Exception e) {
        app.addMessage(new ApplicationMessage("UIClipboard.msg.unable-pasted", null, ApplicationMessage.WARNING)) ;
        
        return ;
      }
    }
  }

  static public class DeleteActionListener extends EventListener<UIClipboard> {
    public void execute(Event<UIClipboard> event) throws Exception{
      UIClipboard uiClipboard = event.getSource() ;
      String itemIndex = event.getRequestContext().getRequestParameter(OBJECTID) ;
      ClipboardCommand command = uiClipboard.clipboard_.remove(Integer.parseInt(itemIndex)-1);
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      ClipboardService  clipboardService = WCMCoreUtils.getService(ClipboardService.class);
      clipboardService.removeClipboardCommand(userId, command);
    }
  }

  static public class ClearAllActionListener extends EventListener<UIClipboard> {
    public void execute(Event<UIClipboard> event) {
      UIClipboard uiClipboard = event.getSource() ;
      uiClipboard.clipboard_.clear() ;
      
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      ClipboardService  clipboardService = WCMCoreUtils.getService(ClipboardService.class);
      clipboardService.clearClipboardList(userId, false);
    }
  }
}

