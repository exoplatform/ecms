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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.HasMetadataTemplatesFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.info.UIViewMetadataContainer;
import org.exoplatform.ecm.webui.component.explorer.popup.info.UIViewMetadataManager;
import org.exoplatform.ecm.webui.component.explorer.popup.info.UIViewMetadataTemplate;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = ViewMetadatasActionComponent.ViewMetadatasActionListener.class)
     }
 )
public class ViewMetadatasActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{new HasMetadataTemplatesFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class ViewMetadatasActionListener extends UIActionBarActionListener<ViewMetadatasActionComponent> {
    public void processEvent(Event<ViewMetadatasActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiJCRExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiJCRExplorer.getCurrentNode() ;
      
      
      Hashtable<String, String> metaDataTemp = WCMCoreUtils.getMetadataTemplates(currentNode);
      UIPopupContainer UIPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(UIViewMetadataManager.class, 700);
      UIViewMetadataManager uiMetadataManager =
        UIPopupContainer.findFirstComponentOfType(UIViewMetadataManager.class);
      UIViewMetadataContainer uiMetadataContainer = uiMetadataManager.getChild(UIViewMetadataContainer.class);
      int i = 0;
      Enumeration<String> enu = metaDataTemp.keys();
      while (enu.hasMoreElements()) {
        String key = (String) enu.nextElement();
        String template = metaDataTemp.get(key);
        if(template != null && template.length() > 0) {
          UIViewMetadataTemplate uiMetaView =
            uiMetadataContainer.createUIComponent(UIViewMetadataTemplate.class, null, key) ;
          uiMetaView.setTemplateType(key) ;
          uiMetadataContainer.addChild(uiMetaView) ;
          if(i != 0) uiMetaView.setRendered(false) ;
          i++ ;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }
}
