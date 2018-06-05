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

import java.util.Arrays;
import java.util.List;

import org.exoplatform.clouddrive.ecms.filters.SyncingCloudFileFilter;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * SyncingFile hidden action in working area used by Cloud Drive Javascript to
 * mark synchronzing files in UI. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RefreshCloudDriveManagerComponent.java 00000 Dec 15, 2014
 *          pnedonosko $
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = SyncingFileManagerComponent.SyncingFileActionListener.class) })
public class SyncingFileManagerComponent extends BaseCloudDriveManagerComponent {

  /** The Constant LOG. */
  protected static final Log                     LOG        = ExoLogger.getLogger(SyncingFileManagerComponent.class);

  /** The Constant EVENT_NAME. */
  public static final String                     EVENT_NAME = "SyncingFile";

  /** The Constant FILTERS. */
  protected static final List<UIExtensionFilter> FILTERS    =
                                                         Arrays.asList(new UIExtensionFilter[] { new SyncingCloudFileFilter() });

  /**
   * The listener interface for receiving syncingFileAction events. The class
   * that is interested in processing a syncingFileAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addSyncingFileActionListener</code>
   * method. When the syncingFileAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SyncingFileActionListener extends EventListener<SyncingFileManagerComponent> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<SyncingFileManagerComponent> event) throws Exception {
      // code adopted from UIAddressBar.RefreshSessionActionListener.execute()
      // -- effect of refresh here,
      // it should be never invoked (menu action invisible)
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

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    if (EVENT_NAME.equals(name)) {
      initContext();
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }
}
