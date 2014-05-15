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
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
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

@Deprecated // not used
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/templates/RefreshViewForm.gtmpl",
                 events = { @EventConfig(listeners = RefreshViewForm.RefreshViewActionListener.class,
                                         phase = Phase.DECODE) })
public class RefreshViewForm extends UIForm {

  protected static final Log LOG = ExoLogger.getLogger(RefreshViewForm.class);

  public static class RefreshViewActionListener extends EventListener<RefreshViewForm> {
    public void execute(Event<RefreshViewForm> event) throws Exception {
      // code adopted from UIAddressBar.RefreshSessionActionListener.execute()
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      explorer.getSession().refresh(false);
      explorer.refreshExplorer();
      UIWorkingArea workingArea = explorer.getChild(UIWorkingArea.class);
      UIActionBar actionBar = workingArea.getChild(UIActionBar.class);
      UIControl control = explorer.getChild(UIControl.class);
      if (control != null) {
        UIAddressBar addressBar = control.getChild(UIAddressBar.class);
        if (addressBar != null) {
          actionBar.setTabOptions(addressBar.getSelectedViewName());
        }
      }
    }
  }

  public RefreshViewForm() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() throws Exception {
    // TODO Auto-generated method stub
    super.begin();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processDecode(WebuiRequestContext context) throws Exception {
    // TODO Auto-generated method stub
    super.processDecode(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processAction(WebuiRequestContext context) throws Exception {
    // TODO Auto-generated method stub
    super.processAction(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    // TODO Auto-generated method stub
    return super.renderEventURL(ajax, name, beanId, params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "Refresh";
  }
}
