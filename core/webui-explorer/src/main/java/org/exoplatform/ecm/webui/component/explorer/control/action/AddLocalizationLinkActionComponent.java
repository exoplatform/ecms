/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.control.action;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotSymlinkFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.symlink.UISymLinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = AddLocalizationLinkActionComponent.AddLocalizationLinkActionListener.class)
     }
 )

public class AddLocalizationLinkActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS =
    Arrays.asList(new UIExtensionFilter[]{new CanAddNodeFilter(),
                                          new IsNotLockedFilter(),
                                          new IsCheckedOutFilter(),
                                          new IsNotSymlinkFilter(),
                                          new IsNotTrashHomeNodeFilter(),
                                          new IsNotInTrashFilter()});

  private static final Log LOG = ExoLogger.getLogger(AddLocalizationLinkActionComponent.class);

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class AddLocalizationLinkActionListener extends UIActionBarActionListener<AddLocalizationLinkActionComponent> {
    public void processEvent(Event<AddLocalizationLinkActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
    UISymLinkManager uiSymLinkManager = event.getSource().createUIComponent(UISymLinkManager.class, null, null);
    uiSymLinkManager.enableLocalizationMode();
    UIPopupContainer.activate(uiSymLinkManager, 600, 155);
    event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
