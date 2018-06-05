/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * The Class ConnectCloudDriveForm.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/CloudDriveConnectDialog.gtmpl", events = {
    @EventConfig(listeners = ConnectCloudDriveForm.ConnectActionListener.class),
    @EventConfig(listeners = ConnectCloudDriveForm.CancelActionListener.class, phase = Phase.DECODE) })
public class ConnectCloudDriveForm extends BaseCloudDriveForm {

  /**
   * The listener interface for receiving cancelAction events. The class that is
   * interested in processing a cancelAction event implements this interface,
   * and the object created with that class is registered with a component using
   * the component's <code>addCancelActionListener</code> method. When the
   * cancelAction event occurs, that object's appropriate method is invoked.
   */
  public static class CancelActionListener extends EventListener<ConnectCloudDriveForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<ConnectCloudDriveForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  /**
   * The listener interface for receiving connectAction events. The class that
   * is interested in processing a connectAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addConnectActionListener</code>
   * method. When the connectAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ConnectActionListener extends EventListener<ConnectCloudDriveForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<ConnectCloudDriveForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.updateAjax(event);
    }
  }
}
