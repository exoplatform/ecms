/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.core;

import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.account.UIUserSelector;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 3, 2008
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    events = {@EventConfig(listeners = UIUserContainer.AddUserActionListener.class)}
)

public class UIUserContainer extends UIContainer implements UIPopupComponent  {

  public UIUserContainer() throws Exception {
    UIUserSelector uiUserSelector = getChild(UIUserSelector.class);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, null);
    }
    uiUserSelector.setMulti(false);
    uiUserSelector.setShowSearchUser(true);
    uiUserSelector.setShowSearch(true);
  }

  public void activate() {

  }

  public void deActivate() {

  }

  static  public class AddUserActionListener extends EventListener<UIUserContainer> {
    public void execute(Event<UIUserContainer> event) throws Exception {
      UIUserContainer uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChild(UIUserSelector.class);
      UIPermissionManagerBase uiParent = uiUserContainer.getAncestorOfType(UIPermissionManagerBase.class);
      UIPermissionFormBase uiPermissionForm = uiParent.getChild(UIPermissionFormBase.class);
      uiPermissionForm.doSelect(UIPermissionInputSet.FIELD_USERORGROUP, uiUserSelector.getSelectedUsers());
      UIPopupWindow uiPopup = uiParent.findComponentById("PopupUserSelector");
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);
    }
  }
}
