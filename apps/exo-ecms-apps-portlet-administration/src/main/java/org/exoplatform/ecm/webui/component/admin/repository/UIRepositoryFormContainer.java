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
package org.exoplatform.ecm.webui.component.admin.repository;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Jun 7, 2007  
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)

public class UIRepositoryFormContainer extends UIContainer implements UIPopupComponent {

  public UIRepositoryFormContainer() throws Exception {
    addChild(UIRepositoryForm.class, null, null) ;
    UIPopupContainer uiPopupAction = addChild(UIPopupContainer.class, null, "UIPopupControl");
    uiPopupAction.getChild(UIPopupWindow.class).setId("UIPopupWindowControl") ;
  }

  protected void refresh(boolean isAddnew, RepositoryEntry re) throws Exception {
    getChild(UIRepositoryForm.class).isAddnew_ = isAddnew ;
    getChild(UIRepositoryForm.class).refresh(re) ;
    getChild(UIRepositoryForm.class).lockForm(!isAddnew) ;
  }

  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }

  public void deActivate() throws Exception {
    // TODO Auto-generated method stub

  }
}
