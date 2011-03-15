/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Nov 8, 2008
 */

@ComponentConfig(lifecycle = UIContainerLifecycle.class)

@Deprecated
public class UIDrivesBrowserContainer extends UIContainer{

 /**
  * Contructor init container for drives browser
  * @throws Exception
  */
 public UIDrivesBrowserContainer() throws Exception {
   addChild(UIRepositoryList.class, null, null);
   addChild(UIDrivesBrowser.class, null, null);
 }
}
