package org.exoplatform.services.wcm.extensions.publication.context;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig;
/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class ContextPlugin extends BaseComponentPlugin {
    private ContextConfig contextConfig;

    public ContextPlugin(InitParams params) {
  ObjectParameter param = params.getObjectParam("contexts");

  if (param != null) {
      contextConfig = (ContextConfig) param.getObject();
  }
    }

    public ContextConfig getContextConfig() {
  return contextConfig;
    }

    public void setContextConfig(ContextConfig contextConfig) {
  this.contextConfig = contextConfig;
    }

}
