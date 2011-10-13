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
package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.DeleteManageComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 18, 2008 9:52:55 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/UIConfirmMessage.gtmpl",
    events = {
        @EventConfig(listeners = UIConfirmMessage.OKActionListener.class),
        @EventConfig(listeners = UIConfirmMessage.CloseActionListener.class)

    }
)
public class UIConfirmMessage extends UIComponent implements UIPopupComponent {

  private String messageKey_;
  private String[] args_ = {};
  protected boolean isOK_ = false;
  protected String nodePath_;

  public UIConfirmMessage() throws Exception {
  }

  public void setMessageKey(String messageKey) { messageKey_ = messageKey; }

  public String getMessageKey() { return messageKey_; }

  public void setArguments(String[] args) { args_ = args; }

  public String[] getArguments() { return args_; }

  public boolean isOK() { return isOK_; }

  public void setNodePath(String nodePath) { nodePath_ = nodePath; }

  public String[] getActions() {
    return new String[] {"OK", "Close"};
  }

  static  public class OKActionListener extends EventListener<UIConfirmMessage> {
    public void execute(Event<UIConfirmMessage> event) throws Exception {
      UIConfirmMessage uiConfirm = event.getSource();
      UIJCRExplorer uiExplorer = uiConfirm.getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      uiWorkingArea.getChild(DeleteManageComponent.class).doDelete(uiConfirm.nodePath_, event);
      uiConfirm.isOK_ = true;
      uiConfirm.deActivate();
    }
  }

  static  public class CloseActionListener extends EventListener<UIConfirmMessage> {
    public void execute(Event<UIConfirmMessage> event) throws Exception {
      UIConfirmMessage uiConfirm = event.getSource();
      uiConfirm.isOK_ = false;
      UIPopupContainer popupAction = uiConfirm.getAncestorOfType(UIPopupContainer.class) ;
      popupAction.deActivate() ;
    }
  }

  public void activate() throws Exception {

  }

  public void deActivate() throws Exception {

  }
}
