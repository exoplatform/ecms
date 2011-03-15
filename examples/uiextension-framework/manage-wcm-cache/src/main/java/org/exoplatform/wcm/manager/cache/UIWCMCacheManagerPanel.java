package org.exoplatform.wcm.manager.cache;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.ext.manager.UIAbstractManager;

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIWCMCacheManagerPanel extends UIAbstractManager {
  public UIWCMCacheManagerPanel() throws Exception {
    addChild(UIWCMCacheManagerForm.class, null, null);
  }

  public void refresh() throws Exception {
    update();
  }

  public void update() throws Exception {
  }

}
