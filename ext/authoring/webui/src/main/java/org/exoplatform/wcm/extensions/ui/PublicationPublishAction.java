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
package org.exoplatform.wcm.extensions.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          benjamin.paillereau@exoplatform.com
 * 27 june 2010
 */
@ComponentConfig(
    events = {
        @EventConfig(listeners = PublicationPublishAction.PublicationPublishActionListener.class)
    }
)
public class PublicationPublishAction extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(
      new UIExtensionFilter[] {
          new CanAddNodeFilter(), new IsNotLockedFilter(), new IsCheckedOutFilter(), new CanPublishFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
  public static class PublicationPublishActionListener extends UIActionBarActionListener<PublicationPublishAction> {

    @Override
    protected void processEvent(Event<PublicationPublishAction> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      PublicationService publicationService = (PublicationService)PortalContainer.getInstance().
          getComponentInstanceOfType(PublicationService.class);
      Node node = uiExplorer.getCurrentNode();
      if(node.isLocked()) {
        node.getSession().addLockToken(LockUtil.getLockToken(node));
      }
      HashMap<String,String> context = new HashMap<String,String>();

      publicationService.changeState(node, "published", context);

      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);

    }
  }
}
