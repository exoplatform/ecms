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
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIAddLanguageContainer;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIMultiLanguageManager;
import org.exoplatform.ecm.webui.component.explorer.upload.UISingleUploadManager;
import org.exoplatform.ecm.webui.utils.Utils;
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
       @EventConfig(listeners = MultiLanguageActionComponent.MultiLanguageActionListener.class)
     }
 )
public class MultiLanguageActionComponent extends UIComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      new CanSetPropertyFilter("UIActionBar.msg.access-denied"), new CanAddNodeFilter(),
      new IsNotLockedFilter(), new IsCheckedOutFilter("UIActionBar.msg.multilang-checkedin"),
      new IsDocumentFilter("UIActionBar.msg.unsupported-multilanguage"),
      new IsNotEditingDocumentFilter()                });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class MultiLanguageActionListener
                                                 extends
                                                 UIActionBarActionListener<MultiLanguageActionComponent> {
    public void processEvent(Event<MultiLanguageActionComponent> event) throws Exception {
      UIActionBar uiActionBar = event.getSource().getAncestorOfType(UIActionBar.class);
      UIJCRExplorer uiExplorer = uiActionBar.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      NodeType nodeType = currentNode.getPrimaryNodeType();
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIPopupContainer.activate(UIMultiLanguageManager.class, null,780, 550);
      UIMultiLanguageManager uiMultiManager =
        UIPopupContainer.findFirstComponentOfType(UIMultiLanguageManager.class);
      UIAddLanguageContainer uiAddContainer = uiMultiManager.getChild(UIAddLanguageContainer.class);
      if(nodeType.getName().equals(Utils.NT_FILE)) {
        String mimeType = uiExplorer.getCurrentNode().getNode(Utils.JCR_CONTENT).
        getProperty(Utils.JCR_MIMETYPE).getString();
        if(mimeType.startsWith("text")) uiAddContainer.setComponentDisplay(nodeType.getName());
        else uiAddContainer.addChild(UISingleUploadManager.class, null, null);
      } else {
        uiAddContainer.setComponentDisplay(nodeType.getName());
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
    }
  }
}
