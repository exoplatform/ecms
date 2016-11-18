
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

import org.exoplatform.clouddrive.ecms.BaseCloudDriveManagerComponent;
import org.exoplatform.clouddrive.ecms.CloudDriveUIMenuAction;
import org.exoplatform.clouddrive.ecms.action.ViewSharingActionComponent.ViewSharingActionListener;
import org.exoplatform.clouddrive.ecms.filters.CloudFileFilter;
import org.exoplatform.clouddrive.ecms.filters.HasEditPermissionsFilter;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

/**
 * Show sharing options (including permissions) of cloud file.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ViewSharingActionComponent.java 00000 Jul 9, 2015 pnedonosko $
 * 
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class,
                 events = { @EventConfig(listeners = ViewSharingActionListener.class) })
public class ViewSharingActionComponent extends BaseCloudDriveManagerComponent implements CloudDriveUIMenuAction {

  /** The Constant FILTERS. */
  protected static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new CloudFileFilter(),
      new HasEditPermissionsFilter() });

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
   * The listener interface for receiving viewSharingAction events.
   * The class that is interested in processing a viewSharingAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addViewSharingActionListener</code> method. When
   * the viewSharingAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ViewSharingActionListener extends UIActionBarActionListener<ViewSharingActionComponent> {
    
    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<ViewSharingActionComponent> event) throws Exception {
      UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer uiPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UISharingManager uiPerMan = uiPopupContainer.activate(UISharingManager.class, 700);

      uiPerMan.checkPermissonInfo(uiJCRExplorer.getCurrentNode());

      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
}
