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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Nov 8, 2006
 * 9:39:58 AM
 */
@ComponentConfig(template = "system:/groovy/webui/core/UITabPane.gtmpl")
public class UIActionManager extends UIContainer implements UIPopupComponent {

  public UIActionManager() throws Exception {
    addChild(UIActionListContainer.class, null, null);
    addChild(UIActionContainer.class, null, null).setRendered(false);
  }

  public void activate() throws Exception {
    UIActionTypeForm uiActionTypeForm = findFirstComponentOfType(UIActionTypeForm.class);
    uiActionTypeForm.update();
    UIActionList uiActionList = findFirstComponentOfType(UIActionList.class);
    uiActionList.updateGrid(getAncestorOfType(UIJCRExplorer.class).getCurrentNode(),
                            uiActionList.getChild(UIPageIterator.class).getCurrentPage());
  }

  /**
   * Remove lock if node is locked for editing
   */
  public void deActivate() throws Exception {
    UIActionForm uiForm = findFirstComponentOfType(UIActionForm.class);
    if (uiForm != null) {
      uiForm.releaseLock();
    }
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    UIPopupWindow uiPopup = getAncestorOfType(UIPopupWindow.class);
    if (uiPopup != null && !uiPopup.isShow()) {
      uiPopup.setShowMask(true);
      deActivate();
    }
    super.processRender(context);
  }

  public void setDefaultConfig() throws Exception {
    UIActionContainer uiActionContainer = getChild(UIActionContainer.class);
    UIActionTypeForm uiActionType = uiActionContainer.getChild(UIActionTypeForm.class);
    uiActionType.setDefaultActionType();
    Class[] renderClasses = { UIActionTypeForm.class, UIActionForm.class };
    uiActionContainer.setRenderedChildrenOfTypes(renderClasses);
  }

}
