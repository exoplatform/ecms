package org.exoplatform.wcm.component.cache;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIWCMCachePanel extends UIAbstractManager {
  public UIWCMCachePanel() throws Exception {
    addChild(UIWCMCacheForm.class, null, null);

  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
    getChild(UIWCMCacheForm.class).update();
  }

}
