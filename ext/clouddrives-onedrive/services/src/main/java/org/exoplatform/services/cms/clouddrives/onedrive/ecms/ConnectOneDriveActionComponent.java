package org.exoplatform.services.cms.clouddrives.onedrive.ecms;

import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.cms.clouddrives.webui.BaseConnectActionComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

@ComponentConfig(events = { @EventConfig(listeners = ConnectOneDriveActionComponent.ConnectOneDriveActionListener.class) })
public class ConnectOneDriveActionComponent extends BaseConnectActionComponent {

  /**
   * OneDrive  id from configuration - onedrive.
   */
  protected static final String PROVIDER_ID = "onedrive";


  public static class ConnectOneDriveActionListener extends UIActionBarActionListener<ConnectOneDriveActionComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<ConnectOneDriveActionComponent> event) throws Exception {
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getProviderId() {
    return PROVIDER_ID;
  }
}
