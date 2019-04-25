package org.exoplatform.clouddrive.onedrive.ecms;

import org.exoplatform.clouddrive.ecms.BaseConnectActionComponent;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

@ComponentConfig(events = { @EventConfig(listeners = ConnectOneDriveActionComponent.ConnectGDriveActionListener.class) })
public class ConnectOneDriveActionComponent extends BaseConnectActionComponent {

  /**
   * Google Drive id from configuration - gdrive.
   */
  protected static final String PROVIDER_ID = "onedrive";

  /**
   * The listener interface for receiving connectGDriveAction events. The class
   * that is interested in processing a connectGDriveAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addConnectGDriveActionListener</code>
   * method. When the connectGDriveAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ConnectGDriveActionListener extends UIActionBarActionListener<ConnectOneDriveActionComponent> {

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
