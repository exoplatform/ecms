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

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotRootNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionRealNodeListener;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ManageHiddenActionComponent.ManageHiddenActionListener.class)
     }
 )
public class ManageHiddenActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{new IsNotRootNodeFilter(),
      new CanSetPropertyFilter(), new IsNotLockedFilter(), new IsCheckedOutFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ManageHiddenActionListener extends UIActionBarActionRealNodeListener<ManageHiddenActionComponent> {
    public void processEvent(Event<ManageHiddenActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      Node selectedNode = uiExplorer.getRealCurrentNode();
      UIApplication uiApp = uiActionBar.getAncestorOfType(UIApplication.class);
      if(selectedNode.isNodeType(Utils.EXO_HIDDENABLE)) {
        selectedNode.removeMixin(Utils.EXO_HIDDENABLE);
        selectedNode.save();
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.removed-hidden-mixin", null));
        
        uiExplorer.updateAjax(event);
      } else if(selectedNode.canAddMixin(Utils.EXO_HIDDENABLE)){
        selectedNode.addMixin(Utils.EXO_HIDDENABLE);
        selectedNode.save();
        uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.added-hidden-mixin", null));
        
        uiExplorer.updateAjax(event);
      }
    }
  }
}
