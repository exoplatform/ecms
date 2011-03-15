package org.exoplatform.services.wcm.extensions.publication.lifecycle;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig;
/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class StatesLifecyclePlugin extends BaseComponentPlugin {
    private LifecyclesConfig lifecyclesConfig;

    public StatesLifecyclePlugin(InitParams params) {
  ObjectParameter param = params.getObjectParam("lifecycles");

  if (param != null) {
      lifecyclesConfig = (LifecyclesConfig) param.getObject();
  }
    }

    public LifecyclesConfig getLifecyclesConfig() {
  return lifecyclesConfig;
    }

    public void setLifecyclesConfig(LifecyclesConfig lifecyclesConfig) {
  this.lifecyclesConfig = lifecyclesConfig;
    }
}
