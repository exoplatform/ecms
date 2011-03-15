/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.unlock;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 3, 2008
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class
)

public class UILockHolderContainer extends UIContainer {

  public UILockHolderContainer() throws Exception {
    addChild(UILockHolderList.class, null, null);
    addChild(UIPermissionSelector.class, null, null);
  }
}
