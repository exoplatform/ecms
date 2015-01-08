/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.upload;

import org.exoplatform.ecm.webui.component.explorer.UIConfirmMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 1, 2009
 * 3:46:19 AM
 */
@ComponentConfig(
    template = "classpath:groovy/ecm/webui/UIConfirmMessage.gtmpl",
    events = {
        @EventConfig(listeners = UIUploadBehaviorWithSameName.ReplaceDataActionListener.class),
        @EventConfig(listeners = UIUploadBehaviorWithSameName.BackActionListener.class),
        @EventConfig(listeners = UIUploadBehaviorWithSameName.KeepfileActionListener.class)
    }
)
public class UIUploadBehaviorWithSameName extends UIConfirmMessage {

  public UIUploadBehaviorWithSameName() throws Exception {
  }

  public String[] getActions() { return new String[] {"ReplaceData", "Back", "Keepfile"}; }

  static  public class ReplaceDataActionListener extends EventListener<UIUploadBehaviorWithSameName> {
    public void execute(Event<UIUploadBehaviorWithSameName> event) throws Exception {
      UIUploadBehaviorWithSameName uiUploadBehavior = event.getSource();
      UIPopupWindow uiPopup = uiUploadBehavior.getParent();
      uiPopup.setShowMask(true);
      UIUploadManager uiUploadManager = uiPopup.getParent();
      UIUploadForm uiForm = uiUploadManager.getChild(UIUploadForm.class);
      uiForm.doUpload(event, false);
      if (uiUploadManager.getChildById(UIUploadManager.SAMENAME_POPUP) != null) {
        uiUploadManager.removeChildById(UIUploadManager.SAMENAME_POPUP);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager);
    }
  }

  static  public class BackActionListener extends EventListener<UIUploadBehaviorWithSameName> {
    public void execute(Event<UIUploadBehaviorWithSameName> event) throws Exception {
      UIUploadBehaviorWithSameName uiUploadBehavior = event.getSource();
      UIPopupWindow uiPopup = uiUploadBehavior.getParent();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup.getParent());
    }
  }

  static  public class KeepfileActionListener extends EventListener<UIUploadBehaviorWithSameName> {
    public void execute(Event<UIUploadBehaviorWithSameName> event) throws Exception {
      UIUploadBehaviorWithSameName uiUploadBehavior = event.getSource();
      UIPopupWindow uiPopup = uiUploadBehavior.getParent();
      uiPopup.setShowMask(true);
      UIUploadManager uiUploadManager = uiPopup.getParent();
      UIUploadForm uiForm = uiUploadManager.getChild(UIUploadForm.class);
      uiForm.doUpload(event, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager);
    }
  }
}

