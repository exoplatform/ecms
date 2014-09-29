/*
 * Copyright (C) 2012 eXo Platform SAS.
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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/templates/CloudDriveConnectDialog.gtmpl", events = {
                     @EventConfig(listeners = ConnectCloudDriveForm.ConnectActionListener.class),
                     @EventConfig(listeners = ConnectCloudDriveForm.CancelActionListener.class,
                                  phase = Phase.DECODE) })
public class ConnectCloudDriveForm extends UIForm implements UIPopupComponent {

  protected static final Log LOG = ExoLogger.getLogger(ConnectCloudDriveForm.class);

  public static class CancelActionListener extends EventListener<ConnectCloudDriveForm> {
    public void execute(Event<ConnectCloudDriveForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public static class ConnectActionListener extends EventListener<ConnectCloudDriveForm> {
    public void execute(Event<ConnectCloudDriveForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.updateAjax(event);
    }
  }

  public ConnectCloudDriveForm() {
  }

  @Override
  public void activate() {
    // nothing

  }

  @Override
  public void deActivate() {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      String nodePath = uiExplorer.getCurrentNode().getPath();
      String workspace = uiExplorer.getCurrentNode().getSession().getWorkspace().getName();
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, nodePath);
    } else {
      LOG.error("Cannot find ancestor of type UIJCRExplorer in form " + this);
    }

    return super.renderEventURL(ajax, name, beanId, params);
  }

}
