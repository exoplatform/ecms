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
package org.exoplatform.wcm.webui.core;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 29, 2009
 */
@ComponentConfig(
  template =  "classpath:groovy/wcm/webui/core/UIPopupWindow.gtmpl",
  events = @EventConfig(listeners = UIPopupWindow.CloseActionListener.class, name = "ClosePopup")
)
public class UIPopupWindow extends org.exoplatform.webui.core.UIPopupWindow {
  private int top_ = -1;
  private int left_ = -1;

  public int getWindowTop()
  {
     return top_;
  }
  public int getWindowLeft()
  {
     return left_;
  }

  public void setCoordindate(int top, int left) {
    top_ = top;
    left_ = left;
  }
  public static class CloseActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIPopupWindow popupWindow = event.getSource();
      UIPopupContainer popupContainer = popupWindow.getAncestorOfType(UIPopupContainer.class);
      popupContainer.removeChildById(popupWindow.getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }
}
