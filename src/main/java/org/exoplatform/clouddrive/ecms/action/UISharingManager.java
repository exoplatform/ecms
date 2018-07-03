
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
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UISharingManager.java 00000 Jul 9, 2015 pnedonosko $
 */
@Deprecated // TODO not required
@ComponentConfig(template = "classpath:groovy/wcm/webui/core/UIPermissionManager.gtmpl", events = {
    @EventConfig(listeners = UISharingManager.CloseActionListener.class) })
public class UISharingManager extends UIPermissionManagerBase {

  /**
   * The listener interface for receiving closeAction events. The class that is
   * interested in processing a closeAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addCloseActionListener</code> method. When the
   * closeAction event occurs, that object's appropriate method is invoked.
   */
  public static class CloseActionListener extends EventListener<UISharingManager> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<UISharingManager> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  /**
   * Instantiates a new UI sharing manager.
   *
   * @throws Exception the exception
   */
  public UISharingManager() throws Exception {
    addChild(UIPermissionInfo.class, null, null);
    addChild(UIPermissionForm.class, null, null);
  }
}
