/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.unlock;

import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
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

public class UIUserContainer extends UIContainer {

  public UIUserContainer() throws Exception {
    UIUserSelector uiUserSelector = getChild(UIUserSelector.class);
    if (uiUserSelector == null) {
      uiUserSelector = addChild(UIUserSelector.class, null, null);
    }
    uiUserSelector.setMulti(false);
    uiUserSelector.setShowSearchUser(true);
    uiUserSelector.setShowSearch(true);
  }

  static  public class AddUserActionListener extends EventListener<UIUserContainer> {
    public void execute(Event<UIUserContainer> event) throws Exception {
      UIUserContainer uiUserContainer = event.getSource();
      UIUserSelector uiUserSelector = uiUserContainer.getChild(UIUserSelector.class);
      LockService lockService = uiUserContainer.getApplicationComponent(LockService.class);
      lockService.addGroupsOrUsersForLock(uiUserSelector.getSelectedUsers());
      UIUnLockManager uiUnLockManager = uiUserContainer.getParent();
      uiUnLockManager.refresh();
    }
  }
}
