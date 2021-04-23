/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.control.filter.DownloadDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * The Class EditorsOpenManageComponent creates and displays UINewDocumentForm.
 */
@ComponentConfig(events = { @EventConfig(listeners = EditorsOpenManageComponent.EditorsOpenActionListener.class) })
public class EditorsOpenManageComponent extends UIAbstractManagerComponent {
  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(EditorsOpenManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new IsAvailableDocumentProviderPresentFilter(), new DownloadDocumentFilter() });

  /**
   * The listener interface for receiving onlyofficeOpenAction events. The class
   * that is interested in processing a onlyofficeOpenAction event implements
   * this interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addOnlyofficeOpenActionListener</code> method. When the
   * onlyofficeOpenAction event occurs, that object's appropriate method is
   * invoked.
   */
  public static class EditorsOpenActionListener extends UIActionBarActionListener<EditorsOpenManageComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<EditorsOpenManageComponent> event) throws Exception {
      // This code will not be invoked
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
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
