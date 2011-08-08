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
package org.exoplatform.ecm.webui.component.admin.taxonomy.action;

import org.exoplatform.ecm.webui.component.admin.taxonomy.UITaxonomyManagerTrees;
import org.exoplatform.ecm.webui.component.admin.taxonomy.UITaxonomyTreeContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 5, 2009
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)

public class UIActionTaxonomyManager extends UIContainer {

  public UIActionTaxonomyManager() throws Exception {
    UIActionTypeForm uiActionTypeForm = getChild(UIActionTypeForm.class);
    if (uiActionTypeForm == null) {
      uiActionTypeForm = addChild(UIActionTypeForm.class, null, null);
    }
    addChild(UIActionForm.class, null, null);
  }

  public void setDefaultConfig() throws Exception {
    UIActionTypeForm uiActionType = getChild(UIActionTypeForm.class);
    uiActionType.setDefaultActionType(null);
    Class[] renderClasses = { UIActionTypeForm.class, UIActionForm.class };
    setRenderedChildrenOfTypes(renderClasses);
  }
  
  public void initPopupComponent(UIComponent uiComp, String id) throws Exception {
    UITaxonomyManagerTrees uiManagerTrees = getAncestorOfType(UITaxonomyManagerTrees.class);
    if (uiManagerTrees != null) {
      uiManagerTrees.removeChildById(UITaxonomyTreeContainer.POPUP_PERMISSION);
      uiManagerTrees.removeChildById(UITaxonomyTreeContainer.POPUP_TAXONOMYHOMEPATH);
    }

    removeChildById(UIActionForm.POPUP_COMPONENT);
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, id);
    uiPopup.setShowMask(true);
    uiPopup.setUIComponent(uiComp);
    uiPopup.setWindowSize(640, 300);
    uiPopup.setShow(true);
    uiPopup.setResizable(true);
  }
}
