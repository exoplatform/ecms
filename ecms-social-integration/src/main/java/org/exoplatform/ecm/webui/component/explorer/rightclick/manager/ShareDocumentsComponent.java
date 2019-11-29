/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIShareDocuments;
import org.exoplatform.services.wcm.core.NodeLocation;
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
 *          exo@exoplatform.com
 * Nov 17, 2014  
 */

@ComponentConfig(
                 events = {
                     @EventConfig(listeners = ShareDocumentsComponent.ShareDocumentsActionListener.class)
                 }
                 )
public class ShareDocumentsComponent extends UIAbstractManagerComponent{
  public static class ShareDocumentsActionListener extends UIWorkingAreaActionListener<ShareDocumentsComponent>{

    @Override
    protected void processEvent(Event<ShareDocumentsComponent> event) throws Exception {
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      if(nodePath == null){
        //rise from UIActionBar
        nodePath = uiExplorer.getCurrentNode().getPath();
      }else{
        //rise from ContextMenu
        nodePath = nodePath.split(":")[1];
      }
      
      UIPopupContainer objUIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      UIShareDocuments uiShareDocuments = uiExplorer.createUIComponent(UIShareDocuments.class, null, null);
      NodeLocation location = new NodeLocation(uiExplorer.getRepositoryName(),uiExplorer.getWorkspaceName(),nodePath);      
      uiShareDocuments.setSelectedNode(location);
      uiShareDocuments.init();
      objUIPopupContainer.activate(uiShareDocuments, 520, 0);
      event.getRequestContext().addUIComponentToUpdateByAjax(objUIPopupContainer);
    }
  }

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
          new IsDocumentFilter()
  });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
      return FILTERS;
  }
  
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
