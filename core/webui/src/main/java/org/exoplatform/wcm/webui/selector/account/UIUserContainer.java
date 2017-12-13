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
package org.exoplatform.wcm.webui.selector.account;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 1, 2009
 */
@ComponentConfig (
  lifecycle = UIContainerLifecycle.class,
  events = {
    @EventConfig(listeners = UIUserContainer.AddUserActionListener.class)
  }
)
public class UIUserContainer extends UIContainer {

  private UISelectable selectable;

  private String sourceComponent;

  public UISelectable getSelectable() {
    return selectable;
  }

  public void setSelectable(UISelectable selectable) {
    this.selectable = selectable;
  }

  public String getSourceComponent() {
    return sourceComponent;
  }

  public void setSourceComponent(String sourceComponent) {
    this.sourceComponent = sourceComponent;
  }

  public UIUserContainer() throws Exception {
    UIUserSelector userSelector = addChild(UIUserSelector.class, null, null);
    userSelector.setMulti(false);
    userSelector.setShowSearchUser(true);
    userSelector.setShowSearch(true);
  }

 public static class AddUserActionListener extends EventListener<UIUserContainer> {
   public void execute(Event<UIUserContainer> event) throws Exception {
     UIUserContainer userContainer = event.getSource();
     UIUserSelector userSelector = userContainer.getChild(UIUserSelector.class);
     userContainer.getSelectable().doSelect(userContainer.getSourceComponent(), userSelector.getSelectedUsers());
   }
 }
}
