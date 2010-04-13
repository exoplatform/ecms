/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.sidebar ;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          nguyenkequanghung@yahoo.com
 * oct 5, 2006
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/sidebar/UISideBar.gtmpl",
    events = {
        @EventConfig(listeners = UISideBar.CloseActionListener.class),
        @EventConfig(listeners = UISideBar.ExplorerActionListener.class),
        @EventConfig(listeners = UISideBar.RelationActionListener.class),
        @EventConfig(listeners = UISideBar.TagExplorerActionListener.class),
        @EventConfig(listeners = UISideBar.ClipboardActionListener.class),
        @EventConfig(listeners = UISideBar.SavedSearchesActionListener.class)
    }
)
public class UISideBar extends UIContainer {
  private String currentComp;
  
  public UISideBar() throws Exception {
    addChild(UITreeExplorer.class, null, null).setRendered(false);
    addChild(UIViewRelationList.class, null, null).setRendered(false);
    addChild(UITagExplorer.class, null, null).setRendered(false);
    addChild(UIClipboard.class, null, null).setRendered(false);
    addChild(UISavedSearches.class, null, null).setRendered(false);
    addChild(UIAllItems.class, null, null);
    addChild(UIAllItemsByType.class, null, null);
  }
  
  public String getCurrentComp() { 
    if(currentComp == null || currentComp.length() == 0) 
      currentComp = getChild(UITreeExplorer.class).getId();
    return currentComp; 
  }
  
  public void setCurrentComp(String currentComp) { this.currentComp = currentComp; }
  
  public void renderSideBarChild(String[] arrId) throws Exception {
    for(String id : arrId) {
      setRenderedChild(id);
      renderChild(id);
    }
  }
  
  public boolean isSystemWorkspace() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).isSystemWorkspace();
  }
  
  public String getRepository() { 
    return getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
  }   
  
  static public class CloseActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UIWorkingArea uiWorkingArea = event.getSource().getParent();
      uiWorkingArea.setShowSideBar(false);
      UIJCRExplorerPortlet explorerPorltet = uiWorkingArea.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = explorerPorltet.findFirstComponentOfType(UIJCRExplorer.class);
      UIJcrExplorerContainer uiJcrExplorerContainer= explorerPorltet.getChild(UIJcrExplorerContainer.class);
      uiExplorer.refreshExplorer();      
      uiJcrExplorerContainer.setRenderedChild(UIJCRExplorer.class);      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);      
    }
  }

  static public class ExplorerActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UISideBar uiSideBar = event.getSource();
      UIJCRExplorer uiExplorer = uiSideBar.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.setSelectNode(uiExplorer.getCurrentPath());
      uiExplorer.setIsViewTag(false);
      uiSideBar.setCurrentComp(uiSideBar.getChild(UITreeExplorer.class).getId());
      uiExplorer.updateAjax(event);
    }
  }

  static public class RelationActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UISideBar uiSideBar = event.getSource();
      uiSideBar.setCurrentComp(uiSideBar.getChild(UIViewRelationList.class).getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
    }
  }
  
  static public class TagExplorerActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UISideBar uiSideBar = event.getSource();
      UIJCRExplorer uiExplorer = uiSideBar.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.setCurrentState();
      uiSideBar.setCurrentComp(uiSideBar.getChild(UITagExplorer.class).getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
    }
  }
  
  static public class ClipboardActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UISideBar uiSideBar = event.getSource();
      uiSideBar.setCurrentComp(uiSideBar.getChild(UIClipboard.class).getId());      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
    }
  }
  
  static public class SavedSearchesActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UISideBar uiSideBar = event.getSource();
      uiSideBar.setCurrentComp(uiSideBar.getChild(UISavedSearches.class).getId());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar.getParent());
    }
  }    
}
