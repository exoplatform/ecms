package org.exoplatform.services.wcm.friendly.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
/**
 * Author : benjamin.paillereau@exoplatform.com
 */
public class FriendlyPlugin extends BaseComponentPlugin {
    private FriendlyConfig friendlyConfig;

    private InitParams initParams;

    public FriendlyPlugin(InitParams params) {
      this.initParams = params;
      ObjectParameter param = params.getObjectParam("friendlies.configuration");

    if (param != null) {
      friendlyConfig = (FriendlyConfig) param.getObject();
    }
    }

    public FriendlyConfig getFriendlyConfig() {
  return friendlyConfig;
    }

    public void setFriendlyConfig(FriendlyConfig friendlyConfig) {
  this.friendlyConfig = friendlyConfig;
    }

  public InitParams getInitParams() {
    return initParams;
  }


}
